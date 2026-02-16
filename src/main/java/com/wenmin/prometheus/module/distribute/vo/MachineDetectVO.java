package com.wenmin.prometheus.module.distribute.vo;

import lombok.Data;

import java.util.List;

@Data
public class MachineDetectVO {
    private String machineId;
    private String machineName;
    private String machineIp;
    private String osType;
    private String osArch;
    private String osDistribution;
    private String hostname;
    private String kernelVersion;
    private Integer cpuCores;
    private String cpuModel;
    private String memoryTotal;
    private String diskTotal;
    private String diskUsed;
    private String diskAvail;
    private String uptime;
    private List<ComponentDetectVO> components;
    private boolean success;
    private String message;
}
