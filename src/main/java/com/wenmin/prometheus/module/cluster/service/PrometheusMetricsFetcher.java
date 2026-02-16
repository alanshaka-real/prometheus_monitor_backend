package com.wenmin.prometheus.module.cluster.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenmin.prometheus.module.cluster.dto.DiscoveredNode;
import com.wenmin.prometheus.module.cluster.dto.NodeMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricsFetcher {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Execute an instant query against Prometheus.
     * Returns the "result" array from the response.
     */
    public List<JsonNode> queryInstant(String prometheusUrl, String promql) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(prometheusUrl)
                    .path("/api/v1/query")
                    .queryParam("query", promql)
                    .build()
                    .encode()
                    .toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"success".equals(root.path("status").asText())) {
                log.warn("Prometheus query failed: {}", root.path("error").asText());
                return Collections.emptyList();
            }
            JsonNode result = root.path("data").path("result");
            List<JsonNode> list = new ArrayList<>();
            for (JsonNode node : result) {
                list.add(node);
            }
            return list;
        } catch (Exception e) {
            log.error("Prometheus query error for [{}]: {}", promql, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch all node_exporter metrics in batch and return a map keyed by IP.
     */
    public Map<String, NodeMetricsDTO> fetchAllNodeMetrics(String prometheusUrl) {
        Map<String, NodeMetricsDTO> metricsMap = new HashMap<>();

        // 1. CPU%
        Map<String, Double> cpuPercent = queryMetricByInstance(prometheusUrl,
                "100 - (avg by(instance)(rate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)");

        // 2. CPU cores
        Map<String, Double> cpuCores = queryMetricByInstance(prometheusUrl,
                "count by(instance)(node_cpu_seconds_total{mode=\"idle\"})");

        // 3. Memory total
        Map<String, Double> memTotal = queryMetricByInstance(prometheusUrl,
                "node_memory_MemTotal_bytes");

        // 4. Memory available
        Map<String, Double> memAvailable = queryMetricByInstance(prometheusUrl,
                "node_memory_MemAvailable_bytes");

        // 5. Disk total
        Map<String, Double> diskTotal = queryMetricByInstance(prometheusUrl,
                "node_filesystem_size_bytes{mountpoint=\"/\",fstype!~\"tmpfs|overlay\"}");

        // 6. Disk available
        Map<String, Double> diskAvailable = queryMetricByInstance(prometheusUrl,
                "node_filesystem_avail_bytes{mountpoint=\"/\",fstype!~\"tmpfs|overlay\"}");

        // 7. Network RX
        Map<String, Double> rxRate = queryMetricByInstance(prometheusUrl,
                "sum by(instance)(rate(node_network_receive_bytes_total{device!~\"lo|veth.*|docker.*|br-.*\"}[5m]))");

        // 8. Network TX
        Map<String, Double> txRate = queryMetricByInstance(prometheusUrl,
                "sum by(instance)(rate(node_network_transmit_bytes_total{device!~\"lo|veth.*|docker.*|br-.*\"}[5m]))");

        // 9. Node status (up)
        Map<String, Double> upMap = queryMetricByInstance(prometheusUrl,
                "up{job=~\"node.*\"}");

        // 10. Boot time
        Map<String, Double> bootTime = queryMetricByInstance(prometheusUrl,
                "node_boot_time_seconds");

        // 11. OS/Kernel info from node_uname_info
        Map<String, Map<String, String>> unameInfo = queryUnameInfo(prometheusUrl);

        // Collect all known IPs
        Set<String> allIps = new HashSet<>();
        allIps.addAll(cpuPercent.keySet());
        allIps.addAll(memTotal.keySet());
        allIps.addAll(upMap.keySet());
        allIps.addAll(bootTime.keySet());

        for (String ip : allIps) {
            NodeMetricsDTO dto = new NodeMetricsDTO();
            dto.setCpuPercent(cpuPercent.getOrDefault(ip, 0.0));
            dto.setCpuCores(cpuCores.getOrDefault(ip, 0.0).intValue());
            dto.setMemoryTotal(memTotal.getOrDefault(ip, 0.0).longValue());
            dto.setMemoryAvailable(memAvailable.getOrDefault(ip, 0.0).longValue());
            dto.setDiskTotal(diskTotal.getOrDefault(ip, 0.0).longValue());
            dto.setDiskAvailable(diskAvailable.getOrDefault(ip, 0.0).longValue());
            dto.setRxRate(rxRate.getOrDefault(ip, 0.0));
            dto.setTxRate(txRate.getOrDefault(ip, 0.0));
            dto.setUp(upMap.getOrDefault(ip, 0.0).intValue());
            dto.setBootTime(bootTime.getOrDefault(ip, 0.0).longValue());

            Map<String, String> uname = unameInfo.get(ip);
            if (uname != null) {
                dto.setOs(uname.getOrDefault("nodename", ""));
                dto.setKernel(uname.getOrDefault("release", ""));
                dto.setNodename(uname.getOrDefault("nodename", ""));
            }

            metricsMap.put(ip, dto);
        }

        return metricsMap;
    }

    /**
     * Discover node_exporter targets from Prometheus /api/v1/targets.
     */
    public List<DiscoveredNode> discoverNodes(String prometheusUrl) {
        List<DiscoveredNode> nodes = new ArrayList<>();
        try {
            String url = prometheusUrl + "/api/v1/targets";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode activeTargets = root.path("data").path("activeTargets");

            for (JsonNode target : activeTargets) {
                String job = target.path("labels").path("job").asText("");
                if (!job.contains("node")) {
                    continue;
                }

                DiscoveredNode dn = new DiscoveredNode();
                String instance = target.path("labels").path("instance").asText("");
                dn.setInstance(instance);
                dn.setIp(extractIp(instance));
                dn.setHostname(instance);
                dn.setJob(job);
                dn.setStatus(target.path("health").asText("unknown"));

                Map<String, String> labels = new HashMap<>();
                JsonNode labelsNode = target.path("labels");
                labelsNode.fieldNames().forEachRemaining(field ->
                        labels.put(field, labelsNode.get(field).asText()));
                dn.setLabels(labels);

                nodes.add(dn);
            }
        } catch (Exception e) {
            log.error("Failed to discover nodes from {}: {}", prometheusUrl, e.getMessage());
        }
        return nodes;
    }

    /**
     * Check if a Prometheus instance is reachable.
     */
    public boolean isReachable(String prometheusUrl) {
        try {
            String url = prometheusUrl + "/api/v1/status/buildinfo";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return "success".equals(root.path("status").asText());
        } catch (Exception e) {
            log.debug("Prometheus not reachable at {}: {}", prometheusUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Query a metric and return Map<IP, value>.
     */
    private Map<String, Double> queryMetricByInstance(String prometheusUrl, String promql) {
        Map<String, Double> result = new HashMap<>();
        List<JsonNode> data = queryInstant(prometheusUrl, promql);
        for (JsonNode item : data) {
            String instance = item.path("metric").path("instance").asText("");
            String ip = extractIp(instance);
            JsonNode valueArray = item.path("value");
            if (valueArray.isArray() && valueArray.size() >= 2) {
                try {
                    double val = Double.parseDouble(valueArray.get(1).asText("0"));
                    result.put(ip, val);
                } catch (NumberFormatException e) {
                    // skip NaN or invalid
                }
            }
        }
        return result;
    }

    /**
     * Query node_uname_info and return Map<IP, {sysname, release, nodename, ...}>.
     */
    private Map<String, Map<String, String>> queryUnameInfo(String prometheusUrl) {
        Map<String, Map<String, String>> result = new HashMap<>();
        List<JsonNode> data = queryInstant(prometheusUrl, "node_uname_info");
        for (JsonNode item : data) {
            JsonNode metric = item.path("metric");
            String instance = metric.path("instance").asText("");
            String ip = extractIp(instance);

            Map<String, String> info = new HashMap<>();
            info.put("sysname", metric.path("sysname").asText(""));
            info.put("release", metric.path("release").asText(""));
            info.put("nodename", metric.path("nodename").asText(""));
            info.put("machine", metric.path("machine").asText(""));
            result.put(ip, info);
        }
        return result;
    }

    /**
     * Extract IP from instance string, e.g. "10.0.1.10:9100" -> "10.0.1.10".
     */
    public static String extractIp(String instance) {
        if (instance == null || instance.isEmpty()) return "";
        int colonIndex = instance.lastIndexOf(':');
        if (colonIndex > 0) {
            return instance.substring(0, colonIndex);
        }
        return instance;
    }
}
