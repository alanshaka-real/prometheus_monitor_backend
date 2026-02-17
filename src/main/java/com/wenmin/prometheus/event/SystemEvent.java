package com.wenmin.prometheus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class SystemEvent extends ApplicationEvent {

    private final String eventType;
    private final String title;
    private final String content;
    private final String category; // notice / pending
    private final List<String> targetUserIds; // null = all users
    private final String referenceId;
    private final String referenceType;

    public SystemEvent(Object source, String eventType, String title, String content,
                       String category, List<String> targetUserIds,
                       String referenceId, String referenceType) {
        super(source);
        this.eventType = eventType;
        this.title = title;
        this.content = content;
        this.category = category;
        this.targetUserIds = targetUserIds;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }
}
