package com.wenmin.prometheus.module.datasource.vo;

import lombok.Data;

@Data
public class ConnectionTestVO {
    private Boolean success;
    private Long latency;
    private String version;
    private String message;
}
