package com.wenmin.prometheus.module.distribute.config;

import java.util.List;
import java.util.Map;

/**
 * GitHub Release 组件映射配置
 * 根据 download.sh 中已验证的命名规则，维护每个组件到 GitHub 仓库的映射关系
 */
public class GitHubReleaseConfig {

    /**
     * 组件配置信息
     */
    public record ComponentConfig(
            String localName,
            String owner,
            String repo,
            String displayName,
            List<String> architectures,
            NamingStrategy namingStrategy
    ) {}

    /**
     * 命名策略枚举
     */
    public enum NamingStrategy {
        /** 直接匹配: {repo}-{version}.linux-{arch}.tar.gz */
        DIRECT,
        /** process-exporter → process_exporter */
        PROCESS_EXPORTER,
        /** mysqld_exporter → mysql_exporter */
        MYSQL_EXPORTER,
        /** redis_exporter: tag 无 v 前缀, 文件名含 v 需去掉 */
        REDIS_EXPORTER,
        /** nginx-prometheus-exporter_{ver}_linux_{arch}.tar.gz → nginx_exporter-{ver}.linux-{arch}.tar.gz */
        NGINX_EXPORTER,
        /** 裸二进制，需打包为 tar.gz */
        CADVISOR
    }

    /** 全部组件配置 */
    public static final Map<String, ComponentConfig> COMPONENTS = Map.of(
            "prometheus", new ComponentConfig(
                    "prometheus", "prometheus", "prometheus", "Prometheus Server",
                    List.of("386", "amd64", "arm64", "armv5", "armv6", "armv7",
                            "mips", "mips64", "mips64le", "mipsle", "ppc64", "ppc64le", "riscv64", "s390x"),
                    NamingStrategy.DIRECT
            ),
            "node_exporter", new ComponentConfig(
                    "node_exporter", "prometheus", "node_exporter", "Node Exporter",
                    List.of("386", "amd64", "arm64", "armv5", "armv6", "armv7",
                            "mips", "mips64", "mips64le", "mipsle", "ppc64", "ppc64le", "riscv64", "s390x"),
                    NamingStrategy.DIRECT
            ),
            "blackbox_exporter", new ComponentConfig(
                    "blackbox_exporter", "prometheus", "blackbox_exporter", "Blackbox Exporter",
                    List.of("386", "amd64", "arm64", "armv5", "armv6", "armv7",
                            "mips", "mips64", "mips64le", "mipsle", "ppc64", "ppc64le", "riscv64", "s390x"),
                    NamingStrategy.DIRECT
            ),
            "process_exporter", new ComponentConfig(
                    "process_exporter", "ncabatoff", "process-exporter", "Process Exporter",
                    List.of("386", "amd64", "arm64", "armv6", "armv7", "ppc64", "ppc64le"),
                    NamingStrategy.PROCESS_EXPORTER
            ),
            "mysql_exporter", new ComponentConfig(
                    "mysql_exporter", "prometheus", "mysqld_exporter", "MySQL Exporter",
                    List.of("386", "amd64", "arm64", "armv5", "armv6", "armv7",
                            "mips", "mips64", "mips64le", "mipsle", "ppc64", "ppc64le", "riscv64", "s390x"),
                    NamingStrategy.MYSQL_EXPORTER
            ),
            "redis_exporter", new ComponentConfig(
                    "redis_exporter", "oliver006", "redis_exporter", "Redis Exporter",
                    List.of("386", "amd64", "arm", "arm64", "mips64", "mips64le", "ppc64", "ppc64le", "s390x"),
                    NamingStrategy.REDIS_EXPORTER
            ),
            "nginx_exporter", new ComponentConfig(
                    "nginx_exporter", "nginxinc", "nginx-prometheus-exporter", "Nginx Exporter",
                    List.of("386", "amd64", "arm64", "armv5", "armv6", "armv7",
                            "ppc64", "ppc64le", "riscv64", "s390x", "mips64", "mips64le"),
                    NamingStrategy.NGINX_EXPORTER
            ),
            "cadvisor", new ComponentConfig(
                    "cadvisor", "google", "cadvisor", "cAdvisor",
                    List.of("amd64", "arm64"),
                    NamingStrategy.CADVISOR
            )
    );

    /**
     * 获取 GitHub API URL
     */
    public static String getApiUrl(ComponentConfig config) {
        return String.format("https://api.github.com/repos/%s/%s/releases/latest",
                config.owner(), config.repo());
    }

    /**
     * 从 tag_name 解析版本号（去掉 v 前缀）
     */
    public static String parseVersion(String tagName) {
        return tagName.startsWith("v") ? tagName.substring(1) : tagName;
    }

    /**
     * 构建 GitHub 下载 URL
     */
    public static String buildDownloadUrl(ComponentConfig config, String tagName, String version, String arch) {
        String base = String.format("https://github.com/%s/%s/releases/download/%s",
                config.owner(), config.repo(), tagName);

        return switch (config.namingStrategy()) {
            case DIRECT ->
                    base + "/" + config.repo() + "-" + version + ".linux-" + arch + ".tar.gz";
            case PROCESS_EXPORTER ->
                    base + "/process-exporter-" + version + ".linux-" + arch + ".tar.gz";
            case MYSQL_EXPORTER ->
                    base + "/mysqld_exporter-" + version + ".linux-" + arch + ".tar.gz";
            case REDIS_EXPORTER ->
                    base + "/redis_exporter-" + tagName + ".linux-" + arch + ".tar.gz";
            case NGINX_EXPORTER -> {
                String actualArch = arch;
                if ("mips64".equals(arch) || "mips64le".equals(arch)) {
                    actualArch = arch + "_softfloat";
                }
                yield base + "/nginx-prometheus-exporter_" + version + "_linux_" + actualArch + ".tar.gz";
            }
            case CADVISOR ->
                    base + "/cadvisor-" + tagName + "-linux-" + arch;
        };
    }

    /**
     * 构建本地文件名（统一命名规范）
     */
    public static String buildLocalFileName(ComponentConfig config, String version, String arch) {
        return config.localName() + "-" + version + ".linux-" + arch + ".tar.gz";
    }
}
