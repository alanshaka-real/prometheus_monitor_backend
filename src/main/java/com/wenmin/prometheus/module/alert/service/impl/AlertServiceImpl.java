package com.wenmin.prometheus.module.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== Alert Rules ====================

    @Override
    public Map<String, Object> listAlertRules(String status, String severity) {
        LambdaQueryWrapper<PromAlertRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PromAlertRule::getStatus, status);
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(PromAlertRule::getSeverity, severity);
        }
        wrapper.orderByDesc(PromAlertRule::getCreatedAt);

        List<PromAlertRule> list = ruleMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromAlertRule createAlertRule(PromAlertRule rule) {
        rule.setId(null);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        if (rule.getStatus() == null) {
            rule.setStatus("enabled");
        }
        ruleMapper.insert(rule);
        return rule;
    }

    @Override
    public PromAlertRule updateAlertRule(String id, PromAlertRule rule) {
        PromAlertRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("告警规则不存在");
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
    public Map<String, Object> listAlertHistory(String severity, String startTime, String endTime) {
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

        List<PromAlertHistory> list = historyMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
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
