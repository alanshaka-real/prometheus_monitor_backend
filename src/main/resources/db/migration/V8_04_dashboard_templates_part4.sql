-- ============================================================================
-- V8_04: Dashboard Templates Part 4 - 业务监控 / 基础设施 / 综合大屏
-- 18 dashboard templates (dtpl-086 ~ dtpl-103)
-- Table: prom_dashboard_template
-- ============================================================================

-- ============================================================================
-- 业务监控 (category:'业务监控', 8 templates: dtpl-086 ~ dtpl-093)
-- ============================================================================

-- --------------------------------------------------------------------------
-- dtpl-086: SLA仪表盘
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-086', 'SLA仪表盘', '业务监控', 'SLA', '',
 '服务等级协议监控仪表盘，展示可用性百分比、可用性趋势、故障次数及平均恢复时间(MTTR)',
 '[{"id":"p1","title":"可用性百分比","type":"gauge","promql":"avg(up) * 100","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":99,"color":"orange"},{"value":99.9,"color":"green"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"可用性趋势","type":"line","promql":"avg(up) * 100","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"可用性"},{"id":"p3","title":"故障次数","type":"stat","promql":"sum(changes(up[1d]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":5,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"次","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"MTTR平均恢复时间","type":"stat","promql":"avg(avg_over_time((1 - up)[1d:1m]) * 1440)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":10,"color":"orange"},{"value":30,"color":"red"}],"options":{"unit":"min","decimals":1,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 '', '["sla","可用性","业务"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-087: 错误率分析
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-087', '错误率分析', '业务监控', '错误', '',
 '分析HTTP 5xx错误率趋势及分布，快速定位错误来源',
 '[{"id":"p1","title":"HTTP 5xx错误率","type":"gauge","promql":"sum(rate(http_requests_total{status=~\\"5..\\"}[5m])) / sum(rate(http_requests_total[5m])) * 100","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"错误率趋势","type":"line","promql":"sum(rate(http_requests_total{status=~\\"5..\\"}[5m])) / sum(rate(http_requests_total[5m])) * 100","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"5xx错误率"},{"id":"p3","title":"错误按Handler分布","type":"line","promql":"sum by(handler)(rate(http_requests_total{status=~\\"5..\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"palette","fillOpacity":30,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{handler}}"},{"id":"p4","title":"错误总量","type":"stat","promql":"sum(increase(http_requests_total{status=~\\"5..\\"}[1h]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":100,"color":"orange"},{"value":1000,"color":"red"}],"options":{"unit":"次","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 '', '["错误率","5xx","http","业务"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-088: 延迟分析P50/P90/P99
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-088', '延迟分析P50/P90/P99', '业务监控', '延迟', '',
 '多分位数延迟分析，展示P50、P90、P99延迟趋势及延迟分布热力图',
 '[{"id":"p1","title":"P50延迟","type":"line","promql":"histogram_quantile(0.5, sum by(le)(rate(http_request_duration_seconds_bucket[5m])))","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"P50"},{"id":"p2","title":"P90延迟","type":"line","promql":"histogram_quantile(0.9, sum by(le)(rate(http_request_duration_seconds_bucket[5m])))","position":{"x":4,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"P90"},{"id":"p3","title":"P99延迟","type":"line","promql":"histogram_quantile(0.99, sum by(le)(rate(http_request_duration_seconds_bucket[5m])))","position":{"x":8,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"P99"},{"id":"p4","title":"延迟分布","type":"heatmap","promql":"sum by(le)(rate(http_request_duration_seconds_bucket[5m]))","position":{"x":0,"y":4},"size":{"w":12,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"spectrum","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{le}}"}]',
 '', '["延迟","latency","p50","p90","p99","业务"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-089: HTTP流量概览
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-089', 'HTTP流量概览', '业务监控', '流量', '',
 'HTTP流量总览，包含请求速率、状态码分布、响应大小及带宽统计',
 '[{"id":"p1","title":"请求速率","type":"line","promql":"sum(rate(http_requests_total[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"QPS"},{"id":"p2","title":"状态码分布","type":"line","promql":"sum by(status)(rate(http_requests_total[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"palette","fillOpacity":30,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{status}}"},{"id":"p3","title":"响应大小","type":"line","promql":"sum(rate(http_response_size_bytes_sum[5m])) / sum(rate(http_response_size_bytes_count[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":0,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"平均响应大小"},{"id":"p4","title":"带宽","type":"stat","promql":"sum(rate(http_response_size_bytes_sum[5m]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":104857600,"color":"orange"},{"value":1073741824,"color":"red"}],"options":{"unit":"bytes/s","decimals":2,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 '', '["http","流量","qps","带宽","业务"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-090: API网关监控
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-090', 'API网关监控', '业务监控', '网关', '',
 'API网关综合监控，覆盖请求速率、延迟、错误率、状态码、上游连接和限流情况',
 '[{"id":"p1","title":"请求速率","type":"line","promql":"sum(rate(http_requests_total[5m]))","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"QPS"},{"id":"p2","title":"延迟P99","type":"line","promql":"histogram_quantile(0.99, sum by(le)(rate(http_request_duration_seconds_bucket[5m])))","position":{"x":4,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"P99"},{"id":"p3","title":"错误率","type":"line","promql":"sum(rate(http_requests_total{status=~\\"5..\\"}[5m])) / sum(rate(http_requests_total[5m])) * 100","position":{"x":8,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"5xx错误率"},{"id":"p4","title":"状态码分布","type":"line","promql":"sum by(status)(rate(http_requests_total[5m]))","position":{"x":0,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"palette","fillOpacity":30,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{status}}"},{"id":"p5","title":"上游连接数","type":"line","promql":"sum(nginx_upstream_connections_active)","position":{"x":4,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"活跃连接"},{"id":"p6","title":"限流触发","type":"line","promql":"sum(rate(nginx_http_requests_total{status=\\"429\\"}[5m]))","position":{"x":8,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"429限流"}]',
 '', '["api","网关","gateway","限流","业务"]', 1, 6, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-091: 微服务概览
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-091', '微服务概览', '业务监控', '微服务', '',
 '微服务整体监控视图，展示各服务可用性、请求速率、延迟、错误率、依赖调用及熔断状态',
 '[{"id":"p1","title":"服务可用性","type":"table","promql":"up","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":1,"color":"green"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{job}}"},{"id":"p2","title":"请求速率 by Service","type":"line","promql":"sum by(job)(rate(http_requests_total[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{job}}"},{"id":"p3","title":"延迟 by Service","type":"line","promql":"histogram_quantile(0.99, sum by(job, le)(rate(http_request_duration_seconds_bucket[5m])))","position":{"x":0,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{job}}"},{"id":"p4","title":"错误率 by Service","type":"line","promql":"sum by(job)(rate(http_requests_total{status=~\\"5..\\"}[5m])) / sum by(job)(rate(http_requests_total[5m])) * 100","position":{"x":4,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{job}}"},{"id":"p5","title":"依赖调用","type":"line","promql":"sum by(target_service)(rate(service_dependency_requests_total[5m]))","position":{"x":8,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{target_service}}"},{"id":"p6","title":"熔断状态","type":"table","promql":"sum by(job)(circuit_breaker_state)","position":{"x":0,"y":8},"size":{"w":12,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":2,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{job}}"}]',
 '', '["微服务","microservice","熔断","依赖","业务"]', 1, 6, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-092: 请求成功率
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-092', '请求成功率', '业务监控', '成功率', '',
 '请求成功率监控，展示总体成功率、趋势变化、按接口成功率分布及失败请求详情',
 '[{"id":"p1","title":"总体成功率","type":"gauge","promql":"(sum(rate(http_requests_total[5m])) - sum(rate(http_requests_total{status=~\\"5..\\"}[5m]))) / sum(rate(http_requests_total[5m])) * 100","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":95,"color":"orange"},{"value":99,"color":"green"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"成功率趋势","type":"line","promql":"(sum(rate(http_requests_total[5m])) - sum(rate(http_requests_total{status=~\\"5..\\"}[5m]))) / sum(rate(http_requests_total[5m])) * 100","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"成功率"},{"id":"p3","title":"按接口成功率","type":"bar","promql":"(sum by(handler)(rate(http_requests_total[5m])) - sum by(handler)(rate(http_requests_total{status=~\\"5..\\"}[5m]))) / sum by(handler)(rate(http_requests_total[5m])) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"palette","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{handler}}"},{"id":"p4","title":"失败请求详情","type":"table","promql":"sum by(handler, status)(increase(http_requests_total{status=~\\"5..\\"}[1h]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"次","decimals":0,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{handler}} - {{status}}"}]',
 '', '["成功率","请求","http","业务"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-093: 吞吐量趋势
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-093', '吞吐量趋势', '业务监控', '吞吐', '',
 '系统吞吐量监控，展示QPS趋势、峰值QPS、日均请求量及按接口QPS分布',
 '[{"id":"p1","title":"QPS趋势","type":"line","promql":"sum(rate(http_requests_total[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"QPS"},{"id":"p2","title":"峰值QPS","type":"stat","promql":"max_over_time(sum(rate(http_requests_total[5m]))[1d:5m])","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":10000,"color":"orange"},{"value":50000,"color":"red"}],"options":{"unit":"req/s","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"日均请求量","type":"stat","promql":"sum(increase(http_requests_total[1d]))","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"blue"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"QPS按接口分布","type":"line","promql":"sum by(handler)(rate(http_requests_total[5m]))","position":{"x":0,"y":4},"size":{"w":12,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{handler}}"}]',
 '', '["qps","吞吐量","throughput","业务"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- ============================================================================
-- 基础设施 (category:'基础设施', 6 templates: dtpl-094 ~ dtpl-099)
-- ============================================================================

-- --------------------------------------------------------------------------
-- dtpl-094: 多主机对比
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-094', '多主机对比', '基础设施', '对比', '',
 '多主机关键指标横向对比，覆盖CPU、内存、磁盘和网络使用率',
 '[{"id":"p1","title":"CPU使用率多主机对比","type":"line","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"内存使用率多主机对比","type":"line","promql":"(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"磁盘使用率多主机对比","type":"bar","promql":"(1 - node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"} / node_filesystem_size_bytes{fstype=~\\"ext4|xfs\\"}) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"palette","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{mountpoint}}"},{"id":"p4","title":"网络带宽多主机对比","type":"line","promql":"irate(node_network_receive_bytes_total{device!~\\"lo|docker.*|br-.*|veth.*\\"}[5m]) * 8","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{device}} 入"}]',
 '', '["多主机","对比","compare","基础设施"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-095: 存储容量规划
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-095', '存储容量规划', '基础设施', '存储', '',
 '存储容量规划视图，展示磁盘使用分布、使用趋势、剩余空间预测及容量告警',
 '[{"id":"p1","title":"磁盘使用率分布","type":"bar","promql":"(1 - node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"} / node_filesystem_size_bytes{fstype=~\\"ext4|xfs\\"}) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{mountpoint}}"},{"id":"p2","title":"磁盘使用趋势","type":"line","promql":"(1 - node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"} / node_filesystem_size_bytes{fstype=~\\"ext4|xfs\\"}) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{mountpoint}}"},{"id":"p3","title":"剩余空间预测(7天)","type":"line","promql":"predict_linear(node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"}[7d], 7*24*3600)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{mountpoint}}"},{"id":"p4","title":"容量告警","type":"stat","promql":"count(((1 - node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"} / node_filesystem_size_bytes{fstype=~\\"ext4|xfs\\"}) * 100) > 85)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":3,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 '', '["存储","容量","磁盘","predict","基础设施"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-096: 网络概览
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-096', '网络概览', '基础设施', '网络', '',
 '网络状态综合监控，包含带宽、包量、网络错误率及TCP连接状态分布',
 '[{"id":"p1","title":"总带宽","type":"line","promql":"sum(irate(node_network_receive_bytes_total{device!~\\"lo|docker.*|br-.*|veth.*\\"}[5m])) * 8","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"入站带宽"},{"id":"p2","title":"包量","type":"line","promql":"sum(irate(node_network_receive_packets_total{device!~\\"lo|docker.*|br-.*|veth.*\\"}[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"pps","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"入站包量"},{"id":"p3","title":"网络错误率","type":"line","promql":"sum(irate(node_network_receive_errs_total{device!~\\"lo|docker.*|br-.*|veth.*\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"errors/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"入站错误"},{"id":"p4","title":"TCP连接数 by State","type":"line","promql":"sum by(state)(node_tcp_connection_states)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{state}}"}]',
 '', '["网络","带宽","tcp","packets","基础设施"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-097: 告警概览
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-097', '告警概览', '基础设施', '告警', '',
 '告警状态总览，展示当前告警数量、告警趋势、按严重程度和实例分布',
 '[{"id":"p1","title":"当前告警数","type":"stat","promql":"count(ALERTS{alertstate=\\"firing\\"})","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"告警趋势","type":"line","promql":"count(ALERTS{alertstate=\\"firing\\"})","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"告警数"},{"id":"p3","title":"按Severity分布","type":"pie","promql":"count by(severity)(ALERTS{alertstate=\\"firing\\"})","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"palette","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{severity}}"},{"id":"p4","title":"按Instance分布","type":"bar","promql":"count by(instance)(ALERTS{alertstate=\\"firing\\"})","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"palette","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 '', '["告警","alert","severity","基础设施"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-098: 资源利用率排行
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-098', '资源利用率排行', '基础设施', '排行', '',
 '各类资源利用率Top10排行榜，快速识别资源消耗最高的主机',
 '[{"id":"p1","title":"CPU使用率 Top10","type":"bar","promql":"topk(10, 100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\"}[5m])) * 100))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"内存使用率 Top10","type":"bar","promql":"topk(10, (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100)","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"磁盘使用率 Top10","type":"bar","promql":"topk(10, (1 - node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"} / node_filesystem_size_bytes{fstype=~\\"ext4|xfs\\"}) * 100)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}} - {{mountpoint}}"},{"id":"p4","title":"网络流量 Top10","type":"bar","promql":"topk(10, sum by(instance)(irate(node_network_receive_bytes_total{device!~\\"lo|docker.*|br-.*|veth.*\\"}[5m])) * 8)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":2,"colorMode":"palette","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 '', '["排行","top10","资源","基础设施"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-099: 服务可用性
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-099', '服务可用性', '基础设施', '可用性', '',
 '服务可用性综合监控，展示整体存活率、各服务状态、不可用服务数及可用性趋势',
 '[{"id":"p1","title":"服务存活概览","type":"gauge","promql":"avg(up) * 100","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":90,"color":"orange"},{"value":99,"color":"green"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"服务状态","type":"table","promql":"up","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":1,"color":"green"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{job}} - {{instance}}"},{"id":"p3","title":"不可用服务数","type":"stat","promql":"count(up == 0) OR vector(0)","position":{"x":0,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":3,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"可用性趋势","type":"line","promql":"avg(up) * 100","position":{"x":3,"y":4},"size":{"w":9,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"可用性"}]',
 '', '["可用性","服务","up","基础设施"]', 1, 4, '[]', '2025-01-01 00:00:00');

-- ============================================================================
-- 综合大屏 (category:'综合大屏', 4 templates: dtpl-100 ~ dtpl-103)
-- ============================================================================

-- --------------------------------------------------------------------------
-- dtpl-100: 运维总览大屏
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-100', '运维总览大屏', '综合大屏', '运维', '',
 '运维综合监控大屏，集中展示服务存活率、告警数、CPU/内存/磁盘均值、网络带宽、请求速率和错误率',
 '[{"id":"p1","title":"服务存活率","type":"gauge","promql":"avg(up) * 100","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":90,"color":"orange"},{"value":99,"color":"green"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"告警数","type":"stat","promql":"count(ALERTS{alertstate=\\"firing\\"}) OR vector(0)","position":{"x":3,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"CPU均值","type":"gauge","promql":"avg(100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\"}[5m])) * 100))","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"内存均值","type":"gauge","promql":"avg((1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100)","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p5","title":"磁盘最大使用率","type":"gauge","promql":"max((1 - node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"} / node_filesystem_size_bytes{fstype=~\\"ext4|xfs\\"}) * 100)","position":{"x":0,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p6","title":"网络总带宽","type":"line","promql":"sum(irate(node_network_receive_bytes_total{device!~\\"lo|docker.*|br-.*|veth.*\\"}[5m])) * 8","position":{"x":3,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"入站带宽"},{"id":"p7","title":"请求速率","type":"line","promql":"sum(rate(http_requests_total[5m]))","position":{"x":6,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"QPS"},{"id":"p8","title":"错误率","type":"line","promql":"sum(rate(http_requests_total{status=~\\"5..\\"}[5m])) / sum(rate(http_requests_total[5m])) * 100","position":{"x":9,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"5xx错误率"}]',
 '', '["大屏","运维","总览","综合"]', 1, 8, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-101: 业务监控大屏
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-101', '业务监控大屏', '综合大屏', '业务', '',
 '业务监控综合大屏，集中展示QPS趋势、P99延迟、错误率、成功率、状态码及热门接口',
 '[{"id":"p1","title":"QPS趋势","type":"line","promql":"sum(rate(http_requests_total[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"QPS"},{"id":"p2","title":"P99延迟","type":"line","promql":"histogram_quantile(0.99, sum by(le)(rate(http_request_duration_seconds_bucket[5m])))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"P99"},{"id":"p3","title":"错误率","type":"gauge","promql":"sum(rate(http_requests_total{status=~\\"5..\\"}[5m])) / sum(rate(http_requests_total[5m])) * 100","position":{"x":0,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"成功率","type":"gauge","promql":"(sum(rate(http_requests_total[5m])) - sum(rate(http_requests_total{status=~\\"5..\\"}[5m]))) / sum(rate(http_requests_total[5m])) * 100","position":{"x":3,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":95,"color":"orange"},{"value":99,"color":"green"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p5","title":"状态码分布","type":"pie","promql":"sum by(status)(increase(http_requests_total[1h]))","position":{"x":6,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"palette","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{status}}"},{"id":"p6","title":"接口 Top10","type":"bar","promql":"topk(10, sum by(handler)(rate(http_requests_total[5m])))","position":{"x":9,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"palette","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{handler}}"}]',
 '', '["大屏","业务","qps","延迟","综合"]', 1, 6, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-102: 基础设施大屏
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-102', '基础设施大屏', '综合大屏', '基础设施', '',
 '基础设施综合大屏，集中展示主机数量、在线率、CPU热力图、内存/磁盘Top5及网络流量',
 '[{"id":"p1","title":"主机数","type":"stat","promql":"count(up{job=~\\"node.*\\"})","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"blue"}],"options":{"unit":"台","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"在线率","type":"gauge","promql":"count(up{job=~\\"node.*\\"} == 1) / count(up{job=~\\"node.*\\"}) * 100","position":{"x":3,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"red"},{"value":90,"color":"orange"},{"value":99,"color":"green"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"CPU热力图","type":"heatmap","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\"}[5m])) * 100)","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"spectrum","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"内存使用率 Top5","type":"bar","promql":"topk(5, (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100)","position":{"x":0,"y":4},"size":{"w":4,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p5","title":"磁盘使用率 Top5","type":"bar","promql":"topk(5, (1 - node_filesystem_avail_bytes{fstype=~\\"ext4|xfs\\"} / node_filesystem_size_bytes{fstype=~\\"ext4|xfs\\"}) * 100)","position":{"x":4,"y":4},"size":{"w":4,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}} - {{mountpoint}}"},{"id":"p6","title":"网络流量","type":"line","promql":"sum by(instance)(irate(node_network_receive_bytes_total{device!~\\"lo|docker.*|br-.*|veth.*\\"}[5m])) * 8","position":{"x":8,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":2,"colorMode":"palette","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 '', '["大屏","基础设施","主机","cpu","内存","综合"]', 1, 6, '[]', '2025-01-01 00:00:00');

-- --------------------------------------------------------------------------
-- dtpl-103: 告警中心大屏
-- --------------------------------------------------------------------------
INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-103', '告警中心大屏', '综合大屏', '告警', '',
 '告警中心综合大屏，集中展示当前告警总数、Critical/Warning告警数、告警趋势、严重程度分布及最近告警列表',
 '[{"id":"p1","title":"当前告警数","type":"stat","promql":"count(ALERTS{alertstate=\\"firing\\"}) OR vector(0)","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"Critical告警","type":"stat","promql":"count(ALERTS{alertstate=\\"firing\\",severity=\\"critical\\"}) OR vector(0)","position":{"x":4,"y":0},"size":{"w":4,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"Warning告警","type":"stat","promql":"count(ALERTS{alertstate=\\"firing\\",severity=\\"warning\\"}) OR vector(0)","position":{"x":8,"y":0},"size":{"w":4,"h":4},"thresholds":[{"value":0,"color":"green"},{"value":1,"color":"orange"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"告警趋势(24h)","type":"line","promql":"count(ALERTS{alertstate=\\"firing\\"}) OR vector(0)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"告警数"},{"id":"p5","title":"告警分布 by Severity","type":"pie","promql":"count by(severity)(ALERTS{alertstate=\\"firing\\"})","position":{"x":6,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"palette","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{severity}}"},{"id":"p6","title":"最近告警列表","type":"table","promql":"ALERTS{alertstate=\\"firing\\"}","position":{"x":9,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"orange"},{"value":1,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{alertname}} - {{severity}}"}]',
 '', '["大屏","告警","alert","critical","warning","综合"]', 1, 6, '[]', '2025-01-01 00:00:00');
