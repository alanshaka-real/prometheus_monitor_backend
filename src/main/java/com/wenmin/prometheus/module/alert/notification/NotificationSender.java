package com.wenmin.prometheus.module.alert.notification;

import java.util.Map;

public interface NotificationSender {
    String getType();
    boolean send(Map<String, String> config, String message);
}
