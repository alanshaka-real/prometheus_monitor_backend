package com.wenmin.prometheus.module.cluster.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.cluster.dto.ClusterDTO;
import com.wenmin.prometheus.module.cluster.entity.PromCluster;
import com.wenmin.prometheus.module.cluster.entity.PromClusterNode;
import com.wenmin.prometheus.module.cluster.service.ClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "集群管理")
@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;

    // ==================== Cluster List ====================

    @Operation(summary = "获取集群列表")
    @GetMapping("/clusters")
    public R<Map<String, Object>> listClusters() {
        return R.ok(clusterService.listClusters());
    }

    // ==================== Topology & Health (before {id} to avoid path conflicts) ====================

    @Operation(summary = "获取集群拓扑图数据")
    @GetMapping("/clusters/topology")
    public R<Map<String, Object>> getTopology(
            @RequestParam(required = false) String clusterId) {
        return R.ok(clusterService.getTopology(clusterId));
    }

    @Operation(summary = "获取集群健康评分")
    @GetMapping("/clusters/health")
    public R<Map<String, Object>> getHealthScore(
            @RequestParam(required = false) String clusterId) {
        return R.ok(clusterService.getHealthScore(clusterId));
    }

    // ==================== Cluster CRUD ====================

    @Operation(summary = "获取集群详情")
    @GetMapping("/clusters/{id}")
    public R<PromCluster> getCluster(@PathVariable String id) {
        return R.ok(clusterService.getClusterById(id));
    }

    @Operation(summary = "创建集群")
    @PostMapping("/clusters")
    public R<PromCluster> createCluster(@RequestBody ClusterDTO dto) {
        return R.ok(clusterService.createCluster(dto));
    }

    @Operation(summary = "更新集群")
    @PutMapping("/clusters/{id}")
    public R<PromCluster> updateCluster(@PathVariable String id, @RequestBody ClusterDTO dto) {
        return R.ok(clusterService.updateCluster(id, dto));
    }

    @Operation(summary = "删除集群")
    @DeleteMapping("/clusters/{id}")
    public R<Void> deleteCluster(@PathVariable String id) {
        clusterService.deleteCluster(id);
        return R.ok();
    }

    // ==================== Cluster Nodes ====================

    @Operation(summary = "获取集群节点列表")
    @GetMapping("/clusters/{id}/nodes")
    public R<Map<String, Object>> getClusterNodes(@PathVariable String id) {
        return R.ok(clusterService.getClusterNodes(id));
    }

    @Operation(summary = "添加节点到集群")
    @PostMapping("/clusters/{id}/nodes")
    public R<PromClusterNode> addNode(@PathVariable String id, @RequestBody PromClusterNode node) {
        return R.ok(clusterService.addNode(id, node));
    }

    @Operation(summary = "删除集群节点")
    @DeleteMapping("/clusters/{clusterId}/nodes/{nodeId}")
    public R<Void> deleteNode(@PathVariable String clusterId, @PathVariable String nodeId) {
        clusterService.deleteNode(nodeId);
        return R.ok();
    }

    // ==================== Node Discovery & Sync ====================

    @Operation(summary = "发现节点")
    @GetMapping("/clusters/{id}/discover")
    public R<Map<String, Object>> discoverNodes(@PathVariable String id) {
        return R.ok(clusterService.discoverNodes(id));
    }

    @Operation(summary = "同步节点")
    @PostMapping("/clusters/{id}/sync")
    public R<Map<String, Object>> syncNodes(@PathVariable String id) {
        return R.ok(clusterService.syncNodes(id));
    }

    // ==================== Node Detail ====================

    @Operation(summary = "获取节点详情")
    @GetMapping("/nodes/{id}")
    public R<PromClusterNode> getNodeDetail(@PathVariable String id) {
        return R.ok(clusterService.getNodeDetail(id));
    }
}
