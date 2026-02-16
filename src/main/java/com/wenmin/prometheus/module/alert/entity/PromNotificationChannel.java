package com.wenmin.prometheus.module.alert.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "prom_notification_channel", autoResultMap = true)
public class PromNotificationChannel {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String type;

    private String name;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> config;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
