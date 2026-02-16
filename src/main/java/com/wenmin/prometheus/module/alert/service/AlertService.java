package com.wenmin.prometheus.module.alert.service;

import com.wenmin.prometheus.module.alert.entity.PromAlertHistory;
import com.wenmin.prometheus.module.alert.entity.PromAlertRule;
import com.wenmin.prometheus.module.alert.entity.PromNotificationChannel;
import com.wenmin.prometheus.module.alert.entity.PromSilence;

import java.util.Map;

public interface AlertService {

    // ---- Alert Rules ----

    Map<String, Object> listAlertRules(String status, String severity);

    PromAlertRule createAlertRule(PromAlertRule rule);

    PromAlertRule updateAlertRule(String id, PromAlertRule rule);

    void deleteAlertRule(String id);

    void toggleAlertRule(String id, String status);

    // ---- Alert History ----

    Map<String, Object> listAlertHistory(String severity, String startTime, String endTime);

    void acknowledgeAlert(String id, String remark, String handledBy);

    // ---- Silences ----

    Map<String, Object> listSilences();

    PromSilence createSilence(PromSilence silence);

    void deleteSilence(String id);

    // ---- Notification Channels ----

    Map<String, Object> listNotificationChannels();

    PromNotificationChannel createNotificationChannel(PromNotificationChannel channel);

    PromNotificationChannel updateNotificationChannel(String id, PromNotificationChannel channel);

    void deleteNotificationChannel(String id);

    void testNotificationChannel(String id);
}
