package com.wenmin.prometheus.module.cluster.dto;

import lombok.Data;

@Data
public class NodeMetricsDTO {

    private double cpuPercent;
    private int cpuCores;
    private long memoryTotal;
    private long memoryAvailable;
    private long diskTotal;
    private long diskAvailable;
    private double rxRate;
    private double txRate;
    private int up; // 0 or 1
    private String os;
    private String kernel;
    private String nodename;
    private long bootTime;
}
