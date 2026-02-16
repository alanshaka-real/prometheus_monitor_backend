-- ============================================================================
-- V8_02: Dashboard Templates Part 2 (dtpl-029 ~ dtpl-056)
-- 28 templates: MySQL(8) + Redis(6) + Nginx(5) + 探针监控(6) + 进程监控(3)
-- ============================================================================

-- ============================================================================
-- MySQL (category: 'MySQL', exporter_type: 'mysqld_exporter') — 8 templates
-- ============================================================================

-- dtpl-029: MySQL概览
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-029', 'MySQL概览', 'MySQL', '综合', 'mysqld_exporter',
 'MySQL 综合监控仪表盘，涵盖连接数、QPS、慢查询、缓冲池命中率、线程运行数、网络流量、查询类型分布和连接使用率',
 '[
   {"id":"p1","title":"连接数","type":"gauge","promql":"mysql_global_status_threads_connected{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":200,"color":"#faad14"},{"value":300,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"QPS","type":"line","promql":"rate(mysql_global_status_queries{instance=~\\"$instance\\"}[5m])","position":{"x":3,"y":0},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"慢查询","type":"line","promql":"rate(mysql_global_status_slow_queries{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":5,"color":"#f5222d"}],"options":{"unit":"ops","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"缓冲池命中率","type":"gauge","promql":"(1 - rate(mysql_global_status_innodb_buffer_pool_reads{instance=~\\"$instance\\"}[5m]) / rate(mysql_global_status_innodb_buffer_pool_read_requests{instance=~\\"$instance\\"}[5m])) * 100","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":95,"color":"#faad14"},{"value":99,"color":"#52c41a"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"线程运行数","type":"stat","promql":"mysql_global_status_threads_running{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":30,"color":"#faad14"},{"value":60,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"网络流量","type":"line","promql":"rate(mysql_global_status_bytes_received{instance=~\\"$instance\\"}[5m])","position":{"x":3,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} received"},
   {"id":"p7","title":"查询类型分布","type":"line","promql":"rate(mysql_global_status_commands_total{command=~\\"select|insert|update|delete\\",instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"normal"},"legendFormat":"{{command}}"},
   {"id":"p8","title":"连接使用率","type":"gauge","promql":"mysql_global_status_threads_connected{instance=~\\"$instance\\"} / mysql_global_variables_max_connections{instance=~\\"$instance\\"} * 100","position":{"x":9,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":70,"color":"#faad14"},{"value":85,"color":"#f5222d"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","overview","综合"]', '1.0.0', 8,
 '["pt-mysql-01","pt-mysql-02","pt-mysql-03","pt-mysql-05","pt-mysql-06","pt-mysql-07","pt-mysql-10","pt-mysql-21"]',
 '2025-01-01 00:00:00');

-- dtpl-030: 性能监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-030', '性能监控', 'MySQL', '性能', 'mysqld_exporter',
 'MySQL 性能监控仪表盘，聚焦 QPS、查询类型分布、慢查询率、行操作速率、排序操作和临时表使用',
 '[
   {"id":"p1","title":"QPS","type":"line","promql":"rate(mysql_global_status_queries{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"查询类型分布","type":"line","promql":"rate(mysql_global_status_commands_total{command=~\\"select|insert|update|delete\\",instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"normal"},"legendFormat":"{{command}}"},
   {"id":"p3","title":"慢查询率","type":"line","promql":"rate(mysql_global_status_slow_queries{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":5,"color":"#f5222d"}],"options":{"unit":"ops","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"行操作速率","type":"line","promql":"rate(mysql_global_status_innodb_row_ops_total{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"normal"},"legendFormat":"{{operation}}"},
   {"id":"p5","title":"排序操作","type":"line","promql":"rate(mysql_global_status_sort_rows{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":8},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"临时表","type":"line","promql":"rate(mysql_global_status_created_tmp_tables{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":8},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","performance","性能"]', '1.0.0', 6,
 '["pt-mysql-05","pt-mysql-06","pt-mysql-07","pt-mysql-12"]',
 '2025-01-01 00:00:00');

-- dtpl-031: InnoDB引擎
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-031', 'InnoDB引擎', 'MySQL', 'InnoDB', 'mysqld_exporter',
 'InnoDB 存储引擎专项监控，包含缓冲池命中率、缓冲池使用率、行操作、死锁、锁等待和日志写入',
 '[
   {"id":"p1","title":"缓冲池命中率","type":"gauge","promql":"(1 - rate(mysql_global_status_innodb_buffer_pool_reads{instance=~\\"$instance\\"}[5m]) / rate(mysql_global_status_innodb_buffer_pool_read_requests{instance=~\\"$instance\\"}[5m])) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":95,"color":"#faad14"},{"value":99,"color":"#52c41a"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"缓冲池使用率","type":"gauge","promql":"(mysql_global_status_innodb_buffer_pool_pages_total{instance=~\\"$instance\\"} - mysql_global_status_innodb_buffer_pool_pages_free{instance=~\\"$instance\\"}) / mysql_global_status_innodb_buffer_pool_pages_total{instance=~\\"$instance\\"} * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":85,"color":"#faad14"},{"value":95,"color":"#f5222d"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"行操作","type":"line","promql":"rate(mysql_global_status_innodb_row_ops_total{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"normal"},"legendFormat":"{{operation}}"},
   {"id":"p4","title":"死锁","type":"stat","promql":"mysql_global_status_innodb_deadlocks{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":10,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"锁等待","type":"line","promql":"rate(mysql_global_status_innodb_row_lock_waits{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":10,"color":"#faad14"},{"value":50,"color":"#f5222d"}],"options":{"unit":"ops","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"日志写入","type":"line","promql":"rate(mysql_global_status_innodb_os_log_written{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":8},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","innodb","engine"]', '1.0.0', 6,
 '["pt-mysql-10","pt-mysql-11","pt-mysql-12","pt-mysql-13","pt-mysql-14"]',
 '2025-01-01 00:00:00');

-- dtpl-032: 主从复制
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-032', '主从复制', 'MySQL', '复制', 'mysqld_exporter',
 'MySQL 主从复制监控仪表盘，包含主从延迟、IO线程状态、SQL线程状态和复制错误',
 '[
   {"id":"p1","title":"主从延迟","type":"gauge","promql":"mysql_slave_status_seconds_behind_master{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":5,"color":"#faad14"},{"value":30,"color":"#f5222d"}],"options":{"unit":"s","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"IO线程状态","type":"stat","promql":"mysql_slave_status_slave_io_running{instance=~\\"$instance\\"}","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":1,"color":"#52c41a"},{"value":0,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"SQL线程状态","type":"stat","promql":"mysql_slave_status_slave_sql_running{instance=~\\"$instance\\"}","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":1,"color":"#52c41a"},{"value":0,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"复制错误","type":"stat","promql":"mysql_slave_status_last_errno{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":12,"h":4},"thresholds":[{"value":0,"color":"#52c41a"},{"value":1,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","replication","复制"]', '1.0.0', 4,
 '["pt-mysql-15","pt-mysql-16","pt-mysql-17","pt-mysql-18"]',
 '2025-01-01 00:00:00');

-- dtpl-033: 查询分析
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-033', '查询分析', 'MySQL', '查询', 'mysqld_exporter',
 'MySQL 查询分析仪表盘，包含 QPS 趋势、Select/Insert/Update/Delete 速率、慢查询和全表扫描',
 '[
   {"id":"p1","title":"QPS趋势","type":"line","promql":"rate(mysql_global_status_queries{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"Select速率","type":"line","promql":"rate(mysql_global_status_commands_total{command=\\"select\\",instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"Insert速率","type":"line","promql":"rate(mysql_global_status_commands_total{command=\\"insert\\",instance=~\\"$instance\\"}[5m])","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"Update速率","type":"line","promql":"rate(mysql_global_status_commands_total{command=\\"update\\",instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"Delete速率","type":"line","promql":"rate(mysql_global_status_commands_total{command=\\"delete\\",instance=~\\"$instance\\"}[5m])","position":{"x":3,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"慢查询","type":"line","promql":"rate(mysql_global_status_slow_queries{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":5,"color":"#f5222d"}],"options":{"unit":"ops","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p7","title":"全表扫描","type":"line","promql":"rate(mysql_global_status_select_scan{instance=~\\"$instance\\"}[5m])","position":{"x":9,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":10,"color":"#faad14"},{"value":100,"color":"#f5222d"}],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","query","分析"]', '1.0.0', 6,
 '["pt-mysql-05","pt-mysql-06","pt-mysql-07","pt-mysql-08","pt-mysql-09"]',
 '2025-01-01 00:00:00');

-- dtpl-034: 连接池监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-034', '连接池监控', 'MySQL', '连接', 'mysqld_exporter',
 'MySQL 连接池监控仪表盘，包含活跃连接、最大连接使用率、中断连接和线程缓存命中率',
 '[
   {"id":"p1","title":"活跃连接","type":"line","promql":"mysql_global_status_threads_connected{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":200,"color":"#faad14"},{"value":300,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"最大连接使用率","type":"gauge","promql":"mysql_global_status_threads_connected{instance=~\\"$instance\\"} / mysql_global_variables_max_connections{instance=~\\"$instance\\"} * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"#faad14"},{"value":85,"color":"#f5222d"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"中断连接","type":"line","promql":"rate(mysql_global_status_aborted_connects{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":5,"color":"#faad14"},{"value":20,"color":"#f5222d"}],"options":{"unit":"ops","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"线程缓存命中率","type":"gauge","promql":"(1 - rate(mysql_global_status_threads_created{instance=~\\"$instance\\"}[5m]) / rate(mysql_global_status_connections{instance=~\\"$instance\\"}[5m])) * 100","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":90,"color":"#faad14"},{"value":99,"color":"#52c41a"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","connection","连接"]', '1.0.0', 4,
 '["pt-mysql-01","pt-mysql-02","pt-mysql-04"]',
 '2025-01-01 00:00:00');

-- dtpl-035: 慢查询分析
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-035', '慢查询分析', 'MySQL', '慢查询', 'mysqld_exporter',
 'MySQL 慢查询分析仪表盘，包含慢查询速率、慢查询累计、查询延迟分布和临时磁盘表',
 '[
   {"id":"p1","title":"慢查询速率","type":"line","promql":"rate(mysql_global_status_slow_queries{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":5,"color":"#f5222d"}],"options":{"unit":"ops","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"慢查询累计","type":"line","promql":"mysql_global_status_slow_queries{instance=~\\"$instance\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"查询延迟分布","type":"line","promql":"rate(mysql_global_status_innodb_row_lock_time{instance=~\\"$instance\\"}[5m]) / 1000","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ms","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"临时磁盘表","type":"line","promql":"rate(mysql_global_status_created_tmp_disk_tables{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":5,"color":"#faad14"},{"value":20,"color":"#f5222d"}],"options":{"unit":"ops","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","slow_query","慢查询"]', '1.0.0', 4,
 '["pt-mysql-06"]',
 '2025-01-01 00:00:00');

-- dtpl-036: 表空间监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-036', '表空间监控', 'MySQL', '存储', 'mysqld_exporter',
 'MySQL 表空间监控仪表盘，包含打开表数、表锁等待、数据文件大小和索引大小',
 '[
   {"id":"p1","title":"打开表数","type":"line","promql":"mysql_global_status_open_tables{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":2000,"color":"#faad14"},{"value":4000,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"表锁等待","type":"line","promql":"rate(mysql_global_status_table_locks_waited{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":5,"color":"#faad14"},{"value":20,"color":"#f5222d"}],"options":{"unit":"ops","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"数据文件大小","type":"line","promql":"mysql_global_status_innodb_data_fsyncs{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"索引大小","type":"line","promql":"mysql_global_status_innodb_buffer_pool_bytes_data{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["mysql","storage","存储"]', '1.0.0', 4,
 '["pt-mysql-19","pt-mysql-20"]',
 '2025-01-01 00:00:00');

-- ============================================================================
-- Redis (category: 'Redis', exporter_type: 'redis_exporter') — 6 templates
-- ============================================================================

-- dtpl-037: Redis概览
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-037', 'Redis概览', 'Redis', '综合', 'redis_exporter',
 'Redis 综合监控仪表盘，涵盖内存使用、QPS、命中率、客户端连接、Key总数和运行时间',
 '[
   {"id":"p1","title":"内存使用","type":"line","promql":"redis_memory_used_bytes{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"QPS","type":"stat","promql":"redis_instantaneous_ops_per_sec{instance=~\\"$instance\\"}","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":5000,"color":"#52c41a"},{"value":30000,"color":"#faad14"},{"value":80000,"color":"#f5222d"}],"options":{"unit":"ops","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"命中率","type":"gauge","promql":"rate(redis_keyspace_hits_total{instance=~\\"$instance\\"}[5m]) / (rate(redis_keyspace_hits_total{instance=~\\"$instance\\"}[5m]) + rate(redis_keyspace_misses_total{instance=~\\"$instance\\"}[5m])) * 100","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":80,"color":"#f5222d"},{"value":90,"color":"#faad14"},{"value":100,"color":"#52c41a"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"客户端连接","type":"gauge","promql":"redis_connected_clients{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":100,"color":"#52c41a"},{"value":500,"color":"#faad14"},{"value":5000,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"Key总数","type":"stat","promql":"sum(redis_db_keys{instance=~\\"$instance\\"}) by (instance)","position":{"x":3,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"运行时间","type":"stat","promql":"redis_uptime_in_seconds{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["redis","overview","综合"]', '1.0.0', 6,
 '["pt-redis-01","pt-redis-06","pt-redis-07","pt-redis-09","pt-redis-18"]',
 '2025-01-01 00:00:00');

-- dtpl-038: 内存分析
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-038', '内存分析', 'Redis', '内存', 'redis_exporter',
 'Redis 内存分析仪表盘，包含内存使用趋势、内存碎片率、峰值内存和 Key 驱逐速率',
 '[
   {"id":"p1","title":"内存使用趋势","type":"line","promql":"redis_memory_used_bytes{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"内存碎片率","type":"gauge","promql":"redis_memory_used_rss_bytes{instance=~\\"$instance\\"} / redis_memory_used_bytes{instance=~\\"$instance\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":1.0,"color":"#f5222d"},{"value":1.5,"color":"#52c41a"},{"value":2.0,"color":"#faad14"}],"options":{"unit":"","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"峰值内存","type":"stat","promql":"redis_memory_used_peak_bytes{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"Key驱逐速率","type":"line","promql":"rate(redis_evicted_keys_total{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"#52c41a"},{"value":10,"color":"#faad14"},{"value":100,"color":"#f5222d"}],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["redis","memory","内存"]', '1.0.0', 4,
 '["pt-redis-01","pt-redis-02","pt-redis-03","pt-redis-04"]',
 '2025-01-01 00:00:00');

-- dtpl-039: 性能监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-039', '性能监控', 'Redis', '性能', 'redis_exporter',
 'Redis 性能监控仪表盘，包含命令处理速率、命令类型分布、命中率趋势和延迟',
 '[
   {"id":"p1","title":"命令处理速率","type":"line","promql":"rate(redis_commands_processed_total{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"命令类型分布","type":"line","promql":"rate(redis_commands_total{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"normal"},"legendFormat":"{{cmd}}"},
   {"id":"p3","title":"命中率趋势","type":"line","promql":"rate(redis_keyspace_hits_total{instance=~\\"$instance\\"}[5m]) / (rate(redis_keyspace_hits_total{instance=~\\"$instance\\"}[5m]) + rate(redis_keyspace_misses_total{instance=~\\"$instance\\"}[5m])) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"延迟","type":"line","promql":"redis_instantaneous_input_kbps{instance=~\\"$instance\\"} + redis_instantaneous_output_kbps{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ms","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["redis","performance","性能"]', '1.0.0', 4,
 '["pt-redis-05","pt-redis-07","pt-redis-08"]',
 '2025-01-01 00:00:00');

-- dtpl-040: 复制监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-040', '复制监控', 'Redis', '复制', 'redis_exporter',
 'Redis 复制监控仪表盘，包含从节点数、复制偏移量差、复制延迟和连接状态',
 '[
   {"id":"p1","title":"从节点数","type":"stat","promql":"redis_connected_slaves{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"#f5222d"},{"value":1,"color":"#faad14"},{"value":2,"color":"#52c41a"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"复制偏移量差","type":"line","promql":"redis_master_repl_offset{instance=~\\"$instance\\"} - on(instance) group_right redis_slave_repl_offset{instance=~\\"$instance\\"}","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[{"value":1024,"color":"#52c41a"},{"value":1048576,"color":"#faad14"},{"value":104857600,"color":"#f5222d"}],"options":{"unit":"bytes","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"复制延迟","type":"line","promql":"redis_replication_delay{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":10,"color":"#f5222d"}],"options":{"unit":"s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"连接状态","type":"stat","promql":"redis_master_link_up{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"#f5222d"},{"value":1,"color":"#52c41a"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["redis","replication","复制"]', '1.0.0', 4,
 '["pt-redis-16","pt-redis-17"]',
 '2025-01-01 00:00:00');

-- dtpl-041: 集群监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-041', '集群监控', 'Redis', '集群', 'redis_exporter',
 'Redis 集群监控仪表盘，包含集群状态、分片分布、Key分布和内存分布',
 '[
   {"id":"p1","title":"集群状态","type":"stat","promql":"redis_cluster_state{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"#f5222d"},{"value":1,"color":"#52c41a"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"分片分布","type":"line","promql":"redis_cluster_slots_assigned{instance=~\\"$instance\\"}","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"Key分布","type":"line","promql":"redis_db_keys{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{db}}"},
   {"id":"p4","title":"内存分布","type":"line","promql":"redis_memory_used_bytes{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["redis","cluster","集群"]', '1.0.0', 4,
 '["pt-redis-01","pt-redis-18"]',
 '2025-01-01 00:00:00');

-- dtpl-042: Key分析
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-042', 'Key分析', 'Redis', 'Key', 'redis_exporter',
 'Redis Key 分析仪表盘，包含 Key 总数趋势、过期 Key、驱逐 Key 和 DB Key 分布',
 '[
   {"id":"p1","title":"Key总数趋势","type":"line","promql":"sum(redis_db_keys{instance=~\\"$instance\\"}) by (instance)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"过期Key","type":"line","promql":"rate(redis_expired_keys_total{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"驱逐Key","type":"line","promql":"rate(redis_evicted_keys_total{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"#52c41a"},{"value":10,"color":"#faad14"},{"value":100,"color":"#f5222d"}],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"DB Key分布","type":"line","promql":"redis_db_keys{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{db}}"}
 ]',
 '', '["redis","key","分析"]', '1.0.0', 4,
 '["pt-redis-04","pt-redis-18"]',
 '2025-01-01 00:00:00');

-- ============================================================================
-- Nginx (category: 'Nginx', exporter_type: 'nginx_exporter') — 5 templates
-- ============================================================================

-- dtpl-043: Nginx概览
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-043', 'Nginx概览', 'Nginx', '综合', 'nginx_exporter',
 'Nginx 综合监控仪表盘，涵盖 Active 连接、请求速率、状态码分布、错误率、QPS 和 Waiting 连接',
 '[
   {"id":"p1","title":"Active连接","type":"line","promql":"nginx_connections_active{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[{"value":5000,"color":"#faad14"},{"value":10000,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"请求速率","type":"line","promql":"irate(nginx_http_requests_total{instance=~\\"$instance\\"}[5m])","position":{"x":4,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"状态码分布","type":"line","promql":"sum by (status) (irate(nginx_http_responses_total{instance=~\\"$instance\\"}[5m]))","position":{"x":8,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"resp/s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{status}}"},
   {"id":"p4","title":"错误率","type":"gauge","promql":"(sum(irate(nginx_http_responses_total{status=~\\"4..|5..\\",instance=~\\"$instance\\"}[5m])) / sum(irate(nginx_http_responses_total{instance=~\\"$instance\\"}[5m]))) * 100","position":{"x":0,"y":4},"size":{"w":3,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":5,"color":"#f5222d"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"QPS","type":"stat","promql":"irate(nginx_http_requests_total{instance=~\\"$instance\\"}[5m])","position":{"x":3,"y":4},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"Waiting连接","type":"line","promql":"nginx_connections_waiting{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["nginx","overview","综合"]', '1.0.0', 6,
 '["pt-nginx-01","pt-nginx-04","pt-nginx-06","pt-nginx-07","pt-nginx-08","pt-nginx-09"]',
 '2025-01-01 00:00:00');

-- dtpl-044: 流量分析
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-044', '流量分析', 'Nginx', '流量', 'nginx_exporter',
 'Nginx 流量分析仪表盘，包含请求速率、响应大小、状态码分布和带宽',
 '[
   {"id":"p1","title":"请求速率","type":"line","promql":"irate(nginx_http_requests_total{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"响应大小","type":"line","promql":"irate(nginx_http_response_size_bytes{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"状态码分布","type":"line","promql":"sum by (status) (irate(nginx_http_responses_total{instance=~\\"$instance\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"resp/s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{status}}"},
   {"id":"p4","title":"带宽","type":"line","promql":"irate(nginx_http_request_size_bytes{instance=~\\"$instance\\"}[5m]) + irate(nginx_http_response_size_bytes{instance=~\\"$instance\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["nginx","traffic","流量"]', '1.0.0', 4,
 '["pt-nginx-06","pt-nginx-08"]',
 '2025-01-01 00:00:00');

-- dtpl-045: 上游监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-045', '上游监控', 'Nginx', '上游', 'nginx_exporter',
 'Nginx 上游后端监控仪表盘，包含响应时间、活跃连接、失败率和上游请求分布',
 '[
   {"id":"p1","title":"响应时间","type":"line","promql":"histogram_quantile(0.95, sum by (upstream, le) (rate(nginx_upstream_request_duration_seconds_bucket{instance=~\\"$instance\\"}[5m])))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0.5,"color":"#faad14"},{"value":2,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{upstream}}"},
   {"id":"p2","title":"活跃连接","type":"line","promql":"nginx_upstream_connections_active{instance=~\\"$instance\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":500,"color":"#faad14"},{"value":1000,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{upstream}}"},
   {"id":"p3","title":"失败率","type":"gauge","promql":"(sum(irate(nginx_upstream_responses_total{status=~\\"502|504\\",instance=~\\"$instance\\"}[5m])) / sum(irate(nginx_upstream_responses_total{instance=~\\"$instance\\"}[5m]))) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0.5,"color":"#faad14"},{"value":3,"color":"#f5222d"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p4","title":"上游请求分布","type":"line","promql":"sum by (upstream) (irate(nginx_upstream_responses_total{instance=~\\"$instance\\"}[5m]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{upstream}}"}
 ]',
 '', '["nginx","upstream","上游"]', '1.0.0', 4,
 '["pt-nginx-10","pt-nginx-11","pt-nginx-12"]',
 '2025-01-01 00:00:00');

-- dtpl-046: 错误分析
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-046', '错误分析', 'Nginx', '错误', 'nginx_exporter',
 'Nginx 错误分析仪表盘，包含 4xx 错误率、5xx 错误率、错误趋势和错误占比',
 '[
   {"id":"p1","title":"4xx错误率","type":"gauge","promql":"(sum(irate(nginx_http_responses_total{status=~\\"4..\\",instance=~\\"$instance\\"}[5m])) / sum(irate(nginx_http_responses_total{instance=~\\"$instance\\"}[5m]))) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":5,"color":"#f5222d"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p2","title":"5xx错误率","type":"gauge","promql":"(sum(irate(nginx_http_responses_total{status=~\\"5..\\",instance=~\\"$instance\\"}[5m])) / sum(irate(nginx_http_responses_total{instance=~\\"$instance\\"}[5m]))) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0.5,"color":"#faad14"},{"value":2,"color":"#f5222d"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p3","title":"错误趋势","type":"line","promql":"sum by (status) (irate(nginx_http_responses_total{status=~\\"4..|5..\\",instance=~\\"$instance\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"resp/s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{status}}"},
   {"id":"p4","title":"错误占比","type":"pie","promql":"sum by (status) (increase(nginx_http_responses_total{status=~\\"4..|5..\\",instance=~\\"$instance\\"}[1h]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{status}}"}
 ]',
 '', '["nginx","error","错误"]', '1.0.0', 4,
 '["pt-nginx-08","pt-nginx-09"]',
 '2025-01-01 00:00:00');

-- dtpl-047: 连接监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-047', '连接监控', 'Nginx', '连接', 'nginx_exporter',
 'Nginx 连接监控仪表盘，包含 Active 连接、Reading/Writing/Waiting、Accepted 速率和连接利用率',
 '[
   {"id":"p1","title":"Active连接","type":"line","promql":"nginx_connections_active{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":5000,"color":"#faad14"},{"value":10000,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"Reading/Writing/Waiting","type":"line","promql":"nginx_connections_reading{instance=~\\"$instance\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"reading - {{instance}}"},
   {"id":"p3","title":"Accepted速率","type":"line","promql":"irate(nginx_connections_accepted{instance=~\\"$instance\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":3000,"color":"#faad14"},{"value":8000,"color":"#f5222d"}],"options":{"unit":"conn/s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"连接利用率","type":"gauge","promql":"nginx_connections_active{instance=~\\"$instance\\"} / nginx_connections_accepted{instance=~\\"$instance\\"} * 100","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"#faad14"},{"value":90,"color":"#f5222d"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["nginx","connection","连接"]', '1.0.0', 4,
 '["pt-nginx-01","pt-nginx-02","pt-nginx-03","pt-nginx-04","pt-nginx-05"]',
 '2025-01-01 00:00:00');

-- ============================================================================
-- 探针监控 (category: '探针监控', exporter_type: 'blackbox_exporter') — 6 templates
-- ============================================================================

-- dtpl-048: HTTP端点监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-048', 'HTTP端点监控', '探针监控', 'HTTP', 'blackbox_exporter',
 'HTTP 端点综合监控仪表盘，涵盖探测成功率、HTTP 耗时、状态码、SSL 到期、内容长度和重定向次数',
 '[
   {"id":"p1","title":"探测成功率","type":"gauge","promql":"probe_success{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"#f5222d"},{"value":1,"color":"#52c41a"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"HTTP耗时","type":"line","promql":"probe_http_duration_seconds{instance=~\\"$instance\\"}","position":{"x":3,"y":0},"size":{"w":5,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":3,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{phase}}"},
   {"id":"p3","title":"状态码","type":"stat","promql":"probe_http_status_code{instance=~\\"$instance\\"}","position":{"x":8,"y":0},"size":{"w":2,"h":4},"thresholds":[{"value":200,"color":"#52c41a"},{"value":400,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"SSL到期天数","type":"gauge","promql":"(probe_ssl_earliest_cert_expiry{instance=~\\"$instance\\"} - time()) / 86400","position":{"x":10,"y":0},"size":{"w":2,"h":4},"thresholds":[{"value":7,"color":"#f5222d"},{"value":30,"color":"#faad14"},{"value":365,"color":"#52c41a"}],"options":{"unit":"days","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"内容长度","type":"stat","promql":"probe_http_content_length{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"重定向次数","type":"stat","promql":"probe_http_redirects{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":3,"color":"#faad14"},{"value":5,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["blackbox","http","endpoint"]', '1.0.0', 6,
 '["pt-blackbox-01","pt-blackbox-02","pt-blackbox-03","pt-blackbox-04","pt-blackbox-05"]',
 '2025-01-01 00:00:00');

-- dtpl-049: 多协议探测
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-049', '多协议探测', '探针监控', '综合', 'blackbox_exporter',
 '多协议综合探测仪表盘，涵盖成功率概览、HTTP 耗时、TCP 耗时和 ICMP 耗时',
 '[
   {"id":"p1","title":"成功率概览","type":"gauge","promql":"avg(probe_success{instance=~\\"$instance\\"})","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0.5,"color":"#f5222d"},{"value":0.9,"color":"#faad14"},{"value":1,"color":"#52c41a"}],"options":{"unit":"","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p2","title":"HTTP耗时","type":"line","promql":"probe_http_duration_seconds{instance=~\\"$instance\\"}","position":{"x":3,"y":0},"size":{"w":9,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":3,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{phase}}"},
   {"id":"p3","title":"TCP耗时","type":"line","promql":"probe_duration_seconds{job=~\\".*tcp.*\\",instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0.5,"color":"#faad14"},{"value":2,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"ICMP耗时","type":"line","promql":"probe_icmp_duration_seconds{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0.1,"color":"#faad14"},{"value":0.5,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{phase}}"}
 ]',
 '', '["blackbox","multiprotocol","综合"]', '1.0.0', 4,
 '["pt-blackbox-01","pt-blackbox-02","pt-blackbox-10","pt-blackbox-13"]',
 '2025-01-01 00:00:00');

-- dtpl-050: SSL证书监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-050', 'SSL证书监控', '探针监控', 'SSL', 'blackbox_exporter',
 'SSL 证书监控仪表盘，包含证书到期天数、到期趋势、证书状态和即将到期告警',
 '[
   {"id":"p1","title":"证书到期天数","type":"gauge","promql":"(probe_ssl_earliest_cert_expiry{instance=~\\"$instance\\"} - time()) / 86400","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":7,"color":"#f5222d"},{"value":30,"color":"#faad14"},{"value":365,"color":"#52c41a"}],"options":{"unit":"days","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"到期趋势","type":"line","promql":"(probe_ssl_earliest_cert_expiry{instance=~\\"$instance\\"} - time()) / 86400","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"days","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"证书状态","type":"table","promql":"probe_ssl_earliest_cert_expiry{instance=~\\"$instance\\"} - time()","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"即将到期告警","type":"stat","promql":"count(((probe_ssl_earliest_cert_expiry{instance=~\\"$instance\\"} - time()) / 86400) < 30)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"#52c41a"},{"value":1,"color":"#faad14"},{"value":3,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""}
 ]',
 '', '["blackbox","ssl","certificate"]', '1.0.0', 4,
 '["pt-blackbox-04"]',
 '2025-01-01 00:00:00');

-- dtpl-051: DNS监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-051', 'DNS监控', '探针监控', 'DNS', 'blackbox_exporter',
 'DNS 监控仪表盘，包含 DNS 查询耗时、成功率、解析时间和 DNS 服务器对比',
 '[
   {"id":"p1","title":"DNS查询耗时","type":"line","promql":"probe_dns_lookup_time_seconds{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0.1,"color":"#faad14"},{"value":0.5,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"成功率","type":"gauge","promql":"probe_success{job=~\\".*dns.*\\",instance=~\\"$instance\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"#f5222d"},{"value":1,"color":"#52c41a"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"解析时间","type":"stat","promql":"probe_dns_lookup_time_seconds{instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0.1,"color":"#faad14"},{"value":0.5,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"DNS服务器对比","type":"line","promql":"probe_dns_lookup_time_seconds{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["blackbox","dns","监控"]', '1.0.0', 4,
 '["pt-blackbox-07","pt-blackbox-08","pt-blackbox-09"]',
 '2025-01-01 00:00:00');

-- dtpl-052: 网络连通性
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-052', '网络连通性', '探针监控', '网络', 'blackbox_exporter',
 '网络连通性监控仪表盘，包含 ICMP 延迟、丢包率、TCP 连接时间和连通性概览',
 '[
   {"id":"p1","title":"ICMP延迟","type":"line","promql":"probe_icmp_duration_seconds{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0.1,"color":"#faad14"},{"value":0.5,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{phase}}"},
   {"id":"p2","title":"丢包率","type":"gauge","promql":"(1 - probe_success{job=~\\".*icmp.*\\",instance=~\\"$instance\\"}) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":5,"color":"#faad14"},{"value":20,"color":"#f5222d"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"TCP连接时间","type":"line","promql":"probe_duration_seconds{job=~\\".*tcp.*\\",instance=~\\"$instance\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0.5,"color":"#faad14"},{"value":2,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"连通性概览","type":"stat","promql":"probe_success{instance=~\\"$instance\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"#f5222d"},{"value":1,"color":"#52c41a"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["blackbox","network","连通性"]', '1.0.0', 4,
 '["pt-blackbox-10","pt-blackbox-11","pt-blackbox-13","pt-blackbox-14"]',
 '2025-01-01 00:00:00');

-- dtpl-053: SLA可用性
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-053', 'SLA可用性', '探针监控', 'SLA', 'blackbox_exporter',
 'SLA 可用性监控仪表盘，包含可用性百分比、可用性趋势、故障次数和平均响应时间',
 '[
   {"id":"p1","title":"可用性百分比","type":"gauge","promql":"avg_over_time(probe_success{instance=~\\"$instance\\"}[24h]) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":95,"color":"#f5222d"},{"value":99,"color":"#faad14"},{"value":99.9,"color":"#52c41a"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"可用性趋势","type":"line","promql":"avg_over_time(probe_success{instance=~\\"$instance\\"}[1h]) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"故障次数","type":"stat","promql":"count_over_time((probe_success{instance=~\\"$instance\\"} == 0)[24h:1m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":0,"color":"#52c41a"},{"value":1,"color":"#faad14"},{"value":10,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"平均响应时间","type":"line","promql":"avg_over_time(probe_duration_seconds{instance=~\\"$instance\\"}[1h])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14"},{"value":3,"color":"#f5222d"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["blackbox","sla","availability"]', '1.0.0', 4,
 '["pt-blackbox-01"]',
 '2025-01-01 00:00:00');

-- ============================================================================
-- 进程监控 (category: '进程监控', exporter_type: 'process_exporter') — 3 templates
-- ============================================================================

-- dtpl-054: 进程资源监控
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-054', '进程资源监控', '进程监控', '资源', 'process_exporter',
 '进程资源监控仪表盘，包含 CPU 使用率、RSS 内存、FD 打开数和线程数',
 '[
   {"id":"p1","title":"CPU使用率","type":"line","promql":"sum by (groupname) (irate(namedprocess_namegroup_cpu_seconds_total{instance=~\\"$instance\\"}[5m])) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"#faad14"},{"value":90,"color":"#f5222d"}],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p2","title":"RSS内存","type":"line","promql":"sum by (groupname) (namedprocess_namegroup_memory_bytes{memtype=\\"resident\\",instance=~\\"$instance\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p3","title":"FD打开数","type":"line","promql":"sum by (groupname) (namedprocess_namegroup_open_filedesc{instance=~\\"$instance\\"})","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":5000,"color":"#faad14"},{"value":20000,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p4","title":"线程数","type":"line","promql":"sum by (groupname) (namedprocess_namegroup_num_threads{instance=~\\"$instance\\"})","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":500,"color":"#faad14"},{"value":2000,"color":"#f5222d"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{groupname}}"}
 ]',
 '', '["process","resource","资源"]', '1.0.0', 4,
 '["pt-process-01","pt-process-03","pt-process-06","pt-process-08"]',
 '2025-01-01 00:00:00');

-- dtpl-055: 应用进程概览
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-055', '应用进程概览', '进程监控', '概览', 'process_exporter',
 '应用进程概览仪表盘，包含 CPU 使用 Top5、内存使用 Top5、FD 使用 Top5 和进程数',
 '[
   {"id":"p1","title":"CPU使用Top5","type":"bar","promql":"topk(5, sum by (groupname) (irate(namedprocess_namegroup_cpu_seconds_total{instance=~\\"$instance\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p2","title":"内存使用Top5","type":"bar","promql":"topk(5, sum by (groupname) (namedprocess_namegroup_memory_bytes{memtype=\\"resident\\",instance=~\\"$instance\\"}))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p3","title":"FD使用Top5","type":"bar","promql":"topk(5, sum by (groupname) (namedprocess_namegroup_open_filedesc{instance=~\\"$instance\\"}))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p4","title":"进程数","type":"stat","promql":"sum(namedprocess_namegroup_num_procs{instance=~\\"$instance\\"})","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["process","overview","概览"]', '1.0.0', 4,
 '["pt-process-01","pt-process-03","pt-process-05","pt-process-06"]',
 '2025-01-01 00:00:00');

-- dtpl-056: 关键进程告警
INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-056', '关键进程告警', '进程监控', '告警', 'process_exporter',
 '关键进程告警仪表盘，包含进程存活状态、CPU 超限、内存超限和 FD 超限',
 '[
   {"id":"p1","title":"进程存活","type":"stat","promql":"namedprocess_namegroup_num_procs{instance=~\\"$instance\\"}","position":{"x":0,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":0,"color":"#f5222d"},{"value":1,"color":"#52c41a"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p2","title":"CPU超限","type":"line","promql":"sum by (groupname) (irate(namedprocess_namegroup_cpu_seconds_total{instance=~\\"$instance\\"}[5m])) * 100 > 80","position":{"x":3,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":80,"color":"#faad14"},{"value":95,"color":"#f5222d"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p3","title":"内存超限","type":"line","promql":"sum by (groupname) (namedprocess_namegroup_memory_bytes{memtype=\\"resident\\",instance=~\\"$instance\\"}) > 4294967296","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":4294967296,"color":"#faad14"},{"value":8589934592,"color":"#f5222d"}],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{groupname}}"},
   {"id":"p4","title":"FD超限","type":"line","promql":"max by (groupname) (namedprocess_namegroup_worst_fd_ratio{instance=~\\"$instance\\"}) * 100 > 60","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[{"value":60,"color":"#faad14"},{"value":85,"color":"#f5222d"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{groupname}}"}
 ]',
 '', '["process","alert","告警"]', '1.0.0', 4,
 '["pt-process-01","pt-process-03","pt-process-06","pt-process-07"]',
 '2025-01-01 00:00:00');
