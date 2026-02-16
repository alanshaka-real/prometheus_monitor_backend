package com.wenmin.prometheus.module.cluster.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenmin.prometheus.common.exception.BusinessException;
import com.wenmin.prometheus.module.cluster.dto.ClusterDTO;
import com.wenmin.prometheus.module.cluster.dto.DiscoveredNode;
import com.wenmin.prometheus.module.cluster.dto.NodeMetricsDTO;
import com.wenmin.prometheus.module.cluster.entity.PromCluster;
import com.wenmin.prometheus.module.cluster.entity.PromClusterNode;
import com.wenmin.prometheus.module.cluster.mapper.PromClusterMapper;
import com.wenmin.prometheus.module.cluster.mapper.PromClusterNodeMapper;
import com.wenmin.prometheus.module.cluster.service.ClusterService;
import com.wenmin.prometheus.module.cluster.service.PrometheusMetricsFetcher;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import com.wenmin.prometheus.module.datasource.mapper.PromInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterServiceImpl implements ClusterService {

    private final PromClusterMapper clusterMapper;
    private final PromClusterNodeMapper nodeMapper;
    private final PromInstanceMapper instanceMapper;
    private final PrometheusMetricsFetcher metricsFetcher;

    // ==================== Resolve Prometheus URL ====================

    private String resolvePrometheusUrl(PromCluster cluster) {
        // Prefer instanceId -> prom_instance.url, fallback to prometheusUrl
        if (StringUtils.hasText(cluster.getInstanceId())) {
            PromInstance instance = instanceMapper.selectById(cluster.getInstanceId());
            if (instance != null && StringUtils.hasText(instance.getUrl())) {
                return instance.getUrl();
            }
        }
        return cluster.getPrometheusUrl();
    }

    // ==================== Cluster List ====================

    @Override
    public Map<String, Object> listClusters() {
        LambdaQueryWrapper<PromCluster> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PromCluster::getCreatedAt);
        List<PromCluster> list = clusterMapper.selectList(wrapper);

        // Enrich with node count
        for (PromCluster cluster : list) {
            LambdaQueryWrapper<PromClusterNode> nodeWrapper = new LambdaQueryWrapper<>();
            nodeWrapper.eq(PromClusterNode::getClusterId, cluster.getId());
            long nodeCount = nodeMapper.selectCount(nodeWrapper);
            cluster.setNodes(null); // Don't load full nodes for list
            // Use a transient approach: embed count in the response
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromCluster getClusterById(String id) {
        PromCluster cluster = clusterMapper.selectById(id);
        if (cluster == null) {
            throw new BusinessException("集群不存在");
        }
        LambdaQueryWrapper<PromClusterNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(PromClusterNode::getClusterId, id);
        nodeWrapper.orderByAsc(PromClusterNode::getHostname);
        List<PromClusterNode> nodes = nodeMapper.selectList(nodeWrapper);
        cluster.setNodes(nodes);
        return cluster;
    }

    // ==================== Cluster Nodes (with real-time metrics) ====================

    @Override
    public Map<String, Object> getClusterNodes(String clusterId) {
        PromCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) {
            throw new BusinessException("集群不存在");
        }

        LambdaQueryWrapper<PromClusterNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromClusterNode::getClusterId, clusterId);
        wrapper.orderByAsc(PromClusterNode::getHostname);
        List<PromClusterNode> list = nodeMapper.selectList(wrapper);

        // Try to overlay real-time Prometheus metrics
        String promUrl = resolvePrometheusUrl(cluster);
        if (StringUtils.hasText(promUrl)) {
            try {
                Map<String, NodeMetricsDTO> liveMetrics = metricsFetcher.fetchAllNodeMetrics(promUrl);
                if (!liveMetrics.isEmpty()) {
                    for (PromClusterNode node : list) {
                        NodeMetricsDTO metrics = liveMetrics.get(node.getIp());
                        if (metrics != null) {
                            overlayMetrics(node, metrics);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch live metrics for cluster {}, using DB cache: {}",
                        clusterId, e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public PromClusterNode getNodeDetail(String id) {
        PromClusterNode node = nodeMapper.selectById(id);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }

        // Try to overlay real-time metrics
        if (StringUtils.hasText(node.getClusterId())) {
            PromCluster cluster = clusterMapper.selectById(node.getClusterId());
            if (cluster != null) {
                String promUrl = resolvePrometheusUrl(cluster);
                if (StringUtils.hasText(promUrl)) {
                    try {
                        Map<String, NodeMetricsDTO> liveMetrics = metricsFetcher.fetchAllNodeMetrics(promUrl);
                        NodeMetricsDTO metrics = liveMetrics.get(node.getIp());
                        if (metrics != null) {
                            overlayMetrics(node, metrics);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to fetch live metrics for node {}: {}", id, e.getMessage());
                    }
                }
            }
        }

        return node;
    }

    // ==================== Topology ====================

    @Override
    public Map<String, Object> getTopology(String clusterId) {
        List<Map<String, Object>> topoNodes = new ArrayList<>();
        List<Map<String, Object>> topoEdges = new ArrayList<>();

        List<PromCluster> clusters;
        if (StringUtils.hasText(clusterId)) {
            PromCluster cluster = clusterMapper.selectById(clusterId);
            if (cluster == null) {
                throw new BusinessException("集群不存在");
            }
            clusters = Collections.singletonList(cluster);
        } else {
            clusters = clusterMapper.selectList(null);
        }

        // Create Prometheus topology nodes — check reachability
        Set<String> promUrls = new HashSet<>();
        for (PromCluster cluster : clusters) {
            String promUrl = resolvePrometheusUrl(cluster);
            if (StringUtils.hasText(promUrl) && promUrls.add(promUrl)) {
                String promStatus;
                try {
                    promStatus = metricsFetcher.isReachable(promUrl) ? "healthy" : "offline";
                } catch (Exception e) {
                    promStatus = "offline";
                }

                Map<String, Object> promNode = new LinkedHashMap<>();
                String promNodeId = "prom-" + cluster.getId();
                promNode.put("id", promNodeId);
                promNode.put("name", "Prometheus");
                promNode.put("type", "prometheus");
                promNode.put("status", promStatus);
                topoNodes.add(promNode);

                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("source", promNodeId);
                edge.put("target", "cluster-" + cluster.getId());
                edge.put("label", "scrape");
                edge.put("status", promStatus);
                topoEdges.add(edge);
            }
        }

        // Create cluster and node topology entries
        for (PromCluster cluster : clusters) {
            String clusterNodeId = "cluster-" + cluster.getId();
            String promUrl = resolvePrometheusUrl(cluster);

            // Fetch live metrics for this cluster's nodes
            Map<String, NodeMetricsDTO> liveMetrics = new HashMap<>();
            if (StringUtils.hasText(promUrl)) {
                try {
                    liveMetrics = metricsFetcher.fetchAllNodeMetrics(promUrl);
                } catch (Exception e) {
                    log.warn("Failed to fetch metrics for topology of cluster {}: {}", cluster.getId(), e.getMessage());
                }
            }

            Map<String, Object> clusterNode = new LinkedHashMap<>();
            clusterNode.put("id", clusterNodeId);
            clusterNode.put("name", cluster.getName());
            clusterNode.put("type", "cluster");
            clusterNode.put("status", cluster.getStatus());
            topoNodes.add(clusterNode);

            LambdaQueryWrapper<PromClusterNode> nodeWrapper = new LambdaQueryWrapper<>();
            nodeWrapper.eq(PromClusterNode::getClusterId, cluster.getId());
            List<PromClusterNode> clusterNodes = nodeMapper.selectList(nodeWrapper);

            for (PromClusterNode cn : clusterNodes) {
                String nodeId = "node-" + cn.getId();

                // Determine node status from live data if available
                String nodeStatus = cn.getStatus();
                NodeMetricsDTO liveNode = liveMetrics.get(cn.getIp());
                if (liveNode != null) {
                    nodeStatus = liveNode.getUp() == 1 ? "online" : "offline";
                }

                Map<String, Object> nodeEntry = new LinkedHashMap<>();
                nodeEntry.put("id", nodeId);
                nodeEntry.put("name", cn.getHostname());
                nodeEntry.put("type", "node");
                nodeEntry.put("status", nodeStatus);

                Map<String, Object> metrics = new LinkedHashMap<>();
                if (liveNode != null) {
                    metrics.put("cpu", round(liveNode.getCpuPercent()));
                    double memPercent = liveNode.getMemoryTotal() > 0
                            ? (1.0 - (double) liveNode.getMemoryAvailable() / liveNode.getMemoryTotal()) * 100
                            : 0;
                    metrics.put("memory", round(memPercent));
                } else {
                    if (cn.getCpu() != null) {
                        metrics.put("cpu", cn.getCpu().get("percent"));
                    }
                    if (cn.getMemory() != null) {
                        metrics.put("memory", cn.getMemory().get("percent"));
                    }
                }
                nodeEntry.put("metrics", metrics);
                topoNodes.add(nodeEntry);

                Map<String, Object> clusterToNode = new LinkedHashMap<>();
                clusterToNode.put("source", clusterNodeId);
                clusterToNode.put("target", nodeId);
                clusterToNode.put("status", nodeStatus);
                topoEdges.add(clusterToNode);

                String serviceId = "svc-" + cn.getId();
                Map<String, Object> serviceNode = new LinkedHashMap<>();
                serviceNode.put("id", serviceId);
                serviceNode.put("name", cn.getHostname() + "-exporter");
                serviceNode.put("type", "service");
                serviceNode.put("status", nodeStatus);
                topoNodes.add(serviceNode);

                Map<String, Object> nodeToService = new LinkedHashMap<>();
                nodeToService.put("source", nodeId);
                nodeToService.put("target", serviceId);
                nodeToService.put("status", nodeStatus);
                topoEdges.add(nodeToService);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", topoNodes);
        result.put("edges", topoEdges);
        return result;
    }

    // ==================== Health Score ====================

    @Override
    public Map<String, Object> getHealthScore(String clusterId) {
        PromCluster targetCluster = null;
        List<PromClusterNode> nodes;
        if (StringUtils.hasText(clusterId)) {
            targetCluster = clusterMapper.selectById(clusterId);
            if (targetCluster == null) {
                throw new BusinessException("集群不存在");
            }
            LambdaQueryWrapper<PromClusterNode> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PromClusterNode::getClusterId, clusterId);
            nodes = nodeMapper.selectList(wrapper);
        } else {
            nodes = nodeMapper.selectList(null);
        }

        // Try to fetch live metrics
        Map<String, NodeMetricsDTO> liveMetrics = new HashMap<>();
        if (targetCluster != null) {
            String promUrl = resolvePrometheusUrl(targetCluster);
            if (StringUtils.hasText(promUrl)) {
                try {
                    liveMetrics = metricsFetcher.fetchAllNodeMetrics(promUrl);
                } catch (Exception e) {
                    log.warn("Failed to fetch live metrics for health score: {}", e.getMessage());
                }
            }
        }

        // If we have live metrics, overlay them onto nodes
        if (!liveMetrics.isEmpty()) {
            for (PromClusterNode node : nodes) {
                NodeMetricsDTO metrics = liveMetrics.get(node.getIp());
                if (metrics != null) {
                    overlayMetrics(node, metrics);
                }
            }
        }

        // ----- Availability (weight 30) -----
        double nodeOnlineRate = 100.0;
        double exporterRunRate = 95.0;
        if (!nodes.isEmpty()) {
            long onlineCount = nodes.stream()
                    .filter(n -> "online".equals(n.getStatus()))
                    .count();
            nodeOnlineRate = (double) onlineCount / nodes.size() * 100;
            exporterRunRate = nodeOnlineRate * 0.95;
        }
        double availabilityScore = nodeOnlineRate * 0.6 + exporterRunRate * 0.4;
        availabilityScore = Math.min(100, Math.max(0, availabilityScore));

        Map<String, Object> nodeOnlineItem = new LinkedHashMap<>();
        nodeOnlineItem.put("name", "节点在线率");
        nodeOnlineItem.put("score", round(nodeOnlineRate));
        nodeOnlineItem.put("status", nodeOnlineRate >= 90 ? "healthy" : nodeOnlineRate >= 70 ? "warning" : "critical");

        Map<String, Object> exporterItem = new LinkedHashMap<>();
        exporterItem.put("name", "Exporter运行率");
        exporterItem.put("score", round(exporterRunRate));
        exporterItem.put("status", exporterRunRate >= 90 ? "healthy" : exporterRunRate >= 70 ? "warning" : "critical");

        Map<String, Object> availability = new LinkedHashMap<>();
        availability.put("name", "可用性");
        availability.put("score", round(availabilityScore));
        availability.put("weight", 30);
        availability.put("items", Arrays.asList(nodeOnlineItem, exporterItem));

        // ----- Performance (weight 25) -----
        double avgCpu = 0;
        double avgMemory = 0;
        if (!nodes.isEmpty()) {
            double cpuSum = 0;
            double memSum = 0;
            int cpuCount = 0;
            int memCount = 0;
            for (PromClusterNode node : nodes) {
                if (node.getCpu() != null && node.getCpu().get("percent") != null) {
                    cpuSum += toDouble(node.getCpu().get("percent"));
                    cpuCount++;
                }
                if (node.getMemory() != null && node.getMemory().get("percent") != null) {
                    memSum += toDouble(node.getMemory().get("percent"));
                    memCount++;
                }
            }
            if (cpuCount > 0) avgCpu = cpuSum / cpuCount;
            if (memCount > 0) avgMemory = memSum / memCount;
        }
        double cpuScore = Math.max(0, 100 - avgCpu);
        double memoryScore = Math.max(0, 100 - avgMemory);
        double performanceScore = cpuScore * 0.5 + memoryScore * 0.5;

        Map<String, Object> cpuItem = new LinkedHashMap<>();
        cpuItem.put("name", "CPU使用率");
        cpuItem.put("score", round(cpuScore));
        cpuItem.put("status", cpuScore >= 70 ? "healthy" : cpuScore >= 40 ? "warning" : "critical");

        Map<String, Object> memoryItem = new LinkedHashMap<>();
        memoryItem.put("name", "内存使用率");
        memoryItem.put("score", round(memoryScore));
        memoryItem.put("status", memoryScore >= 70 ? "healthy" : memoryScore >= 40 ? "warning" : "critical");

        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("name", "性能");
        performance.put("score", round(performanceScore));
        performance.put("weight", 25);
        performance.put("items", Arrays.asList(cpuItem, memoryItem));

        // ----- Capacity (weight 20) -----
        double avgDisk = 0;
        if (!nodes.isEmpty()) {
            double diskSum = 0;
            int diskCount = 0;
            for (PromClusterNode node : nodes) {
                if (node.getDisk() != null && node.getDisk().get("percent") != null) {
                    diskSum += toDouble(node.getDisk().get("percent"));
                    diskCount++;
                }
            }
            if (diskCount > 0) avgDisk = diskSum / diskCount;
        }
        double diskScore = Math.max(0, 100 - avgDisk);
        double capacityScore = diskScore;

        Map<String, Object> diskItem = new LinkedHashMap<>();
        diskItem.put("name", "磁盘使用率");
        diskItem.put("score", round(diskScore));
        diskItem.put("status", diskScore >= 70 ? "healthy" : diskScore >= 40 ? "warning" : "critical");

        Map<String, Object> storageItem = new LinkedHashMap<>();
        storageItem.put("name", "存储预测");
        storageItem.put("score", 88.0);
        storageItem.put("status", "healthy");

        Map<String, Object> capacity = new LinkedHashMap<>();
        capacity.put("name", "容量");
        capacity.put("score", round(capacityScore));
        capacity.put("weight", 20);
        capacity.put("items", Arrays.asList(diskItem, storageItem));

        // ----- Security (weight 15) - semi-static -----
        Map<String, Object> tlsItem = new LinkedHashMap<>();
        tlsItem.put("name", "TLS配置");
        tlsItem.put("score", 90.0);
        tlsItem.put("status", "healthy");

        Map<String, Object> authItem = new LinkedHashMap<>();
        authItem.put("name", "认证策略");
        authItem.put("score", 85.0);
        authItem.put("status", "healthy");

        Map<String, Object> security = new LinkedHashMap<>();
        security.put("name", "安全");
        security.put("score", 87.5);
        security.put("weight", 15);
        security.put("items", Arrays.asList(tlsItem, authItem));

        // ----- Operations (weight 10) - semi-static -----
        Map<String, Object> backupItem = new LinkedHashMap<>();
        backupItem.put("name", "备份策略");
        backupItem.put("score", 92.0);
        backupItem.put("status", "healthy");

        Map<String, Object> monitorItem = new LinkedHashMap<>();
        monitorItem.put("name", "监控覆盖");
        monitorItem.put("score", 88.0);
        monitorItem.put("status", "healthy");

        Map<String, Object> operations = new LinkedHashMap<>();
        operations.put("name", "运维");
        operations.put("score", 90.0);
        operations.put("weight", 10);
        operations.put("items", Arrays.asList(backupItem, monitorItem));

        // ----- Total Score -----
        double total = round(availabilityScore) * 0.30
                + round(performanceScore) * 0.25
                + round(capacityScore) * 0.20
                + 87.5 * 0.15
                + 90.0 * 0.10;

        // ----- Trend -----
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Random random = new Random(Objects.hash(clusterId));
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("time", day.format(DateTimeFormatter.ISO_LOCAL_DATE));
            double dailyScore = total + (random.nextDouble() * 6 - 3);
            dailyScore = Math.min(100, Math.max(0, dailyScore));
            point.put("score", round(dailyScore));
            trend.add(point);
        }

        List<Map<String, Object>> categories = Arrays.asList(availability, performance, capacity, security, operations);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", round(total));
        result.put("categories", categories);
        result.put("trend", trend);
        return result;
    }

    // ==================== CRUD ====================

    @Override
    public PromCluster createCluster(ClusterDTO dto) {
        PromCluster cluster = new PromCluster();
        cluster.setName(dto.getName());
        cluster.setDescription(dto.getDescription());
        cluster.setRegion(dto.getRegion());
        cluster.setInstanceId(dto.getInstanceId());
        cluster.setStatus("healthy");

        // If instanceId is set, resolve URL from prom_instance
        if (StringUtils.hasText(dto.getInstanceId())) {
            PromInstance instance = instanceMapper.selectById(dto.getInstanceId());
            if (instance != null) {
                cluster.setPrometheusUrl(instance.getUrl());
            }
        }

        clusterMapper.insert(cluster);
        return cluster;
    }

    @Override
    public PromCluster updateCluster(String id, ClusterDTO dto) {
        PromCluster cluster = clusterMapper.selectById(id);
        if (cluster == null) {
            throw new BusinessException("集群不存在");
        }

        if (dto.getName() != null) cluster.setName(dto.getName());
        if (dto.getDescription() != null) cluster.setDescription(dto.getDescription());
        if (dto.getRegion() != null) cluster.setRegion(dto.getRegion());
        if (dto.getInstanceId() != null) {
            cluster.setInstanceId(dto.getInstanceId());
            PromInstance instance = instanceMapper.selectById(dto.getInstanceId());
            if (instance != null) {
                cluster.setPrometheusUrl(instance.getUrl());
            }
        }

        clusterMapper.updateById(cluster);
        return cluster;
    }

    @Override
    public void deleteCluster(String id) {
        PromCluster cluster = clusterMapper.selectById(id);
        if (cluster == null) {
            throw new BusinessException("集群不存在");
        }
        clusterMapper.deleteById(id);
        // Also soft-delete child nodes
        LambdaQueryWrapper<PromClusterNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(PromClusterNode::getClusterId, id);
        List<PromClusterNode> childNodes = nodeMapper.selectList(nodeWrapper);
        for (PromClusterNode node : childNodes) {
            nodeMapper.deleteById(node.getId());
        }
    }

    @Override
    public PromClusterNode addNode(String clusterId, PromClusterNode node) {
        PromCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) {
            throw new BusinessException("集群不存在");
        }
        node.setClusterId(clusterId);
        if (node.getStatus() == null) node.setStatus("online");
        if (node.getRole() == null) node.setRole("worker");
        nodeMapper.insert(node);
        return node;
    }

    @Override
    public void deleteNode(String nodeId) {
        PromClusterNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }
        nodeMapper.deleteById(nodeId);
    }

    @Override
    public Map<String, Object> discoverNodes(String clusterId) {
        PromCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) {
            throw new BusinessException("集群不存在");
        }

        String promUrl = resolvePrometheusUrl(cluster);
        if (!StringUtils.hasText(promUrl)) {
            throw new BusinessException("集群未配置 Prometheus 地址");
        }

        List<DiscoveredNode> discovered = metricsFetcher.discoverNodes(promUrl);

        // Check which are already registered
        LambdaQueryWrapper<PromClusterNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromClusterNode::getClusterId, clusterId);
        List<PromClusterNode> existingNodes = nodeMapper.selectList(wrapper);
        Set<String> existingIps = new HashSet<>();
        for (PromClusterNode n : existingNodes) {
            existingIps.add(n.getIp());
        }
        for (DiscoveredNode dn : discovered) {
            dn.setAlreadyRegistered(existingIps.contains(dn.getIp()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", discovered);
        result.put("total", discovered.size());
        result.put("newCount", discovered.stream().filter(d -> !d.isAlreadyRegistered()).count());
        return result;
    }

    @Override
    public Map<String, Object> syncNodes(String clusterId) {
        PromCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) {
            throw new BusinessException("集群不存在");
        }

        String promUrl = resolvePrometheusUrl(cluster);
        if (!StringUtils.hasText(promUrl)) {
            throw new BusinessException("集群未配置 Prometheus 地址");
        }

        List<DiscoveredNode> discovered = metricsFetcher.discoverNodes(promUrl);

        // Get existing nodes
        LambdaQueryWrapper<PromClusterNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromClusterNode::getClusterId, clusterId);
        List<PromClusterNode> existingNodes = nodeMapper.selectList(wrapper);
        Set<String> existingIps = new HashSet<>();
        for (PromClusterNode n : existingNodes) {
            existingIps.add(n.getIp());
        }

        int added = 0;
        for (DiscoveredNode dn : discovered) {
            if (!existingIps.contains(dn.getIp())) {
                PromClusterNode node = new PromClusterNode();
                node.setHostname(dn.getHostname());
                node.setIp(dn.getIp());
                node.setRole("worker");
                node.setStatus("up".equals(dn.getStatus()) ? "online" : "offline");
                node.setClusterId(clusterId);
                node.setLabels(dn.getLabels());

                // Set default JSON fields
                Map<String, Object> defaultCpu = new LinkedHashMap<>();
                defaultCpu.put("total", 0);
                defaultCpu.put("used", 0);
                defaultCpu.put("percent", 0);
                defaultCpu.put("unit", "cores");
                node.setCpu(defaultCpu);

                Map<String, Object> defaultMemory = new LinkedHashMap<>();
                defaultMemory.put("total", 0);
                defaultMemory.put("used", 0);
                defaultMemory.put("percent", 0);
                defaultMemory.put("unit", "MB");
                node.setMemory(defaultMemory);

                Map<String, Object> defaultDisk = new LinkedHashMap<>();
                defaultDisk.put("total", 0);
                defaultDisk.put("used", 0);
                defaultDisk.put("percent", 0);
                defaultDisk.put("unit", "GB");
                node.setDisk(defaultDisk);

                Map<String, Object> defaultNetwork = new LinkedHashMap<>();
                defaultNetwork.put("rxBytes", 0);
                defaultNetwork.put("txBytes", 0);
                defaultNetwork.put("rxRate", 0);
                defaultNetwork.put("txRate", 0);
                node.setNetwork(defaultNetwork);

                nodeMapper.insert(node);
                added++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("discovered", discovered.size());
        result.put("added", added);
        result.put("existing", existingIps.size());
        return result;
    }

    // ==================== Helpers ====================

    /**
     * Overlay live Prometheus metrics onto a node entity's JSON fields.
     */
    private void overlayMetrics(PromClusterNode node, NodeMetricsDTO metrics) {
        // Update status based on 'up'
        if (metrics.getUp() == 1) {
            node.setStatus("online");
        } else {
            node.setStatus("offline");
        }

        // CPU
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("total", metrics.getCpuCores());
        double cpuUsed = round(metrics.getCpuPercent() * metrics.getCpuCores() / 100.0);
        cpu.put("used", cpuUsed);
        cpu.put("percent", round(metrics.getCpuPercent()));
        cpu.put("unit", "cores");
        node.setCpu(cpu);

        // Memory (convert bytes to MB for display)
        Map<String, Object> memory = new LinkedHashMap<>();
        long memTotalMB = metrics.getMemoryTotal() / (1024 * 1024);
        long memUsedMB = (metrics.getMemoryTotal() - metrics.getMemoryAvailable()) / (1024 * 1024);
        double memPercent = metrics.getMemoryTotal() > 0
                ? (1.0 - (double) metrics.getMemoryAvailable() / metrics.getMemoryTotal()) * 100
                : 0;
        memory.put("total", memTotalMB);
        memory.put("used", memUsedMB);
        memory.put("percent", round(memPercent));
        memory.put("unit", "MB");
        node.setMemory(memory);

        // Disk (convert bytes to GB for display)
        Map<String, Object> disk = new LinkedHashMap<>();
        double diskTotalGB = metrics.getDiskTotal() / (1024.0 * 1024 * 1024);
        double diskUsedGB = (metrics.getDiskTotal() - metrics.getDiskAvailable()) / (1024.0 * 1024 * 1024);
        double diskPercent = metrics.getDiskTotal() > 0
                ? (1.0 - (double) metrics.getDiskAvailable() / metrics.getDiskTotal()) * 100
                : 0;
        disk.put("total", round(diskTotalGB));
        disk.put("used", round(diskUsedGB));
        disk.put("percent", round(diskPercent));
        disk.put("unit", "GB");
        node.setDisk(disk);

        // Network
        Map<String, Object> network = new LinkedHashMap<>();
        network.put("rxBytes", 0);
        network.put("txBytes", 0);
        network.put("rxRate", round(metrics.getRxRate()));
        network.put("txRate", round(metrics.getTxRate()));
        node.setNetwork(network);

        // OS / Kernel
        if (StringUtils.hasText(metrics.getKernel())) {
            node.setKernel(metrics.getKernel());
        }
        if (StringUtils.hasText(metrics.getNodename())) {
            // Use nodename from uname_info if available
        }

        // Uptime from boot time
        if (metrics.getBootTime() > 0) {
            long uptimeSeconds = Instant.now().getEpochSecond() - metrics.getBootTime();
            node.setUptime(formatUptime(uptimeSeconds));
        }
    }

    private String formatUptime(long seconds) {
        if (seconds <= 0) return "0";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        if (days > 0) {
            return days + "天" + (hours > 0 ? hours + "小时" : "");
        }
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
        return minutes + "分钟";
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
