package com.wenmin.prometheus.module.datasource.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "prom_instance", autoResultMap = true)
public class PromInstance {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String url;

    private String status;

    @JsonProperty("group")
    private String groupName;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> labels;

    private String scrapeInterval;

    private String retentionTime;

    private String version;

    private String machineId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> configJson;

    private Boolean lifecycleEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
