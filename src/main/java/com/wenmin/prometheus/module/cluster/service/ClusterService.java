package com.wenmin.prometheus.module.cluster.service;

import com.wenmin.prometheus.module.cluster.dto.ClusterDTO;
import com.wenmin.prometheus.module.cluster.entity.PromCluster;
import com.wenmin.prometheus.module.cluster.entity.PromClusterNode;

import java.util.Map;

public interface ClusterService {

    Map<String, Object> listClusters();

    PromCluster getClusterById(String id);

    Map<String, Object> getClusterNodes(String clusterId);

    PromClusterNode getNodeDetail(String id);

    Map<String, Object> getTopology(String clusterId);

    Map<String, Object> getHealthScore(String clusterId);

    PromCluster createCluster(ClusterDTO dto);

    PromCluster updateCluster(String id, ClusterDTO dto);

    void deleteCluster(String id);

    PromClusterNode addNode(String clusterId, PromClusterNode node);

    void deleteNode(String nodeId);

    Map<String, Object> discoverNodes(String clusterId);

    Map<String, Object> syncNodes(String clusterId);
}
