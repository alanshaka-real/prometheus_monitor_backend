package com.wenmin.prometheus.module.distribute.util;

import com.wenmin.prometheus.module.distribute.dto.ComponentConfigDTO;

import java.util.Map;

/**
 * Static utility class for generating CLI arguments, configuration files,
 * and systemd service units for non-Prometheus components.
 *
 * Follows the same pattern as {@link PrometheusYamlGenerator}.
 */
public class ComponentConfigGenerator {

    private ComponentConfigGenerator() {}

    /** component name → actual binary name (only when they differ) */
    private static final Map<String, String> BINARY_NAME_MAP = Map.of(
            "process_exporter", "process-exporter"
    );

    private static final Map<String, Integer> DEFAULT_PORTS = Map.of(
            "node_exporter", 9100,
            "blackbox_exporter", 9115,
            "process_exporter", 9256,
            "cadvisor", 8080,
            "mysql_exporter", 9104,
            "redis_exporter", 9121,
            "nginx_exporter", 9113
    );

    public static int getDefaultPort(String component) {
        return DEFAULT_PORTS.getOrDefault(component, 9100);
    }

    /**
     * Build the ExecStart CLI command for a given component.
     */
    public static String buildExecStart(String component, String installDir, ComponentConfigDTO config) {
        if (config == null) config = new ComponentConfigDTO();

        int port = config.getPort() != null ? config.getPort() : getDefaultPort(component);
        String logLevel = config.getLogLevel();

        StringBuilder sb = new StringBuilder();
        String binary = BINARY_NAME_MAP.getOrDefault(component, component);
        sb.append(installDir).append("/").append(binary);

        switch (component) {
            case "node_exporter":
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
                }
                if (config.getDisabledCollectors() != null && !config.getDisabledCollectors().isBlank()) {
                    for (String collector : config.getDisabledCollectors().split(",")) {
                        String trimmed = collector.trim();
                        if (!trimmed.isEmpty()) {
                            sb.append(" \\\n  --no-collector.").append(trimmed);
                        }
                    }
                }
                break;

            case "blackbox_exporter":
                sb.append(" \\\n  --config.file=").append(installDir).append("/blackbox.yml");
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
                }
                break;

            case "process_exporter":
                // process-exporter uses single-dash flags
                sb.append(" \\\n  -web.listen-address=:").append(port);
                if (Boolean.TRUE.equals(config.getProcessUseConfig())) {
                    sb.append(" \\\n  -config.path=").append(installDir).append("/process_exporter_config.yml");
                } else if (config.getProcessNames() != null && !config.getProcessNames().isBlank()) {
                    sb.append(" \\\n  -procnames=").append(config.getProcessNames().trim());
                }
                if (Boolean.FALSE.equals(config.getProcessChildren())) {
                    sb.append(" \\\n  -children=false");
                }
                if (Boolean.FALSE.equals(config.getProcessThreads())) {
                    sb.append(" \\\n  -threads=false");
                }
                if (Boolean.TRUE.equals(config.getProcessRecheck())) {
                    sb.append(" \\\n  -recheck");
                }
                if (Boolean.FALSE.equals(config.getProcessGatherSmaps())) {
                    sb.append(" \\\n  -gather-smaps=false");
                }
                break;

            case "cadvisor":
                // cAdvisor uses --port= instead of --web.listen-address=:
                sb.append(" \\\n  --port=").append(port);
                if (Boolean.TRUE.equals(config.getDockerOnly())) {
                    sb.append(" \\\n  --docker_only");
                }
                if (config.getHousekeepingInterval() != null && !config.getHousekeepingInterval().isBlank()) {
                    sb.append(" \\\n  --housekeeping_interval=").append(config.getHousekeepingInterval().trim());
                }
                if (config.getCadvisorStorageDuration() != null && !config.getCadvisorStorageDuration().isBlank()) {
                    sb.append(" \\\n  --storage_duration=").append(config.getCadvisorStorageDuration().trim());
                }
                if (config.getCadvisorDisableMetrics() != null && !config.getCadvisorDisableMetrics().isBlank()) {
                    sb.append(" \\\n  --disable_metrics=").append(config.getCadvisorDisableMetrics().trim());
                }
                if (Boolean.TRUE.equals(config.getCadvisorEnableLoadReader())) {
                    sb.append(" \\\n  --enable_load_reader");
                }
                if (Boolean.FALSE.equals(config.getCadvisorStoreContainerLabels())) {
                    sb.append(" \\\n  --store_container_labels=false");
                }
                if (config.getCadvisorWhitelistedLabels() != null && !config.getCadvisorWhitelistedLabels().isBlank()) {
                    sb.append(" \\\n  --whitelisted_container_labels=").append(config.getCadvisorWhitelistedLabels().trim());
                }
                if (config.getCadvisorMaxProcs() != null) {
                    sb.append(" \\\n  --max_procs=").append(config.getCadvisorMaxProcs());
                }
                break;

            case "mysql_exporter":
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
                }
                // Only output flags that differ from exporter built-in defaults
                if (Boolean.TRUE.equals(config.getMysqlCollectSlaveStatus())) {
                    sb.append(" \\\n  --collect.slave_status");
                }
                if (Boolean.TRUE.equals(config.getMysqlCollectInfoSchemaTables())) {
                    sb.append(" \\\n  --collect.info_schema.tables");
                }
                if (Boolean.TRUE.equals(config.getMysqlCollectPerfSchemaEvents())) {
                    sb.append(" \\\n  --collect.perf_schema.eventswaits");
                }
                if (Boolean.TRUE.equals(config.getMysqlCollectAutoIncrementColumns())) {
                    sb.append(" \\\n  --collect.auto_increment.columns");
                }
                if (Boolean.TRUE.equals(config.getMysqlCollectBinlogSize())) {
                    sb.append(" \\\n  --collect.binlog_size");
                }
                if (Boolean.FALSE.equals(config.getMysqlCollectGlobalVariables())) {
                    sb.append(" \\\n  --no-collect.global_variables");
                }
                if (Boolean.FALSE.equals(config.getMysqlCollectGlobalStatus())) {
                    sb.append(" \\\n  --no-collect.global_status");
                }
                if (config.getMysqlLockWaitTimeout() != null) {
                    sb.append(" \\\n  --exporter.lock_wait_timeout=").append(config.getMysqlLockWaitTimeout());
                }
                break;

            case "redis_exporter":
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
                }
                if (config.getRedisAddr() != null && !config.getRedisAddr().isBlank()) {
                    sb.append(" \\\n  --redis.addr=").append(config.getRedisAddr().trim());
                }
                if (config.getRedisPassword() != null && !config.getRedisPassword().isBlank()) {
                    sb.append(" \\\n  --redis.password=").append(config.getRedisPassword());
                }
                if (Boolean.TRUE.equals(config.getRedisIsCluster())) {
                    sb.append(" \\\n  --is-cluster");
                }
                if (Boolean.TRUE.equals(config.getRedisIsSentinel())) {
                    sb.append(" \\\n  --is-sentinel");
                }
                if (Boolean.TRUE.equals(config.getRedisIncludeSystemMetrics())) {
                    sb.append(" \\\n  --include-system-metrics");
                }
                if (config.getRedisConnectionTimeout() != null && !config.getRedisConnectionTimeout().isBlank()) {
                    sb.append(" \\\n  --connection-timeout=").append(config.getRedisConnectionTimeout().trim());
                }
                if (config.getRedisNamespace() != null && !config.getRedisNamespace().isBlank()
                        && !"redis".equals(config.getRedisNamespace().trim())) {
                    sb.append(" \\\n  --namespace=").append(config.getRedisNamespace().trim());
                }
                if (config.getRedisCheckKeys() != null && !config.getRedisCheckKeys().isBlank()) {
                    sb.append(" \\\n  --check-keys=").append(config.getRedisCheckKeys().trim());
                }
                if (Boolean.TRUE.equals(config.getRedisSkipTlsVerification())) {
                    sb.append(" \\\n  --skip-tls-verification");
                }
                break;

            case "nginx_exporter":
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
                }
                if (config.getNginxScrapeUri() != null && !config.getNginxScrapeUri().isBlank()) {
                    sb.append(" \\\n  --nginx.scrape-uri=").append(config.getNginxScrapeUri().trim());
                }
                if (Boolean.TRUE.equals(config.getNginxIsPlus())) {
                    sb.append(" \\\n  --nginx.plus");
                }
                if (config.getNginxTimeout() != null && !config.getNginxTimeout().isBlank()) {
                    sb.append(" \\\n  --nginx.timeout=").append(config.getNginxTimeout().trim());
                }
                if (config.getNginxRetries() != null && config.getNginxRetries() > 0) {
                    sb.append(" \\\n  --nginx.retries=").append(config.getNginxRetries());
                }
                if (config.getNginxRetryInterval() != null && !config.getNginxRetryInterval().isBlank()
                        && !"0s".equals(config.getNginxRetryInterval().trim())) {
                    sb.append(" \\\n  --nginx.retry-interval=").append(config.getNginxRetryInterval().trim());
                }
                if (Boolean.FALSE.equals(config.getNginxSslVerify())) {
                    sb.append(" \\\n  --nginx.ssl-verify=false");
                }
                break;

            default:
                sb.append(" \\\n  --web.listen-address=:").append(port);
                break;
        }

        return sb.toString();
    }

    /**
     * Generate blackbox.yml with configurable probe modules.
     * When config is null, outputs the same default YAML as before.
     */
    public static String generateBlackboxYaml(ComponentConfigDTO config) {
        String timeout = "5s";
        String httpMethod = "GET";
        String validCodes = "";
        boolean followRedirects = true;
        String preferredIp = "ip4";
        boolean tlsInsecure = false;
        boolean enableIcmp = true;
        boolean enableTcp = true;

        if (config != null) {
            if (config.getBlackboxTimeout() != null && !config.getBlackboxTimeout().isBlank()) {
                timeout = config.getBlackboxTimeout().trim();
            }
            if (config.getBlackboxProbeHttpMethod() != null && !config.getBlackboxProbeHttpMethod().isBlank()) {
                httpMethod = config.getBlackboxProbeHttpMethod().trim().toUpperCase();
            }
            if (config.getBlackboxProbeHttpValidCodes() != null && !config.getBlackboxProbeHttpValidCodes().isBlank()) {
                validCodes = config.getBlackboxProbeHttpValidCodes().trim();
            }
            if (config.getBlackboxProbeFollowRedirects() != null) {
                followRedirects = config.getBlackboxProbeFollowRedirects();
            }
            if (config.getBlackboxProbePreferredIp() != null && !config.getBlackboxProbePreferredIp().isBlank()) {
                preferredIp = config.getBlackboxProbePreferredIp().trim();
            }
            if (config.getBlackboxProbeTlsInsecure() != null) {
                tlsInsecure = config.getBlackboxProbeTlsInsecure();
            }
            if (config.getBlackboxEnableIcmp() != null) {
                enableIcmp = config.getBlackboxEnableIcmp();
            }
            if (config.getBlackboxEnableTcp() != null) {
                enableTcp = config.getBlackboxEnableTcp();
            }
        }

        // Build valid_status_codes array string
        String codesArray;
        if (validCodes.isEmpty()) {
            codesArray = "[]";
        } else {
            StringBuilder cb = new StringBuilder("[");
            String[] parts = validCodes.split(",");
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) cb.append(", ");
                cb.append(parts[i].trim());
            }
            cb.append("]");
            codesArray = cb.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("modules:\n");

        // http_2xx module
        sb.append("  http_2xx:\n");
        sb.append("    prober: http\n");
        sb.append("    timeout: ").append(timeout).append("\n");
        sb.append("    http:\n");
        sb.append("      method: ").append(httpMethod).append("\n");
        sb.append("      valid_http_versions: [\"HTTP/1.1\", \"HTTP/2.0\"]\n");
        sb.append("      valid_status_codes: ").append(codesArray).append("\n");
        sb.append("      follow_redirects: ").append(followRedirects).append("\n");
        sb.append("      preferred_ip_protocol: ").append(preferredIp).append("\n");
        if (tlsInsecure) {
            sb.append("      tls_config:\n");
            sb.append("        insecure_skip_verify: true\n");
        }

        // http_post_2xx module
        sb.append("  http_post_2xx:\n");
        sb.append("    prober: http\n");
        sb.append("    timeout: ").append(timeout).append("\n");
        sb.append("    http:\n");
        sb.append("      method: POST\n");
        sb.append("      valid_http_versions: [\"HTTP/1.1\", \"HTTP/2.0\"]\n");
        sb.append("      valid_status_codes: ").append(codesArray).append("\n");
        if (tlsInsecure) {
            sb.append("      tls_config:\n");
            sb.append("        insecure_skip_verify: true\n");
        }

        // tcp_connect module
        if (enableTcp) {
            sb.append("  tcp_connect:\n");
            sb.append("    prober: tcp\n");
            sb.append("    timeout: ").append(timeout).append("\n");
        }

        // icmp module
        if (enableIcmp) {
            sb.append("  icmp:\n");
            sb.append("    prober: icmp\n");
            sb.append("    timeout: ").append(timeout).append("\n");
        }

        return sb.toString();
    }

    /**
     * Overload: generate default blackbox.yml (backward compatible).
     */
    public static String generateBlackboxYaml() {
        return generateBlackboxYaml(null);
    }

    /**
     * Generate process_exporter_config.yml content from the DTO's processConfigYaml field.
     * Returns null if not using config mode or no YAML content provided.
     */
    public static String generateProcessExporterConfig(ComponentConfigDTO config) {
        if (config == null || !Boolean.TRUE.equals(config.getProcessUseConfig())) {
            return null;
        }
        if (config.getProcessConfigYaml() != null && !config.getProcessConfigYaml().isBlank()) {
            return config.getProcessConfigYaml();
        }
        // Provide a sensible default config
        return "process_names:\n"
                + "  - name: \"{{.Comm}}\"\n"
                + "    cmdline:\n"
                + "    - '.+'\n";
    }

    /**
     * Generate MySQL .my.cnf credentials file content.
     */
    public static String generateMyCnf(ComponentConfigDTO config) {
        String user = config.getMysqlUser() != null ? config.getMysqlUser() : "exporter";
        String password = config.getMysqlPassword() != null ? config.getMysqlPassword() : "";
        String host = config.getMysqlHost() != null && !config.getMysqlHost().isBlank()
                ? config.getMysqlHost() : "localhost";
        int port = config.getMysqlPort() != null ? config.getMysqlPort() : 3306;

        return "[client]\n"
                + "user=" + user + "\n"
                + "password=" + password + "\n"
                + "host=" + host + "\n"
                + "port=" + port + "\n";
    }

    /**
     * Build the DATA_SOURCE_NAME environment variable for mysql_exporter.
     */
    public static String buildMysqlDataSourceName(ComponentConfigDTO config) {
        String user = config.getMysqlUser() != null ? config.getMysqlUser() : "exporter";
        String password = config.getMysqlPassword() != null ? config.getMysqlPassword() : "";
        String host = config.getMysqlHost() != null && !config.getMysqlHost().isBlank()
                ? config.getMysqlHost() : "localhost";
        int port = config.getMysqlPort() != null ? config.getMysqlPort() : 3306;

        return user + ":" + password + "@tcp(" + host + ":" + port + ")/";
    }

    /**
     * Generate a complete systemd unit file for a non-Prometheus component.
     */
    public static String generateSystemdService(String component, String installDir,
                                                  ComponentConfigDTO config, String sshUsername) {
        if (config == null) config = new ComponentConfigDTO();
        String execStart = buildExecStart(component, installDir, config);

        StringBuilder sb = new StringBuilder();
        sb.append("[Unit]\n");
        sb.append("Description=").append(component).append("\n");
        sb.append("After=network.target\n");
        sb.append("\n");
        sb.append("[Service]\n");
        sb.append("Type=simple\n");
        sb.append("User=").append(sshUsername).append("\n");

        // MySQL Exporter: add DATA_SOURCE_NAME environment variable
        if ("mysql_exporter".equals(component)) {
            String dsn = buildMysqlDataSourceName(config);
            sb.append("Environment=DATA_SOURCE_NAME=").append(dsn).append("\n");
        }

        sb.append("ExecStart=").append(execStart).append("\n");
        sb.append("Restart=always\n");
        sb.append("RestartSec=5\n");
        sb.append("\n");
        sb.append("[Install]\n");
        sb.append("WantedBy=multi-user.target\n");

        return sb.toString();
    }
}
