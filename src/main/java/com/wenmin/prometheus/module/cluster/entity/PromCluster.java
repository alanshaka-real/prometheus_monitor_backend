package com.wenmin.prometheus.module.cluster.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prom_cluster")
public class PromCluster {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String description;

    private String region;

    private String prometheusUrl;

    private String instanceId;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /**
     * Transient field: cluster nodes, not persisted in DB.
     */
    @TableField(exist = false)
    private java.util.List<PromClusterNode> nodes;
}
