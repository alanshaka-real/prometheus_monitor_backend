-- ============================================
-- PromQL模板增强迁移脚本
-- 新增字段 + 更新已有模板 + 插入100+新模板
-- ============================================

-- 1. ALTER TABLE: 新增字段
ALTER TABLE prom_promql_template
    ADD COLUMN sub_category VARCHAR(50) DEFAULT '' COMMENT '二级分类',
    ADD COLUMN exporter_type VARCHAR(20) DEFAULT '' COMMENT '关联组件类型: node/blackbox/process/cadvisor/mysqld/redis/nginx/custom',
    ADD COLUMN unit VARCHAR(20) DEFAULT 'none' COMMENT '语义单位: percent/bytes/seconds/count/ops/bps/none',
    ADD COLUMN unit_format VARCHAR(20) DEFAULT 'none' COMMENT '格式化策略: percent/bytes_iec/duration/number/bps/ops/none',
    ADD COLUMN sort_order INT DEFAULT 0 COMMENT '子分类内排序';

CREATE INDEX idx_exporter_type ON prom_promql_template(exporter_type);
CREATE INDEX idx_sub_category ON prom_promql_template(sub_category);

-- 2. UPDATE: 补充已有12条模板的新字段
UPDATE prom_promql_template SET category='Node Exporter', sub_category='CPU指标', exporter_type='node', unit='percent', unit_format='percent', sort_order=1,
    query='100 - (avg by(instance)(irate(node_cpu_seconds_total{mode="idle",instance=~"{{instance}}"}[5m])) * 100)' WHERE id='tpl-1';
UPDATE prom_promql_template SET category='Node Exporter', sub_category='内存指标', exporter_type='node', unit='percent', unit_format='percent', sort_order=1,
    query='(1 - node_memory_MemAvailable_bytes{instance=~"{{instance}}"} / node_memory_MemTotal_bytes{instance=~"{{instance}}"}) * 100' WHERE id='tpl-2';
UPDATE prom_promql_template SET category='Node Exporter', sub_category='磁盘指标', exporter_type='node', unit='percent', unit_format='percent', sort_order=1,
    query='(1 - node_filesystem_avail_bytes{fstype!~"tmpfs|overlay",instance=~"{{instance}}"} / node_filesystem_size_bytes{instance=~"{{instance}}"}) * 100' WHERE id='tpl-3';
UPDATE prom_promql_template SET category='Node Exporter', sub_category='网络指标', exporter_type='node', unit='bps', unit_format='bps', sort_order=1,
    query='irate(node_network_receive_bytes_total{device!~"lo|veth.*",instance=~"{{instance}}"}[5m]) * 8' WHERE id='tpl-4';
UPDATE prom_promql_template SET category='Node Exporter', sub_category='网络指标', exporter_type='node', unit='bps', unit_format='bps', sort_order=2,
    query='irate(node_network_transmit_bytes_total{device!~"lo|veth.*",instance=~"{{instance}}"}[5m]) * 8' WHERE id='tpl-5';
UPDATE prom_promql_template SET category='应用监控', sub_category='HTTP', exporter_type='custom', unit='ops', unit_format='ops', sort_order=1,
    query='sum(rate(http_requests_total{instance=~"{{instance}}"}[5m]))' WHERE id='tpl-6';
UPDATE prom_promql_template SET category='应用监控', sub_category='HTTP', exporter_type='custom', unit='seconds', unit_format='duration', sort_order=2,
    query='histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket{instance=~"{{instance}}"}[5m])) by (le))' WHERE id='tpl-7';
UPDATE prom_promql_template SET category='cAdvisor', sub_category='CPU指标', exporter_type='cadvisor', unit='percent', unit_format='percent', sort_order=1,
    query='sum(rate(container_cpu_usage_seconds_total{name!="",instance=~"{{instance}}"}[5m])) by (name) * 100' WHERE id='tpl-8';
UPDATE prom_promql_template SET category='cAdvisor', sub_category='内存指标', exporter_type='cadvisor', unit='percent', unit_format='percent', sort_order=1,
    query='container_memory_usage_bytes{name!="",instance=~"{{instance}}"} / container_spec_memory_limit_bytes{name!=""} * 100' WHERE id='tpl-9';
UPDATE prom_promql_template SET category='Blackbox Exporter', sub_category='可用性', exporter_type='blackbox', unit='percent', unit_format='percent', sort_order=1,
    query='probe_success{instance=~"{{instance}}"}' WHERE id='tpl-10';
UPDATE prom_promql_template SET category='MySQL Exporter', sub_category='连接', exporter_type='mysqld', unit='count', unit_format='number', sort_order=1,
    query='mysql_global_status_threads_connected{instance=~"{{instance}}"}' WHERE id='tpl-11';
UPDATE prom_promql_template SET category='Redis Exporter', sub_category='内存', exporter_type='redis', unit='bytes', unit_format='bytes_iec', sort_order=1,
    query='redis_memory_used_bytes{instance=~"{{instance}}"}' WHERE id='tpl-12';

-- 3. INSERT: 100+ 新模板
-- ============================================
-- Node Exporter (~28 新增)
-- ============================================

-- CPU指标
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-101', 'CPU User模式占比', 'Node Exporter', 'CPU指标', 'node',
 'avg by(instance)(irate(node_cpu_seconds_total{mode="user",instance=~"{{instance}}"}[5m])) * 100',
 '各实例CPU在user模式下的时间占比', '["instance"]', 'percent', 'percent', 2),
('tpl-102', 'CPU System模式占比', 'Node Exporter', 'CPU指标', 'node',
 'avg by(instance)(irate(node_cpu_seconds_total{mode="system",instance=~"{{instance}}"}[5m])) * 100',
 '各实例CPU在system模式下的时间占比', '["instance"]', 'percent', 'percent', 3),
('tpl-103', 'CPU IOWait占比', 'Node Exporter', 'CPU指标', 'node',
 'avg by(instance)(irate(node_cpu_seconds_total{mode="iowait",instance=~"{{instance}}"}[5m])) * 100',
 '各实例CPU等待IO的时间占比，过高说明磁盘瓶颈', '["instance"]', 'percent', 'percent', 4),
('tpl-104', 'CPU Steal占比', 'Node Exporter', 'CPU指标', 'node',
 'avg by(instance)(irate(node_cpu_seconds_total{mode="steal",instance=~"{{instance}}"}[5m])) * 100',
 '虚拟机被宿主机抢占的CPU时间占比', '["instance"]', 'percent', 'percent', 5),
('tpl-105', '系统负载(1分钟)', 'Node Exporter', 'CPU指标', 'node',
 'node_load1{instance=~"{{instance}}"}',
 '1分钟平均系统负载', '["instance"]', 'count', 'number', 6),
('tpl-106', '系统负载(5分钟)', 'Node Exporter', 'CPU指标', 'node',
 'node_load5{instance=~"{{instance}}"}',
 '5分钟平均系统负载', '["instance"]', 'count', 'number', 7),
('tpl-107', '系统负载(15分钟)', 'Node Exporter', 'CPU指标', 'node',
 'node_load15{instance=~"{{instance}}"}',
 '15分钟平均系统负载', '["instance"]', 'count', 'number', 8),
('tpl-108', '上下文切换率', 'Node Exporter', 'CPU指标', 'node',
 'irate(node_context_switches_total{instance=~"{{instance}}"}[5m])',
 '每秒上下文切换次数', '["instance"]', 'ops', 'ops', 9);

-- 内存指标
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-111', '可用内存', 'Node Exporter', '内存指标', 'node',
 'node_memory_MemAvailable_bytes{instance=~"{{instance}}"}',
 '各实例当前可用内存大小', '["instance"]', 'bytes', 'bytes_iec', 2),
('tpl-112', '已用内存', 'Node Exporter', '内存指标', 'node',
 'node_memory_MemTotal_bytes{instance=~"{{instance}}"} - node_memory_MemAvailable_bytes{instance=~"{{instance}}"}',
 '各实例已使用内存大小', '["instance"]', 'bytes', 'bytes_iec', 3),
('tpl-113', 'Buffer/Cache大小', 'Node Exporter', '内存指标', 'node',
 'node_memory_Buffers_bytes{instance=~"{{instance}}"} + node_memory_Cached_bytes{instance=~"{{instance}}"}',
 '各实例Buffer和Cache占用内存', '["instance"]', 'bytes', 'bytes_iec', 4),
('tpl-114', 'Swap使用率', 'Node Exporter', '内存指标', 'node',
 '(1 - node_memory_SwapFree_bytes{instance=~"{{instance}}"} / node_memory_SwapTotal_bytes{instance=~"{{instance}}"}) * 100',
 '各实例Swap使用百分比', '["instance"]', 'percent', 'percent', 5),
('tpl-115', 'Swap已用', 'Node Exporter', '内存指标', 'node',
 'node_memory_SwapTotal_bytes{instance=~"{{instance}}"} - node_memory_SwapFree_bytes{instance=~"{{instance}}"}',
 '各实例已使用Swap大小', '["instance"]', 'bytes', 'bytes_iec', 6);

-- 磁盘指标
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-121', '磁盘可用空间', 'Node Exporter', '磁盘指标', 'node',
 'node_filesystem_avail_bytes{fstype!~"tmpfs|overlay",instance=~"{{instance}}"}',
 '各挂载点可用磁盘空间', '["instance"]', 'bytes', 'bytes_iec', 2),
('tpl-122', '磁盘读取速率', 'Node Exporter', '磁盘指标', 'node',
 'irate(node_disk_read_bytes_total{instance=~"{{instance}}"}[5m])',
 '磁盘每秒读取字节数', '["instance"]', 'bytes', 'bytes_iec', 3),
('tpl-123', '磁盘写入速率', 'Node Exporter', '磁盘指标', 'node',
 'irate(node_disk_written_bytes_total{instance=~"{{instance}}"}[5m])',
 '磁盘每秒写入字节数', '["instance"]', 'bytes', 'bytes_iec', 4),
('tpl-124', '磁盘读IOPS', 'Node Exporter', '磁盘指标', 'node',
 'irate(node_disk_reads_completed_total{instance=~"{{instance}}"}[5m])',
 '磁盘每秒读操作次数', '["instance"]', 'ops', 'ops', 5),
('tpl-125', '磁盘写IOPS', 'Node Exporter', '磁盘指标', 'node',
 'irate(node_disk_writes_completed_total{instance=~"{{instance}}"}[5m])',
 '磁盘每秒写操作次数', '["instance"]', 'ops', 'ops', 6),
('tpl-126', '磁盘IO利用率', 'Node Exporter', '磁盘指标', 'node',
 'irate(node_disk_io_time_seconds_total{instance=~"{{instance}}"}[5m]) * 100',
 '磁盘IO利用率百分比，接近100%说明磁盘繁忙', '["instance"]', 'percent', 'percent', 7);

-- 网络指标
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-131', '网络接收包率', 'Node Exporter', '网络指标', 'node',
 'irate(node_network_receive_packets_total{device!~"lo|veth.*",instance=~"{{instance}}"}[5m])',
 '网络设备每秒接收数据包数', '["instance"]', 'ops', 'ops', 3),
('tpl-132', '网络发送包率', 'Node Exporter', '网络指标', 'node',
 'irate(node_network_transmit_packets_total{device!~"lo|veth.*",instance=~"{{instance}}"}[5m])',
 '网络设备每秒发送数据包数', '["instance"]', 'ops', 'ops', 4),
('tpl-133', '网络接收错误率', 'Node Exporter', '网络指标', 'node',
 'irate(node_network_receive_errs_total{device!~"lo|veth.*",instance=~"{{instance}}"}[5m])',
 '网络设备每秒接收错误数', '["instance"]', 'ops', 'ops', 5),
('tpl-134', '网络发送错误率', 'Node Exporter', '网络指标', 'node',
 'irate(node_network_transmit_errs_total{device!~"lo|veth.*",instance=~"{{instance}}"}[5m])',
 '网络设备每秒发送错误数', '["instance"]', 'ops', 'ops', 6),
('tpl-135', '网络接收丢包率', 'Node Exporter', '网络指标', 'node',
 'irate(node_network_receive_drop_total{device!~"lo|veth.*",instance=~"{{instance}}"}[5m])',
 '网络设备每秒接收丢包数', '["instance"]', 'ops', 'ops', 7);

-- 文件系统
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-141', 'Inode使用率', 'Node Exporter', '文件系统', 'node',
 '(1 - node_filesystem_files_free{fstype!~"tmpfs|overlay",instance=~"{{instance}}"} / node_filesystem_files{instance=~"{{instance}}"}) * 100',
 '各挂载点Inode使用百分比', '["instance"]', 'percent', 'percent', 1),
('tpl-142', '文件描述符数量', 'Node Exporter', '文件系统', 'node',
 'node_filefd_allocated{instance=~"{{instance}}"}',
 '已分配的文件描述符数量', '["instance"]', 'count', 'number', 2);

-- 系统信息
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-151', '系统运行时间', 'Node Exporter', '系统信息', 'node',
 'time() - node_boot_time_seconds{instance=~"{{instance}}"}',
 '各实例自上次启动以来的运行时间', '["instance"]', 'seconds', 'duration', 1),
('tpl-152', '时间偏移', 'Node Exporter', '系统信息', 'node',
 'node_timex_offset_seconds{instance=~"{{instance}}"}',
 '各实例系统时间偏移量（NTP同步偏差）', '["instance"]', 'seconds', 'duration', 2);

-- ============================================
-- Blackbox Exporter (~11 新增)
-- ============================================

-- 可用性
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-201', '探测总耗时', 'Blackbox Exporter', '可用性', 'blackbox',
 'probe_duration_seconds{instance=~"{{instance}}"}',
 '探测目标的总耗时', '["instance"]', 'seconds', 'duration', 2);

-- HTTP探测
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-211', 'HTTP状态码', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_status_code{instance=~"{{instance}}"}',
 'HTTP探测返回的状态码', '["instance"]', 'count', 'number', 1),
('tpl-212', 'HTTP响应大小', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_content_length{instance=~"{{instance}}"}',
 'HTTP响应内容大小', '["instance"]', 'bytes', 'bytes_iec', 2),
('tpl-213', 'DNS解析耗时', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_duration_seconds{phase="resolve",instance=~"{{instance}}"}',
 'HTTP探测中DNS解析阶段耗时', '["instance"]', 'seconds', 'duration', 3),
('tpl-214', 'TCP连接耗时', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_duration_seconds{phase="connect",instance=~"{{instance}}"}',
 'HTTP探测中TCP连接阶段耗时', '["instance"]', 'seconds', 'duration', 4),
('tpl-215', 'TLS握手耗时', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_duration_seconds{phase="tls",instance=~"{{instance}}"}',
 'HTTP探测中TLS握手阶段耗时', '["instance"]', 'seconds', 'duration', 5),
('tpl-216', '服务处理耗时', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_duration_seconds{phase="processing",instance=~"{{instance}}"}',
 'HTTP探测中服务器处理阶段耗时', '["instance"]', 'seconds', 'duration', 6),
('tpl-217', '内容传输耗时', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_duration_seconds{phase="transfer",instance=~"{{instance}}"}',
 'HTTP探测中内容传输阶段耗时', '["instance"]', 'seconds', 'duration', 7),
('tpl-218', 'HTTP重定向次数', 'Blackbox Exporter', 'HTTP探测', 'blackbox',
 'probe_http_redirects{instance=~"{{instance}}"}',
 'HTTP探测中发生的重定向次数', '["instance"]', 'count', 'number', 8);

-- SSL证书
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-221', 'SSL证书过期时间', 'Blackbox Exporter', 'SSL证书', 'blackbox',
 'probe_ssl_earliest_cert_expiry{instance=~"{{instance}}"}',
 'SSL证书最早过期的Unix时间戳', '["instance"]', 'seconds', 'number', 1),
('tpl-222', 'SSL证书剩余天数', 'Blackbox Exporter', 'SSL证书', 'blackbox',
 '(probe_ssl_earliest_cert_expiry{instance=~"{{instance}}"} - time()) / 86400',
 'SSL证书距过期剩余天数', '["instance"]', 'count', 'number', 2);

-- DNS探测
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-231', 'DNS解析耗时', 'Blackbox Exporter', 'DNS探测', 'blackbox',
 'probe_dns_lookup_time_seconds{instance=~"{{instance}}"}',
 'DNS探测的解析耗时', '["instance"]', 'seconds', 'duration', 1);

-- ============================================
-- Process Exporter (~9条)
-- ============================================

-- CPU
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-301', '进程CPU使用率', 'Process Exporter', 'CPU', 'process',
 'irate(namedprocess_namegroup_cpu_seconds_total{instance=~"{{instance}}"}[5m]) * 100',
 '各进程组CPU使用百分比', '["instance"]', 'percent', 'percent', 1);

-- 内存
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-311', '进程驻留内存(RSS)', 'Process Exporter', '内存', 'process',
 'namedprocess_namegroup_memory_bytes{memtype="resident",instance=~"{{instance}}"}',
 '各进程组驻留内存大小', '["instance"]', 'bytes', 'bytes_iec', 1),
('tpl-312', '进程虚拟内存', 'Process Exporter', '内存', 'process',
 'namedprocess_namegroup_memory_bytes{memtype="virtual",instance=~"{{instance}}"}',
 '各进程组虚拟内存大小', '["instance"]', 'bytes', 'bytes_iec', 2);

-- 文件描述符
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-321', '进程打开FD数', 'Process Exporter', '文件描述符', 'process',
 'namedprocess_namegroup_open_filedesc{instance=~"{{instance}}"}',
 '各进程组打开的文件描述符数量', '["instance"]', 'count', 'number', 1),
('tpl-322', '进程最大FD使用率', 'Process Exporter', '文件描述符', 'process',
 'namedprocess_namegroup_open_filedesc{instance=~"{{instance}}"} / namedprocess_namegroup_worst_fd_ratio{instance=~"{{instance}}"} * 100',
 '进程文件描述符使用占比', '["instance"]', 'percent', 'percent', 2);

-- 进程
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-331', '进程数量', 'Process Exporter', '进程', 'process',
 'namedprocess_namegroup_num_procs{instance=~"{{instance}}"}',
 '各进程组的进程数', '["instance"]', 'count', 'number', 1),
('tpl-332', '线程数量', 'Process Exporter', '进程', 'process',
 'namedprocess_namegroup_num_threads{instance=~"{{instance}}"}',
 '各进程组的线程数', '["instance"]', 'count', 'number', 2);

-- IO
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-341', '进程读取速率', 'Process Exporter', 'IO', 'process',
 'irate(namedprocess_namegroup_read_bytes_total{instance=~"{{instance}}"}[5m])',
 '各进程组每秒磁盘读取字节数', '["instance"]', 'bytes', 'bytes_iec', 1),
('tpl-342', '进程写入速率', 'Process Exporter', 'IO', 'process',
 'irate(namedprocess_namegroup_write_bytes_total{instance=~"{{instance}}"}[5m])',
 '各进程组每秒磁盘写入字节数', '["instance"]', 'bytes', 'bytes_iec', 2);

-- ============================================
-- cAdvisor / 容器监控 (~13 新增)
-- ============================================

-- CPU指标
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-401', '容器System CPU时间', 'cAdvisor', 'CPU指标', 'cadvisor',
 'sum(rate(container_cpu_system_seconds_total{name!="",instance=~"{{instance}}"}[5m])) by (name) * 100',
 '容器内核态CPU使用率', '["instance"]', 'percent', 'percent', 2),
('tpl-402', '容器User CPU时间', 'cAdvisor', 'CPU指标', 'cadvisor',
 'sum(rate(container_cpu_user_seconds_total{name!="",instance=~"{{instance}}"}[5m])) by (name) * 100',
 '容器用户态CPU使用率', '["instance"]', 'percent', 'percent', 3),
('tpl-403', '容器CPU限流时间', 'cAdvisor', 'CPU指标', 'cadvisor',
 'sum(rate(container_cpu_cfs_throttled_seconds_total{name!="",instance=~"{{instance}}"}[5m])) by (name)',
 '容器被CFS限流的CPU时间', '["instance"]', 'seconds', 'duration', 4),
('tpl-404', '容器CFS限流周期比例', 'cAdvisor', 'CPU指标', 'cadvisor',
 'sum(rate(container_cpu_cfs_throttled_periods_total{name!="",instance=~"{{instance}}"}[5m])) by (name) / sum(rate(container_cpu_cfs_periods_total{name!="",instance=~"{{instance}}"}[5m])) by (name) * 100',
 '容器CFS限流周期占总周期的比例', '["instance"]', 'percent', 'percent', 5);

-- 内存指标
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-411', '容器内存工作集', 'cAdvisor', '内存指标', 'cadvisor',
 'container_memory_working_set_bytes{name!="",instance=~"{{instance}}"}',
 '容器内存工作集大小（OOM判断依据）', '["instance"]', 'bytes', 'bytes_iec', 2),
('tpl-412', '容器内存使用量', 'cAdvisor', '内存指标', 'cadvisor',
 'container_memory_usage_bytes{name!="",instance=~"{{instance}}"}',
 '容器总内存使用量（含缓存）', '["instance"]', 'bytes', 'bytes_iec', 3),
('tpl-413', '容器内存缓存', 'cAdvisor', '内存指标', 'cadvisor',
 'container_memory_cache{name!="",instance=~"{{instance}}"}',
 '容器页面缓存使用量', '["instance"]', 'bytes', 'bytes_iec', 4),
('tpl-414', '容器内存限制', 'cAdvisor', '内存指标', 'cadvisor',
 'container_spec_memory_limit_bytes{name!="",instance=~"{{instance}}"}',
 '容器内存限制值', '["instance"]', 'bytes', 'bytes_iec', 5);

-- 文件系统
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-421', '容器FS使用率', 'cAdvisor', '文件系统', 'cadvisor',
 'container_fs_usage_bytes{name!="",instance=~"{{instance}}"} / container_fs_limit_bytes{name!=""} * 100',
 '容器文件系统使用百分比', '["instance"]', 'percent', 'percent', 1),
('tpl-422', '容器FS使用量', 'cAdvisor', '文件系统', 'cadvisor',
 'container_fs_usage_bytes{name!="",instance=~"{{instance}}"}',
 '容器文件系统已使用字节数', '["instance"]', 'bytes', 'bytes_iec', 2);

-- 网络
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-431', '容器网络接收速率', 'cAdvisor', '网络', 'cadvisor',
 'sum(rate(container_network_receive_bytes_total{name!="",instance=~"{{instance}}"}[5m])) by (name)',
 '容器每秒网络接收字节数', '["instance"]', 'bytes', 'bytes_iec', 1),
('tpl-432', '容器网络发送速率', 'cAdvisor', '网络', 'cadvisor',
 'sum(rate(container_network_transmit_bytes_total{name!="",instance=~"{{instance}}"}[5m])) by (name)',
 '容器每秒网络发送字节数', '["instance"]', 'bytes', 'bytes_iec', 2);

-- 状态
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-441', '容器重启次数', 'cAdvisor', '状态', 'cadvisor',
 'increase(container_start_time_seconds{name!="",instance=~"{{instance}}"}[1h])',
 '容器最近1小时内重启次数', '["instance"]', 'count', 'number', 1);

-- ============================================
-- MySQL Exporter (~14 新增)
-- ============================================

-- 连接
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-502', '运行线程数', 'MySQL Exporter', '连接', 'mysqld',
 'mysql_global_status_threads_running{instance=~"{{instance}}"}',
 'MySQL当前活跃执行的线程数', '["instance"]', 'count', 'number', 2),
('tpl-503', '最大连接数使用率', 'MySQL Exporter', '连接', 'mysqld',
 'mysql_global_status_threads_connected{instance=~"{{instance}}"} / mysql_global_variables_max_connections{instance=~"{{instance}}"} * 100',
 'MySQL当前连接数占最大连接数的比例', '["instance"]', 'percent', 'percent', 3),
('tpl-504', '中断连接率', 'MySQL Exporter', '连接', 'mysqld',
 'irate(mysql_global_status_aborted_connects{instance=~"{{instance}}"}[5m])',
 '每秒中断的连接数（认证失败等）', '["instance"]', 'ops', 'ops', 4);

-- 查询性能
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-511', 'MySQL QPS', 'MySQL Exporter', '查询性能', 'mysqld',
 'irate(mysql_global_status_queries{instance=~"{{instance}}"}[5m])',
 'MySQL每秒查询次数', '["instance"]', 'ops', 'ops', 1),
('tpl-512', '慢查询增长率', 'MySQL Exporter', '查询性能', 'mysqld',
 'irate(mysql_global_status_slow_queries{instance=~"{{instance}}"}[5m])',
 '每秒新增慢查询数', '["instance"]', 'ops', 'ops', 2),
('tpl-513', 'SELECT执行率', 'MySQL Exporter', '查询性能', 'mysqld',
 'irate(mysql_global_status_commands_total{command="select",instance=~"{{instance}}"}[5m])',
 '每秒SELECT语句执行次数', '["instance"]', 'ops', 'ops', 3),
('tpl-514', 'INSERT执行率', 'MySQL Exporter', '查询性能', 'mysqld',
 'irate(mysql_global_status_commands_total{command="insert",instance=~"{{instance}}"}[5m])',
 '每秒INSERT语句执行次数', '["instance"]', 'ops', 'ops', 4),
('tpl-515', 'UPDATE执行率', 'MySQL Exporter', '查询性能', 'mysqld',
 'irate(mysql_global_status_commands_total{command="update",instance=~"{{instance}}"}[5m])',
 '每秒UPDATE语句执行次数', '["instance"]', 'ops', 'ops', 5),
('tpl-516', 'DELETE执行率', 'MySQL Exporter', '查询性能', 'mysqld',
 'irate(mysql_global_status_commands_total{command="delete",instance=~"{{instance}}"}[5m])',
 '每秒DELETE语句执行次数', '["instance"]', 'ops', 'ops', 6);

-- InnoDB
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-521', 'InnoDB缓冲池命中率', 'MySQL Exporter', 'InnoDB', 'mysqld',
 '(1 - irate(mysql_global_status_innodb_buffer_pool_reads{instance=~"{{instance}}"}[5m]) / irate(mysql_global_status_innodb_buffer_pool_read_requests{instance=~"{{instance}}"}[5m])) * 100',
 'InnoDB缓冲池命中率，低于99%需要关注', '["instance"]', 'percent', 'percent', 1),
('tpl-522', 'InnoDB缓冲池使用率', 'MySQL Exporter', 'InnoDB', 'mysqld',
 'mysql_global_status_innodb_buffer_pool_pages_data{instance=~"{{instance}}"} / mysql_global_status_innodb_buffer_pool_pages_total{instance=~"{{instance}}"} * 100',
 'InnoDB缓冲池已用页面占比', '["instance"]', 'percent', 'percent', 2),
('tpl-523', 'InnoDB脏页比例', 'MySQL Exporter', 'InnoDB', 'mysqld',
 'mysql_global_status_innodb_buffer_pool_pages_dirty{instance=~"{{instance}}"} / mysql_global_status_innodb_buffer_pool_pages_total{instance=~"{{instance}}"} * 100',
 'InnoDB缓冲池脏页占比', '["instance"]', 'percent', 'percent', 3);

-- 流量
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-531', 'MySQL接收字节速率', 'MySQL Exporter', '流量', 'mysqld',
 'irate(mysql_global_status_bytes_received{instance=~"{{instance}}"}[5m])',
 'MySQL每秒接收字节数', '["instance"]', 'bytes', 'bytes_iec', 1),
('tpl-532', 'MySQL发送字节速率', 'MySQL Exporter', '流量', 'mysqld',
 'irate(mysql_global_status_bytes_sent{instance=~"{{instance}}"}[5m])',
 'MySQL每秒发送字节数', '["instance"]', 'bytes', 'bytes_iec', 2);

-- 复制
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-541', '主从延迟', 'MySQL Exporter', '复制', 'mysqld',
 'mysql_slave_status_seconds_behind_master{instance=~"{{instance}}"}',
 'MySQL从库复制延迟秒数', '["instance"]', 'seconds', 'duration', 1);

-- ============================================
-- Redis Exporter (~13 新增)
-- ============================================

-- 内存
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-602', 'Redis内存使用率', 'Redis Exporter', '内存', 'redis',
 'redis_memory_used_bytes{instance=~"{{instance}}"} / redis_memory_max_bytes{instance=~"{{instance}}"} * 100',
 'Redis内存使用占maxmemory的百分比', '["instance"]', 'percent', 'percent', 2),
('tpl-603', 'Redis内存碎片率', 'Redis Exporter', '内存', 'redis',
 'redis_memory_used_rss_bytes{instance=~"{{instance}}"} / redis_memory_used_bytes{instance=~"{{instance}}"}',
 'Redis内存碎片率，大于1.5需要关注', '["instance"]', 'count', 'number', 3);

-- 连接
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-611', '客户端连接数', 'Redis Exporter', '连接', 'redis',
 'redis_connected_clients{instance=~"{{instance}}"}',
 'Redis当前客户端连接数', '["instance"]', 'count', 'number', 1),
('tpl-612', '阻塞客户端数', 'Redis Exporter', '连接', 'redis',
 'redis_blocked_clients{instance=~"{{instance}}"}',
 'Redis被阻塞的客户端数（BLPOP等命令）', '["instance"]', 'count', 'number', 2),
('tpl-613', '被拒绝连接率', 'Redis Exporter', '连接', 'redis',
 'irate(redis_rejected_connections_total{instance=~"{{instance}}"}[5m])',
 '每秒被拒绝的连接数（超过maxclients）', '["instance"]', 'ops', 'ops', 3);

-- 性能
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-621', 'Redis每秒操作数', 'Redis Exporter', '性能', 'redis',
 'irate(redis_commands_processed_total{instance=~"{{instance}}"}[5m])',
 'Redis每秒处理的命令数', '["instance"]', 'ops', 'ops', 1),
('tpl-622', '命令处理增长率', 'Redis Exporter', '性能', 'redis',
 'increase(redis_commands_processed_total{instance=~"{{instance}}"}[1m])',
 'Redis每分钟处理命令数增长', '["instance"]', 'count', 'number', 2),
('tpl-623', '缓存命中率', 'Redis Exporter', '性能', 'redis',
 'irate(redis_keyspace_hits_total{instance=~"{{instance}}"}[5m]) / (irate(redis_keyspace_hits_total{instance=~"{{instance}}"}[5m]) + irate(redis_keyspace_misses_total{instance=~"{{instance}}"}[5m])) * 100',
 'Redis缓存命中率，低于90%需要优化', '["instance"]', 'percent', 'percent', 3);

-- 持久化
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-631', '上次RDB距今时间', 'Redis Exporter', '持久化', 'redis',
 'time() - redis_rdb_last_save_timestamp_seconds{instance=~"{{instance}}"}',
 'Redis上次RDB快照至今经过的时间', '["instance"]', 'seconds', 'duration', 1),
('tpl-632', 'RDB以来变更数', 'Redis Exporter', '持久化', 'redis',
 'redis_rdb_changes_since_last_save{instance=~"{{instance}}"}',
 '自上次RDB快照以来数据变更次数', '["instance"]', 'count', 'number', 2);

-- Key
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-641', '驱逐Key增长率', 'Redis Exporter', 'Key', 'redis',
 'irate(redis_evicted_keys_total{instance=~"{{instance}}"}[5m])',
 '每秒被驱逐的Key数量', '["instance"]', 'ops', 'ops', 1),
('tpl-642', '各DB Key数量', 'Redis Exporter', 'Key', 'redis',
 'redis_db_keys{instance=~"{{instance}}"}',
 'Redis各数据库中的Key总数', '["instance"]', 'count', 'number', 2);

-- ============================================
-- Nginx Exporter (~8 新增)
-- ============================================

-- 连接
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-701', '活跃连接数', 'Nginx Exporter', '连接', 'nginx',
 'nginx_connections_active{instance=~"{{instance}}"}',
 'Nginx当前活跃连接数', '["instance"]', 'count', 'number', 1),
('tpl-702', '读连接数', 'Nginx Exporter', '连接', 'nginx',
 'nginx_connections_reading{instance=~"{{instance}}"}',
 'Nginx正在读取请求头的连接数', '["instance"]', 'count', 'number', 2),
('tpl-703', '写连接数', 'Nginx Exporter', '连接', 'nginx',
 'nginx_connections_writing{instance=~"{{instance}}"}',
 'Nginx正在向客户端发送响应的连接数', '["instance"]', 'count', 'number', 3),
('tpl-704', '等待连接数', 'Nginx Exporter', '连接', 'nginx',
 'nginx_connections_waiting{instance=~"{{instance}}"}',
 'Nginx当前空闲的keep-alive连接数', '["instance"]', 'count', 'number', 4),
('tpl-705', '接受连接速率', 'Nginx Exporter', '连接', 'nginx',
 'irate(nginx_connections_accepted{instance=~"{{instance}}"}[5m])',
 'Nginx每秒接受的新连接数', '["instance"]', 'ops', 'ops', 5),
('tpl-706', '处理连接速率', 'Nginx Exporter', '连接', 'nginx',
 'irate(nginx_connections_handled{instance=~"{{instance}}"}[5m])',
 'Nginx每秒处理的连接数', '["instance"]', 'ops', 'ops', 6);

-- 请求
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-711', 'Nginx请求速率', 'Nginx Exporter', '请求', 'nginx',
 'irate(nginx_http_requests_total{instance=~"{{instance}}"}[5m])',
 'Nginx每秒HTTP请求数', '["instance"]', 'ops', 'ops', 1),
('tpl-712', '各状态码响应速率', 'Nginx Exporter', '请求', 'nginx',
 'sum by(status)(irate(nginx_http_requests_total{instance=~"{{instance}}"}[5m]))',
 'Nginx按HTTP状态码统计的每秒响应数', '["instance"]', 'ops', 'ops', 2);

-- ============================================
-- 应用监控/Custom (~5 新增)
-- ============================================

-- HTTP
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-803', '接口延迟 P50', '应用监控', 'HTTP', 'custom',
 'histogram_quantile(0.50, sum(rate(http_request_duration_seconds_bucket{instance=~"{{instance}}"}[5m])) by (le))',
 'HTTP请求50分位延迟（中位数）', '["instance"]', 'seconds', 'duration', 3),
('tpl-804', '接口延迟 P95', '应用监控', 'HTTP', 'custom',
 'histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{instance=~"{{instance}}"}[5m])) by (le))',
 'HTTP请求95分位延迟', '["instance"]', 'seconds', 'duration', 4),
('tpl-805', '错误率(5xx)', '应用监控', 'HTTP', 'custom',
 'sum(rate(http_requests_total{code=~"5..",instance=~"{{instance}}"}[5m])) / sum(rate(http_requests_total{instance=~"{{instance}}"}[5m])) * 100',
 'HTTP 5xx错误率百分比', '["instance"]', 'percent', 'percent', 5);

-- 可用性
INSERT INTO prom_promql_template (id, name, category, sub_category, exporter_type, query, description, variables, unit, unit_format, sort_order) VALUES
('tpl-811', '服务存活状态', '应用监控', '可用性', 'custom',
 'up{instance=~"{{instance}}"}',
 '目标存活状态（1=正常，0=异常）', '["instance"]', 'count', 'number', 1),
('tpl-812', '采集耗时', '应用监控', '可用性', 'custom',
 'scrape_duration_seconds{instance=~"{{instance}}"}',
 'Prometheus采集目标指标的耗时', '["instance"]', 'seconds', 'duration', 2);
