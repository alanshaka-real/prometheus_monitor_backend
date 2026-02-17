package com.wenmin.prometheus.module.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.alert.entity.PromAlertHistory;
import com.wenmin.prometheus.module.alert.entity.PromAlertRule;
import com.wenmin.prometheus.module.alert.entity.PromNotificationChannel;
import com.wenmin.prometheus.module.alert.entity.PromSilence;
import com.wenmin.prometheus.module.alert.mapper.PromAlertHistoryMapper;
import com.wenmin.prometheus.module.alert.mapper.PromAlertRuleMapper;
import com.wenmin.prometheus.module.alert.mapper.PromNotificationChannelMapper;
import com.wenmin.prometheus.module.alert.mapper.PromSilenceMapper;
import com.wenmin.prometheus.module.alert.notification.NotificationChannelFactory;
import com.wenmin.prometheus.module.alert.notification.NotificationSender;
import com.wenmin.prometheus.module.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.mapper.PromInstanceMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final PromAlertRuleMapper ruleMapper;
    private final PromAlertHistoryMapper historyMapper;
    private final PromSilenceMapper silenceMapper;
    private final PromNotificationChannelMapper channelMapper;
    private final NotificationChannelFactory channelFactory;
    private final PromInstanceMapper instanceMapper;
    private final RestTemplate restTemplate;

    @Value("${alert.validation.remote-enabled:true}")
    private boolean remoteValidationEnabled;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== Alert Rules ====================

    @Override
    public Map<String, Object> listAlertRules(String status, String severity, Integer page, Integer pageSize) {
        LambdaQueryWrapper<PromAlertRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PromAlertRule::getStatus, status);
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(PromAlertRule::getSeverity, severity);
        }
        wrapper.orderByDesc(PromAlertRule::getCreatedAt);

        Page<PromAlertRule> pageObj = new Page<>(page, pageSize);
        IPage<PromAlertRule> pageResult = ruleMapper.selectPage(pageObj, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @Override
    public PromAlertRule createAlertRule(PromAlertRule rule) {
        // PromQL 基础语法校验（快速预检）
        validatePromQLExpr(rule.getExpr());
        // PromQL 远程语法校验（通过 Prometheus 验证）
        validateOnPrometheus(rule.getExpr());

        rule.setId(null);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        if (rule.getStatus() == null) {
            rule.setStatus("enabled");
        }
        ruleMapper.insert(rule);
        return rule;
    }

    /**
     * PromQL 表达式基础语法校验。
     * <p>
     * 校验规则：
     * 1. 表达式不为空/空白
     * 2. 括号匹配：()、{}、[] 数量一致
     * 3. 不包含明显非法字符（如 @、#、$、;、反引号等）
     * 4. 基本结构合理：不以运算符开头（+、*、/、%、^，但 - 可用于负号）
     */
    private void validatePromQLExpr(String expr) {
        // 1. 非空校验
        if (expr == null || expr.isBlank()) {
            throw new BusinessException("PromQL 表达式不能为空");
        }

        String trimmed = expr.trim();

        // 2. 括号匹配校验
        int parenCount = 0;
        int braceCount = 0;
        int bracketCount = 0;
        for (char c : trimmed.toCharArray()) {
            switch (c) {
                case '(' -> parenCount++;
                case ')' -> parenCount--;
                case '{' -> braceCount++;
                case '}' -> braceCount--;
                case '[' -> bracketCount++;
                case ']' -> bracketCount--;
            }
            // 右括号数量不应超过左括号
            if (parenCount < 0 || braceCount < 0 || bracketCount < 0) {
                throw new BusinessException("PromQL 表达式括号不匹配");
            }
        }
        if (parenCount != 0 || braceCount != 0 || bracketCount != 0) {
            throw new BusinessException("PromQL 表达式括号不匹配");
        }

        // 3. 非法字符校验（排除 PromQL 中合法的字符）
        // PromQL 合法字符包括：字母、数字、_、:、.、()、{}、[]、""、''、+-*/%^、比较运算符 =!<>、逗号、空格、换行、~
        if (trimmed.matches(".*[;@#\\$`\\\\].*")) {
            throw new BusinessException("PromQL 表达式包含非法字符");
        }

        // 4. 不以二元运算符开头（- 允许用作负号）
        if (trimmed.matches("^[+*/%^].*")) {
            throw new BusinessException("PromQL 表达式不能以运算符开头");
        }
    }

    /**
     * 通过 Prometheus 实例远程验证 PromQL 表达式语法。
     * Prometheus 不可用时降级为仅本地检查。
     */
    private void validateOnPrometheus(String expr) {
        if (!remoteValidationEnabled) {
            return;
        }

        PromInstance instance = instanceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromInstance>()
                        .eq(PromInstance::getStatus, "online")
                        .last("LIMIT 1"));
        if (instance == null) {
            log.debug("没有可用的 Prometheus 实例，跳过远程 PromQL 验证");
            return;
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(instance.getUrl() + "/api/v1/query")
                    .queryParam("query", expr)
                    .queryParam("time", System.currentTimeMillis() / 1000)
                    .build().encode().toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && "error".equals(response.get("status"))) {
                String errorType = (String) response.get("errorType");
                String error = (String) response.get("error");
                if ("bad_data".equals(errorType) && error != null && error.contains("parse error")) {
                    throw new BusinessException("PromQL 语法错误: " + error);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Prometheus returns 400 for bad queries with JSON body
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("parse error")) {
                throw new BusinessException("PromQL 语法错误: " + extractErrorMessage(body));
            }
        } catch (Exception e) {
            log.debug("Prometheus 远程验证不可用，降级为本地检查: {}", e.getMessage());
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(responseBody, Map.class);
            return (String) body.getOrDefault("error", responseBody);
        } catch (Exception e) {
            return responseBody;
        }
    }

    @Override
    public PromAlertRule updateAlertRule(String id, PromAlertRule rule) {
        PromAlertRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("告警规则不存在");
        }
        if (rule.getExpr() != null && !rule.getExpr().equals(existing.getExpr())) {
            validatePromQLExpr(rule.getExpr());
            validateOnPrometheus(rule.getExpr());
        }
        rule.setId(id);
        rule.setUpdatedAt(LocalDateTime.now());
        rule.setCreatedAt(existing.getCreatedAt());
        ruleMapper.updateById(rule);
        return ruleMapper.selectById(id);
    }

    @Override
    public void deleteAlertRule(String id) {
        PromAlertRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("告警规则不存在");
        }
        ruleMapper.deleteById(id);
    }

    @Override
    public void toggleAlertRule(String id, String status) {
        PromAlertRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException("告警规则不存在");
        }
        rule.setStatus(status);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);
    }

    // ==================== Alert History ====================

    @Override
    public Map<String, Object> listAlertHistory(String severity, String startTime, String endTime, Integer page, Integer pageSize) {
        LambdaQueryWrapper<PromAlertHistory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(severity)) {
            wrapper.eq(PromAlertHistory::getSeverity, severity);
        }
        if (StringUtils.hasText(startTime) && StringUtils.hasText(endTime)) {
            wrapper.between(PromAlertHistory::getStartsAt,
                    LocalDateTime.parse(startTime, DTF),
                    LocalDateTime.parse(endTime, DTF));
        } else {
            if (StringUtils.hasText(startTime)) {
                wrapper.ge(PromAlertHistory::getStartsAt, LocalDateTime.parse(startTime, DTF));
            }
            if (StringUtils.hasText(endTime)) {
                wrapper.le(PromAlertHistory::getStartsAt, LocalDateTime.parse(endTime, DTF));
            }
        }
        wrapper.orderByDesc(PromAlertHistory::getStartsAt);

        Page<PromAlertHistory> pageObj = new Page<>(page, pageSize);
        IPage<PromAlertHistory> pageResult = historyMapper.selectPage(pageObj, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @Override
    public void acknowledgeAlert(String id, String remark, String handledBy) {
        PromAlertHistory history = historyMapper.selectById(id);
        if (history == null) {
            throw new BusinessException("告警记录不存在");
        }
        history.setHandledBy(handledBy);
        history.setHandledAt(LocalDateTime.now());
        history.setRemark(remark);
        history.setStatus("resolved");
        historyMapper.updateById(history);
    }

    // ==================== Silences ====================

    @Override
    public Map<String, Object> listSilences() {
        LambdaQueryWrapper<PromSilence> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PromSilence::getCreatedAt);

        List<PromSilence> list = silenceMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromSilence createSilence(PromSilence silence) {
        silence.setId(null);
        silence.setCreatedAt(LocalDateTime.now());
        if (silence.getStatus() == null) {
            silence.setStatus("active");
        }
        silenceMapper.insert(silence);
        return silence;
    }

    @Override
    public void deleteSilence(String id) {
        PromSilence existing = silenceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("静默规则不存在");
        }
        silenceMapper.deleteById(id);
    }

    // ==================== Notification Channels ====================

    @Override
    public Map<String, Object> listNotificationChannels() {
        LambdaQueryWrapper<PromNotificationChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PromNotificationChannel::getCreatedAt);

        List<PromNotificationChannel> list = channelMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromNotificationChannel createNotificationChannel(PromNotificationChannel channel) {
        channel.setId(null);
        channel.setCreatedAt(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());
        if (channel.getEnabled() == null) {
            channel.setEnabled(true);
        }
        channelMapper.insert(channel);
        return channel;
    }

    @Override
    public PromNotificationChannel updateNotificationChannel(String id, PromNotificationChannel channel) {
        PromNotificationChannel existing = channelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("通知渠道不存在");
        }
        channel.setId(id);
        channel.setUpdatedAt(LocalDateTime.now());
        channel.setCreatedAt(existing.getCreatedAt());
        channelMapper.updateById(channel);
        return channelMapper.selectById(id);
    }

    @Override
    public void deleteNotificationChannel(String id) {
        PromNotificationChannel existing = channelMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("通知渠道不存在");
        }
        channelMapper.deleteById(id);
    }

    @Override
    public void testNotificationChannel(String id) {
        PromNotificationChannel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw new BusinessException("通知渠道不存在");
        }

        NotificationSender sender = channelFactory.getSender(channel.getType());
        if (sender == null) {
            throw new BusinessException("不支持的通知类型: " + channel.getType());
        }

        boolean success = sender.send(channel.getConfig(), "[Prometheus Monitor] 测试通知 - " + channel.getName());
        if (!success) {
            throw new BusinessException("通知发送失败");
        }
    }
}
