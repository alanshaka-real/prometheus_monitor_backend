package com.wenmin.prometheus.module.datasource.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExporterStatusVO {

    private String exporterId;

    private String status;

    private Integer pid;

    private Boolean portListening;

    private LocalDateTime checkedAt;

    private String message;
}
