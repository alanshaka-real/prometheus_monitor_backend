-- ============================================================
-- V7_08: 进程监控面板模板 (10条)
-- 覆盖: CPU / 内存 / 文件描述符 / 线程 / I/O
-- 指标来源: process-exporter (namedprocess_namegroup_*)
-- ============================================================

INSERT IGNORE INTO prom_panel_template
    (id, name, description, category, sub_category, exporter_type,
     chart_type, promql, unit, unit_format,
     default_width, default_height, thresholds, options, tags, sort_order, created_at)
VALUES

-- ===================== CPU (2) =====================

('pt-process-01',
 '进程 CPU 使用率',
 '各进程组的总 CPU 使用率（user + system），按 groupname 分组展示趋势',
 '进程监控', 'CPU', 'process_exporter',
 'line',
 'sum by (groupname) (irate(namedprocess_namegroup_cpu_seconds_total{instance=~"{{instance}}"}[5m])) * 100',
 '%', 'percent',
 6, 3,
 '{"warning": 70, "critical": 90}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "CPU %", "fillOpacity": 15}',
 '["process", "cpu", "usage"]',
 1, '2025-01-01 00:00:00'),

('pt-process-02',
 '进程 CPU 各模式',
 '各进程组的 CPU 分模式用量（user / system），堆叠展示查看各模式占比',
 '进程监控', 'CPU', 'process_exporter',
 'line',
 'sum by (groupname, mode) (irate(namedprocess_namegroup_cpu_seconds_total{instance=~"{{instance}}"}[5m])) * 100',
 '%', 'percent',
 6, 3,
 '{"warning": 60, "critical": 85}',
 '{"legend": true, "stacked": true, "decimals": 2, "yAxisLabel": "CPU %", "fillOpacity": 40, "legendFormat": "{{groupname}} - {{mode}}"}',
 '["process", "cpu", "mode", "stacked"]',
 2, '2025-01-01 00:00:00'),

-- ===================== 内存 (3) =====================

('pt-process-03',
 '常驻内存 RSS',
 '各进程组的常驻内存（Resident Set Size），反映实际物理内存占用',
 '进程监控', '内存', 'process_exporter',
 'line',
 'sum by (groupname) (namedprocess_namegroup_memory_bytes{memtype="resident",instance=~"{{instance}}"})',
 'bytes', 'bytes_iec',
 6, 3,
 '{"warning": 2147483648, "critical": 4294967296}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "RSS", "fillOpacity": 15}',
 '["process", "memory", "rss"]',
 3, '2025-01-01 00:00:00'),

('pt-process-04',
 '虚拟内存 VMS',
 '各进程组的虚拟内存大小（Virtual Memory Size），包含映射但未驻留的内存',
 '进程监控', '内存', 'process_exporter',
 'line',
 'sum by (groupname) (namedprocess_namegroup_memory_bytes{memtype="virtual",instance=~"{{instance}}"})',
 'bytes', 'bytes_iec',
 6, 3,
 '{"warning": 8589934592, "critical": 17179869184}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "VMS", "fillOpacity": 10}',
 '["process", "memory", "vms"]',
 4, '2025-01-01 00:00:00'),

('pt-process-05',
 '内存使用 TOP5',
 '按常驻内存排序的 TOP5 进程组，快速定位内存大户',
 '进程监控', '内存', 'process_exporter',
 'bar',
 'topk(5, sum by (groupname) (namedprocess_namegroup_memory_bytes{memtype="resident",instance=~"{{instance}}"})) ',
 'bytes', 'bytes_iec',
 6, 3,
 '{"warning": 2147483648, "critical": 4294967296}',
 '{"legend": false, "orientation": "horizontal", "showValue": true, "decimals": 2, "sortBy": "value", "sortOrder": "desc"}',
 '["process", "memory", "top5"]',
 5, '2025-01-01 00:00:00'),

-- ===================== 文件描述符 (2) =====================

('pt-process-06',
 'FD 打开数',
 '各进程组当前打开的文件描述符数量，持续增长可能存在 FD 泄漏',
 '进程监控', '文件描述符', 'process_exporter',
 'line',
 'sum by (groupname) (namedprocess_namegroup_open_filedesc{instance=~"{{instance}}"})',
 'count', 'number',
 6, 3,
 '{"warning": 5000, "critical": 20000}',
 '{"legend": true, "stacked": false, "decimals": 0, "yAxisLabel": "Open FDs", "fillOpacity": 10}',
 '["process", "fd", "open"]',
 6, '2025-01-01 00:00:00'),

('pt-process-07',
 'FD 使用率',
 '进程组中最差（最高）的文件描述符使用率，接近 1 表示即将耗尽 FD',
 '进程监控', '文件描述符', 'process_exporter',
 'gauge',
 'max by (groupname) (namedprocess_namegroup_worst_fd_ratio{instance=~"{{instance}}"}) * 100',
 '%', 'percent',
 3, 3,
 '{"warning": 60, "critical": 85}',
 '{"decimals": 1, "minValue": 0, "maxValue": 100, "thresholdMode": "percentage"}',
 '["process", "fd", "usage", "ratio"]',
 7, '2025-01-01 00:00:00'),

-- ===================== 线程 (1) =====================

('pt-process-08',
 '线程数',
 '各进程组的总线程数，线程数突增可能意味着线程泄漏或并发异常',
 '进程监控', '线程', 'process_exporter',
 'line',
 'sum by (groupname) (namedprocess_namegroup_num_threads{instance=~"{{instance}}"})',
 'count', 'number',
 6, 3,
 '{"warning": 500, "critical": 2000}',
 '{"legend": true, "stacked": false, "decimals": 0, "yAxisLabel": "Threads", "fillOpacity": 10}',
 '["process", "thread", "count"]',
 8, '2025-01-01 00:00:00'),

-- ===================== I/O (2) =====================

('pt-process-09',
 '读取字节速率',
 '各进程组每秒从磁盘读取的字节数，高读取可能意味着缓存未命中或日志扫描',
 '进程监控', 'I/O', 'process_exporter',
 'line',
 'sum by (groupname) (irate(namedprocess_namegroup_read_bytes_total{instance=~"{{instance}}"}[5m]))',
 'Bps', 'bytes_iec',
 6, 3,
 '{"warning": 52428800, "critical": 209715200}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "Read (Bps)", "fillOpacity": 15}',
 '["process", "io", "read"]',
 9, '2025-01-01 00:00:00'),

('pt-process-10',
 '写入字节速率',
 '各进程组每秒向磁盘写入的字节数，持续高写入需关注日志量或数据持久化频率',
 '进程监控', 'I/O', 'process_exporter',
 'line',
 'sum by (groupname) (irate(namedprocess_namegroup_write_bytes_total{instance=~"{{instance}}"}[5m]))',
 'Bps', 'bytes_iec',
 6, 3,
 '{"warning": 52428800, "critical": 209715200}',
 '{"legend": true, "stacked": false, "decimals": 2, "yAxisLabel": "Write (Bps)", "fillOpacity": 15}',
 '["process", "io", "write"]',
 10, '2025-01-01 00:00:00');
