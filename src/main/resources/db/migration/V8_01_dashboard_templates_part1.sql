-- ============================================================================
-- V8_01: Dashboard Templates Part 1 (dtpl-001 ~ dtpl-028)
-- Categories: 主机监控 (10), 容器监控 (6), K8s集群 (12)
-- Table: prom_dashboard_template
-- ============================================================================

-- Delete old template seed data
DELETE FROM prom_dashboard_template WHERE id LIKE 'tmpl-%';

INSERT IGNORE INTO prom_dashboard_template
  (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES

-- ============================================================================
-- 主机监控 (category: '主机监控', exporter_type: 'node_exporter', 10 templates)
-- ============================================================================

-- dtpl-001: 全面主机概览
('dtpl-001', '全面主机概览', '主机监控', '综合', 'node_exporter',
 '涵盖CPU、内存、磁盘、网络、系统负载等核心指标的综合监控面板，适用于日常巡检和快速定位主机问题',
 '[
   {"id":"p1","title":"CPU使用率","type":"line","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\",instance=~\\"{{instance}}\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"内存使用率","type":"line","promql":"(1 - node_memory_MemAvailable_bytes{instance=~\\"{{instance}}\\"} / node_memory_MemTotal_bytes{instance=~\\"{{instance}}\\"}) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":85,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"磁盘使用率","type":"line","promql":"(1 - node_filesystem_avail_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"} / node_filesystem_size_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"}) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{mountpoint}}"},
   {"id":"p4","title":"网络流量","type":"line","promql":"irate(node_network_receive_bytes_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m]) * 8","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}} 接收"},
   {"id":"p5","title":"系统负载","type":"line","promql":"node_load1{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":8},"size":{"w":6,"h":3},"thresholds":[{"value":4,"color":"orange"},{"value":8,"color":"red"}],"options":{"unit":"","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} load1"},
   {"id":"p6","title":"文件描述符使用率","type":"line","promql":"node_filefd_allocated{instance=~\\"{{instance}}\\"} / node_filefd_maximum{instance=~\\"{{instance}}\\"} * 100","position":{"x":6,"y":8},"size":{"w":6,"h":3},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p7","title":"运行时间","type":"line","promql":"(time() - node_boot_time_seconds{instance=~\\"{{instance}}\\"}) / 86400","position":{"x":0,"y":11},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"days","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p8","title":"TCP连接数","type":"line","promql":"node_netstat_Tcp_CurrEstab{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":11},"size":{"w":6,"h":3},"thresholds":[{"value":500,"color":"orange"},{"value":1000,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["主机","综合","node_exporter"]', '1.0.0', 8,
 '["pt-node-01","pt-node-09","pt-node-17","pt-node-25","pt-node-03","pt-node-35","pt-node-33","pt-node-29"]',
 '2025-01-01 00:00:00'),

-- dtpl-002: CPU深度分析
('dtpl-002', 'CPU深度分析', '主机监控', 'CPU', 'node_exporter',
 '深入分析CPU各维度指标，包括使用率、各模式占比、iowait、上下文切换等，帮助定位CPU瓶颈',
 '[
   {"id":"p1","title":"CPU使用率","type":"line","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\",instance=~\\"{{instance}}\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"CPU各模式占比","type":"line","promql":"avg by(mode,instance)(irate(node_cpu_seconds_total{instance=~\\"{{instance}}\\",mode=~\\"user|system|iowait|idle|nice|steal\\"}[5m])) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{mode}}"},
   {"id":"p3","title":"CPU iowait","type":"line","promql":"avg by(instance)(irate(node_cpu_seconds_total{mode=\\"iowait\\",instance=~\\"{{instance}}\\"}[5m])) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":10,"color":"orange"},{"value":30,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"上下文切换率","type":"line","promql":"irate(node_context_switches_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":50000,"color":"orange"},{"value":100000,"color":"red"}],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"系统负载","type":"line","promql":"node_load1{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":4,"color":"orange"},{"value":8,"color":"red"}],"options":{"unit":"","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} load1"},
   {"id":"p6","title":"CPU steal时间","type":"line","promql":"avg by(instance)(irate(node_cpu_seconds_total{mode=\\"steal\\",instance=~\\"{{instance}}\\"}[5m])) * 100","position":{"x":6,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":5,"color":"orange"},{"value":15,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["主机","CPU","node_exporter"]', '1.0.0', 6,
 '["pt-node-01","pt-node-02","pt-node-04","pt-node-05","pt-node-03","pt-node-08"]',
 '2025-01-01 00:00:00'),

-- dtpl-003: 内存分析
('dtpl-003', '内存分析', '主机监控', '内存', 'node_exporter',
 '全面分析主机内存使用情况，包括使用率、Swap、可用内存、页错误和缓存详情',
 '[
   {"id":"p1","title":"内存使用率","type":"line","promql":"(1 - node_memory_MemAvailable_bytes{instance=~\\"{{instance}}\\"} / node_memory_MemTotal_bytes{instance=~\\"{{instance}}\\"}) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":85,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"内存使用详情","type":"line","promql":"node_memory_MemTotal_bytes{instance=~\\"{{instance}}\\"} - node_memory_MemFree_bytes{instance=~\\"{{instance}}\\"} - node_memory_Buffers_bytes{instance=~\\"{{instance}}\\"} - node_memory_Cached_bytes{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":40,"showLegend":true,"stackMode":"stacked"},"legendFormat":"used"},
   {"id":"p3","title":"Swap使用率","type":"line","promql":"(1 - node_memory_SwapFree_bytes{instance=~\\"{{instance}}\\"} / node_memory_SwapTotal_bytes{instance=~\\"{{instance}}\\"}) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":50,"color":"orange"},{"value":80,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"可用内存","type":"line","promql":"node_memory_MemAvailable_bytes{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p5","title":"页错误率","type":"line","promql":"irate(node_vmstat_pgfault{instance=~\\"{{instance}}\\"}[5m])","position":{"x":0,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":5000,"color":"orange"},{"value":20000,"color":"red"}],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p6","title":"缓存大小","type":"line","promql":"node_memory_Cached_bytes{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":8},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["主机","内存","node_exporter"]', '1.0.0', 6,
 '["pt-node-09","pt-node-10","pt-node-11","pt-node-13","pt-node-15","pt-node-16"]',
 '2025-01-01 00:00:00'),

-- dtpl-004: 磁盘I/O分析
('dtpl-004', '磁盘I/O分析', '主机监控', '磁盘', 'node_exporter',
 '深入分析磁盘I/O性能，包括使用率、IOPS、读写吞吐、延迟和inode使用情况',
 '[
   {"id":"p1","title":"磁盘使用率","type":"line","promql":"(1 - node_filesystem_avail_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"} / node_filesystem_size_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"}) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{mountpoint}}"},
   {"id":"p2","title":"磁盘IOPS","type":"line","promql":"irate(node_disk_reads_completed_total{instance=~\\"{{instance}}\\",device!~\\"dm-.*\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"iops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}} reads"},
   {"id":"p3","title":"磁盘读吞吐","type":"line","promql":"irate(node_disk_read_bytes_total{instance=~\\"{{instance}}\\",device!~\\"dm-.*\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"},
   {"id":"p4","title":"磁盘写吞吐","type":"line","promql":"irate(node_disk_written_bytes_total{instance=~\\"{{instance}}\\",device!~\\"dm-.*\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"},
   {"id":"p5","title":"磁盘I/O延迟","type":"line","promql":"irate(node_disk_read_time_seconds_total{instance=~\\"{{instance}}\\",device!~\\"dm-.*\\"}[5m]) / irate(node_disk_reads_completed_total{instance=~\\"{{instance}}\\",device!~\\"dm-.*\\"}[5m])","position":{"x":0,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":0.01,"color":"orange"},{"value":0.05,"color":"red"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"},
   {"id":"p6","title":"inode使用率","type":"line","promql":"(1 - node_filesystem_files_free{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"} / node_filesystem_files{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"}) * 100","position":{"x":6,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{mountpoint}}"}
 ]',
 '', '["主机","磁盘","IO","node_exporter"]', '1.0.0', 6,
 '["pt-node-17","pt-node-18","pt-node-22","pt-node-23","pt-node-20","pt-node-21"]',
 '2025-01-01 00:00:00'),

-- dtpl-005: 网络流量分析
('dtpl-005', '网络流量分析', '主机监控', '网络', 'node_exporter',
 '全面分析主机网络流量情况，包括接收/发送带宽、包量、错误、丢包和TCP连接状态',
 '[
   {"id":"p1","title":"接收带宽","type":"line","promql":"irate(node_network_receive_bytes_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m]) * 8","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"},
   {"id":"p2","title":"发送带宽","type":"line","promql":"irate(node_network_transmit_bytes_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m]) * 8","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"},
   {"id":"p3","title":"网络包量","type":"line","promql":"irate(node_network_receive_packets_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"pps","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}} rx"},
   {"id":"p4","title":"网络错误","type":"line","promql":"irate(node_network_receive_errs_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m]) + irate(node_network_transmit_errs_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"},
   {"id":"p5","title":"网络丢包率","type":"line","promql":"irate(node_network_receive_drop_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m]) + irate(node_network_transmit_drop_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m])","position":{"x":0,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"},
   {"id":"p6","title":"TCP连接状态","type":"line","promql":"node_netstat_Tcp_CurrEstab{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":500,"color":"orange"},{"value":1000,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["主机","网络","node_exporter"]', '1.0.0', 6,
 '["pt-node-25","pt-node-26","pt-node-27","pt-node-28","pt-node-30","pt-node-29"]',
 '2025-01-01 00:00:00'),

-- dtpl-006: 多主机对比
('dtpl-006', '多主机对比', '主机监控', '对比', 'node_exporter',
 '多台主机的核心指标横向对比，快速发现异常主机，适合多机环境运维',
 '[
   {"id":"p1","title":"CPU使用率对比","type":"line","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"内存使用率对比","type":"line","promql":"(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":85,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"磁盘使用率对比","type":"line","promql":"(1 - node_filesystem_avail_bytes{fstype!~\\"tmpfs|overlay\\"} / node_filesystem_size_bytes{fstype!~\\"tmpfs|overlay\\"}) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{mountpoint}}"},
   {"id":"p4","title":"网络流量对比","type":"line","promql":"irate(node_network_receive_bytes_total{device!~\\"lo|veth.*|docker.*|br.*\\"}[5m]) * 8","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}}"}
 ]',
 '', '["主机","对比","node_exporter"]', '1.0.0', 4,
 '["pt-node-01","pt-node-09","pt-node-17","pt-node-25"]',
 '2025-01-01 00:00:00'),

-- dtpl-007: 系统负载分析
('dtpl-007', '系统负载分析', '主机监控', '系统', 'node_exporter',
 '分析系统负载相关指标，包括load1/5/15、CPU使用率、进程数和上下文切换',
 '[
   {"id":"p1","title":"系统负载(load1/5/15)","type":"line","promql":"node_load1{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":4,"color":"orange"},{"value":8,"color":"red"}],"options":{"unit":"","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} load1"},
   {"id":"p2","title":"CPU使用率","type":"line","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\",instance=~\\"{{instance}}\\"}[5m])) * 100)","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"进程总数","type":"line","promql":"node_procs_running{instance=~\\"{{instance}}\\"} + node_procs_blocked{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":200,"color":"orange"},{"value":500,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"上下文切换率","type":"line","promql":"irate(node_context_switches_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":50000,"color":"orange"},{"value":100000,"color":"red"}],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["主机","系统","负载","node_exporter"]', '1.0.0', 4,
 '["pt-node-03","pt-node-01","pt-node-34","pt-node-05"]',
 '2025-01-01 00:00:00'),

-- dtpl-008: 文件系统监控
('dtpl-008', '文件系统监控', '主机监控', '文件系统', 'node_exporter',
 '聚焦文件系统相关指标，包括磁盘使用率、inode使用率、FD使用率和挂载点详情',
 '[
   {"id":"p1","title":"磁盘使用率","type":"bar","promql":"(1 - node_filesystem_avail_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"} / node_filesystem_size_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"}) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{mountpoint}}"},
   {"id":"p2","title":"inode使用率","type":"bar","promql":"(1 - node_filesystem_files_free{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"} / node_filesystem_files{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"}) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":80,"showLegend":true,"stackMode":"none"},"legendFormat":"{{mountpoint}}"},
   {"id":"p3","title":"文件描述符使用率","type":"gauge","promql":"node_filefd_allocated{instance=~\\"{{instance}}\\"} / node_filefd_maximum{instance=~\\"{{instance}}\\"} * 100","position":{"x":0,"y":4},"size":{"w":6,"h":3},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"挂载点详情","type":"table","promql":"node_filesystem_size_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{mountpoint}}"}
 ]',
 '', '["主机","文件系统","node_exporter"]', '1.0.0', 4,
 '["pt-node-17","pt-node-21","pt-node-35","pt-node-37"]',
 '2025-01-01 00:00:00'),

-- dtpl-009: Linux性能总览
('dtpl-009', 'Linux性能总览', '主机监控', '综合', 'node_exporter',
 'Linux服务器核心性能指标总览，包含CPU、内存、磁盘、网络、负载和运行时间',
 '[
   {"id":"p1","title":"CPU使用率","type":"line","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\",instance=~\\"{{instance}}\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"内存使用率","type":"line","promql":"(1 - node_memory_MemAvailable_bytes{instance=~\\"{{instance}}\\"} / node_memory_MemTotal_bytes{instance=~\\"{{instance}}\\"}) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":85,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"磁盘使用率","type":"line","promql":"(1 - node_filesystem_avail_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"} / node_filesystem_size_bytes{instance=~\\"{{instance}}\\",fstype!~\\"tmpfs|overlay\\"}) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{mountpoint}}"},
   {"id":"p4","title":"网络流量","type":"line","promql":"irate(node_network_receive_bytes_total{instance=~\\"{{instance}}\\",device!~\\"lo|veth.*|docker.*|br.*\\"}[5m]) * 8","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{device}} rx"},
   {"id":"p5","title":"系统负载","type":"line","promql":"node_load1{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":8},"size":{"w":6,"h":3},"thresholds":[{"value":4,"color":"orange"},{"value":8,"color":"red"}],"options":{"unit":"","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} load1"},
   {"id":"p6","title":"运行时间","type":"line","promql":"(time() - node_boot_time_seconds{instance=~\\"{{instance}}\\"}) / 86400","position":{"x":6,"y":8},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"days","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["主机","Linux","综合","node_exporter"]', '1.0.0', 6,
 '["pt-node-01","pt-node-09","pt-node-17","pt-node-25","pt-node-03","pt-node-33"]',
 '2025-01-01 00:00:00'),

-- dtpl-010: Windows主机监控
('dtpl-010', 'Windows主机监控', '主机监控', 'Windows', 'node_exporter',
 'Windows服务器监控面板，使用windows_exporter指标覆盖CPU、内存、磁盘、网络',
 '[
   {"id":"p1","title":"CPU使用率","type":"line","promql":"100 - (avg by(instance)(irate(windows_cpu_time_total{mode=\\"idle\\",instance=~\\"{{instance}}\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"内存使用率","type":"line","promql":"(1 - windows_os_physical_memory_free_bytes{instance=~\\"{{instance}}\\"} / windows_cs_physical_memory_bytes{instance=~\\"{{instance}}\\"}) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":85,"color":"orange"},{"value":95,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"磁盘使用率","type":"line","promql":"(1 - windows_logical_disk_free_bytes{instance=~\\"{{instance}}\\"} / windows_logical_disk_size_bytes{instance=~\\"{{instance}}\\"}) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{volume}}"},
   {"id":"p4","title":"网络流量","type":"line","promql":"irate(windows_net_bytes_received_total{instance=~\\"{{instance}}\\",nic!~\\"isatap.*|VPN.*\\"}[5m]) * 8","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} {{nic}} rx"}
 ]',
 '', '["主机","Windows","windows_exporter"]', '1.0.0', 4,
 '["pt-node-01","pt-node-09","pt-node-17","pt-node-25"]',
 '2025-01-01 00:00:00'),

-- ============================================================================
-- 容器监控 (category: '容器监控', exporter_type: 'cadvisor', 6 templates)
-- ============================================================================

-- dtpl-011: Docker容器概览
('dtpl-011', 'Docker容器概览', '容器监控', 'Docker', 'cadvisor',
 'Docker容器核心指标概览，包括CPU、内存、网络、磁盘读写、容器数量和重启次数',
 '[
   {"id":"p1","title":"容器CPU使用率","type":"line","promql":"sum by(name)(rate(container_cpu_usage_seconds_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m])) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":60,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p2","title":"容器内存使用量","type":"line","promql":"sum by(name)(container_memory_usage_bytes{image!=\\"\\",name=~\\"{{container}}\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p3","title":"网络收发速率","type":"line","promql":"sum by(name)(rate(container_network_receive_bytes_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}} rx"},
   {"id":"p4","title":"磁盘读写速率","type":"line","promql":"sum by(name)(rate(container_fs_reads_bytes_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}} read"},
   {"id":"p5","title":"容器数量","type":"stat","promql":"count(container_last_seen{image!=\\"\\"})","position":{"x":0,"y":8},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p6","title":"容器重启次数","type":"line","promql":"sum by(name)(changes(container_last_seen{image!=\\"\\",name=~\\"{{container}}\\"}[1h]))","position":{"x":6,"y":8},"size":{"w":6,"h":3},"thresholds":[{"value":3,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"}
 ]',
 '', '["容器","Docker","cadvisor"]', '1.0.0', 6,
 '["pt-cadvisor-01","pt-cadvisor-06","pt-cadvisor-11","pt-cadvisor-15","pt-cadvisor-19","pt-cadvisor-20"]',
 '2025-01-01 00:00:00'),

-- dtpl-012: K8s Pod资源监控
('dtpl-012', 'K8s Pod资源监控', '容器监控', 'K8s', 'cadvisor',
 'Kubernetes Pod级别的资源使用监控，包含CPU、内存、网络、存储及Pod状态',
 '[
   {"id":"p1","title":"Pod CPU使用率","type":"line","promql":"sum by(pod)(rate(container_cpu_usage_seconds_total{image!=\\"\\",pod=~\\"{{pod}}\\"}[5m])) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":60,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}"},
   {"id":"p2","title":"Pod内存使用量","type":"line","promql":"sum by(pod)(container_memory_working_set_bytes{image!=\\"\\",pod=~\\"{{pod}}\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}"},
   {"id":"p3","title":"Pod网络流量","type":"line","promql":"sum by(pod)(rate(container_network_receive_bytes_total{pod=~\\"{{pod}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}} rx"},
   {"id":"p4","title":"Pod存储使用","type":"line","promql":"sum by(pod)(container_fs_usage_bytes{image!=\\"\\",pod=~\\"{{pod}}\\"})","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}"},
   {"id":"p5","title":"Pod数量","type":"stat","promql":"count(kube_pod_info{pod=~\\"{{pod}}\\"})","position":{"x":0,"y":8},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p6","title":"Pod重启次数","type":"line","promql":"sum by(pod,container)(kube_pod_container_status_restarts_total{pod=~\\"{{pod}}\\"})","position":{"x":6,"y":8},"size":{"w":6,"h":3},"thresholds":[{"value":3,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}/{{container}}"}
 ]',
 '', '["容器","K8s","Pod","cadvisor"]', '1.0.0', 6,
 '["pt-cadvisor-01","pt-cadvisor-06","pt-cadvisor-11","pt-cadvisor-17","pt-cadvisor-19","pt-cadvisor-20"]',
 '2025-01-01 00:00:00'),

-- dtpl-013: 容器网络分析
('dtpl-013', '容器网络分析', '容器监控', '网络', 'cadvisor',
 '深入分析容器网络情况，包括接收/发送速率、网络错误和丢包',
 '[
   {"id":"p1","title":"网络接收速率","type":"line","promql":"sum by(name)(rate(container_network_receive_bytes_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p2","title":"网络发送速率","type":"line","promql":"sum by(name)(rate(container_network_transmit_bytes_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p3","title":"网络错误","type":"line","promql":"sum by(name)(rate(container_network_receive_errors_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]) + rate(container_network_transmit_errors_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p4","title":"网络丢包","type":"line","promql":"sum by(name)(rate(container_network_receive_packets_dropped_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]) + rate(container_network_transmit_packets_dropped_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"ops","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"}
 ]',
 '', '["容器","网络","cadvisor"]', '1.0.0', 4,
 '["pt-cadvisor-11","pt-cadvisor-12","pt-cadvisor-13","pt-cadvisor-14"]',
 '2025-01-01 00:00:00'),

-- dtpl-014: 容器存储使用
('dtpl-014', '容器存储使用', '容器监控', '存储', 'cadvisor',
 '容器文件系统和存储IO监控，包括使用量、使用率、读写速率',
 '[
   {"id":"p1","title":"文件系统使用量","type":"line","promql":"sum by(name)(container_fs_usage_bytes{image!=\\"\\",name=~\\"{{container}}\\"})","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p2","title":"文件系统使用率","type":"line","promql":"sum by(name)(container_fs_usage_bytes{image!=\\"\\",name=~\\"{{container}}\\"}) / sum by(name)(container_fs_limit_bytes{image!=\\"\\",name=~\\"{{container}}\\"} > 0) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p3","title":"磁盘读速率","type":"line","promql":"sum by(name)(rate(container_fs_reads_bytes_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p4","title":"磁盘写速率","type":"line","promql":"sum by(name)(rate(container_fs_writes_bytes_total{image!=\\"\\",name=~\\"{{container}}\\"}[5m]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"}
 ]',
 '', '["容器","存储","cadvisor"]', '1.0.0', 4,
 '["pt-cadvisor-17","pt-cadvisor-18","pt-cadvisor-15","pt-cadvisor-16"]',
 '2025-01-01 00:00:00'),

-- dtpl-015: 容器CPU/内存Top10
('dtpl-015', '容器CPU/内存Top10', '容器监控', '排行', 'cadvisor',
 '容器CPU和内存使用排行榜，快速找出资源消耗最大的容器',
 '[
   {"id":"p1","title":"CPU使用率Top10","type":"bar","promql":"topk(10, sum by(name)(rate(container_cpu_usage_seconds_total{image!=\\"\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":60,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p2","title":"内存使用Top10","type":"bar","promql":"topk(10, sum by(name)(container_memory_working_set_bytes{image!=\\"\\"}))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p3","title":"CPU使用率趋势","type":"line","promql":"topk(10, sum by(name)(rate(container_cpu_usage_seconds_total{image!=\\"\\"}[5m])) * 100)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":60,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p4","title":"内存使用趋势","type":"line","promql":"topk(10, sum by(name)(container_memory_working_set_bytes{image!=\\"\\"}))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"}
 ]',
 '', '["容器","排行","Top10","cadvisor"]', '1.0.0', 4,
 '["pt-cadvisor-01","pt-cadvisor-06","pt-cadvisor-07","pt-cadvisor-08"]',
 '2025-01-01 00:00:00'),

-- dtpl-016: 容器重启监控
('dtpl-016', '容器重启监控', '容器监控', '状态', 'cadvisor',
 '监控容器重启和异常状态，包括重启次数、OOM次数、容器状态和异常容器列表',
 '[
   {"id":"p1","title":"容器重启次数","type":"line","promql":"sum by(pod,container)(kube_pod_container_status_restarts_total)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":3,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}/{{container}}"},
   {"id":"p2","title":"OOM Kill次数","type":"line","promql":"sum by(name)(container_oom_events_total{image!=\\"\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p3","title":"容器运行状态","type":"table","promql":"kube_pod_container_status_running * 1 + kube_pod_container_status_waiting * 2 + kube_pod_container_status_terminated * 3","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}/{{container}}"},
   {"id":"p4","title":"异常容器列表","type":"table","promql":"kube_pod_container_status_waiting_reason{reason=~\\"CrashLoopBackOff|Error|ImagePullBackOff|OOMKilled\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}/{{container}} {{reason}}"}
 ]',
 '', '["容器","重启","OOM","cadvisor"]', '1.0.0', 4,
 '["pt-cadvisor-19","pt-cadvisor-10","pt-cadvisor-20","pt-cadvisor-08"]',
 '2025-01-01 00:00:00'),

-- ============================================================================
-- K8s集群 (category: 'K8s集群', exporter_type: 'kube-state-metrics', 12 templates)
-- ============================================================================

-- dtpl-017: 集群概览
('dtpl-017', '集群概览', 'K8s集群', '集群', 'kube-state-metrics',
 'Kubernetes集群核心指标总览，涵盖节点、Pod、资源请求率等关键信息',
 '[
   {"id":"p1","title":"节点数","type":"stat","promql":"count(kube_node_info)","position":{"x":0,"y":0},"size":{"w":3,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p2","title":"Pod总数","type":"stat","promql":"count(kube_pod_info)","position":{"x":3,"y":0},"size":{"w":3,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p3","title":"CPU请求率","type":"gauge","promql":"sum(kube_pod_container_resource_requests{resource=\\"cpu\\"}) / sum(kube_node_status_allocatable{resource=\\"cpu\\"}) * 100","position":{"x":6,"y":0},"size":{"w":3,"h":3},"thresholds":[{"value":70,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p4","title":"内存请求率","type":"gauge","promql":"sum(kube_pod_container_resource_requests{resource=\\"memory\\"}) / sum(kube_node_status_allocatable{resource=\\"memory\\"}) * 100","position":{"x":9,"y":0},"size":{"w":3,"h":3},"thresholds":[{"value":70,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p5","title":"命名空间数","type":"stat","promql":"count(kube_namespace_created)","position":{"x":0,"y":3},"size":{"w":3,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p6","title":"运行中Pod数","type":"stat","promql":"count(kube_pod_status_phase{phase=\\"Running\\"})","position":{"x":3,"y":3},"size":{"w":3,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p7","title":"待调度Pod数","type":"stat","promql":"count(kube_pod_status_phase{phase=\\"Pending\\"}) OR on() vector(0)","position":{"x":6,"y":3},"size":{"w":3,"h":3},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p8","title":"失败Pod数","type":"stat","promql":"count(kube_pod_status_phase{phase=\\"Failed\\"}) OR on() vector(0)","position":{"x":9,"y":3},"size":{"w":3,"h":3},"thresholds":[{"value":1,"color":"orange"},{"value":3,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""}
 ]',
 '', '["K8s","集群","概览","kube-state-metrics"]', '1.0.0', 8,
 '["pt-kube-01","pt-kube-02","pt-kube-03","pt-kube-04","pt-kube-05","pt-kube-09","pt-kube-10"]',
 '2025-01-01 00:00:00'),

-- dtpl-018: 节点监控
('dtpl-018', '节点监控', 'K8s集群', '节点', 'kube-state-metrics',
 'Kubernetes节点详细监控，包含节点状态、CPU/内存分配率、Pod数量等',
 '[
   {"id":"p1","title":"节点状态","type":"table","promql":"kube_node_status_condition{condition=\\"Ready\\",status=\\"true\\"}","position":{"x":0,"y":0},"size":{"w":12,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{node}}"},
   {"id":"p2","title":"CPU分配率","type":"bar","promql":"sum by(node)(kube_pod_container_resource_requests{resource=\\"cpu\\"}) / on(node) group_left kube_node_status_allocatable{resource=\\"cpu\\"} * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{node}}"},
   {"id":"p3","title":"内存分配率","type":"bar","promql":"sum by(node)(kube_pod_container_resource_requests{resource=\\"memory\\"}) / on(node) group_left kube_node_status_allocatable{resource=\\"memory\\"} * 100","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{node}}"},
   {"id":"p4","title":"节点Pod数量","type":"bar","promql":"count by(node)(kube_pod_info)","position":{"x":0,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":100,"color":"orange"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{node}}"},
   {"id":"p5","title":"NotReady节点数","type":"stat","promql":"count(kube_node_status_condition{condition=\\"Ready\\",status=\\"true\\"} == 0) OR on() vector(0)","position":{"x":6,"y":8},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p6","title":"节点条件","type":"line","promql":"sum by(condition)(kube_node_status_condition{status=\\"true\\"})","position":{"x":0,"y":12},"size":{"w":12,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{condition}}"}
 ]',
 '', '["K8s","节点","kube-state-metrics"]', '1.0.0', 6,
 '["pt-kube-06","pt-kube-07","pt-kube-08","pt-kube-09","pt-kube-10"]',
 '2025-01-01 00:00:00'),

-- dtpl-019: Pod监控
('dtpl-019', 'Pod监控', 'K8s集群', 'Pod', 'kube-state-metrics',
 'Kubernetes Pod综合监控，包含CPU、内存、网络、状态、重启和待调度信息',
 '[
   {"id":"p1","title":"Pod CPU使用率","type":"line","promql":"sum by(pod)(rate(container_cpu_usage_seconds_total{image!=\\"\\",pod=~\\"{{pod}}\\"}[5m])) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":60,"color":"orange"},{"value":85,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}"},
   {"id":"p2","title":"Pod内存使用量","type":"line","promql":"sum by(pod)(container_memory_working_set_bytes{image!=\\"\\",pod=~\\"{{pod}}\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}"},
   {"id":"p3","title":"Pod网络流量","type":"line","promql":"sum by(pod)(rate(container_network_receive_bytes_total{pod=~\\"{{pod}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}} rx"},
   {"id":"p4","title":"Pod状态分布","type":"line","promql":"count by(phase)(kube_pod_status_phase)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{phase}}"},
   {"id":"p5","title":"Pod重启次数","type":"line","promql":"sum by(pod,container)(kube_pod_container_status_restarts_total{pod=~\\"{{pod}}\\"})","position":{"x":0,"y":8},"size":{"w":6,"h":3},"thresholds":[{"value":3,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}/{{container}}"},
   {"id":"p6","title":"待调度Pod","type":"stat","promql":"count(kube_pod_status_phase{phase=\\"Pending\\"}) OR on() vector(0)","position":{"x":6,"y":8},"size":{"w":6,"h":3},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""}
 ]',
 '', '["K8s","Pod","kube-state-metrics"]', '1.0.0', 6,
 '["pt-cadvisor-01","pt-cadvisor-06","pt-cadvisor-11","pt-cadvisor-19","pt-cadvisor-20","pt-kube-02"]',
 '2025-01-01 00:00:00'),

-- dtpl-020: Deployment状态
('dtpl-020', 'Deployment状态', 'K8s集群', '工作负载', 'kube-state-metrics',
 'Kubernetes Deployment运行状态监控，包含副本状态、不可用副本、更新进度和回滚',
 '[
   {"id":"p1","title":"Deployment副本状态","type":"table","promql":"kube_deployment_status_replicas_available / kube_deployment_spec_replicas","position":{"x":0,"y":0},"size":{"w":12,"h":4},"thresholds":[],"options":{"unit":"","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}/{{deployment}}"},
   {"id":"p2","title":"不可用副本数","type":"stat","promql":"sum(kube_deployment_status_replicas_unavailable) OR on() vector(0)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p3","title":"Deployment更新进度","type":"line","promql":"kube_deployment_status_observed_generation / kube_deployment_metadata_generation","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}/{{deployment}}"},
   {"id":"p4","title":"Deployment回滚次数","type":"line","promql":"changes(kube_deployment_metadata_generation[1h])","position":{"x":0,"y":8},"size":{"w":12,"h":3},"thresholds":[{"value":1,"color":"orange"},{"value":3,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}/{{deployment}}"}
 ]',
 '', '["K8s","Deployment","工作负载","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-11","pt-kube-12","pt-kube-13","pt-kube-14"]',
 '2025-01-01 00:00:00'),

-- dtpl-021: Service监控
('dtpl-021', 'Service监控', 'K8s集群', '服务', 'kube-state-metrics',
 'Kubernetes Service和Endpoint监控，包含Endpoint数量、就绪率、Service统计和变化趋势',
 '[
   {"id":"p1","title":"Endpoint数量","type":"bar","promql":"count by(namespace, service)(kube_endpoint_address)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{namespace}}/{{service}}"},
   {"id":"p2","title":"Endpoint就绪率","type":"gauge","promql":"sum(kube_endpoint_address_available) / (sum(kube_endpoint_address_available) + sum(kube_endpoint_address_not_ready)) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"orange"},{"value":95,"color":"green"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p3","title":"Service数量","type":"stat","promql":"count(kube_service_info)","position":{"x":0,"y":4},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p4","title":"Endpoint变化趋势","type":"line","promql":"changes(kube_endpoint_created[1h])","position":{"x":6,"y":4},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}/{{endpoint}}"}
 ]',
 '', '["K8s","Service","Endpoint","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-17","pt-kube-18","pt-kube-19"]',
 '2025-01-01 00:00:00'),

-- dtpl-022: 存储概览
('dtpl-022', '存储概览', 'K8s集群', '存储', 'kube-state-metrics',
 'Kubernetes存储资源概览，包含PV总数、PVC状态、PV使用率和PVC绑定状态分布',
 '[
   {"id":"p1","title":"PV总数","type":"stat","promql":"count(kube_persistentvolume_info)","position":{"x":0,"y":0},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p2","title":"PVC状态","type":"table","promql":"kube_persistentvolumeclaim_status_phase","position":{"x":6,"y":0},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}/{{persistentvolumeclaim}}"},
   {"id":"p3","title":"PV使用率","type":"bar","promql":"kubelet_volume_stats_used_bytes / kubelet_volume_stats_capacity_bytes * 100","position":{"x":0,"y":3},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{persistentvolumeclaim}}"},
   {"id":"p4","title":"PVC绑定状态","type":"pie","promql":"count by(phase)(kube_persistentvolumeclaim_status_phase)","position":{"x":6,"y":3},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{phase}}"}
 ]',
 '', '["K8s","存储","PV","PVC","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-21","pt-kube-22","pt-kube-23","pt-kube-24"]',
 '2025-01-01 00:00:00'),

-- dtpl-023: API Server性能
('dtpl-023', 'API Server性能', 'K8s集群', 'API Server', 'kube-state-metrics',
 'Kubernetes API Server性能监控，包含请求率、延迟、错误率和请求总量',
 '[
   {"id":"p1","title":"API请求率","type":"line","promql":"sum(rate(apiserver_request_total[5m])) by(verb)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{verb}}"},
   {"id":"p2","title":"API请求延迟P99","type":"line","promql":"histogram_quantile(0.99, sum(rate(apiserver_request_duration_seconds_bucket[5m])) by(verb, le))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{verb}}"},
   {"id":"p3","title":"API错误率","type":"line","promql":"sum(rate(apiserver_request_total{code=~\\"4..|5..\\"}[5m])) by(code)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":5,"color":"orange"},{"value":20,"color":"red"}],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{code}}"},
   {"id":"p4","title":"API请求总量","type":"stat","promql":"sum(apiserver_request_total)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""}
 ]',
 '', '["K8s","API Server","性能","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-25","pt-kube-26","pt-kube-27"]',
 '2025-01-01 00:00:00'),

-- dtpl-024: etcd监控
('dtpl-024', 'etcd监控', 'K8s集群', 'etcd', 'kube-state-metrics',
 'etcd集群核心指标监控，包含Leader变更、DB大小、提案失败和快照耗时',
 '[
   {"id":"p1","title":"Leader变更次数","type":"line","promql":"changes(etcd_server_leader_changes_seen_total[1h])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":3,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p2","title":"etcd DB大小","type":"line","promql":"etcd_mvcc_db_total_size_in_bytes","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p3","title":"提案失败数","type":"line","promql":"rate(etcd_server_proposals_failed_total[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},
   {"id":"p4","title":"快照保存耗时","type":"line","promql":"histogram_quantile(0.99, sum(rate(etcd_debugging_snap_save_total_duration_seconds_bucket[5m])) by(instance, le))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}
 ]',
 '', '["K8s","etcd","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-28","pt-kube-29","pt-kube-30"]',
 '2025-01-01 00:00:00'),

-- dtpl-025: 调度器与控制器
('dtpl-025', '调度器与控制器', 'K8s集群', '控制面', 'kube-state-metrics',
 'Kubernetes调度器和控制器管理器监控，包含调度次数、延迟、队列深度和错误',
 '[
   {"id":"p1","title":"调度次数","type":"line","promql":"sum(rate(scheduler_schedule_attempts_total[5m])) by(result)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{result}}"},
   {"id":"p2","title":"调度延迟P99","type":"line","promql":"histogram_quantile(0.99, sum(rate(scheduler_scheduling_algorithm_duration_seconds_bucket[5m])) by(le))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":0.1,"color":"orange"},{"value":1,"color":"red"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"p99"},
   {"id":"p3","title":"控制器队列深度","type":"line","promql":"sum by(name)(workqueue_depth)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":100,"color":"orange"},{"value":500,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"},
   {"id":"p4","title":"控制器处理错误","type":"line","promql":"sum by(name)(rate(workqueue_retries_total[5m]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{name}}"}
 ]',
 '', '["K8s","调度器","控制器","控制面","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-25","pt-kube-26","pt-kube-27"]',
 '2025-01-01 00:00:00'),

-- dtpl-026: 命名空间资源使用
('dtpl-026', '命名空间资源使用', 'K8s集群', '命名空间', 'kube-state-metrics',
 '按命名空间维度查看资源使用情况，包含CPU/内存请求、Pod数和资源配额',
 '[
   {"id":"p1","title":"CPU请求(按命名空间)","type":"line","promql":"sum by(namespace)(kube_pod_container_resource_requests{resource=\\"cpu\\"})","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"cores","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}"},
   {"id":"p2","title":"内存请求(按命名空间)","type":"line","promql":"sum by(namespace)(kube_pod_container_resource_requests{resource=\\"memory\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}"},
   {"id":"p3","title":"Pod数(按命名空间)","type":"line","promql":"count by(namespace)(kube_pod_info)","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}"},
   {"id":"p4","title":"资源配额使用率","type":"line","promql":"kube_resourcequota{type=\\"used\\"} / kube_resourcequota{type=\\"hard\\"} * 100","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":70,"color":"orange"},{"value":90,"color":"red"}],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}} {{resource}}"}
 ]',
 '', '["K8s","命名空间","资源","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-03","pt-kube-04","pt-kube-05","pt-kube-02"]',
 '2025-01-01 00:00:00'),

-- dtpl-027: 工作负载概览
('dtpl-027', '工作负载概览', 'K8s集群', '工作负载', 'kube-state-metrics',
 'Kubernetes各类工作负载状态总览，涵盖Deployment、StatefulSet、DaemonSet、Job和CronJob',
 '[
   {"id":"p1","title":"Deployment总数","type":"stat","promql":"count(kube_deployment_created)","position":{"x":0,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p2","title":"StatefulSet总数","type":"stat","promql":"count(kube_statefulset_created)","position":{"x":4,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p3","title":"DaemonSet总数","type":"stat","promql":"count(kube_daemonset_created)","position":{"x":8,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p4","title":"Job状态","type":"line","promql":"sum(kube_job_status_succeeded) by(namespace)","position":{"x":0,"y":3},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}} succeeded"},
   {"id":"p5","title":"CronJob状态","type":"line","promql":"kube_cronjob_status_active","position":{"x":6,"y":3},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{namespace}}/{{cronjob}}"},
   {"id":"p6","title":"不可用工作负载","type":"stat","promql":"count(kube_deployment_status_replicas_unavailable > 0) OR on() vector(0)","position":{"x":0,"y":7},"size":{"w":12,"h":3},"thresholds":[{"value":1,"color":"orange"},{"value":5,"color":"red"}],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""}
 ]',
 '', '["K8s","工作负载","Deployment","StatefulSet","DaemonSet","kube-state-metrics"]', '1.0.0', 6,
 '["pt-kube-11","pt-kube-12","pt-kube-13","pt-kube-14","pt-kube-15","pt-kube-16"]',
 '2025-01-01 00:00:00'),

-- dtpl-028: Ingress/CoreDNS监控
('dtpl-028', 'Ingress/CoreDNS监控', 'K8s集群', '网络', 'kube-state-metrics',
 'Kubernetes Ingress和CoreDNS网络组件监控，包含Ingress规则数、DNS请求率、错误率和延迟',
 '[
   {"id":"p1","title":"Ingress规则数","type":"stat","promql":"count(kube_ingress_info)","position":{"x":0,"y":0},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":false,"stackMode":"none"},"legendFormat":""},
   {"id":"p2","title":"CoreDNS请求率","type":"line","promql":"sum(rate(coredns_dns_requests_total[5m])) by(type)","position":{"x":6,"y":0},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{type}}"},
   {"id":"p3","title":"CoreDNS错误率","type":"line","promql":"sum(rate(coredns_dns_responses_total{rcode!=\\"NOERROR\\"}[5m])) by(rcode)","position":{"x":0,"y":3},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"orange"},{"value":10,"color":"red"}],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{rcode}}"},
   {"id":"p4","title":"CoreDNS请求延迟","type":"line","promql":"histogram_quantile(0.99, sum(rate(coredns_dns_request_duration_seconds_bucket[5m])) by(le))","position":{"x":6,"y":3},"size":{"w":6,"h":4},"thresholds":[{"value":0.1,"color":"orange"},{"value":1,"color":"red"}],"options":{"unit":"s","decimals":3,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"p99"}
 ]',
 '', '["K8s","Ingress","CoreDNS","网络","kube-state-metrics"]', '1.0.0', 4,
 '["pt-kube-19","pt-kube-20"]',
 '2025-01-01 00:00:00');
