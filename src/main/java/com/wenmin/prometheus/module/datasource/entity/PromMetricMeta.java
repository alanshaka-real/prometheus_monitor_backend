package com.wenmin.prometheus.module.datasource.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "prom_metric_meta", autoResultMap = true)
public class PromMetricMeta {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String type;

    private String help;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> labels;

    private String exporter;

    private String unit;

    private Boolean favorite;

    private LocalDateTime createdAt;
}
