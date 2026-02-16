package com.wenmin.prometheus.module.distribute.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "prom_distribute_task", autoResultMap = true)
public class PromDistributeTask {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String mode;

    private String status;

    private Integer machineCount;

    private Integer successCount;

    private Integer failCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> components;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
