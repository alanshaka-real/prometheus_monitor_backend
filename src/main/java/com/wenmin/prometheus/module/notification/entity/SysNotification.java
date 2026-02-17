package com.wenmin.prometheus.module.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification")
public class SysNotification {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private String category;

    private String type;

    private String title;

    private String content;

    private String referenceId;

    private String referenceType;

    private Integer isRead;

    private Integer isHandled;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
