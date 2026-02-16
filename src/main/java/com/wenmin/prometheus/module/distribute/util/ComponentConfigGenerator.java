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
        sb.append(installDir).append("/").append(component);

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
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
                }
                if (config.getProcessNames() != null && !config.getProcessNames().isBlank()) {
                    sb.append(" \\\n  --procnames=").append(config.getProcessNames().trim());
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
                break;

            case "mysql_exporter":
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
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
                break;

            case "nginx_exporter":
                sb.append(" \\\n  --web.listen-address=:").append(port);
                if (logLevel != null && !logLevel.isBlank()) {
                    sb.append(" \\\n  --log.level=").append(logLevel);
                }
                if (config.getNginxScrapeUri() != null && !config.getNginxScrapeUri().isBlank()) {
                    sb.append(" \\\n  --nginx.scrape-uri=").append(config.getNginxScrapeUri().trim());
                }
                break;

            default:
                sb.append(" \\\n  --web.listen-address=:").append(port);
                break;
        }

        return sb.toString();
    }

    /**
     * Generate default blackbox.yml with 4 probe modules:
     * http_2xx, http_post_2xx, tcp_connect, icmp.
     */
    public static String generateBlackboxYaml() {
        return "modules:\n"
                + "  http_2xx:\n"
                + "    prober: http\n"
                + "    timeout: 5s\n"
                + "    http:\n"
                + "      method: GET\n"
                + "      valid_http_versions: [\"HTTP/1.1\", \"HTTP/2.0\"]\n"
                + "      valid_status_codes: []\n"
                + "      follow_redirects: true\n"
                + "      preferred_ip_protocol: ip4\n"
                + "  http_post_2xx:\n"
                + "    prober: http\n"
                + "    timeout: 5s\n"
                + "    http:\n"
                + "      method: POST\n"
                + "      valid_http_versions: [\"HTTP/1.1\", \"HTTP/2.0\"]\n"
                + "      valid_status_codes: []\n"
                + "  tcp_connect:\n"
                + "    prober: tcp\n"
                + "    timeout: 5s\n"
                + "  icmp:\n"
                + "    prober: icmp\n"
                + "    timeout: 5s\n";
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
