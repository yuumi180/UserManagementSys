package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.AiChatRequest;
import com.example.demo.service.AiAnalysisService;
import com.example.demo.service.AiChatModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private AiAnalysisService aiAnalysisService;

    @GetMapping("/dashboard-analysis")
    public Result<?> dashboardAnalysis() {
        return Result.success(aiAnalysisService.analyzeDashboard());
    }

    @GetMapping("/log-risk-analysis")
    public Result<?> logRiskAnalysis() {
        return Result.success(aiAnalysisService.analyzeLogs());
    }

    @PostMapping("/chat")
    public Result<?> chat(@RequestBody AiChatRequest request) {
        return Result.success(aiAnalysisService.chat(request));
    }

    @PostMapping(value = "/chat-stream", produces = "application/x-ndjson;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> chatStream(@RequestBody AiChatRequest request) {
        StreamingResponseBody body = outputStream -> {
            try {
                AiAnalysisService.ChatPrepared prepared = aiAnalysisService.prepareChat(request);
                writeEvent(outputStream, "meta", prepared.toMeta());

                AiChatModelService.ModelReply modelReply = aiAnalysisService.streamChat(request, prepared, chunk -> {
                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("type", "chunk");
                    event.put("content", chunk);
                    writeEventUnchecked(outputStream, event);
                });

                Map<String, Object> done = new LinkedHashMap<>();
                done.put("type", "done");
                done.put("model", modelReply.toMap());
                writeEvent(outputStream, done);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            } catch (Exception e) {
                writeError(outputStream, e);
            }
        };
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    private void writeError(OutputStream outputStream, Exception e) throws IOException {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "error");
        event.put("message", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        writeEvent(outputStream, event);
    }

    private void writeEvent(OutputStream outputStream, String type, Map<String, Object> data) throws IOException {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("data", data);
        writeEvent(outputStream, event);
    }

    private void writeEventUnchecked(OutputStream outputStream, Map<String, Object> event) {
        try {
            writeEvent(outputStream, event);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeEvent(OutputStream outputStream, Map<String, Object> event) throws IOException {
        outputStream.write(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
        outputStream.write('\n');
        outputStream.flush();
    }
}
