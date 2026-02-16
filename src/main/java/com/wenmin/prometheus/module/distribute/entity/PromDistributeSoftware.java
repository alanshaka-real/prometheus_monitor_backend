package com.wenmin.prometheus.module.distribute.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prom_distribute_software")
public class PromDistributeSoftware {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String displayName;

    private String version;

    private String osType;

    private String osArch;

    private String fileName;

    private Long fileSize;

    private String fileHash;

    private Integer defaultPort;

    private String installScript;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
