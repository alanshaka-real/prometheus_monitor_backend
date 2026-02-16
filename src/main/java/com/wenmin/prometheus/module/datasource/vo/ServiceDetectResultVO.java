package com.wenmin.prometheus.module.datasource.vo;

import lombok.Data;

import java.util.List;

@Data
public class ServiceDetectResultVO {
    private int totalProbes;
    private int detectedCount;
    private int alreadyRegisteredCount;
    private List<DetectedService> services;

    @Data
    public static class DetectedService {
        private String machineId;
        private String machineName;
        private String machineIp;
        private String serviceType;
        private String exporterType;
        private int port;
        private boolean detected;
        private boolean alreadyRegistered;
        private String existingExporterId;
        private Long latencyMs;
        private String message;
    }
}
