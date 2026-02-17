package com.wenmin.prometheus.module.alert.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.mapper.PromInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警规则定时评估调度器。
 * 每隔固定时间评估所有启用的告警规则，
 * 对每条规则执行 PromQL 即时查询，有结果即触发告警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluationScheduler {

    private final PromAlertRuleMapper ruleMapper;
    private final PromAlertHistoryMapper historyMapper;
    private final PromNotificationChannelMapper channelMapper;
    private final PromSilenceMapper silenceMapper;
    private final PromInstanceMapper instanceMapper;
    private final NotificationChannelFactory channelFactory;
    private final RestTemplate restTemplate;
    private final AlertGroupManager alertGroupManager;

    @Value("${alert.evaluation.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedRateString = "${alert.evaluation.interval-ms:60000}")
    public void evaluateAlertRules() {
        if (!enabled) {
            return;
        }

        log.debug("开始告警规则评估...");

        // Query all enabled alert rules
        List<PromAlertRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<PromAlertRule>()
                        .eq(PromAlertRule::getStatus, "enabled"));

        if (rules.isEmpty()) {
            log.debug("没有启用的告警规则，跳过评估");
            return;
        }

        // Get first available Prometheus instance
        String prometheusUrl = getPrometheusUrl();
        if (prometheusUrl == null) {
            log.warn("没有可用的 Prometheus 实例，跳过告警评估");
            return;
        }

        log.info("开始评估 {} 条告警规则", rules.size());

        for (PromAlertRule rule : rules) {
            try {
                evaluateRule(rule, prometheusUrl);
            } catch (Exception e) {
                log.error("评估告警规则失败: {} ({})", rule.getName(), rule.getId(), e);
            }
        }

        log.info("告警规则评估完成");
    }

    private void evaluateRule(PromAlertRule rule, String prometheusUrl) {
        URI uri = UriComponentsBuilder.fromHttpUrl(prometheusUrl + "/api/v1/query")
                .queryParam("query", rule.getExpr())
                .build().encode().toUri();

        Map<String, Object> response;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(uri, Map.class);
            response = resp;
        } catch (Exception e) {
            log.debug("Prometheus 查询失败: rule={}, expr={}, error={}",
                    rule.getName(), rule.getExpr(), e.getMessage());
            return;
        }

        if (response == null || !"success".equals(response.get("status"))) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) return;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
        if (results == null || results.isEmpty()) {
            return;
        }

        // Alert triggered - create history records for each result
        for (Map<String, Object> result : results) {
            @SuppressWarnings("unchecked")
            Map<String, String> metric = (Map<String, String>) result.get("metric");
            @SuppressWarnings("unchecked")
            List<Object> value = (List<Object>) result.get("value");

            String instance = metric != null ? metric.getOrDefault("instance", "") : "";
            String alertValue = (value != null && value.size() > 1) ? String.valueOf(value.get(1)) : "";

            // Check if there's already a firing alert for this rule+instance
            long existingCount = historyMapper.selectCount(
                    new LambdaQueryWrapper<PromAlertHistory>()
                            .eq(PromAlertHistory::getAlertName, rule.getName())
                            .eq(PromAlertHistory::getInstance, instance)
                            .eq(PromAlertHistory::getStatus, "firing"));

            if (existingCount > 0) {
                log.debug("告警已存在，跳过: rule={}, instance={}", rule.getName(), instance);
                continue;
            }

            // Check silence rules before sending notifications
            boolean isSilenced = isAlertSilenced(rule, instance);

            // Create alert history record
            PromAlertHistory history = new PromAlertHistory();
            history.setAlertName(rule.getName());
            history.setSeverity(rule.getSeverity());
            history.setStatus("firing");
            history.setInstance(instance);
            history.setValue(alertValue);
            history.setStartsAt(LocalDateTime.now());
            history.setCreatedAt(LocalDateTime.now());
            history.setSilenced(isSilenced);
            historyMapper.insert(history);

            log.info("告警触发: rule={}, severity={}, instance={}, value={}, silenced={}",
                    rule.getName(), rule.getSeverity(), instance, alertValue, isSilenced);

            // Send notifications only if not silenced and convergence allows
            if (!isSilenced) {
                String groupName = rule.getGroupName() != null ? rule.getGroupName() : "default";
                if (alertGroupManager.shouldNotify(rule.getName(), rule.getSeverity(), groupName)) {
                    sendNotifications(rule, instance, alertValue);
                } else {
                    log.debug("告警收敛抑制通知: rule={}, group={}", rule.getName(), groupName);
                }
            }
        }
    }

    private void sendNotifications(PromAlertRule rule, String instance, String value) {
        List<PromNotificationChannel> channels = channelMapper.selectList(
                new LambdaQueryWrapper<PromNotificationChannel>()
                        .eq(PromNotificationChannel::getEnabled, true));

        if (channels.isEmpty()) {
            log.debug("没有启用的通知渠道");
            return;
        }

        String message = String.format(
                "[%s] 告警触发: %s\n严重级别: %s\n实例: %s\n当前值: %s\n时间: %s",
                rule.getSeverity().toUpperCase(),
                rule.getName(),
                rule.getSeverity(),
                instance,
                value,
                LocalDateTime.now());

        for (PromNotificationChannel channel : channels) {
            try {
                NotificationSender sender = channelFactory.getSender(channel.getType());
                if (sender != null) {
                    sender.send(channel.getConfig(), message);
                }
            } catch (Exception e) {
                log.error("发送通知失败: channel={}, type={}", channel.getName(), channel.getType(), e);
            }
        }
    }

    /**
     * Check if an alert matches any active silence rule.
     */
    private boolean isAlertSilenced(PromAlertRule rule, String instance) {
        List<PromSilence> activeSilences = silenceMapper.selectList(
                new LambdaQueryWrapper<PromSilence>()
                        .eq(PromSilence::getStatus, "active")
                        .le(PromSilence::getStartsAt, LocalDateTime.now())
                        .ge(PromSilence::getEndsAt, LocalDateTime.now()));

        if (activeSilences.isEmpty()) {
            return false;
        }

        for (PromSilence silence : activeSilences) {
            if (matchesSilence(silence, rule, instance)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean matchesSilence(PromSilence silence, PromAlertRule rule, String instance) {
        List<Map<String, Object>> matchers = silence.getMatchers();
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        for (Map<String, Object> matcher : matchers) {
            String name = (String) matcher.get("name");
            String value = (String) matcher.get("value");
            boolean isRegex = Boolean.TRUE.equals(matcher.get("isRegex"));

            if (name == null || value == null) continue;

            String actual = switch (name) {
                case "alertname" -> rule.getName();
                case "severity" -> rule.getSeverity();
                case "instance" -> instance;
                default -> rule.getLabels() != null ? rule.getLabels().get(name) : null;
            };

            if (actual == null) return false;

            if (isRegex) {
                if (!actual.matches(value)) return false;
            } else {
                if (!actual.equals(value)) return false;
            }
        }
        return true;
    }

    /**
     * Auto-expire silence rules that have passed their end time.
     */
    @Scheduled(fixedRate = 60000)
    public void expireSilenceRules() {
        List<PromSilence> activeSilences = silenceMapper.selectList(
                new LambdaQueryWrapper<PromSilence>()
                        .eq(PromSilence::getStatus, "active")
                        .lt(PromSilence::getEndsAt, LocalDateTime.now()));

        for (PromSilence silence : activeSilences) {
            silence.setStatus("expired");
            silenceMapper.updateById(silence);
            log.info("静默规则已过期: id={}", silence.getId());
        }
    }

    private String getPrometheusUrl() {
        PromInstance instance = instanceMapper.selectOne(
                new LambdaQueryWrapper<PromInstance>()
                        .eq(PromInstance::getStatus, "online")
                        .last("LIMIT 1"));
        return instance != null ? instance.getUrl() : null;
    }
}
