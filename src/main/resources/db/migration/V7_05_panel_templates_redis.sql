-- ============================================================
-- V7_05: Panel Templates - Redis (redis_exporter)
-- 18 panel templates covering memory, commands, connections,
-- persistence, and replication monitoring.
-- ============================================================

INSERT IGNORE INTO prom_panel_template
    (id, name, description, category, sub_category, exporter_type, chart_type, promql, unit, unit_format, default_width, default_height, thresholds, options, tags, sort_order, created_at)
VALUES

-- ============================================================
-- 内存 (sub_category: '内存', 4 panels)
-- ============================================================

('pt-redis-01', '内存使用量', '当前 Redis 实例已分配的内存总量，用于评估内存水位',
 'Redis', '内存', 'redis_exporter', 'line',
 'redis_memory_used_bytes',
 'bytes', 'bytes_si', 6, 3,
 '[{"value":1073741824,"color":"#52c41a","label":"< 1GB"},{"value":4294967296,"color":"#faad14","label":"< 4GB"},{"value":8589934592,"color":"#f5222d","label":">= 8GB"}]',
 '{"decimals":2,"fillOpacity":20,"showLegend":true,"stackMode":"none","colorMode":"thresholds","legendFormat":"{{instance}}"}',
 '["redis","memory","内存"]', 1, '2025-01-01 00:00:00'),

('pt-redis-02', '内存使用峰值', '自 Redis 启动以来内存使用的历史峰值',
 'Redis', '内存', 'redis_exporter', 'stat',
 'redis_memory_used_peak_bytes',
 'bytes', 'bytes_si', 3, 3,
 '[{"value":2147483648,"color":"#52c41a","label":"< 2GB"},{"value":8589934592,"color":"#faad14","label":"< 8GB"},{"value":17179869184,"color":"#f5222d","label":">= 16GB"}]',
 '{"decimals":2,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","memory","peak","内存"]', 2, '2025-01-01 00:00:00'),

('pt-redis-03', '内存碎片率', 'RSS 内存与已用内存的比值，>1.5 表示碎片严重，<1 表示使用了 swap',
 'Redis', '内存', 'redis_exporter', 'gauge',
 'redis_memory_used_rss_bytes / redis_memory_used_bytes',
 '', 'none', 3, 3,
 '[{"value":1.0,"color":"#f5222d","label":"< 1.0 (swap)"},{"value":1.5,"color":"#52c41a","label":"正常"},{"value":2.0,"color":"#faad14","label":"碎片偏高"},{"value":999,"color":"#f5222d","label":"碎片严重"}]',
 '{"decimals":2,"min":0,"max":3,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","memory","fragmentation","碎片"]', 3, '2025-01-01 00:00:00'),

('pt-redis-04', 'Key 驱逐数', '因 maxmemory 策略被驱逐的 Key 速率，持续 >0 说明内存不足',
 'Redis', '内存', 'redis_exporter', 'line',
 'rate(redis_evicted_keys_total[5m])',
 'ops', 'ops_per_sec', 6, 3,
 '[{"value":0,"color":"#52c41a","label":"无驱逐"},{"value":10,"color":"#faad14","label":"有驱逐"},{"value":100,"color":"#f5222d","label":"大量驱逐"}]',
 '{"decimals":1,"fillOpacity":15,"showLegend":true,"stackMode":"none","colorMode":"thresholds","legendFormat":"{{instance}}"}',
 '["redis","memory","eviction","驱逐"]', 4, '2025-01-01 00:00:00'),

-- ============================================================
-- 命令 (sub_category: '命令', 4 panels)
-- ============================================================

('pt-redis-05', '命令处理总量', '每秒命令处理速率趋势，反映 Redis 负载变化',
 'Redis', '命令', 'redis_exporter', 'line',
 'rate(redis_commands_processed_total[5m])',
 'ops', 'ops_per_sec', 6, 3,
 '[{"value":10000,"color":"#52c41a","label":"低负载"},{"value":50000,"color":"#faad14","label":"中负载"},{"value":100000,"color":"#f5222d","label":"高负载"}]',
 '{"decimals":0,"fillOpacity":15,"showLegend":true,"stackMode":"none","colorMode":"thresholds","legendFormat":"{{instance}}"}',
 '["redis","commands","throughput","吞吐"]', 5, '2025-01-01 00:00:00'),

('pt-redis-06', 'QPS', 'Redis 瞬时每秒操作数，实时反映当前负载',
 'Redis', '命令', 'redis_exporter', 'stat',
 'redis_instantaneous_ops_per_sec',
 'ops', 'ops_per_sec', 3, 3,
 '[{"value":5000,"color":"#52c41a","label":"低"},{"value":30000,"color":"#faad14","label":"中"},{"value":80000,"color":"#f5222d","label":"高"}]',
 '{"decimals":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","commands","qps"]', 6, '2025-01-01 00:00:00'),

('pt-redis-07', '命中率', 'Keyspace 命中率百分比，低于 90% 需关注缓存策略',
 'Redis', '命令', 'redis_exporter', 'gauge',
 'rate(redis_keyspace_hits_total[5m]) / (rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m])) * 100',
 '%', 'percent', 3, 3,
 '[{"value":80,"color":"#f5222d","label":"< 80% 差"},{"value":90,"color":"#faad14","label":"< 90% 一般"},{"value":100,"color":"#52c41a","label":">= 90% 良好"}]',
 '{"decimals":1,"min":0,"max":100,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","commands","hitrate","命中率"]', 7, '2025-01-01 00:00:00'),

('pt-redis-08', '命令类型分布', '按命令类型(GET/SET/DEL等)分解的每秒执行速率',
 'Redis', '命令', 'redis_exporter', 'line',
 'rate(redis_commands_total[5m])',
 'ops', 'ops_per_sec', 6, 3,
 '[]',
 '{"decimals":1,"fillOpacity":10,"showLegend":true,"stackMode":"normal","colorMode":"fixed","legendFormat":"{{cmd}}"}',
 '["redis","commands","distribution","命令分布"]', 8, '2025-01-01 00:00:00'),

-- ============================================================
-- 连接 (sub_category: '连接', 4 panels)
-- ============================================================

('pt-redis-09', '客户端连接数', '当前已建立的客户端连接数，接近 maxclients 时需扩容',
 'Redis', '连接', 'redis_exporter', 'gauge',
 'redis_connected_clients',
 '', 'short', 3, 3,
 '[{"value":100,"color":"#52c41a","label":"正常"},{"value":500,"color":"#faad14","label":"偏多"},{"value":5000,"color":"#f5222d","label":"过多"}]',
 '{"decimals":0,"min":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","connections","clients","连接"]', 9, '2025-01-01 00:00:00'),

('pt-redis-10', '阻塞客户端数', '执行 BLPOP/BRPOP/WAIT 等阻塞命令的客户端数',
 'Redis', '连接', 'redis_exporter', 'stat',
 'redis_blocked_clients',
 '', 'short', 3, 3,
 '[{"value":0,"color":"#52c41a","label":"无阻塞"},{"value":5,"color":"#faad14","label":"少量阻塞"},{"value":20,"color":"#f5222d","label":"大量阻塞"}]',
 '{"decimals":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","connections","blocked","阻塞"]', 10, '2025-01-01 00:00:00'),

('pt-redis-11', '拒绝连接数', '因 maxclients 限制被拒绝的连接速率，>0 表示需增大上限',
 'Redis', '连接', 'redis_exporter', 'line',
 'rate(redis_rejected_connections_total[5m])',
 'ops', 'ops_per_sec', 6, 3,
 '[{"value":0,"color":"#52c41a","label":"无拒绝"},{"value":1,"color":"#faad14","label":"有拒绝"},{"value":10,"color":"#f5222d","label":"频繁拒绝"}]',
 '{"decimals":2,"fillOpacity":15,"showLegend":true,"stackMode":"none","colorMode":"thresholds","legendFormat":"{{instance}}"}',
 '["redis","connections","rejected","拒绝"]', 11, '2025-01-01 00:00:00'),

('pt-redis-12', '连接接收率', '每秒新接收的连接数，反映客户端连接建立频率',
 'Redis', '连接', 'redis_exporter', 'line',
 'rate(redis_connections_received_total[5m])',
 'ops', 'ops_per_sec', 6, 3,
 '[{"value":100,"color":"#52c41a","label":"正常"},{"value":1000,"color":"#faad14","label":"偏高"},{"value":5000,"color":"#f5222d","label":"过高"}]',
 '{"decimals":1,"fillOpacity":15,"showLegend":true,"stackMode":"none","colorMode":"thresholds","legendFormat":"{{instance}}"}',
 '["redis","connections","received","接收"]', 12, '2025-01-01 00:00:00'),

-- ============================================================
-- 持久化 (sub_category: '持久化', 3 panels)
-- ============================================================

('pt-redis-13', 'RDB 上次保存距今', '距上次成功 RDB 持久化的秒数，过长说明备份异常',
 'Redis', '持久化', 'redis_exporter', 'stat',
 'time() - redis_rdb_last_save_timestamp_seconds',
 's', 'duration_sec', 4, 3,
 '[{"value":3600,"color":"#52c41a","label":"< 1h"},{"value":86400,"color":"#faad14","label":"< 1d"},{"value":604800,"color":"#f5222d","label":">= 1d"}]',
 '{"decimals":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","persistence","rdb","持久化"]', 13, '2025-01-01 00:00:00'),

('pt-redis-14', 'RDB 待保存变更', '自上次 RDB 保存以来发生的数据变更次数',
 'Redis', '持久化', 'redis_exporter', 'stat',
 'redis_rdb_changes_since_last_save',
 '', 'short', 4, 3,
 '[{"value":1000,"color":"#52c41a","label":"< 1K"},{"value":100000,"color":"#faad14","label":"< 100K"},{"value":1000000,"color":"#f5222d","label":">= 1M"}]',
 '{"decimals":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","persistence","rdb","changes","待保存"]', 14, '2025-01-01 00:00:00'),

('pt-redis-15', 'AOF 重写时间', '上次 AOF 重写(rewrite)操作所耗时间(秒)，-1 表示从未执行',
 'Redis', '持久化', 'redis_exporter', 'stat',
 'redis_aof_last_rewrite_duration_sec',
 's', 'duration_sec', 4, 3,
 '[{"value":1,"color":"#52c41a","label":"< 1s 快"},{"value":10,"color":"#faad14","label":"< 10s 一般"},{"value":60,"color":"#f5222d","label":">= 60s 慢"}]',
 '{"decimals":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","persistence","aof","rewrite","重写"]', 15, '2025-01-01 00:00:00'),

-- ============================================================
-- 复制 (sub_category: '复制', 3 panels)
-- ============================================================

('pt-redis-16', '从节点数', '当前连接到此 Redis 主节点的从节点(replica)数量',
 'Redis', '复制', 'redis_exporter', 'stat',
 'redis_connected_slaves',
 '', 'short', 4, 3,
 '[{"value":0,"color":"#f5222d","label":"无从节点"},{"value":1,"color":"#faad14","label":"1 个"},{"value":2,"color":"#52c41a","label":">= 2 个"}]',
 '{"decimals":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","replication","slaves","复制"]', 16, '2025-01-01 00:00:00'),

('pt-redis-17', '复制偏移量差', '主从节点复制偏移量差值，差值越大表示从节点延迟越严重',
 'Redis', '复制', 'redis_exporter', 'line',
 'redis_master_repl_offset - on(instance) group_right redis_slave_repl_offset',
 'bytes', 'bytes_si', 8, 3,
 '[{"value":1024,"color":"#52c41a","label":"< 1KB 同步"},{"value":1048576,"color":"#faad14","label":"< 1MB 有延迟"},{"value":104857600,"color":"#f5222d","label":">= 100MB 严重延迟"}]',
 '{"decimals":0,"fillOpacity":15,"showLegend":true,"stackMode":"none","colorMode":"thresholds","legendFormat":"{{instance}}"}',
 '["redis","replication","offset","lag","延迟"]', 17, '2025-01-01 00:00:00'),

('pt-redis-18', 'Key 总数', '所有数据库(db0~dbN)的 Key 总量',
 'Redis', '复制', 'redis_exporter', 'stat',
 'sum(redis_db_keys) by (instance)',
 '', 'short', 4, 3,
 '[{"value":100000,"color":"#52c41a","label":"< 100K"},{"value":1000000,"color":"#faad14","label":"< 1M"},{"value":10000000,"color":"#f5222d","label":">= 10M"}]',
 '{"decimals":0,"colorMode":"thresholds","showLegend":false,"legendFormat":"{{instance}}"}',
 '["redis","keys","总量"]', 18, '2025-01-01 00:00:00');
