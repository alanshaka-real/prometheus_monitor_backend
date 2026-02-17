package com.wenmin.prometheus.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SystemEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishNotice(String eventType, String title, String content,
                              List<String> targetUserIds, String referenceId, String referenceType) {
        publisher.publishEvent(new SystemEvent(
                this, eventType, title, content, "notice",
                targetUserIds, referenceId, referenceType));
    }

    public void publishPending(String eventType, String title, String content,
                               List<String> targetUserIds, String referenceId, String referenceType) {
        publisher.publishEvent(new SystemEvent(
                this, eventType, title, content, "pending",
                targetUserIds, referenceId, referenceType));
    }
}
