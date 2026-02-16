package com.wenmin.prometheus.module.alert.notification;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationChannelFactory {
    private final Map<String, NotificationSender> senderMap;

    public NotificationChannelFactory(List<NotificationSender> senders) {
        this.senderMap = senders.stream()
                .collect(Collectors.toMap(NotificationSender::getType, Function.identity()));
    }

    public NotificationSender getSender(String type) {
        return senderMap.get(type);
    }
}
