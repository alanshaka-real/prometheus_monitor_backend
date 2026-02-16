package com.wenmin.prometheus.module.query.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "prom_promql_template", autoResultMap = true)
public class PromPromqlTemplate {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String category;

    private String subCategory;

    private String exporterType;

    private String query;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> variables;

    private String unit;

    private String unitFormat;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
