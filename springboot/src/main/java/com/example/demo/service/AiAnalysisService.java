package com.example.demo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.AiChatRequest;
import com.example.demo.entity.OperationLog;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiAnalysisService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private OperationLogMapper operationLogMapper;

    @Resource
    private AiChatModelService aiChatModelService;

    public Map<String, Object> analyzeDashboard() {
        UserSnapshot snapshot = buildUserSnapshot();

        List<String> anomalies = new ArrayList<>();
        if (snapshot.total == 0) {
            anomalies.add("当前没有用户数据，仪表盘无法形成稳定画像。");
        }
        if (snapshot.missingAge > 0) {
            anomalies.add("有 " + snapshot.missingAge + " 条用户记录缺少有效年龄。");
        }
        if (snapshot.missingAddress > 0) {
            anomalies.add("有 " + snapshot.missingAddress + " 条用户记录缺少地址，城市分布可能失真。");
        }
        if (snapshot.missingNickname > 0) {
            anomalies.add("有 " + snapshot.missingNickname + " 条用户记录缺少昵称。");
        }
        if (snapshot.total > 0 && Math.max(snapshot.male, snapshot.female) * 1.0 / snapshot.total > 0.8) {
            anomalies.add("性别分布明显偏向单侧，建议确认样本来源或字段填写质量。");
        }
        if (anomalies.isEmpty()) {
            anomalies.add("暂未发现明显的数据完整性异常。");
        }

        List<String> suggestions = new ArrayList<>();
        if (!snapshot.topCities.isEmpty()) {
            suggestions.add("优先围绕 " + snapshot.topCities.get(0).get("name") + " 用户群体完善画像和运营策略。");
        }
        if (snapshot.missingAge + snapshot.missingAddress + snapshot.missingNickname > 0) {
            suggestions.add("建议先补齐年龄、地址、昵称等基础字段，再做更细粒度分析。");
        }
        suggestions.add("可按年龄段和城市建立用户分层，后续用于权限、运营或数据看板筛选。");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", buildDashboardSummary(snapshot));
        result.put("totalUsers", snapshot.total);
        result.put("maleUsers", snapshot.male);
        result.put("femaleUsers", snapshot.female);
        result.put("avgAge", Math.round(snapshot.avgAge));
        result.put("ageGroups", snapshot.ageGroups);
        result.put("topCities", snapshot.topCities);
        result.put("anomalies", anomalies);
        result.put("suggestions", suggestions);
        return result;
    }

    public Map<String, Object> analyzeLogs() {
        LogSnapshot snapshot = buildLogSnapshot();

        List<String> risks = new ArrayList<>();
        int score = 0;
        if (snapshot.deleteCount >= 5) {
            score += 40;
            risks.add("最近 100 条日志中删除操作达到 " + snapshot.deleteCount + " 次，建议核对是否存在误删或批量清理。");
        } else if (snapshot.deleteCount > 0) {
            score += 15;
            risks.add("发现 " + snapshot.deleteCount + " 次删除操作，风险较低但仍建议保留审计记录。");
        }
        if (snapshot.failedCount > 0) {
            score += Math.min(30, (int) snapshot.failedCount * 5);
            risks.add("发现 " + snapshot.failedCount + " 条失败或异常结果，需要查看接口返回信息。");
        }
        for (Map.Entry<String, Long> entry : snapshot.urlCount.entrySet()) {
            if (entry.getValue() >= 20) {
                score += 20;
                risks.add("接口 " + entry.getKey() + " 访问频次较高，最近 100 条中出现 " + entry.getValue() + " 次。");
            }
        }
        if (snapshot.logs.isEmpty()) {
            risks.add("暂无操作日志，无法判断风险趋势。");
        }
        if (risks.isEmpty()) {
            risks.add("最近 100 条日志未发现明显高风险行为。");
        }

        String level = score >= 60 ? "高" : score >= 25 ? "中" : "低";
        List<String> suggestions = new ArrayList<>();
        suggestions.add("保留删除、新增、登录等关键操作的审计记录。");
        if (snapshot.deleteCount > 0) {
            suggestions.add("对删除操作增加二次确认或按角色限制。");
        }
        if (snapshot.failedCount > 0) {
            suggestions.add("优先排查失败请求对应的 URL 和参数，避免重复触发异常。");
        }
        if (snapshot.loginCount > 10) {
            suggestions.add("登录操作较频繁，可结合 IP 和账号维度做进一步限流。");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "已分析最近 " + snapshot.logs.size() + " 条操作日志，当前风险等级为 " + level + "。");
        result.put("riskLevel", level);
        result.put("score", Math.min(score, 100));
        result.put("totalLogs", snapshot.logs.size());
        result.put("deleteCount", snapshot.deleteCount);
        result.put("loginCount", snapshot.loginCount);
        result.put("failedCount", snapshot.failedCount);
        result.put("frequentUrls", snapshot.frequentUrls);
        result.put("risks", risks);
        result.put("suggestions", suggestions);
        return result;
    }

    public Map<String, Object> chat(AiChatRequest request) {
        ChatPrepared prepared = prepareChat(request);
        AiChatModelService.ModelReply modelReply = aiChatModelService.generate(
                request,
                prepared.getContext(),
                prepared.getFallbackAnswer()
        );
        return prepared.toResult(modelReply.getAnswer(), modelReply.toMap());
    }

    public ChatPrepared prepareChat(AiChatRequest request) {
        String message = request == null ? "" : safeText(request.getMessage());
        UserSnapshot userSnapshot = buildUserSnapshot();
        RoleSnapshot roleSnapshot = buildRoleSnapshot();
        LogSnapshot logSnapshot = buildLogSnapshot();

        List<String> sources = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        String fallbackAnswer;
        String normalized = message.toLowerCase(Locale.ROOT);

        if (isBlank(message)) {
            fallbackAnswer = "请告诉我你想分析什么数据，例如“用户总数是多少”“哪些城市用户最多”“最近日志有没有风险”。";
            suggestions.add("分析用户结构");
            suggestions.add("查看城市 Top5");
            suggestions.add("总结最近日志风险");
        } else if (containsAny(normalized, "日志", "风险", "异常", "失败", "删除", "操作", "log", "risk")) {
            sources.add("operation_log");
            fallbackAnswer = buildLogChatAnswer(message, logSnapshot);
            suggestions.add("列出高频接口");
            suggestions.add("分析删除操作风险");
            suggestions.add("查看最近失败请求");
        } else if (containsAny(normalized, "角色", "权限", "role", "permission")) {
            sources.add("role");
            fallbackAnswer = buildRoleChatAnswer(message, roleSnapshot);
            suggestions.add("当前有哪些角色");
            suggestions.add("角色数量是否合理");
            suggestions.add("继续查看权限配置");
        } else {
            sources.add("users");
            fallbackAnswer = buildUserChatAnswer(message, userSnapshot);
            suggestions.add("分析年龄分布");
            suggestions.add("查看男女比例");
            suggestions.add("哪些城市用户最多");
        }

        Map<String, Object> metrics = buildMetrics(userSnapshot, roleSnapshot, logSnapshot);
        Map<String, Object> context = buildReadableContext(userSnapshot, roleSnapshot, logSnapshot);
        return new ChatPrepared(fallbackAnswer, sources, suggestions, metrics, context);
    }

    public AiChatModelService.ModelReply streamChat(AiChatRequest request,
                                                    ChatPrepared prepared,
                                                    AiChatModelService.ChunkConsumer consumer) {
        return aiChatModelService.streamGenerate(request, prepared.getContext(), prepared.getFallbackAnswer(), consumer);
    }

    private Map<String, Object> buildMetrics(UserSnapshot userSnapshot,
                                             RoleSnapshot roleSnapshot,
                                             LogSnapshot logSnapshot) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalUsers", userSnapshot.total);
        metrics.put("avgAge", Math.round(userSnapshot.avgAge));
        metrics.put("maleUsers", userSnapshot.male);
        metrics.put("femaleUsers", userSnapshot.female);
        metrics.put("roleCount", roleSnapshot.total);
        metrics.put("recentLogCount", logSnapshot.logs.size());
        metrics.put("riskDeleteCount", logSnapshot.deleteCount);
        metrics.put("riskFailedCount", logSnapshot.failedCount);
        return metrics;
    }

    private String buildUserChatAnswer(String message, UserSnapshot snapshot) {
        String normalized = message.toLowerCase(Locale.ROOT);
        StringBuilder answer = new StringBuilder();
        answer.append("基于当前用户表，系统共有 ").append(snapshot.total).append(" 名用户");
        if (snapshot.total > 0) {
            answer.append("，男用户 ").append(snapshot.male)
                    .append(" 人，女用户 ").append(snapshot.female)
                    .append(" 人，平均年龄约 ").append(Math.round(snapshot.avgAge)).append(" 岁。");
        } else {
            answer.append("。当前没有足够数据做进一步画像。");
        }

        if (containsAny(normalized, "年龄", "age", "分布")) {
            answer.append("\n\n年龄分布：").append(formatMap(snapshot.ageGroups)).append("。");
        }
        if (containsAny(normalized, "城市", "地址", "地区", "top")) {
            answer.append("\n\n城市 Top5：").append(formatRankList(snapshot.topCities, "人")).append("。");
        }
        if (containsAny(normalized, "缺失", "质量", "完整")) {
            answer.append("\n\n数据质量：缺少年龄 ").append(snapshot.missingAge)
                    .append(" 条，缺少地址 ").append(snapshot.missingAddress)
                    .append(" 条，缺少昵称 ").append(snapshot.missingNickname).append(" 条。");
        }
        if (snapshot.total > 0 && snapshot.topCities.isEmpty()) {
            answer.append("\n\n地址字段暂时没有解析出明确城市，建议统一地址格式。");
        }
        return answer.toString();
    }

    private String buildRoleChatAnswer(String message, RoleSnapshot snapshot) {
        if (snapshot.total == 0) {
            return "当前还没有角色数据，建议先配置管理员、运营、审计等基础角色。";
        }
        StringBuilder answer = new StringBuilder();
        answer.append("当前系统共有 ").append(snapshot.total).append(" 个角色：")
                .append(snapshot.roles.stream()
                        .map(role -> nullToDash(role.getName()) + "(" + nullToDash(role.getCode()) + ")")
                        .collect(Collectors.joining("、")))
                .append("。");

        List<String> emptyDescriptions = snapshot.roles.stream()
                .filter(role -> isBlank(role.getDescription()))
                .map(Role::getCode)
                .map(this::nullToDash)
                .collect(Collectors.toList());
        if (!emptyDescriptions.isEmpty()) {
            answer.append("\n\n其中 ").append(String.join("、", emptyDescriptions))
                    .append(" 缺少角色描述，建议补充职责边界。");
        } else {
            answer.append("\n\n角色描述完整，适合作为后续权限审计的基础。");
        }
        return answer.toString();
    }

    private String buildLogChatAnswer(String message, LogSnapshot snapshot) {
        if (snapshot.logs.isEmpty()) {
            return "最近没有操作日志，暂时无法判断风险。建议确认日志切面是否覆盖登录、新增、修改、删除等关键接口。";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("最近 ").append(snapshot.logs.size()).append(" 条操作日志中，登录操作 ")
                .append(snapshot.loginCount).append(" 次，删除操作 ")
                .append(snapshot.deleteCount).append(" 次，失败或异常结果 ")
                .append(snapshot.failedCount).append(" 次。");

        if (snapshot.deleteCount >= 5 || snapshot.failedCount > 0) {
            answer.append("\n\n风险判断：需要关注。建议优先核对失败请求和删除操作的操作者、时间和 URL。");
        } else {
            answer.append("\n\n风险判断：整体较低，未发现明显高频删除或失败请求。");
        }

        if (!snapshot.frequentUrls.isEmpty()) {
            answer.append("\n\n高频接口：").append(formatRankList(snapshot.frequentUrls, "次")).append("。");
        }
        return answer.toString();
    }

    private UserSnapshot buildUserSnapshot() {
        List<User> users = userMapper.selectList(null);
        UserSnapshot snapshot = new UserSnapshot();
        snapshot.users = users;
        snapshot.total = users.size();
        snapshot.male = users.stream().filter(user -> isMale(user.getSex())).count();
        snapshot.female = users.stream().filter(user -> isFemale(user.getSex())).count();
        snapshot.missingAge = users.stream().filter(user -> user.getAge() == null || user.getAge() <= 0).count();
        snapshot.missingAddress = users.stream().filter(user -> isBlank(user.getAddress())).count();
        snapshot.missingNickname = users.stream().filter(user -> isBlank(user.getNickname())).count();
        snapshot.avgAge = users.stream()
                .map(User::getAge)
                .filter(Objects::nonNull)
                .filter(age -> age > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        snapshot.ageGroups.put("18岁以下", users.stream().filter(user -> user.getAge() != null && user.getAge() < 18).count());
        snapshot.ageGroups.put("18-25岁", users.stream().filter(user -> inAgeRange(user, 18, 25)).count());
        snapshot.ageGroups.put("26-35岁", users.stream().filter(user -> inAgeRange(user, 26, 35)).count());
        snapshot.ageGroups.put("36-45岁", users.stream().filter(user -> inAgeRange(user, 36, 45)).count());
        snapshot.ageGroups.put("46岁以上", users.stream().filter(user -> user.getAge() != null && user.getAge() > 45).count());

        snapshot.topCities = users.stream()
                .map(user -> extractCity(user.getAddress()))
                .filter(city -> !isBlank(city))
                .collect(Collectors.groupingBy(city -> city, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> city = new LinkedHashMap<>();
                    city.put("name", entry.getKey());
                    city.put("value", entry.getValue());
                    return city;
                })
                .collect(Collectors.toList());
        return snapshot;
    }

    private RoleSnapshot buildRoleSnapshot() {
        RoleSnapshot snapshot = new RoleSnapshot();
        snapshot.roles = roleMapper.selectList(null).stream()
                .sorted(Comparator.comparing(role -> role.getCreateTime() == null ? 0L : role.getCreateTime().getTime()))
                .collect(Collectors.toList());
        snapshot.total = snapshot.roles.size();
        return snapshot;
    }

    private LogSnapshot buildLogSnapshot() {
        Page<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(1, 100),
                Wrappers.<OperationLog>lambdaQuery().orderByDesc(OperationLog::getCreateTime)
        );
        LogSnapshot snapshot = new LogSnapshot();
        snapshot.logs = page.getRecords();
        snapshot.deleteCount = snapshot.logs.stream()
                .filter(log -> contains(log.getOperation(), "删除") || "DELETE".equalsIgnoreCase(log.getMethod()))
                .count();
        snapshot.loginCount = snapshot.logs.stream()
                .filter(log -> contains(log.getOperation(), "登录"))
                .count();
        snapshot.failedCount = snapshot.logs.stream()
                .filter(log -> containsIgnoreCase(log.getResult(), "error") || contains(log.getResult(), "失败"))
                .count();
        snapshot.urlCount = snapshot.logs.stream()
                .map(OperationLog::getUrl)
                .filter(url -> !isBlank(url))
                .collect(Collectors.groupingBy(url -> url, Collectors.counting()));
        snapshot.frequentUrls = snapshot.urlCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("url", entry.getKey());
                    item.put("value", entry.getValue());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        return snapshot;
    }

    private Map<String, Object> buildReadableContext(UserSnapshot userSnapshot,
                                                     RoleSnapshot roleSnapshot,
                                                     LogSnapshot logSnapshot) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("userSummary", buildDashboardSummary(userSnapshot));
        context.put("ageGroups", userSnapshot.ageGroups);
        context.put("topCities", userSnapshot.topCities);
        context.put("roles", roleSnapshot.roles.stream().map(role -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", role.getName());
            item.put("code", role.getCode());
            item.put("description", role.getDescription());
            return item;
        }).collect(Collectors.toList()));
        context.put("recentLogs", logSnapshot.logs.stream().limit(5).map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", log.getUsername());
            item.put("operation", log.getOperation());
            item.put("method", log.getMethod());
            item.put("url", log.getUrl());
            item.put("createTime", log.getCreateTime() == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.getCreateTime()));
            return item;
        }).collect(Collectors.toList()));
        return context;
    }

    private String buildDashboardSummary(UserSnapshot snapshot) {
        if (snapshot.total == 0) {
            return "当前还没有用户数据，建议先导入或新增用户后再进行分析。";
        }
        String cityText = snapshot.topCities.isEmpty()
                ? "暂无明显城市集中趋势"
                : "用户主要集中在 " + snapshot.topCities.get(0).get("name");
        return "当前共有 " + snapshot.total + " 名用户，男用户 " + snapshot.male + " 人，女用户 " + snapshot.female
                + " 人，平均年龄约 " + Math.round(snapshot.avgAge) + " 岁。" + cityText + "。";
    }

    private String formatMap(Map<String, Long> data) {
        return data.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue() + " 人")
                .collect(Collectors.joining("；"));
    }

    private String formatRankList(List<Map<String, Object>> data, String unit) {
        if (data == null || data.isEmpty()) {
            return "暂无数据";
        }
        return data.stream()
                .map(item -> String.valueOf(item.get("name")) + " " + item.get("value") + " " + unit)
                .collect(Collectors.joining("；"));
    }

    private boolean inAgeRange(User user, int min, int max) {
        return user.getAge() != null && user.getAge() >= min && user.getAge() <= max;
    }

    private String extractCity(String address) {
        if (isBlank(address)) {
            return "";
        }
        int cityIndex = address.indexOf("市");
        if (cityIndex > 0) {
            return address.substring(0, cityIndex + 1);
        }
        int provinceIndex = address.indexOf("省");
        if (provinceIndex > 0 && address.length() > provinceIndex + 1) {
            return address.substring(0, Math.min(address.length(), provinceIndex + 4));
        }
        return address.length() > 6 ? address.substring(0, 6) : address;
    }

    private boolean isMale(String sex) {
        return "男".equals(sex) || "M".equalsIgnoreCase(sex) || "male".equalsIgnoreCase(sex);
    }

    private boolean isFemale(String sex) {
        return "女".equals(sex) || "F".equalsIgnoreCase(sex) || "female".equalsIgnoreCase(sex);
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String nullToDash(String text) {
        return isBlank(text) ? "-" : text;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static class ChatPrepared {
        private final String fallbackAnswer;
        private final List<String> sources;
        private final List<String> suggestions;
        private final Map<String, Object> metrics;
        private final Map<String, Object> context;

        private ChatPrepared(String fallbackAnswer,
                             List<String> sources,
                             List<String> suggestions,
                             Map<String, Object> metrics,
                             Map<String, Object> context) {
            this.fallbackAnswer = fallbackAnswer;
            this.sources = sources;
            this.suggestions = suggestions;
            this.metrics = metrics;
            this.context = context;
        }

        public String getFallbackAnswer() {
            return fallbackAnswer;
        }

        public List<String> getSources() {
            return sources;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public Map<String, Object> toMeta() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sources", sources);
            result.put("suggestions", suggestions);
            result.put("metrics", metrics);
            result.put("context", context);
            return result;
        }

        public Map<String, Object> toResult(String answer, Map<String, Object> model) {
            Map<String, Object> result = toMeta();
            result.put("answer", answer);
            result.put("model", model);
            return result;
        }
    }

    private static class UserSnapshot {
        private List<User> users = new ArrayList<>();
        private int total;
        private long male;
        private long female;
        private long missingAge;
        private long missingAddress;
        private long missingNickname;
        private double avgAge;
        private Map<String, Long> ageGroups = new LinkedHashMap<>();
        private List<Map<String, Object>> topCities = new ArrayList<>();
    }

    private static class RoleSnapshot {
        private int total;
        private List<Role> roles = new ArrayList<>();
    }

    private static class LogSnapshot {
        private List<OperationLog> logs = new ArrayList<>();
        private long deleteCount;
        private long loginCount;
        private long failedCount;
        private Map<String, Long> urlCount = new LinkedHashMap<>();
        private List<Map<String, Object>> frequentUrls = new ArrayList<>();
    }
}
