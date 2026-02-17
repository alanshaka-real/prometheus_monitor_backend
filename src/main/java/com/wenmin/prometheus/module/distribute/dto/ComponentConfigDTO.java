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

    // ==================== Blackbox Exporter ====================
    private String blackboxTimeout;             // probe timeout, e.g. "5s"
    private String blackboxProbeHttpMethod;      // GET / POST
    private String blackboxProbeHttpValidCodes;  // comma-separated, empty = all 2xx
    private Boolean blackboxProbeFollowRedirects; // follow HTTP redirects
    private String blackboxProbePreferredIp;     // ip4 / ip6
    private Boolean blackboxProbeTlsInsecure;    // skip TLS certificate verification
    private Boolean blackboxEnableIcmp;          // enable ICMP probe module
    private Boolean blackboxEnableTcp;           // enable TCP probe module

    // ==================== Process Exporter ====================
    private String processNames;  // comma-separated process name patterns
    private Boolean processUseConfig;   // true = use config.path, false = use -procnames
    private String processConfigYaml;   // YAML content for process_exporter_config.yml
    private Boolean processChildren;    // -children flag
    private Boolean processThreads;     // -threads flag
    private Boolean processRecheck;     // -recheck flag
    private Boolean processGatherSmaps; // -gather-smaps flag

    // ==================== cAdvisor ====================
    private Boolean dockerOnly;
    private String housekeepingInterval;       // e.g. "10s"
    private String cadvisorStorageDuration;    // e.g. "2m"
    private String cadvisorDisableMetrics;     // comma-separated metric names
    private Boolean cadvisorEnableLoadReader;  // --enable_load_reader
    private Boolean cadvisorStoreContainerLabels; // --store_container_labels
    private String cadvisorWhitelistedLabels;  // --whitelisted_container_labels
    private Integer cadvisorMaxProcs;          // --max_procs

    // ==================== MySQL Exporter ====================
    private String mysqlUser;
    private String mysqlPassword;
    private String mysqlHost;
    private Integer mysqlPort;
    private Boolean mysqlCollectSlaveStatus;           // --collect.slave_status
    private Boolean mysqlCollectInfoSchemaTables;      // --collect.info_schema.tables
    private Boolean mysqlCollectPerfSchemaEvents;      // --collect.perf_schema.eventswaits
    private Boolean mysqlCollectAutoIncrementColumns;  // --collect.auto_increment.columns
    private Boolean mysqlCollectBinlogSize;            // --collect.binlog_size
    private Boolean mysqlCollectGlobalVariables;       // --collect.global_variables
    private Boolean mysqlCollectGlobalStatus;          // --collect.global_status
    private Integer mysqlLockWaitTimeout;              // --exporter.lock_wait_timeout

    // ==================== Redis Exporter ====================
    private String redisAddr;
    private String redisPassword;
    private Boolean redisIsCluster;            // --is-cluster
    private Boolean redisIsSentinel;           // --is-sentinel
    private Boolean redisIncludeSystemMetrics; // --include-system-metrics
    private String redisConnectionTimeout;     // --connection-timeout, e.g. "15s"
    private String redisNamespace;             // --namespace
    private String redisCheckKeys;             // --check-keys
    private Boolean redisSkipTlsVerification;  // --skip-tls-verification

    // ==================== Nginx Exporter ====================
    private String nginxScrapeUri;
    private Boolean nginxIsPlus;          // --nginx.plus
    private String nginxTimeout;          // --nginx.timeout, e.g. "5s"
    private Integer nginxRetries;         // --nginx.retries
    private String nginxRetryInterval;    // --nginx.retry-interval, e.g. "0s"
    private Boolean nginxSslVerify;       // --nginx.ssl-verify
}
