package com.wenmin.prometheus.module.distribute.vo;

import lombok.Data;

@Data
public class SoftwareUploadVO {

    private String name;
    private String displayName;
    private String version;
    private String osType;
    private String osArch;
    private String fileName;
    private long fileSize;
    private String message;
    private int replacedCount;
}
