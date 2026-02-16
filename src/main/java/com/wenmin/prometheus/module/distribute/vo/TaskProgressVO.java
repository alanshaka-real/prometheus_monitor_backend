package com.wenmin.prometheus.module.distribute.vo;

import lombok.Data;

@Data
public class TaskProgressVO {
    private String taskId;
    private String detailId;
    private String machineIp;
    private String status;
    private int progress;
    private String currentStep;
    private String component;
}
