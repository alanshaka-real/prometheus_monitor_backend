package com.wenmin.prometheus.module.dashboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "prom_dashboard_template", autoResultMap = true)
public class PromDashboardTemplate {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String category;

    private String subCategory;

    private String exporterType;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> panels;

    private String thumbnail;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private String version;

    private Integer panelCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> panelTemplateIds;

    private LocalDateTime createdAt;
}
