package com.wenmin.prometheus.module.notification.vo;

import lombok.Data;

@Data
public class NotificationCountVO {
    private long noticeUnread;
    private long messageUnread;
    private long pendingUnread;
    private long total;
}
