package com.wenmin.prometheus.module.dashboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "prom_dashboard", autoResultMap = true)
public class PromDashboard {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> panels;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> timeRange;

    private Integer refreshInterval;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private String folderId;

    private String createdBy;

    private Boolean favorite;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
