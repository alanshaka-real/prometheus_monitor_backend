package com.wenmin.prometheus.module.alert.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "prom_alert_rule", autoResultMap = true)
public class PromAlertRule {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String groupName;

    private String expr;

    private String duration;

    private String severity;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> labels;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> annotations;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
