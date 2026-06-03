package com.example.demo.service;

import com.example.demo.dto.AiChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiChatModelService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.chat.enabled:true}")
    private boolean enabled;

    @Value("${ai.chat.provider:openai-compatible}")
    private String provider;

    @Value("${ai.chat.timeout-ms:30000}")
    private int timeoutMs;

    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.ollama.model:qwen2.5:14b}")
    private String ollamaModel;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    public ModelReply generate(AiChatRequest request, Map<String, Object> context, String fallbackAnswer) {
        EffectiveModelConfig config = resolveConfig(request);
        String reason = validateRequest(request, config);
        if (reason != null) {
            return ModelReply.fallback(config.provider, config.model, fallbackAnswer, reason);
        }

        try {
            if ("ollama".equals(config.provider)) {
                return callOllama(request, context, config);
            }
            if ("openai-compatible".equals(config.provider)) {
                return callOpenAiCompatible(request, context, config);
            }
            return ModelReply.fallback(config.provider, config.model, fallbackAnswer, "Unsupported provider");
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ModelReply.fallback(config.provider, config.model, fallbackAnswer, cleanReason(e));
        }
    }

    public ModelReply streamGenerate(AiChatRequest request,
                                     Map<String, Object> context,
                                     String fallbackAnswer,
                                     ChunkConsumer consumer) {
        EffectiveModelConfig config = resolveConfig(request);
        String reason = validateRequest(request, config);
        if (reason != null) {
            streamFallback(fallbackAnswer, consumer);
            return ModelReply.fallback(config.provider, config.model, fallbackAnswer, reason);
        }

        try {
            if ("ollama".equals(config.provider)) {
                return streamOllama(request, context, config, consumer);
            }
            if ("openai-compatible".equals(config.provider)) {
                return streamOpenAiCompatible(request, context, config, consumer);
            }
            streamFallback(fallbackAnswer, consumer);
            return ModelReply.fallback(config.provider, config.model, fallbackAnswer, "Unsupported provider");
        } catch (UncheckedIOException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            streamFallback(fallbackAnswer, consumer);
            return ModelReply.fallback(config.provider, config.model, fallbackAnswer, cleanReason(e));
        }
    }

    private String validateRequest(AiChatRequest request, EffectiveModelConfig config) {
        if (!enabled) {
            return "AI model is disabled";
        }
        if (request == null || !hasText(request.getMessage())) {
            return "Empty message";
        }
        if (!"ollama".equals(config.provider) && !"openai-compatible".equals(config.provider)) {
            return "Unsupported provider";
        }
        if ("openai-compatible".equals(config.provider) && !hasText(config.apiKey)) {
            return "OpenAI-compatible api key is empty";
        }
        if (!hasText(config.baseUrl)) {
            return "Model base URL is empty";
        }
        if (!hasText(config.model)) {
            return "Model name is empty";
        }
        return null;
    }

    private ModelReply callOllama(AiChatRequest request,
                                  Map<String, Object> context,
                                  EffectiveModelConfig config) throws IOException, InterruptedException {
        Map<String, Object> payload = buildOllamaPayload(request, context, config.model, false);
        JsonNode response = postJson(trimTrailingSlash(config.baseUrl) + "/api/chat", payload, null);
        String answer = response.path("message").path("content").asText("");
        if (!hasText(answer)) {
            throw new IllegalStateException("Ollama returned an empty response");
        }
        return ModelReply.success("ollama", config.model, answer.trim());
    }

    private ModelReply callOpenAiCompatible(AiChatRequest request,
                                            Map<String, Object> context,
                                            EffectiveModelConfig config) throws IOException, InterruptedException {
        Map<String, Object> payload = buildOpenAiPayload(request, context, config.model, false);
        JsonNode response = postJson(trimTrailingSlash(config.baseUrl) + "/chat/completions", payload, authHeaders(config));
        String answer = response.path("choices").path(0).path("message").path("content").asText("");
        if (!hasText(answer)) {
            throw new IllegalStateException("OpenAI-compatible provider returned an empty response");
        }
        return ModelReply.success(config.provider, config.model, answer.trim());
    }

    private ModelReply streamOllama(AiChatRequest request,
                                    Map<String, Object> context,
                                    EffectiveModelConfig config,
                                    ChunkConsumer consumer) throws IOException, InterruptedException {
        Map<String, Object> payload = buildOllamaPayload(request, context, config.model, true);
        try (BufferedReader reader = postStream(trimTrailingSlash(config.baseUrl) + "/api/chat", payload, null)) {
            StringBuilder answer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!hasText(line)) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                String chunk = node.path("message").path("content").asText("");
                if (hasText(chunk)) {
                    answer.append(chunk);
                    consumer.accept(chunk);
                }
                if (node.path("done").asBoolean(false)) {
                    break;
                }
            }
            if (answer.length() == 0) {
                throw new IllegalStateException("Ollama returned an empty stream");
            }
            return ModelReply.success("ollama", config.model, answer.toString());
        }
    }

    private ModelReply streamOpenAiCompatible(AiChatRequest request,
                                              Map<String, Object> context,
                                              EffectiveModelConfig config,
                                              ChunkConsumer consumer) throws IOException, InterruptedException {
        Map<String, Object> payload = buildOpenAiPayload(request, context, config.model, true);
        try (BufferedReader reader = postStream(trimTrailingSlash(config.baseUrl) + "/chat/completions", payload, authHeaders(config))) {
            StringBuilder answer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                if (!hasText(data)) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(data);
                String chunk = node.path("choices").path(0).path("delta").path("content").asText("");
                if (hasText(chunk)) {
                    answer.append(chunk);
                    consumer.accept(chunk);
                }
            }
            if (answer.length() == 0) {
                throw new IllegalStateException("OpenAI-compatible provider returned an empty stream");
            }
            return ModelReply.success(config.provider, config.model, answer.toString());
        }
    }

    private Map<String, Object> buildOpenAiPayload(AiChatRequest request,
                                                   Map<String, Object> context,
                                                   String model,
                                                   boolean stream) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", buildMessages(request, context));
        payload.put("temperature", 0.2);
        payload.put("stream", stream);
        return payload;
    }

    private Map<String, Object> buildOllamaPayload(AiChatRequest request,
                                                   Map<String, Object> context,
                                                   String model,
                                                   boolean stream) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", buildMessages(request, context));
        payload.put("stream", stream);

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0.2);
        payload.put("options", options);
        return payload;
    }

    private List<Map<String, String>> buildMessages(AiChatRequest request,
                                                    Map<String, Object> context) throws IOException {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", buildSystemPrompt()));

        if (request.getHistory() != null) {
            int skipCount = Math.max(0, request.getHistory().size() - 6);
            request.getHistory().stream()
                    .filter(item -> item != null && hasText(item.getContent()))
                    .skip(skipCount)
                    .forEach(item -> {
                        String role = "assistant".equals(item.getRole()) ? "assistant" : "user";
                        messages.add(message(role, limit(item.getContent(), 600)));
                    });
        }

        String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        messages.add(message("user",
                "用户问题：\n" + request.getMessage().trim()
                        + "\n\n业务数据上下文 JSON：\n" + limit(contextJson, 8000)
                        + "\n\n请用中文回答，先给结论，再给关键依据和下一步建议。"));
        return messages;
    }

    private String buildSystemPrompt() {
        return "你是用户管理系统的数据分析助手。"
                + "你只能基于后端提供的业务数据上下文回答，不能编造不存在的数据。"
                + "不要输出密码、令牌、密钥等敏感信息。"
                + "你只能做查询、解释、分析和建议，不能声称已经执行新增、修改、删除等操作。"
                + "如果上下文不足以回答，要明确说明缺少哪些数据。";
    }

    private JsonNode postJson(String url,
                              Map<String, Object> payload,
                              Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient().send(
                buildRequest(url, payload, extraHeaders),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Model provider returned HTTP " + response.statusCode() + ": " + limit(response.body(), 300));
        }
        return objectMapper.readTree(response.body());
    }

    private BufferedReader postStream(String url,
                                      Map<String, Object> payload,
                                      Map<String, String> extraHeaders) throws IOException, InterruptedException {
        HttpResponse<InputStream> response = httpClient().send(
                buildRequest(url, payload, extraHeaders),
                HttpResponse.BodyHandlers.ofInputStream()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IllegalStateException("Model provider returned HTTP " + response.statusCode() + ": " + limit(body, 300));
        }
        return new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
    }

    private HttpRequest buildRequest(String url,
                                     Map<String, Object> payload,
                                     Map<String, String> extraHeaders) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));

        if (extraHeaders != null) {
            extraHeaders.forEach(builder::header);
        }
        return builder.build();
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    private Map<String, String> authHeaders(EffectiveModelConfig config) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + config.apiKey);
        return headers;
    }

    private void streamFallback(String fallbackAnswer, ChunkConsumer consumer) {
        if (!hasText(fallbackAnswer)) {
            return;
        }
        int step = 18;
        for (int i = 0; i < fallbackAnswer.length(); i += step) {
            consumer.accept(fallbackAnswer.substring(i, Math.min(i + step, fallbackAnswer.length())));
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private EffectiveModelConfig resolveConfig(AiChatRequest request) {
        AiChatRequest.ModelConfig userConfig = request == null ? null : request.getModelConfig();
        String effectiveProvider = normalizeProvider(firstText(userConfig == null ? null : userConfig.getProvider(), provider, "openai-compatible"));
        String effectiveBaseUrl;
        String effectiveApiKey;
        String effectiveModel;

        if ("ollama".equals(effectiveProvider)) {
            effectiveBaseUrl = firstText(userConfig == null ? null : userConfig.getBaseUrl(), ollamaBaseUrl, "http://localhost:11434");
            effectiveApiKey = "";
            effectiveModel = firstText(userConfig == null ? null : userConfig.getModel(), ollamaModel, "qwen2.5:14b");
        } else {
            effectiveBaseUrl = firstText(userConfig == null ? null : userConfig.getBaseUrl(), openAiBaseUrl, "https://api.openai.com/v1");
            effectiveApiKey = firstText(userConfig == null ? null : userConfig.getApiKey(), openAiApiKey, "");
            effectiveModel = firstText(userConfig == null ? null : userConfig.getModel(), openAiModel, "gpt-4o-mini");
        }

        return new EffectiveModelConfig(effectiveProvider, effectiveBaseUrl, effectiveApiKey, effectiveModel);
    }

    private String normalizeProvider(String value) {
        String normalized = hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "openai-compatible";
        if ("openai".equals(normalized)) {
            return "openai-compatible";
        }
        return normalized;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String trimTrailingSlash(String value) {
        if (!hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String cleanReason(Exception e) {
        String message = e.getMessage();
        if (!hasText(message)) {
            return e.getClass().getSimpleName();
        }
        return limit(message, 300);
    }

    private String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...[内容已截断]";
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public interface ChunkConsumer {
        void accept(String chunk);
    }

    private static class EffectiveModelConfig {
        private final String provider;
        private final String baseUrl;
        private final String apiKey;
        private final String model;

        private EffectiveModelConfig(String provider, String baseUrl, String apiKey, String model) {
            this.provider = provider;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
        }
    }

    public static class ModelReply {
        private final String provider;
        private final String model;
        private final String answer;
        private final boolean fallback;
        private final String reason;

        private ModelReply(String provider, String model, String answer, boolean fallback, String reason) {
            this.provider = provider;
            this.model = model;
            this.answer = answer;
            this.fallback = fallback;
            this.reason = reason;
        }

        public static ModelReply success(String provider, String model, String answer) {
            return new ModelReply(provider, model, answer, false, "");
        }

        public static ModelReply fallback(String provider, String model, String answer, String reason) {
            return new ModelReply(provider, model, answer, true, reason);
        }

        public String getAnswer() {
            return answer;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("provider", provider);
            result.put("model", model);
            result.put("fallback", fallback);
            result.put("reason", reason);
            return result;
        }
    }
}
