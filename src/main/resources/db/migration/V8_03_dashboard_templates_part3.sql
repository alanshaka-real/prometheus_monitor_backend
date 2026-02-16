-- ============================================================
-- V8_03: Dashboard Templates Part 3 (dtpl-057 ~ dtpl-085)
-- 覆盖: JVM / Kafka / PostgreSQL / MongoDB / Elasticsearch / RabbitMQ / HAProxy
-- ============================================================

-- ===================== JVM (6 templates) =====================

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-057', 'JVM概览', 'JVM', '综合', 'jmx_exporter',
 'JVM 综合监控仪表盘，包含堆内存使用、GC暂停、线程、类加载等核心指标概览',
 '[{"id":"p1","title":"堆内存使用","type":"line","promql":"jvm_memory_bytes_used{area=\\"heap\\",instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"GC暂停时间","type":"line","promql":"rate(jvm_gc_pause_seconds_sum{instance=~\\"{{instance}}\\"}[5m])","position":{"x":4,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{action}}"},{"id":"p3","title":"线程数","type":"line","promql":"jvm_threads_current{instance=~\\"{{instance}}\\"}","position":{"x":8,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"已加载类数","type":"stat","promql":"jvm_classes_loaded{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p5","title":"堆内存使用率","type":"gauge","promql":"jvm_memory_bytes_used{area=\\"heap\\",instance=~\\"{{instance}}\\"} / jvm_memory_bytes_max{area=\\"heap\\",instance=~\\"{{instance}}\\"} * 100","position":{"x":4,"y":4},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p6","title":"非堆内存使用","type":"line","promql":"jvm_memory_bytes_used{area=\\"nonheap\\",instance=~\\"{{instance}}\\"}","position":{"x":8,"y":4},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{id}}"}]',
 NULL, '["jvm","overview","heap","gc","threads"]', 1, 6,
 '["pt-jvm-01","pt-jvm-04","pt-jvm-11","pt-jvm-14","pt-jvm-03","pt-jvm-10"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-058', 'GC分析', 'JVM', 'GC', 'jmx_exporter',
 'JVM 垃圾回收深度分析仪表盘，展示 GC 暂停时间、GC 次数、Young GC 和 Full GC 分别的表现',
 '[{"id":"p1","title":"GC暂停时间","type":"line","promql":"rate(jvm_gc_pause_seconds_sum{instance=~\\"{{instance}}\\"}[5m])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{action}}"},{"id":"p2","title":"GC次数","type":"line","promql":"rate(jvm_gc_pause_seconds_count{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{action}}"},{"id":"p3","title":"Young GC 耗时","type":"line","promql":"rate(jvm_gc_pause_seconds_sum{action=\\"end of minor GC\\",instance=~\\"{{instance}}\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"Full GC 耗时","type":"line","promql":"rate(jvm_gc_pause_seconds_sum{action=\\"end of major GC\\",instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["jvm","gc","young","full","pause"]', 1, 4,
 '["pt-jvm-04","pt-jvm-05","pt-jvm-06","pt-jvm-07"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-059', '线程监控', 'JVM', '线程', 'jmx_exporter',
 'JVM 线程详细监控仪表盘，展示存活线程数、守护线程、线程峰值和线程状态分布',
 '[{"id":"p1","title":"存活线程数","type":"line","promql":"jvm_threads_current{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"守护线程数","type":"stat","promql":"jvm_threads_daemon{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":0},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"线程峰值","type":"stat","promql":"jvm_threads_peak{instance=~\\"{{instance}}\\"}","position":{"x":9,"y":0},"size":{"w":3,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"线程状态分布","type":"line","promql":"jvm_threads_states_threads{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":12,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{state}}"}]',
 NULL, '["jvm","threads","daemon","peak","states"]', 1, 4,
 '["pt-jvm-11","pt-jvm-12","pt-jvm-13"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-060', '内存池分析', 'JVM', '内存', 'jmx_exporter',
 'JVM 各内存池深度分析仪表盘，展示堆内存各区域、Metaspace、CodeCache 和直接缓冲区',
 '[{"id":"p1","title":"堆内存各区域","type":"line","promql":"jvm_memory_bytes_used{area=\\"heap\\",instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{id}}"},{"id":"p2","title":"Metaspace 使用量","type":"line","promql":"jvm_memory_bytes_used{area=\\"nonheap\\",id=\\"Metaspace\\",instance=~\\"{{instance}}\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"CodeCache 使用量","type":"line","promql":"jvm_memory_bytes_used{area=\\"nonheap\\",id=~\\"Code.*Cache.*\\",instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{id}}"},{"id":"p4","title":"直接缓冲区","type":"line","promql":"jvm_buffer_memory_used_bytes{id=\\"direct\\",instance=~\\"{{instance}}\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["jvm","memory","metaspace","codecache","buffer"]', 1, 4,
 '["pt-jvm-01","pt-jvm-08","pt-jvm-09"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-061', 'Spring Boot应用', 'JVM', 'Spring', 'jmx_exporter',
 'Spring Boot 应用综合监控仪表盘，包含堆内存、HTTP 请求率、响应时间、错误率、线程数和 GC 指标',
 '[{"id":"p1","title":"堆内存使用","type":"line","promql":"jvm_memory_bytes_used{area=\\"heap\\",instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"HTTP请求率","type":"line","promql":"sum by (uri) (rate(http_server_requests_seconds_count{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":4,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{uri}}"},{"id":"p3","title":"HTTP响应时间","type":"line","promql":"rate(http_server_requests_seconds_sum{instance=~\\"{{instance}}\\"}[5m]) / rate(http_server_requests_seconds_count{instance=~\\"{{instance}}\\"}[5m])","position":{"x":8,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{uri}}"},{"id":"p4","title":"HTTP错误率","type":"line","promql":"sum by (status) (rate(http_server_requests_seconds_count{status=~\\"5..\\",instance=~\\"{{instance}}\\"}[5m])) / sum(rate(http_server_requests_seconds_count{instance=~\\"{{instance}}\\"}[5m])) * 100","position":{"x":0,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{status}}"},{"id":"p5","title":"线程数","type":"line","promql":"jvm_threads_current{instance=~\\"{{instance}}\\"}","position":{"x":4,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p6","title":"GC暂停时间","type":"line","promql":"rate(jvm_gc_pause_seconds_sum{instance=~\\"{{instance}}\\"}[5m])","position":{"x":8,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{action}}"}]',
 NULL, '["jvm","spring","boot","http","gc"]', 1, 6,
 '["pt-jvm-01","pt-jvm-04","pt-jvm-11"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-062', 'Tomcat监控', 'JVM', 'Tomcat', 'jmx_exporter',
 'Tomcat 容器监控仪表盘，展示活跃线程、请求处理时间、会话数和错误数',
 '[{"id":"p1","title":"活跃线程","type":"line","promql":"tomcat_threads_current{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"请求处理时间","type":"line","promql":"rate(tomcat_requestprocessor_processing_time_total{instance=~\\"{{instance}}\\"}[5m]) / rate(tomcat_requestprocessor_request_count_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ms","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"活跃会话数","type":"line","promql":"tomcat_sessions_active_current{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"错误请求数","type":"line","promql":"rate(tomcat_requestprocessor_error_count_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["jvm","tomcat","threads","sessions","errors"]', 1, 4,
 '["pt-jvm-11"]',
 '2025-01-01 00:00:00');

-- ===================== Kafka (5 templates) =====================

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-063', '集群概览', 'Kafka', '集群', 'kafka_exporter',
 'Kafka 集群综合概览仪表盘，展示分区数、Leader 分区、ISR 收缩、消息速率、消费者 Lag 和活跃控制器',
 '[{"id":"p1","title":"分区数","type":"stat","promql":"sum(kafka_broker_info{instance=~\\"{{instance}}\\"}) by (instance)","position":{"x":0,"y":0},"size":{"w":3,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"Leader分区数","type":"stat","promql":"count(kafka_topic_partition_leader{instance=~\\"{{instance}}\\"}) by (instance)","position":{"x":3,"y":0},"size":{"w":3,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"ISR收缩分区","type":"line","promql":"sum(kafka_topic_partition_under_replicated_partition{instance=~\\"{{instance}}\\"}) by (topic)","position":{"x":6,"y":0},"size":{"w":6,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{topic}}"},{"id":"p4","title":"消息速率","type":"line","promql":"sum by (topic) (rate(kafka_topic_partition_current_offset{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"msg/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{topic}}"},{"id":"p5","title":"消费者Lag","type":"line","promql":"sum by (consumergroup, topic) (kafka_consumergroup_lag{instance=~\\"{{instance}}\\"})","position":{"x":4,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{consumergroup}}-{{topic}}"},{"id":"p6","title":"活跃控制器","type":"stat","promql":"sum(kafka_controller_active_controller_count{instance=~\\"{{instance}}\\"})","position":{"x":8,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 NULL, '["kafka","cluster","partitions","lag","controller"]', 1, 6,
 '["pt-kafka-01","pt-kafka-02","pt-kafka-03","pt-kafka-05","pt-kafka-08","pt-kafka-04"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-064', 'Topic监控', 'Kafka', 'Topic', 'kafka_exporter',
 'Kafka Topic 详细监控仪表盘，展示消息速率、字节流入、字节流出和分区分布',
 '[{"id":"p1","title":"消息速率 by Topic","type":"line","promql":"sum by (topic) (rate(kafka_topic_partition_current_offset{instance=~\\"{{instance}}\\",topic=~\\"{{topic}}\\"}[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"msg/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{topic}}"},{"id":"p2","title":"字节流入","type":"line","promql":"sum by (topic) (rate(kafka_server_brokertopicmetrics_bytesin_total{instance=~\\"{{instance}}\\",topic=~\\"{{topic}}\\"}[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{topic}}"},{"id":"p3","title":"字节流出","type":"line","promql":"sum by (topic) (rate(kafka_server_brokertopicmetrics_bytesout_total{instance=~\\"{{instance}}\\",topic=~\\"{{topic}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{topic}}"},{"id":"p4","title":"分区分布","type":"line","promql":"count by (topic) (kafka_topic_partition_leader{instance=~\\"{{instance}}\\",topic=~\\"{{topic}}\\"})","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{topic}}"}]',
 NULL, '["kafka","topic","messages","bytes","partitions"]', 1, 4,
 '["pt-kafka-05","pt-kafka-06","pt-kafka-07"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-065', '消费者延迟', 'Kafka', '消费者', 'kafka_exporter',
 'Kafka 消费者延迟监控仪表盘，展示 Lag 概览、Lag Top10、Offset 速率和消费组数',
 '[{"id":"p1","title":"Lag 概览","type":"line","promql":"sum by (consumergroup, topic) (kafka_consumergroup_lag{instance=~\\"{{instance}}\\"})","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{consumergroup}}-{{topic}}"},{"id":"p2","title":"Lag Top10","type":"bar","promql":"topk(10, sum by (consumergroup) (kafka_consumergroup_lag{instance=~\\"{{instance}}\\"}))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{consumergroup}}"},{"id":"p3","title":"Offset 消费速率","type":"line","promql":"sum by (consumergroup, topic) (rate(kafka_consumergroup_current_offset{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":8,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{consumergroup}}-{{topic}}"},{"id":"p4","title":"消费组数","type":"stat","promql":"count(count by (consumergroup) (kafka_consumergroup_current_offset{instance=~\\"{{instance}}\\"}))","position":{"x":8,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 NULL, '["kafka","consumer","lag","offset","groups"]', 1, 4,
 '["pt-kafka-08","pt-kafka-10","pt-kafka-09","pt-kafka-11"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-066', '生产者监控', 'Kafka', '生产者', 'kafka_exporter',
 'Kafka 生产者监控仪表盘，展示请求速率、字节速率、重试率和批量大小',
 '[{"id":"p1","title":"请求速率","type":"line","promql":"sum by (instance) (rate(kafka_server_brokertopicmetrics_totalproducerequests_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"字节速率","type":"line","promql":"sum by (instance) (rate(kafka_server_brokertopicmetrics_bytesin_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"重试率","type":"line","promql":"sum by (instance) (rate(kafka_server_brokertopicmetrics_failedproducerequests_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"批量大小","type":"line","promql":"kafka_server_brokertopicmetrics_messagesin_total{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["kafka","producer","requests","bytes","retry"]', 1, 4,
 '["pt-kafka-12","pt-kafka-13"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-067', 'Broker性能', 'Kafka', 'Broker', 'kafka_exporter',
 'Kafka Broker 性能监控仪表盘，展示网络 IO、请求处理时间、日志刷写和副本同步',
 '[{"id":"p1","title":"网络IO","type":"line","promql":"sum by (instance) (rate(kafka_server_brokertopicmetrics_bytesin_total{instance=~\\"{{instance}}\\"}[5m]) + rate(kafka_server_brokertopicmetrics_bytesout_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"请求处理时间","type":"line","promql":"rate(kafka_network_requestmetrics_totaltimems{instance=~\\"{{instance}}\\",request=\\"Produce\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ms","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{request}}"},{"id":"p3","title":"日志刷写速率","type":"line","promql":"rate(kafka_log_log_flush_rate_and_time_ms_count{instance=~\\"{{instance}}\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"副本同步","type":"line","promql":"sum(kafka_topic_partition_under_replicated_partition{instance=~\\"{{instance}}\\"}) by (instance)","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["kafka","broker","network","io","replication"]', 1, 4,
 '["pt-kafka-14","pt-kafka-03"]',
 '2025-01-01 00:00:00');

-- ===================== PostgreSQL (5 templates) =====================

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-068', 'PG概览', 'PostgreSQL', '综合', 'postgres_exporter',
 'PostgreSQL 综合概览仪表盘，展示活跃连接、QPS、缓存命中率、事务率、连接使用率和死元组',
 '[{"id":"p1","title":"活跃连接数","type":"gauge","promql":"sum(pg_stat_activity_count{state=\\"active\\",instance=~\\"{{instance}}\\"})","position":{"x":0,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"QPS","type":"line","promql":"sum by (datname) (rate(pg_stat_database_xact_commit{instance=~\\"{{instance}}\\"}[5m]) + rate(pg_stat_database_xact_rollback{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":4,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{datname}}"},{"id":"p3","title":"缓存命中率","type":"gauge","promql":"sum(pg_stat_database_blks_hit{instance=~\\"{{instance}}\\"}) / (sum(pg_stat_database_blks_hit{instance=~\\"{{instance}}\\"}) + sum(pg_stat_database_blks_read{instance=~\\"{{instance}}\\"})) * 100","position":{"x":8,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"事务速率","type":"line","promql":"sum by (datname) (rate(pg_stat_database_xact_commit{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{datname}}"},{"id":"p5","title":"连接使用率","type":"gauge","promql":"sum(pg_stat_activity_count{instance=~\\"{{instance}}\\"}) / pg_settings_max_connections{instance=~\\"{{instance}}\\"} * 100","position":{"x":4,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p6","title":"死元组 TOP10","type":"line","promql":"topk(10, sum by (relname) (pg_stat_user_tables_n_dead_tup{instance=~\\"{{instance}}\\"}))","position":{"x":8,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{relname}}"}]',
 NULL, '["postgresql","overview","connections","qps","cache"]', 1, 6,
 '["pt-pg-01","pt-pg-04","pt-pg-15","pt-pg-03","pt-pg-13"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-069', '性能监控', 'PostgreSQL', '性能', 'postgres_exporter',
 'PostgreSQL 性能监控仪表盘，展示 QPS 趋势、事务提交/回滚、临时文件和块读取速率',
 '[{"id":"p1","title":"QPS 趋势","type":"line","promql":"sum by (datname) (rate(pg_stat_database_xact_commit{instance=~\\"{{instance}}\\",datname=~\\"{{database}}\\"}[5m]) + rate(pg_stat_database_xact_rollback{instance=~\\"{{instance}}\\",datname=~\\"{{database}}\\"}[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{datname}}"},{"id":"p2","title":"事务提交/回滚","type":"line","promql":"sum by (datname) (rate(pg_stat_database_xact_commit{instance=~\\"{{instance}}\\",datname=~\\"{{database}}\\"}[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"commit-{{datname}}"},{"id":"p3","title":"临时文件大小","type":"line","promql":"sum by (datname) (pg_stat_database_temp_bytes{instance=~\\"{{instance}}\\",datname=~\\"{{database}}\\"})","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{datname}}"},{"id":"p4","title":"块读取速率","type":"line","promql":"sum by (datname) (rate(pg_stat_database_blks_read{instance=~\\"{{instance}}\\",datname=~\\"{{database}}\\"}[5m]))","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{datname}}"}]',
 NULL, '["postgresql","performance","qps","transactions","tempfiles"]', 1, 4,
 '["pt-pg-04","pt-pg-06","pt-pg-07","pt-pg-16"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-070', '复制监控', 'PostgreSQL', '复制', 'postgres_exporter',
 'PostgreSQL 主从复制监控仪表盘，展示复制延迟、WAL 大小、从节点数和 WAL 写入速率',
 '[{"id":"p1","title":"复制延迟","type":"gauge","promql":"pg_replication_lag{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":2,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"WAL 大小","type":"line","promql":"pg_wal_lsn_diff{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"从节点数","type":"stat","promql":"count(pg_stat_replication_backend_xmin{instance=~\\"{{instance}}\\"})","position":{"x":0,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"WAL 写入速率","type":"line","promql":"rate(pg_wal_lsn_diff{instance=~\\"{{instance}}\\"}[5m])","position":{"x":4,"y":4},"size":{"w":8,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["postgresql","replication","lag","wal","standby"]', 1, 4,
 '["pt-pg-10","pt-pg-11","pt-pg-12"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-071', '锁分析', 'PostgreSQL', '锁', 'postgres_exporter',
 'PostgreSQL 锁详细分析仪表盘，展示锁按类型分布、死锁数、锁等待时间和阻塞查询数',
 '[{"id":"p1","title":"锁按类型分布","type":"line","promql":"sum by (mode) (pg_locks_count{instance=~\\"{{instance}}\\"})","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{mode}}"},{"id":"p2","title":"死锁数","type":"stat","promql":"sum by (datname) (pg_stat_database_deadlocks{instance=~\\"{{instance}}\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"锁等待时间","type":"line","promql":"pg_stat_activity_max_tx_duration{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{datname}}"},{"id":"p4","title":"阻塞查询数","type":"line","promql":"sum(pg_stat_activity_count{wait_event_type=\\"Lock\\",instance=~\\"{{instance}}\\"})","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"blocked queries"}]',
 NULL, '["postgresql","locks","deadlock","blocking","wait"]', 1, 4,
 '["pt-pg-08","pt-pg-09"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-072', '查询统计', 'PostgreSQL', '查询', 'postgres_exporter',
 'PostgreSQL 查询统计仪表盘，展示查询类型分布、慢查询、顺序扫描和索引扫描率',
 '[{"id":"p1","title":"查询类型分布","type":"line","promql":"sum by (datname) (rate(pg_stat_database_xact_commit{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"commit-{{datname}}"},{"id":"p2","title":"慢查询","type":"line","promql":"sum(pg_stat_activity_count{state=\\"active\\",instance=~\\"{{instance}}\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"active queries"},{"id":"p3","title":"顺序扫描","type":"line","promql":"sum by (relname) (rate(pg_stat_user_tables_seq_scan{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{relname}}"},{"id":"p4","title":"索引扫描率","type":"line","promql":"sum by (relname) (rate(pg_stat_user_tables_idx_scan{instance=~\\"{{instance}}\\"}[5m])) / (sum by (relname) (rate(pg_stat_user_tables_idx_scan{instance=~\\"{{instance}}\\"}[5m])) + sum by (relname) (rate(pg_stat_user_tables_seq_scan{instance=~\\"{{instance}}\\"}[5m]))) * 100","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{relname}}"}]',
 NULL, '["postgresql","query","slow","seqscan","indexscan"]', 1, 4,
 '["pt-pg-04","pt-pg-05","pt-pg-14"]',
 '2025-01-01 00:00:00');

-- ===================== MongoDB (4 templates) =====================

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-073', 'MongoDB概览', 'MongoDB', '综合', 'mongodb_exporter',
 'MongoDB 综合概览仪表盘，展示连接数、操作速率、常驻内存、文档数、数据大小和复制延迟',
 '[{"id":"p1","title":"连接数","type":"gauge","promql":"mongodb_connections_current{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"操作速率","type":"line","promql":"rate(mongodb_op_counters_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":4,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{type}}"},{"id":"p3","title":"常驻内存","type":"line","promql":"mongodb_memory{type=\\"resident\\",instance=~\\"{{instance}}\\"} * 1024 * 1024","position":{"x":8,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"文档数","type":"stat","promql":"sum by (db) (mongodb_dbstats_objects{instance=~\\"{{instance}}\\"})","position":{"x":0,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p5","title":"数据大小","type":"stat","promql":"sum(mongodb_dbstats_dataSize{instance=~\\"{{instance}}\\"})","position":{"x":4,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p6","title":"复制延迟","type":"line","promql":"mongodb_mongod_replset_member_replication_lag{instance=~\\"{{instance}}\\"}","position":{"x":8,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"s","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["mongodb","overview","connections","operations","memory"]', 1, 6,
 '["pt-mongo-01","pt-mongo-03","pt-mongo-06","pt-mongo-09"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-074', '性能监控', 'MongoDB', '性能', 'mongodb_exporter',
 'MongoDB 性能监控仪表盘，展示 CRUD 操作速率、查询延迟、文档读写速率和页面缺失',
 '[{"id":"p1","title":"CRUD 操作速率","type":"line","promql":"rate(mongodb_op_counters_total{type=~\\"insert|query|update|delete\\",instance=~\\"{{instance}}\\"}[5m])","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":30,"showLegend":true,"stackMode":"stacked"},"legendFormat":"{{type}}"},{"id":"p2","title":"查询延迟","type":"line","promql":"rate(mongodb_mongod_op_latencies_latency_total{instance=~\\"{{instance}}\\"}[5m]) / rate(mongodb_mongod_op_latencies_ops_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"us","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{type}}"},{"id":"p3","title":"文档读写速率","type":"line","promql":"rate(mongodb_mongod_metrics_document_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{state}}"},{"id":"p4","title":"页面缺失","type":"line","promql":"rate(mongodb_extra_info_page_faults_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["mongodb","performance","crud","latency","pagefaults"]', 1, 4,
 '["pt-mongo-04","pt-mongo-05","pt-mongo-03"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-075', '复制监控', 'MongoDB', '复制', 'mongodb_exporter',
 'MongoDB 复制集监控仪表盘，展示 Oplog 窗口、复制延迟、成员状态和操作重放速率',
 '[{"id":"p1","title":"Oplog 窗口","type":"stat","promql":"mongodb_mongod_replset_oplog_tail_timestamp{instance=~\\"{{instance}}\\"} - mongodb_mongod_replset_oplog_head_timestamp{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"复制延迟","type":"gauge","promql":"mongodb_mongod_replset_member_replication_lag{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":1,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"成员状态","type":"table","promql":"mongodb_mongod_replset_member_state{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{name}}"},{"id":"p4","title":"操作重放速率","type":"line","promql":"rate(mongodb_mongod_replset_oplog_tail_timestamp{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["mongodb","replication","oplog","lag","members"]', 1, 4,
 '["pt-mongo-08","pt-mongo-09"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-076', '存储分析', 'MongoDB', '存储', 'mongodb_exporter',
 'MongoDB 存储分析仪表盘，展示数据大小、索引大小、集合数量和存储引擎统计',
 '[{"id":"p1","title":"数据大小","type":"bar","promql":"sum by (db) (mongodb_dbstats_dataSize{instance=~\\"{{instance}}\\"})","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{db}}"},{"id":"p2","title":"索引大小","type":"bar","promql":"sum by (db) (mongodb_dbstats_indexSize{instance=~\\"{{instance}}\\"})","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{db}}"},{"id":"p3","title":"集合数量","type":"stat","promql":"sum by (db) (mongodb_dbstats_collections{instance=~\\"{{instance}}\\"})","position":{"x":0,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"存储引擎统计","type":"line","promql":"sum by (db) (mongodb_dbstats_storageSize{instance=~\\"{{instance}}\\"})","position":{"x":4,"y":4},"size":{"w":8,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{db}}"}]',
 NULL, '["mongodb","storage","data","index","collections"]', 1, 4,
 '["pt-mongo-10"]',
 '2025-01-01 00:00:00');

-- ===================== Elasticsearch (4 templates) =====================

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-077', 'ES集群概览', 'Elasticsearch', '集群', 'elasticsearch_exporter',
 'Elasticsearch 集群综合概览仪表盘，展示集群健康、节点数、分片概览、索引速率、搜索速率和 JVM 堆使用',
 '[{"id":"p1","title":"集群健康","type":"stat","promql":"elasticsearch_cluster_health_status{color=\\"green\\",instance=~\\"{{instance}}\\"} * 1 + elasticsearch_cluster_health_status{color=\\"yellow\\",instance=~\\"{{instance}}\\"} * 2 + elasticsearch_cluster_health_status{color=\\"red\\",instance=~\\"{{instance}}\\"} * 3","position":{"x":0,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p2","title":"节点数","type":"stat","promql":"elasticsearch_cluster_health_number_of_nodes{instance=~\\"{{instance}}\\"}","position":{"x":4,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p3","title":"分片概览","type":"stat","promql":"elasticsearch_cluster_health_active_shards{instance=~\\"{{instance}}\\"}","position":{"x":8,"y":0},"size":{"w":4,"h":3},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p4","title":"索引速率","type":"line","promql":"sum by (instance) (rate(elasticsearch_indices_indexing_index_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p5","title":"搜索速率","type":"line","promql":"sum by (instance) (rate(elasticsearch_indices_search_query_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":4,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p6","title":"JVM堆内存使用","type":"line","promql":"elasticsearch_jvm_memory_used_bytes{area=\\"heap\\",instance=~\\"{{instance}}\\"}","position":{"x":8,"y":3},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["elasticsearch","cluster","health","nodes","shards","indexing","search"]', 1, 6,
 '["pt-es-01","pt-es-02","pt-es-03","pt-es-04","pt-es-06","pt-es-08"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-078', '索引与搜索', 'Elasticsearch', '索引', 'elasticsearch_exporter',
 'Elasticsearch 索引与搜索性能仪表盘，展示索引速率、索引延迟、搜索速率和搜索延迟',
 '[{"id":"p1","title":"索引速率","type":"line","promql":"sum by (instance) (rate(elasticsearch_indices_indexing_index_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"索引延迟","type":"line","promql":"rate(elasticsearch_indices_indexing_index_time_seconds_total{instance=~\\"{{instance}}\\"}[5m]) / rate(elasticsearch_indices_indexing_index_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"搜索速率","type":"line","promql":"sum by (instance) (rate(elasticsearch_indices_search_query_total{instance=~\\"{{instance}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"搜索延迟","type":"line","promql":"rate(elasticsearch_indices_search_query_time_seconds{instance=~\\"{{instance}}\\"}[5m]) / rate(elasticsearch_indices_search_query_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["elasticsearch","indexing","search","rate","latency"]', 1, 4,
 '["pt-es-04","pt-es-05","pt-es-06","pt-es-07"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-079', '节点监控', 'Elasticsearch', '节点', 'elasticsearch_exporter',
 'Elasticsearch 节点级监控仪表盘，展示 CPU 使用率、内存使用、磁盘使用和网络传输',
 '[{"id":"p1","title":"CPU使用率","type":"line","promql":"elasticsearch_process_cpu_percent{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"内存使用","type":"line","promql":"elasticsearch_os_memory_used_bytes{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"磁盘使用","type":"line","promql":"elasticsearch_filesystem_data_size_bytes{instance=~\\"{{instance}}\\"} - elasticsearch_filesystem_data_free_bytes{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"网络传输","type":"line","promql":"rate(elasticsearch_transport_rx_size_bytes_total{instance=~\\"{{instance}}\\"}[5m]) + rate(elasticsearch_transport_tx_size_bytes_total{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["elasticsearch","node","cpu","memory","disk","network"]', 1, 4,
 '["pt-es-10"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-080', 'JVM监控', 'Elasticsearch', 'JVM', 'elasticsearch_exporter',
 'Elasticsearch 节点 JVM 监控仪表盘，展示堆内存使用、GC 耗时、线程数和缓冲池',
 '[{"id":"p1","title":"堆内存使用","type":"line","promql":"elasticsearch_jvm_memory_used_bytes{area=\\"heap\\",instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"GC耗时","type":"line","promql":"rate(elasticsearch_jvm_gc_collection_seconds_count{instance=~\\"{{instance}}\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{gc}}-{{instance}}"},{"id":"p3","title":"线程数","type":"line","promql":"elasticsearch_jvm_threads_count{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"缓冲池","type":"line","promql":"elasticsearch_jvm_memory_pool_used_bytes{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pool}}-{{instance}}"}]',
 NULL, '["elasticsearch","jvm","heap","gc","threads","buffer"]', 1, 4,
 '["pt-es-08","pt-es-09"]',
 '2025-01-01 00:00:00');

-- ===================== RabbitMQ (3 templates) =====================

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-081', 'RabbitMQ概览', 'RabbitMQ', '综合', 'rabbitmq_exporter',
 'RabbitMQ 综合概览仪表盘，展示队列消息数、消费者数、发布速率、确认速率、连接数和通道数',
 '[{"id":"p1","title":"队列消息数","type":"line","promql":"rabbitmq_queue_messages{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"},{"id":"p2","title":"消费者数","type":"bar","promql":"rabbitmq_queue_consumers{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}","position":{"x":4,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"},{"id":"p3","title":"发布速率","type":"line","promql":"sum by (queue) (rate(rabbitmq_queue_messages_published_total{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}[5m]))","position":{"x":8,"y":0},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"msg/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"},{"id":"p4","title":"确认速率","type":"line","promql":"sum by (queue) (rate(rabbitmq_queue_messages_acked_total{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}[5m]))","position":{"x":0,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"msg/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"},{"id":"p5","title":"连接数","type":"stat","promql":"rabbitmq_connections{instance=~\\"{{instance}}\\"}","position":{"x":4,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p6","title":"通道数","type":"stat","promql":"rabbitmq_channels{instance=~\\"{{instance}}\\"}","position":{"x":8,"y":4},"size":{"w":4,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"value","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 NULL, '["rabbitmq","overview","queues","consumers","publish","ack"]', 1, 6,
 '["pt-rmq-01","pt-rmq-02","pt-rmq-03","pt-rmq-04","pt-rmq-05","pt-rmq-06"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-082', '队列监控', 'RabbitMQ', '队列', 'rabbitmq_exporter',
 'RabbitMQ 队列详细监控仪表盘，展示队列深度、消费速率、未确认消息和队列内存',
 '[{"id":"p1","title":"队列深度","type":"line","promql":"rabbitmq_queue_messages{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"},{"id":"p2","title":"消费速率","type":"line","promql":"sum by (queue) (rate(rabbitmq_queue_messages_delivered_total{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}[5m]))","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"msg/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"},{"id":"p3","title":"未确认消息","type":"line","promql":"rabbitmq_queue_messages_unacked{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"},{"id":"p4","title":"队列内存","type":"line","promql":"rabbitmq_queue_memory{instance=~\\"{{instance}}\\",queue=~\\"{{queue}}\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{queue}}"}]',
 NULL, '["rabbitmq","queue","depth","consume","unacked","memory"]', 1, 4,
 '["pt-rmq-01","pt-rmq-03","pt-rmq-04"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-083', '节点监控', 'RabbitMQ', '节点', 'rabbitmq_exporter',
 'RabbitMQ 节点级监控仪表盘，展示内存使用、文件描述符使用率、磁盘空间和 Erlang 进程数',
 '[{"id":"p1","title":"内存使用","type":"line","promql":"rabbitmq_node_mem_used{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"FD使用率","type":"gauge","promql":"rabbitmq_node_fd_used{instance=~\\"{{instance}}\\"} / rabbitmq_node_fd_total{instance=~\\"{{instance}}\\"} * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"磁盘可用空间","type":"line","promql":"rabbitmq_node_disk_free{instance=~\\"{{instance}}\\"}","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p4","title":"Erlang 进程数","type":"line","promql":"rabbitmq_node_proc_used{instance=~\\"{{instance}}\\"}","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 NULL, '["rabbitmq","node","memory","fd","disk","erlang"]', 1, 4,
 '["pt-rmq-07","pt-rmq-08"]',
 '2025-01-01 00:00:00');

-- ===================== HAProxy (2 templates) =====================

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-084', 'HAProxy概览', 'HAProxy', '综合', 'haproxy_exporter',
 'HAProxy 综合概览仪表盘，展示前端连接数、后端响应时间、请求速率和错误率',
 '[{"id":"p1","title":"前端连接数","type":"line","promql":"haproxy_frontend_current_sessions{instance=~\\"{{instance}}\\",proxy=~\\"{{frontend}}\\"}","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{proxy}}"},{"id":"p2","title":"后端响应时间","type":"line","promql":"haproxy_backend_response_time_average_seconds{instance=~\\"{{instance}}\\",proxy=~\\"{{backend}}\\"}","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"s","decimals":4,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{proxy}}"},{"id":"p3","title":"请求速率","type":"line","promql":"rate(haproxy_frontend_http_requests_total{instance=~\\"{{instance}}\\",proxy=~\\"{{frontend}}\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{proxy}}"},{"id":"p4","title":"错误率","type":"line","promql":"rate(haproxy_frontend_http_responses_total{code=\\"5xx\\",instance=~\\"{{instance}}\\",proxy=~\\"{{frontend}}\\"}[5m]) / rate(haproxy_frontend_http_requests_total{instance=~\\"{{instance}}\\",proxy=~\\"{{frontend}}\\"}[5m]) * 100","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{proxy}}"}]',
 NULL, '["haproxy","overview","frontend","backend","requests","errors"]', 1, 4,
 '["pt-haproxy-01","pt-haproxy-04","pt-haproxy-03","pt-haproxy-05"]',
 '2025-01-01 00:00:00');

INSERT IGNORE INTO prom_dashboard_template
    (id, name, category, sub_category, exporter_type, description, panels, thumbnail, tags, version, panel_count, panel_template_ids, created_at)
VALUES
('dtpl-085', '后端健康', 'HAProxy', '后端', 'haproxy_exporter',
 'HAProxy 后端健康监控仪表盘，展示活跃服务器比例、健康检查失败、后端流量和连接错误',
 '[{"id":"p1","title":"活跃服务器比例","type":"gauge","promql":"haproxy_backend_active_servers{instance=~\\"{{instance}}\\",proxy=~\\"{{backend}}\\"} / haproxy_backend_servers{instance=~\\"{{instance}}\\",proxy=~\\"{{backend}}\\"} * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":"{{proxy}}"},{"id":"p2","title":"健康检查失败","type":"line","promql":"rate(haproxy_backend_check_failures_total{instance=~\\"{{instance}}\\",proxy=~\\"{{backend}}\\"}[5m])","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{proxy}}"},{"id":"p3","title":"后端流量","type":"line","promql":"rate(haproxy_backend_bytes_in_total{instance=~\\"{{instance}}\\",proxy=~\\"{{backend}}\\"}[5m]) + rate(haproxy_backend_bytes_out_total{instance=~\\"{{instance}}\\",proxy=~\\"{{backend}}\\"}[5m])","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"Bps","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{proxy}}"},{"id":"p4","title":"连接错误","type":"line","promql":"rate(haproxy_backend_connection_errors_total{instance=~\\"{{instance}}\\",proxy=~\\"{{backend}}\\"}[5m])","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"ops/s","decimals":2,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{proxy}}"}]',
 NULL, '["haproxy","backend","health","check","traffic","errors"]', 1, 4,
 '["pt-haproxy-06","pt-haproxy-05"]',
 '2025-01-01 00:00:00');
