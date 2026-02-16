package com.wenmin.prometheus.module.datasource.vo;

import lombok.Data;

@Data
public class ExporterConfigVO {

    private String serviceFileContent;

    private String cliFlags;

    private String binaryVersion;

    private String runningStatus;

    private Boolean serviceExists;
}
