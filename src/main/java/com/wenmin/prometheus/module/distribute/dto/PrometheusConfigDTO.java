package com.wenmin.prometheus.module.distribute.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PrometheusConfigDTO {

    // Installation settings
    private String installDir;   // Default: /opt/prometheus/prometheus
    private String dataDir;      // Default: {installDir}/data

    // prometheus.yml sections
    private GlobalConfig global;
    private List<ScrapeConfig> scrapeConfigs;
    private AlertingConfig alerting;
    private List<String> ruleFiles;
    private List<RemoteWriteConfig> remoteWrite;
    private List<RemoteReadConfig> remoteRead;

    // CLI flags (systemd ExecStart)
    private CliFlags cliFlags;

    // Raw YAML content (highest priority)
    private String rawYaml;

    @Data
    public static class GlobalConfig {
        private String scrapeInterval = "15s";
        private String scrapeTimeout = "10s";
        private String evaluationInterval = "15s";
        private Map<String, String> externalLabels;
    }

    @Data
    public static class ScrapeConfig {
        private String jobName;
        private String metricsPath = "/metrics";
        private String scheme = "http";
        private String scrapeInterval;
        private String scrapeTimeout;
        private List<StaticConfig> staticConfigs;
    }

    @Data
    public static class StaticConfig {
        private List<String> targets;
        private Map<String, String> labels;
    }

    @Data
    public static class AlertingConfig {
        private List<AlertmanagerStaticConfig> alertmanagers;
    }

    @Data
    public static class AlertmanagerStaticConfig {
        private List<StaticConfig> staticConfigs;
    }

    @Data
    public static class RemoteWriteConfig {
        private String url;
        private Map<String, Object> queueConfig;
    }

    @Data
    public static class RemoteReadConfig {
        private String url;
        private boolean readRecent;
    }

    @Data
    public static class CliFlags {
        // Storage
        private String storageTsdbRetentionTime = "15d";
        private String storageTsdbRetentionSize;
        private Boolean storageWalCompression = true;
        // Web
        private String webListenAddress = "0.0.0.0:9090";
        private String webExternalUrl;
        private Boolean webEnableLifecycle = false;
        private Boolean webEnableAdminApi = false;
        // Query
        private String queryTimeout = "2m";
        private Integer queryMaxConcurrency = 20;
        // Log
        private String logLevel = "info";
        private String logFormat = "logfmt";
    }
}
