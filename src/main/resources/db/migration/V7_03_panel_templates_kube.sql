-- ============================================
-- Kubernetes 面板模板种子数据
-- kube-state-metrics + apiserver + etcd
-- 30 条面板模板 (pt-kube-01 ~ pt-kube-30)
-- ============================================

INSERT IGNORE INTO prom_panel_template
    (id, name, description, category, sub_category, exporter_type, chart_type, promql, unit, unit_format, default_width, default_height, thresholds, options, tags, sort_order, created_at)
VALUES

-- ============================================
-- 集群概览 (sub_category: 集群)
-- ============================================

('pt-kube-01', '节点总数', '集群中所有节点的总数量', 'K8s集群', '集群', 'kube-state-metrics', 'stat',
 'count(kube_node_info)',
 '', 'number', 3, 2,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]}}',
 '["kubernetes","node","cluster"]', 1, '2025-01-01 00:00:00'),

('pt-kube-02', 'Pod 总数', '集群中所有 Pod 的总数量', 'K8s集群', '集群', 'kube-state-metrics', 'stat',
 'count(kube_pod_info)',
 '', 'number', 3, 2,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]}}',
 '["kubernetes","pod","cluster"]', 2, '2025-01-01 00:00:00'),

('pt-kube-03', '集群 CPU 请求率', '所有节点 CPU 资源请求量占可分配量的百分比', 'K8s集群', '集群', 'kube-state-metrics', 'gauge',
 'sum(kube_pod_container_resource_requests{resource="cpu"}) / sum(kube_node_status_allocatable{resource="cpu"}) * 100',
 'percent', 'percent', 3, 3,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":70,"color":"yellow"},{"value":85,"color":"red"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]},"max":100}',
 '["kubernetes","cpu","requests","cluster"]', 3, '2025-01-01 00:00:00'),

('pt-kube-04', '集群内存请求率', '所有节点内存资源请求量占可分配量的百分比', 'K8s集群', '集群', 'kube-state-metrics', 'gauge',
 'sum(kube_pod_container_resource_requests{resource="memory"}) / sum(kube_node_status_allocatable{resource="memory"}) * 100',
 'percent', 'percent', 3, 3,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":70,"color":"yellow"},{"value":85,"color":"red"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]},"max":100}',
 '["kubernetes","memory","requests","cluster"]', 4, '2025-01-01 00:00:00'),

('pt-kube-05', '命名空间数量', '集群中命名空间的总数量', 'K8s集群', '集群', 'kube-state-metrics', 'stat',
 'count(kube_namespace_created)',
 '', 'number', 3, 2,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]}}',
 '["kubernetes","namespace","cluster"]', 5, '2025-01-01 00:00:00'),

-- ============================================
-- 节点 (sub_category: 节点)
-- ============================================

('pt-kube-06', '节点状态', '各节点的 Ready/NotReady 状态', 'K8s集群', '节点', 'kube-state-metrics', 'table',
 'kube_node_status_condition{condition="Ready",status="true"}',
 '', 'none', 12, 4,
 '{}',
 '{"columns":["node","value"],"transformations":[{"id":"labelsToFields"}]}',
 '["kubernetes","node","status"]', 6, '2025-01-01 00:00:00'),

('pt-kube-07', '节点 CPU 分配率', '每个节点上 Pod CPU 请求量占节点可分配 CPU 的比例', 'K8s集群', '节点', 'kube-state-metrics', 'bar',
 'sum by (node)(kube_pod_container_resource_requests{resource="cpu"}) / on(node) group_left kube_node_status_allocatable{resource="cpu"} * 100',
 'percent', 'percent', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":70,"color":"yellow"},{"value":90,"color":"red"}]}',
 '{"orientation":"horizontal"}',
 '["kubernetes","node","cpu","allocation"]', 7, '2025-01-01 00:00:00'),

('pt-kube-08', '节点内存分配率', '每个节点上 Pod 内存请求量占节点可分配内存的比例', 'K8s集群', '节点', 'kube-state-metrics', 'bar',
 'sum by (node)(kube_pod_container_resource_requests{resource="memory"}) / on(node) group_left kube_node_status_allocatable{resource="memory"} * 100',
 'percent', 'percent', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":70,"color":"yellow"},{"value":90,"color":"red"}]}',
 '{"orientation":"horizontal"}',
 '["kubernetes","node","memory","allocation"]', 8, '2025-01-01 00:00:00'),

('pt-kube-09', '节点 Pod 数', '每个节点上运行的 Pod 数量', 'K8s集群', '节点', 'kube-state-metrics', 'bar',
 'count by (node)(kube_pod_info)',
 '', 'number', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":100,"color":"yellow"}]}',
 '{"orientation":"horizontal"}',
 '["kubernetes","node","pod","count"]', 9, '2025-01-01 00:00:00'),

('pt-kube-10', '节点 NotReady', 'NotReady 状态的节点数量', 'K8s集群', '节点', 'kube-state-metrics', 'stat',
 'count(kube_node_status_condition{condition="Ready",status="true"} == 0) OR on() vector(0)',
 '', 'number', 3, 2,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":1,"color":"red"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]}}',
 '["kubernetes","node","notready"]', 10, '2025-01-01 00:00:00'),

-- ============================================
-- 工作负载 (sub_category: 工作负载)
-- ============================================

('pt-kube-11', 'Deployment 副本状态', 'Deployment 期望副本数与可用副本数对比', 'K8s集群', '工作负载', 'kube-state-metrics', 'table',
 'kube_deployment_status_replicas_available / kube_deployment_spec_replicas',
 '', 'none', 12, 5,
 '{}',
 '{"columns":["namespace","deployment","value"],"transformations":[{"id":"labelsToFields"}]}',
 '["kubernetes","deployment","replicas"]', 11, '2025-01-01 00:00:00'),

('pt-kube-12', 'Deployment 不可用副本', '存在不可用副本的 Deployment 数量', 'K8s集群', '工作负载', 'kube-state-metrics', 'stat',
 'count(kube_deployment_status_replicas_unavailable > 0) OR on() vector(0)',
 '', 'number', 3, 2,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":1,"color":"red"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]}}',
 '["kubernetes","deployment","unavailable"]', 12, '2025-01-01 00:00:00'),

('pt-kube-13', 'StatefulSet 状态', 'StatefulSet 期望副本数与就绪副本数对比', 'K8s集群', '工作负载', 'kube-state-metrics', 'table',
 'kube_statefulset_status_replicas_ready / kube_statefulset_replicas',
 '', 'none', 12, 5,
 '{}',
 '{"columns":["namespace","statefulset","value"],"transformations":[{"id":"labelsToFields"}]}',
 '["kubernetes","statefulset","replicas"]', 13, '2025-01-01 00:00:00'),

('pt-kube-14', 'DaemonSet 状态', 'DaemonSet 期望调度数与就绪数对比', 'K8s集群', '工作负载', 'kube-state-metrics', 'table',
 'kube_daemonset_status_number_ready / kube_daemonset_status_desired_number_scheduled',
 '', 'none', 12, 5,
 '{}',
 '{"columns":["namespace","daemonset","value"],"transformations":[{"id":"labelsToFields"}]}',
 '["kubernetes","daemonset","status"]', 14, '2025-01-01 00:00:00'),

('pt-kube-15', 'Job 成功/失败', 'Job 成功与失败数量的变化趋势', 'K8s集群', '工作负载', 'kube-state-metrics', 'line',
 'sum(kube_job_status_succeeded) by (namespace)',
 '', 'number', 6, 4,
 '{}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"},"queries":[{"name":"成功","promql":"sum(kube_job_status_succeeded) by (namespace)"},{"name":"失败","promql":"sum(kube_job_status_failed) by (namespace)"}]}',
 '["kubernetes","job","status"]', 15, '2025-01-01 00:00:00'),

('pt-kube-16', 'CronJob 最近状态', 'CronJob 最近调度时间与活跃 Job 数', 'K8s集群', '工作负载', 'kube-state-metrics', 'table',
 'kube_cronjob_status_active',
 '', 'none', 12, 5,
 '{}',
 '{"columns":["namespace","cronjob","value"],"transformations":[{"id":"labelsToFields"}]}',
 '["kubernetes","cronjob","status"]', 16, '2025-01-01 00:00:00'),

-- ============================================
-- 服务 (sub_category: 服务)
-- ============================================

('pt-kube-17', 'Service Endpoint 数', '各 Service 关联的 Endpoint 数量', 'K8s集群', '服务', 'kube-state-metrics', 'bar',
 'count by (namespace, service)(kube_endpoint_address)',
 '', 'number', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"}]}',
 '{"orientation":"horizontal"}',
 '["kubernetes","service","endpoint"]', 17, '2025-01-01 00:00:00'),

('pt-kube-18', 'Endpoint 就绪率', 'Endpoint 就绪地址数占总数的比率', 'K8s集群', '服务', 'kube-state-metrics', 'gauge',
 'sum(kube_endpoint_address_available) / (sum(kube_endpoint_address_available) + sum(kube_endpoint_address_not_ready)) * 100',
 'percent', 'percent', 3, 3,
 '{"mode":"absolute","steps":[{"value":0,"color":"red"},{"value":80,"color":"yellow"},{"value":95,"color":"green"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]},"max":100}',
 '["kubernetes","endpoint","ready"]', 18, '2025-01-01 00:00:00'),

('pt-kube-19', 'Ingress 规则数', 'Ingress 资源的总数量', 'K8s集群', '服务', 'kube-state-metrics', 'stat',
 'count(kube_ingress_info)',
 '', 'number', 3, 2,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]}}',
 '["kubernetes","ingress","count"]', 19, '2025-01-01 00:00:00'),

('pt-kube-20', 'CoreDNS 请求率', 'CoreDNS 每秒 DNS 查询请求数', 'K8s集群', '服务', 'kube-state-metrics', 'line',
 'sum(rate(coredns_dns_requests_total[5m])) by (type)',
 'ops', 'ops', 6, 4,
 '{}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"}}',
 '["kubernetes","coredns","dns","qps"]', 20, '2025-01-01 00:00:00'),

-- ============================================
-- 存储 (sub_category: 存储)
-- ============================================

('pt-kube-21', 'PV 总数', 'PersistentVolume 按阶段统计的数量', 'K8s集群', '存储', 'kube-state-metrics', 'stat',
 'count(kube_persistentvolume_info)',
 '', 'number', 3, 2,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"}]}',
 '{"reduceOptions":{"calcs":["lastNotNull"]}}',
 '["kubernetes","pv","storage"]', 21, '2025-01-01 00:00:00'),

('pt-kube-22', 'PVC 状态', 'PersistentVolumeClaim 的绑定状态与容量', 'K8s集群', '存储', 'kube-state-metrics', 'table',
 'kube_persistentvolumeclaim_status_phase',
 '', 'none', 12, 5,
 '{}',
 '{"columns":["namespace","persistentvolumeclaim","phase","value"],"transformations":[{"id":"labelsToFields"}]}',
 '["kubernetes","pvc","status"]', 22, '2025-01-01 00:00:00'),

('pt-kube-23', 'PV 使用率', 'PersistentVolume 实际使用量占总容量的百分比', 'K8s集群', '存储', 'kube-state-metrics', 'bar',
 'kubelet_volume_stats_used_bytes / kubelet_volume_stats_capacity_bytes * 100',
 'percent', 'percent', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":70,"color":"yellow"},{"value":90,"color":"red"}]}',
 '{"orientation":"horizontal"}',
 '["kubernetes","pv","usage","storage"]', 23, '2025-01-01 00:00:00'),

('pt-kube-24', 'PVC 绑定状态', 'PersistentVolumeClaim 按绑定状态分布', 'K8s集群', '存储', 'kube-state-metrics', 'pie',
 'count by (phase)(kube_persistentvolumeclaim_status_phase)',
 '', 'number', 4, 4,
 '{}',
 '{"legendPosition":"right"}',
 '["kubernetes","pvc","bound","storage"]', 24, '2025-01-01 00:00:00'),

-- ============================================
-- API Server (sub_category: API Server)
-- ============================================

('pt-kube-25', 'API 请求率', 'API Server 每秒请求数，按 verb 分组', 'K8s集群', 'API Server', 'kube-state-metrics', 'line',
 'sum(rate(apiserver_request_total[5m])) by (verb)',
 'ops', 'ops', 6, 4,
 '{}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"}}',
 '["kubernetes","apiserver","request","qps"]', 25, '2025-01-01 00:00:00'),

('pt-kube-26', 'API 请求延迟', 'API Server 请求延迟 P99 分位数，按 verb 分组', 'K8s集群', 'API Server', 'kube-state-metrics', 'line',
 'histogram_quantile(0.99, sum(rate(apiserver_request_duration_seconds_bucket[5m])) by (verb, le))',
 'seconds', 'duration', 6, 4,
 '{}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"}}',
 '["kubernetes","apiserver","latency","p99"]', 26, '2025-01-01 00:00:00'),

('pt-kube-27', 'API 错误率', 'API Server 4xx 和 5xx 错误请求的每秒速率', 'K8s集群', 'API Server', 'kube-state-metrics', 'line',
 'sum(rate(apiserver_request_total{code=~"4..|5.."}[5m])) by (code)',
 'ops', 'ops', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":5,"color":"yellow"},{"value":20,"color":"red"}]}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"}}',
 '["kubernetes","apiserver","error","4xx","5xx"]', 27, '2025-01-01 00:00:00'),

-- ============================================
-- etcd (sub_category: etcd)
-- ============================================

('pt-kube-28', 'etcd Leader 变更', 'etcd 集群 Leader 变更次数趋势', 'K8s集群', 'etcd', 'kube-state-metrics', 'line',
 'changes(etcd_server_leader_changes_seen_total[1h])',
 '', 'number', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":3,"color":"yellow"},{"value":5,"color":"red"}]}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"}}',
 '["kubernetes","etcd","leader"]', 28, '2025-01-01 00:00:00'),

('pt-kube-29', 'etcd DB 大小', 'etcd MVCC 数据库的总大小趋势', 'K8s集群', 'etcd', 'kube-state-metrics', 'line',
 'etcd_mvcc_db_total_size_in_bytes',
 'bytes', 'bytes_iec', 6, 4,
 '{}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"}}',
 '["kubernetes","etcd","db","size"]', 29, '2025-01-01 00:00:00'),

('pt-kube-30', 'etcd 提案失败', 'etcd 共识提案失败数量趋势', 'K8s集群', 'etcd', 'kube-state-metrics', 'line',
 'rate(etcd_server_proposals_failed_total[5m])',
 'ops', 'ops', 6, 4,
 '{"mode":"absolute","steps":[{"value":0,"color":"green"},{"value":1,"color":"yellow"},{"value":5,"color":"red"}]}',
 '{"legend":{"show":true},"tooltip":{"mode":"multi"}}',
 '["kubernetes","etcd","proposals","failed"]', 30, '2025-01-01 00:00:00');
