package com.wenmin.prometheus.module.cluster.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "prom_cluster_node", autoResultMap = true)
public class PromClusterNode {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String hostname;

    private String ip;

    private String role;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> cpu;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> memory;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> disk;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> network;

    private String status;

    private String os;

    private String kernel;

    private String uptime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> labels;

    private String clusterId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
