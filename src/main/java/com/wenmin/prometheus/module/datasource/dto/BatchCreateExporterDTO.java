package com.wenmin.prometheus.module.datasource.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BatchCreateExporterDTO {
    private String instanceId;
    private List<ExporterItem> exporters;

    @Data
    public static class ExporterItem {
        private String type;
        private String name;
        private String host;
        private Integer port;
        private String interval;
        private String metricsPath;
        private Map<String, String> labels;
    }
}
