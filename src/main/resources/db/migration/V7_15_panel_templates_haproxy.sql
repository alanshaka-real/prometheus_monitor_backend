-- ============================================================
-- V7_15: HAProxy 监控面板模板 (6条)
-- 覆盖: 前端 / 后端
-- ============================================================

INSERT IGNORE INTO prom_panel_template
    (id, name, description, category, sub_category, exporter_type,
     chart_type, promql, unit, unit_format,
     default_width, default_height, thresholds, options, tags, sort_order, created_at)
VALUES

-- ===================== 前端 (3) =====================

('pt-haproxy-01',
 '前端当前连接数',
 'HAProxy 各前端当前活跃会话数，接近 maxconn 时新请求将排队或被拒',
 'HAProxy', '前端', 'haproxy_exporter',
 'line',
 'haproxy_frontend_current_sessions{instance=~"{{instance}}",proxy=~"{{frontend}}"}',
 'count', 'short',
 6, 3,
 '{"warning": 500, "critical": 1000}',
 '{"legend": true, "stacked": false, "decimals": 0, "yAxisLabel": "Sessions", "fillOpacity": 15}',
 '["haproxy", "frontend", "sessions"]',
 1, '2025-01-01 00:00:00'),

('pt-haproxy-02',
 '前端流量',
 'HAProxy 各前端每秒接收和发送的字节数，衡量前端承载的带宽',
 'HAProxy', '前端', 'haproxy_exporter',
 'line',
 'rate(haproxy_frontend_bytes_in_total{instance=~"{{instance}}",proxy=~"{{frontend}}"}[5m]) + rate(haproxy_frontend_bytes_out_total{instance=~"{{instance}}",proxy=~"{{frontend}}"}[5m])',
 'Bps', 'bytes_iec',
 6, 3,
 '{"warning": 104857600, "critical": 524288000}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "Traffic (Bps)", "fillOpacity": 15}',
 '["haproxy", "frontend", "traffic"]',
 2, '2025-01-01 00:00:00'),

('pt-haproxy-03',
 '前端请求速率',
 'HAProxy 各前端每秒处理的 HTTP 请求数',
 'HAProxy', '前端', 'haproxy_exporter',
 'line',
 'rate(haproxy_frontend_http_requests_total{instance=~"{{instance}}",proxy=~"{{frontend}}"}[5m])',
 'ops', 'short',
 6, 3,
 '{}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "Requests/s", "fillOpacity": 10}',
 '["haproxy", "frontend", "requests"]',
 3, '2025-01-01 00:00:00'),

-- ===================== 后端 (3) =====================

('pt-haproxy-04',
 '后端平均响应时间',
 'HAProxy 各后端的平均响应时间，高延迟需检查后端服务性能',
 'HAProxy', '后端', 'haproxy_exporter',
 'line',
 'haproxy_backend_response_time_average_seconds{instance=~"{{instance}}",proxy=~"{{backend}}"}',
 's', 'duration',
 6, 3,
 '{"warning": 0.5, "critical": 2}',
 '{"legend": true, "stacked": false, "decimals": 4, "yAxisLabel": "Response Time (s)", "fillOpacity": 10}',
 '["haproxy", "backend", "response_time"]',
 4, '2025-01-01 00:00:00'),

('pt-haproxy-05',
 '后端错误率',
 'HAProxy 各后端每秒返回的连接错误和响应错误速率，错误率上升需排查后端健康',
 'HAProxy', '后端', 'haproxy_exporter',
 'line',
 'rate(haproxy_backend_connection_errors_total{instance=~"{{instance}}",proxy=~"{{backend}}"}[5m]) + rate(haproxy_backend_response_errors_total{instance=~"{{instance}}",proxy=~"{{backend}}"}[5m])',
 'ops', 'short',
 6, 3,
 '{"warning": 1, "critical": 10}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "Errors/s", "fillOpacity": 10}',
 '["haproxy", "backend", "errors"]',
 5, '2025-01-01 00:00:00'),

('pt-haproxy-06',
 '后端健康服务器比例',
 'HAProxy 各后端活跃服务器数占总服务器数的百分比，低于 100% 说明有后端节点下线',
 'HAProxy', '后端', 'haproxy_exporter',
 'gauge',
 'haproxy_backend_active_servers{instance=~"{{instance}}",proxy=~"{{backend}}"} / haproxy_backend_servers{instance=~"{{instance}}",proxy=~"{{backend}}"} * 100',
 '%', 'percent',
 3, 3,
 '{"warning": 80, "critical": 50}',
 '{"decimals": 1, "minValue": 0, "maxValue": 100, "thresholdMode": "percentage"}',
 '["haproxy", "backend", "health"]',
 6, '2025-01-01 00:00:00');
