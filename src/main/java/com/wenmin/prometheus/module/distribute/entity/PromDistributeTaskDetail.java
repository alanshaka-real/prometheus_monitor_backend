package com.wenmin.prometheus.module.distribute.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "prom_distribute_task_detail", autoResultMap = true)
public class PromDistributeTaskDetail {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String taskId;

    private String machineId;

    private String machineIp;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> components;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> componentConfig;

    private String status;

    private Integer progress;

    private String currentStep;

    private String logText;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
