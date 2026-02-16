package com.wenmin.prometheus.module.panel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "prom_panel_template", autoResultMap = true)
public class PromPanelTemplate {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String description;

    private String category;

    private String subCategory;

    private String exporterType;

    private String chartType;

    private String promql;

    private String unit;

    private String unitFormat;

    private Integer defaultWidth;

    private Integer defaultHeight;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object thresholds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> options;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
