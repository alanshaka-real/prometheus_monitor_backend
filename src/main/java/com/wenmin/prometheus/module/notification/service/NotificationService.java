package com.wenmin.prometheus.module.notification.service;

import java.util.Map;

public interface NotificationService {

    Map<String, Object> getCounts(String userId);

    Map<String, Object> listNotices(String userId, Integer page, Integer pageSize);

    Map<String, Object> listChatMessageSummaries(String userId);

    Map<String, Object> listPending(String userId, Integer page, Integer pageSize);

    void markRead(String userId, String id);

    void markAllRead(String userId, String category);

    void handlePending(String userId, String id);
}
