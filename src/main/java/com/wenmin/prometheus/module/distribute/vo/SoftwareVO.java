package com.wenmin.prometheus.module.distribute.vo;

import lombok.Data;

@Data
public class SoftwareVO {
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
    private String description;
}
