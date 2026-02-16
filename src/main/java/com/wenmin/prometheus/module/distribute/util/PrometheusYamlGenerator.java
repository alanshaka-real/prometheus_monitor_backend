package com.wenmin.prometheus.module.distribute.util;

import com.wenmin.prometheus.module.distribute.dto.PrometheusConfigDTO;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

public class PrometheusYamlGenerator {

    private PrometheusYamlGenerator() {}

    /**
     * Generate prometheus.yml content from config DTO.
     * If rawYaml is set, return it directly (user-uploaded file takes priority).
     */
    public static String generate(PrometheusConfigDTO config) {
        if (config == null) {
            return generateDefault();
        }
        if (config.getRawYaml() != null && !config.getRawYaml().isBlank()) {
            return config.getRawYaml();
        }

        Map<String, Object> root = new LinkedHashMap<>();

        // global
        Map<String, Object> global = new LinkedHashMap<>();
        PrometheusConfigDTO.GlobalConfig gc = config.getGlobal();
        if (gc == null) gc = new PrometheusConfigDTO.GlobalConfig();
        global.put("scrape_interval", gc.getScrapeInterval());
        global.put("scrape_timeout", gc.getScrapeTimeout());
        global.put("evaluation_interval", gc.getEvaluationInterval());
        if (gc.getExternalLabels() != null && !gc.getExternalLabels().isEmpty()) {
            global.put("external_labels", new LinkedHashMap<>(gc.getExternalLabels()));
        }
        root.put("global", global);

        // alerting
        if (config.getAlerting() != null && config.getAlerting().getAlertmanagers() != null
                && !config.getAlerting().getAlertmanagers().isEmpty()) {
            Map<String, Object> alerting = new LinkedHashMap<>();
            List<Map<String, Object>> alertmanagers = new ArrayList<>();
            for (PrometheusConfigDTO.AlertmanagerStaticConfig am : config.getAlerting().getAlertmanagers()) {
                Map<String, Object> amMap = new LinkedHashMap<>();
                if (am.getStaticConfigs() != null) {
                    amMap.put("static_configs", buildStaticConfigs(am.getStaticConfigs()));
                }
                alertmanagers.add(amMap);
            }
            alerting.put("alertmanagers", alertmanagers);
            root.put("alerting", alerting);
        }

        // rule_files
        if (config.getRuleFiles() != null && !config.getRuleFiles().isEmpty()) {
            root.put("rule_files", new ArrayList<>(config.getRuleFiles()));
        }

        // scrape_configs
        List<Map<String, Object>> scrapeConfigs = new ArrayList<>();
        if (config.getScrapeConfigs() != null && !config.getScrapeConfigs().isEmpty()) {
            for (PrometheusConfigDTO.ScrapeConfig sc : config.getScrapeConfigs()) {
                scrapeConfigs.add(buildScrapeConfig(sc));
            }
        } else {
            // Default: self-monitoring job
            Map<String, Object> selfJob = new LinkedHashMap<>();
            selfJob.put("job_name", "prometheus");
            List<Map<String, Object>> statics = new ArrayList<>();
            Map<String, Object> staticCfg = new LinkedHashMap<>();
            staticCfg.put("targets", List.of("localhost:9090"));
            statics.add(staticCfg);
            selfJob.put("static_configs", statics);
            scrapeConfigs.add(selfJob);
        }
        root.put("scrape_configs", scrapeConfigs);

        // remote_write
        if (config.getRemoteWrite() != null && !config.getRemoteWrite().isEmpty()) {
            List<Map<String, Object>> rwList = new ArrayList<>();
            for (PrometheusConfigDTO.RemoteWriteConfig rw : config.getRemoteWrite()) {
                Map<String, Object> rwMap = new LinkedHashMap<>();
                rwMap.put("url", rw.getUrl());
                if (rw.getQueueConfig() != null && !rw.getQueueConfig().isEmpty()) {
                    rwMap.put("queue_config", new LinkedHashMap<>(rw.getQueueConfig()));
                }
                rwList.add(rwMap);
            }
            root.put("remote_write", rwList);
        }

        // remote_read
        if (config.getRemoteRead() != null && !config.getRemoteRead().isEmpty()) {
            List<Map<String, Object>> rrList = new ArrayList<>();
            for (PrometheusConfigDTO.RemoteReadConfig rr : config.getRemoteRead()) {
                Map<String, Object> rrMap = new LinkedHashMap<>();
                rrMap.put("url", rr.getUrl());
                rrMap.put("read_recent", rr.isReadRecent());
                rrList.add(rrMap);
            }
            root.put("remote_read", rrList);
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);
        return yaml.dump(root);
    }

    /**
     * Build CLI flags string for systemd ExecStart.
     */
    public static String buildCliFlags(PrometheusConfigDTO config, String installDir) {
        if (config == null) config = new PrometheusConfigDTO();
        PrometheusConfigDTO.CliFlags flags = config.getCliFlags();
        if (flags == null) flags = new PrometheusConfigDTO.CliFlags();

        String dataDir = config.getDataDir();
        if (dataDir == null || dataDir.isBlank()) {
            dataDir = installDir + "/data";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--config.file=").append(installDir).append("/prometheus.yml");
        sb.append(" \\\n  --storage.tsdb.path=").append(dataDir);

        if (flags.getStorageTsdbRetentionTime() != null && !flags.getStorageTsdbRetentionTime().isBlank()) {
            sb.append(" \\\n  --storage.tsdb.retention.time=").append(flags.getStorageTsdbRetentionTime());
        }
        if (flags.getStorageTsdbRetentionSize() != null && !flags.getStorageTsdbRetentionSize().isBlank()) {
            sb.append(" \\\n  --storage.tsdb.retention.size=").append(flags.getStorageTsdbRetentionSize());
        }
        if (Boolean.TRUE.equals(flags.getStorageWalCompression())) {
            sb.append(" \\\n  --storage.tsdb.wal-compression");
        }

        if (flags.getWebListenAddress() != null && !flags.getWebListenAddress().isBlank()) {
            sb.append(" \\\n  --web.listen-address=").append(flags.getWebListenAddress());
        }
        if (flags.getWebExternalUrl() != null && !flags.getWebExternalUrl().isBlank()) {
            sb.append(" \\\n  --web.external-url=").append(flags.getWebExternalUrl());
        }
        if (Boolean.TRUE.equals(flags.getWebEnableLifecycle())) {
            sb.append(" \\\n  --web.enable-lifecycle");
        }
        if (Boolean.TRUE.equals(flags.getWebEnableAdminApi())) {
            sb.append(" \\\n  --web.enable-admin-api");
        }

        if (flags.getQueryTimeout() != null && !flags.getQueryTimeout().isBlank()) {
            sb.append(" \\\n  --query.timeout=").append(flags.getQueryTimeout());
        }
        if (flags.getQueryMaxConcurrency() != null) {
            sb.append(" \\\n  --query.max-concurrency=").append(flags.getQueryMaxConcurrency());
        }

        if (flags.getLogLevel() != null && !flags.getLogLevel().isBlank()) {
            sb.append(" \\\n  --log.level=").append(flags.getLogLevel());
        }
        if (flags.getLogFormat() != null && !flags.getLogFormat().isBlank()) {
            sb.append(" \\\n  --log.format=").append(flags.getLogFormat());
        }

        return sb.toString();
    }

    private static String generateDefault() {
        PrometheusConfigDTO defaultConfig = new PrometheusConfigDTO();
        defaultConfig.setGlobal(new PrometheusConfigDTO.GlobalConfig());
        return generate(defaultConfig);
    }

    private static Map<String, Object> buildScrapeConfig(PrometheusConfigDTO.ScrapeConfig sc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("job_name", sc.getJobName());
        if (sc.getMetricsPath() != null && !"/metrics".equals(sc.getMetricsPath())) {
            map.put("metrics_path", sc.getMetricsPath());
        }
        if (sc.getScheme() != null && !"http".equals(sc.getScheme())) {
            map.put("scheme", sc.getScheme());
        }
        if (sc.getScrapeInterval() != null && !sc.getScrapeInterval().isBlank()) {
            map.put("scrape_interval", sc.getScrapeInterval());
        }
        if (sc.getScrapeTimeout() != null && !sc.getScrapeTimeout().isBlank()) {
            map.put("scrape_timeout", sc.getScrapeTimeout());
        }
        if (sc.getStaticConfigs() != null && !sc.getStaticConfigs().isEmpty()) {
            map.put("static_configs", buildStaticConfigs(sc.getStaticConfigs()));
        }
        return map;
    }

    private static List<Map<String, Object>> buildStaticConfigs(List<PrometheusConfigDTO.StaticConfig> configs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PrometheusConfigDTO.StaticConfig sc : configs) {
            Map<String, Object> map = new LinkedHashMap<>();
            if (sc.getTargets() != null) {
                map.put("targets", new ArrayList<>(sc.getTargets()));
            }
            if (sc.getLabels() != null && !sc.getLabels().isEmpty()) {
                map.put("labels", new LinkedHashMap<>(sc.getLabels()));
            }
            result.add(map);
        }
        return result;
    }
}
