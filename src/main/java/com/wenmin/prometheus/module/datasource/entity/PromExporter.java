package com.wenmin.prometheus.module.datasource.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "prom_exporter", autoResultMap = true)
public class PromExporter {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String type;

    private String name;

    private String host;

    private Integer port;

    @TableField(value = "`interval`")
    private String interval;

    private String metricsPath;

    private String status;

    private String instanceId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> labels;

    private String machineId;

    private String serviceName;

    private String installDir;

    private String binaryPath;

    private String cliFlags;

    private Integer pid;

    private LocalDateTime lastStatusCheck;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
