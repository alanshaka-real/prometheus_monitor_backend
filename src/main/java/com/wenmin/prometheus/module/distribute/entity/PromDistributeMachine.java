package com.wenmin.prometheus.module.distribute.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "prom_distribute_machine", autoResultMap = true)
public class PromDistributeMachine {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String ip;

    private Integer sshPort;

    private String sshUsername;

    private String sshPassword;

    private String osType;

    private String osArch;

    private String osDistribution;

    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> labels;

    private LocalDateTime lastCheckedAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
