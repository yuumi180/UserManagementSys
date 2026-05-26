package com.example.demo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.OperationLog;
import com.example.demo.entity.User;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiAnalysisService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private OperationLogMapper operationLogMapper;

    public Map<String, Object> analyzeDashboard() {
        List<User> users = userMapper.selectList(null);
        int total = users.size();
        long male = users.stream().filter(user -> "男".equals(user.getSex())).count();
        long female = users.stream().filter(user -> "女".equals(user.getSex())).count();
        long missingAge = users.stream().filter(user -> user.getAge() == null || user.getAge() <= 0).count();
        long missingAddress = users.stream().filter(user -> isBlank(user.getAddress())).count();
        long missingNickname = users.stream().filter(user -> isBlank(user.getNickname())).count();

        double avgAge = users.stream()
                .map(User::getAge)
                .filter(Objects::nonNull)
                .filter(age -> age > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        Map<String, Long> ageGroups = new LinkedHashMap<>();
        ageGroups.put("18岁以下", users.stream().filter(user -> user.getAge() != null && user.getAge() < 18).count());
        ageGroups.put("18-25岁", users.stream().filter(user -> inAgeRange(user, 18, 25)).count());
        ageGroups.put("26-35岁", users.stream().filter(user -> inAgeRange(user, 26, 35)).count());
        ageGroups.put("36-45岁", users.stream().filter(user -> inAgeRange(user, 36, 45)).count());
        ageGroups.put("46岁以上", users.stream().filter(user -> user.getAge() != null && user.getAge() > 45).count());

        List<Map<String, Object>> topCities = users.stream()
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

        List<String> anomalies = new ArrayList<>();
        if (total == 0) {
            anomalies.add("当前没有用户数据，仪表盘无法形成稳定画像。");
        }
        if (missingAge > 0) {
            anomalies.add("有 " + missingAge + " 条用户记录缺少有效年龄。");
        }
        if (missingAddress > 0) {
            anomalies.add("有 " + missingAddress + " 条用户记录缺少地址，城市分布可能失真。");
        }
        if (missingNickname > 0) {
            anomalies.add("有 " + missingNickname + " 条用户记录缺少昵称。");
        }
        if (total > 0 && Math.max(male, female) * 1.0 / total > 0.8) {
            anomalies.add("性别分布明显偏向单侧，建议确认样本来源或字段填写质量。");
        }
        if (anomalies.isEmpty()) {
            anomalies.add("暂未发现明显的数据完整性异常。");
        }

        List<String> suggestions = new ArrayList<>();
        if (!topCities.isEmpty()) {
            suggestions.add("优先围绕 " + topCities.get(0).get("name") + " 用户群体完善画像和运营策略。");
        }
        if (missingAge + missingAddress + missingNickname > 0) {
            suggestions.add("建议先补齐年龄、地址、昵称等基础字段，再做更细粒度分析。");
        }
        suggestions.add("可按年龄段和城市建立用户分层，后续用于权限、运营或数据看板筛选。");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", buildDashboardSummary(total, male, female, avgAge, topCities));
        result.put("totalUsers", total);
        result.put("maleUsers", male);
        result.put("femaleUsers", female);
        result.put("avgAge", Math.round(avgAge));
        result.put("ageGroups", ageGroups);
        result.put("topCities", topCities);
        result.put("anomalies", anomalies);
        result.put("suggestions", suggestions);
        return result;
    }

    public Map<String, Object> analyzeLogs() {
        Page<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(1, 100),
                Wrappers.<OperationLog>lambdaQuery().orderByDesc(OperationLog::getCreateTime)
        );
        List<OperationLog> logs = page.getRecords();

        long deleteCount = logs.stream().filter(log -> contains(log.getOperation(), "删除") || "DELETE".equalsIgnoreCase(log.getMethod())).count();
        long loginCount = logs.stream().filter(log -> contains(log.getOperation(), "登录")).count();
        long failedCount = logs.stream().filter(log -> contains(log.getResult(), "error") || contains(log.getResult(), "失败")).count();

        Map<String, Long> urlCount = logs.stream()
                .map(OperationLog::getUrl)
                .filter(url -> !isBlank(url))
                .collect(Collectors.groupingBy(url -> url, Collectors.counting()));

        List<Map<String, Object>> frequentUrls = urlCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("url", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        List<String> risks = new ArrayList<>();
        int score = 0;
        if (deleteCount >= 5) {
            score += 40;
            risks.add("最近 100 条日志中删除操作达到 " + deleteCount + " 次，建议核对是否存在误删或批量清理。");
        } else if (deleteCount > 0) {
            score += 15;
            risks.add("发现 " + deleteCount + " 次删除操作，风险较低但仍建议保留审计记录。");
        }
        if (failedCount > 0) {
            score += Math.min(30, (int) failedCount * 5);
            risks.add("发现 " + failedCount + " 条失败或异常结果，需要查看接口返回信息。");
        }
        for (Map.Entry<String, Long> entry : urlCount.entrySet()) {
            if (entry.getValue() >= 20) {
                score += 20;
                risks.add("接口 " + entry.getKey() + " 访问频次较高，最近 100 条中出现 " + entry.getValue() + " 次。");
            }
        }
        if (logs.isEmpty()) {
            risks.add("暂无操作日志，无法判断风险趋势。");
        }
        if (risks.isEmpty()) {
            risks.add("最近 100 条日志未发现明显高风险行为。");
        }

        String level = score >= 60 ? "高" : score >= 25 ? "中" : "低";
        List<String> suggestions = new ArrayList<>();
        suggestions.add("保留删除、新增、登录等关键操作的审计记录。");
        if (deleteCount > 0) {
            suggestions.add("对删除操作增加二次确认或按角色限制。");
        }
        if (failedCount > 0) {
            suggestions.add("优先排查失败请求对应的 URL 和参数，避免重复触发异常。");
        }
        if (loginCount > 10) {
            suggestions.add("登录操作较频繁，可结合 IP 和账号维度做进一步限流。");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "已分析最近 " + logs.size() + " 条操作日志，当前风险等级为" + level + "。");
        result.put("riskLevel", level);
        result.put("score", Math.min(score, 100));
        result.put("totalLogs", logs.size());
        result.put("deleteCount", deleteCount);
        result.put("loginCount", loginCount);
        result.put("failedCount", failedCount);
        result.put("frequentUrls", frequentUrls);
        result.put("risks", risks);
        result.put("suggestions", suggestions);
        return result;
    }

    private String buildDashboardSummary(int total, long male, long female, double avgAge, List<Map<String, Object>> topCities) {
        if (total == 0) {
            return "当前还没有用户数据，建议先导入或新增用户后再进行分析。";
        }
        String cityText = topCities.isEmpty() ? "暂无明显城市集中趋势" : "用户主要集中在 " + topCities.get(0).get("name");
        return "当前共有 " + total + " 名用户，男用户 " + male + " 人，女用户 " + female
                + " 人，平均年龄约 " + Math.round(avgAge) + " 岁。" + cityText + "。";
    }

    private boolean inAgeRange(User user, int min, int max) {
        return user.getAge() != null && user.getAge() >= min && user.getAge() <= max;
    }

    private String extractCity(String address) {
        if (isBlank(address)) {
            return "";
        }
        int index = address.indexOf("市");
        if (index > 0) {
            return address.substring(0, index + 1);
        }
        return address.length() > 6 ? address.substring(0, 6) : address;
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
