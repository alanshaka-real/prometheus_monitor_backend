package com.wenmin.prometheus.module.distribute.dto;

import lombok.Data;

/**
 * Flat DTO for non-Prometheus component configuration.
 * Unused fields for a given component type are null.
 */
@Data
public class ComponentConfigDTO {

    // ==================== Common fields ====================
    private String installDir;
    private Integer port;
    private String logLevel;  // debug, info, warn, error

    // ==================== Node Exporter ====================
    private String disabledCollectors;  // comma-separated collector names

    // ==================== Process Exporter ====================
    private String processNames;  // comma-separated process name patterns

    // ==================== cAdvisor ====================
    private Boolean dockerOnly;
    private String housekeepingInterval;  // e.g. "10s"

    // ==================== MySQL Exporter ====================
    private String mysqlUser;
    private String mysqlPassword;
    private String mysqlHost;
    private Integer mysqlPort;

    // ==================== Redis Exporter ====================
    private String redisAddr;
    private String redisPassword;

    // ==================== Nginx Exporter ====================
    private String nginxScrapeUri;
}
