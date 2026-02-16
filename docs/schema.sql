-- Prometheus Monitor Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS prometheus_monitor DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE prometheus_monitor;

-- ============================================
-- 1. Authentication & Permission Tables (5)
-- ============================================

CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(255) DEFAULT '',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    nick_name VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    gender VARCHAR(10) DEFAULT NULL COMMENT '性别',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/disabled',
    last_login DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    built_in TINYINT DEFAULT 0,
    user_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL COMMENT 'menu/button/api',
    parent_id VARCHAR(36) DEFAULT '',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 2. Datasource Management Tables (3)
-- ============================================

CREATE TABLE IF NOT EXISTS prom_instance (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'unknown' COMMENT 'online/offline/unknown',
    group_name VARCHAR(50) DEFAULT '',
    labels JSON,
    scrape_interval VARCHAR(10) DEFAULT '15s',
    retention_time VARCHAR(10) DEFAULT '15d',
    version VARCHAR(20) DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_group (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_exporter (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL COMMENT 'node/blackbox/process/cadvisor/mysqld/redis/nginx/custom',
    name VARCHAR(100) NOT NULL,
    host VARCHAR(100) NOT NULL,
    port INT NOT NULL,
    `interval` VARCHAR(10) DEFAULT '15s',
    metrics_path VARCHAR(100) DEFAULT '/metrics',
    status VARCHAR(20) DEFAULT 'stopped' COMMENT 'running/stopped/error',
    instance_id VARCHAR(36),
    labels JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_type (type),
    INDEX idx_instance_id (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_metric_meta (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'counter/gauge/histogram/summary',
    help TEXT,
    labels JSON,
    exporter VARCHAR(50),
    unit VARCHAR(20) DEFAULT '',
    favorite TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_type (type),
    INDEX idx_exporter (exporter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 3. Dashboard Tables (2)
-- ============================================

CREATE TABLE IF NOT EXISTS prom_dashboard (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    panels JSON,
    time_range JSON,
    refresh_interval INT DEFAULT 30,
    tags JSON,
    folder_id VARCHAR(36) DEFAULT '',
    created_by VARCHAR(50) DEFAULT '',
    favorite TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_dashboard_template (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    panels JSON,
    thumbnail VARCHAR(255) DEFAULT '',
    tags JSON,
    version VARCHAR(20) DEFAULT '1.0.0',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 4. Query Tables (2)
-- ============================================

CREATE TABLE IF NOT EXISTS prom_query_history (
    id VARCHAR(36) PRIMARY KEY,
    query TEXT NOT NULL,
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    duration DOUBLE DEFAULT 0 COMMENT 'in milliseconds',
    result_count INT DEFAULT 0,
    favorite TINYINT DEFAULT 0,
    user_id VARCHAR(36),
    INDEX idx_executed_at (executed_at),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_promql_template (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    sub_category VARCHAR(50) DEFAULT '' COMMENT '二级分类',
    exporter_type VARCHAR(20) DEFAULT '' COMMENT '关联组件类型: node/blackbox/process/cadvisor/mysqld/redis/nginx/custom',
    query TEXT NOT NULL,
    description TEXT,
    variables JSON,
    unit VARCHAR(20) DEFAULT 'none' COMMENT '语义单位: percent/bytes/seconds/count/ops/bps/none',
    unit_format VARCHAR(20) DEFAULT 'none' COMMENT '格式化策略: percent/bytes_iec/duration/number/bps/ops/none',
    sort_order INT DEFAULT 0 COMMENT '子分类内排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_exporter_type (exporter_type),
    INDEX idx_sub_category (sub_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 5. Alert Tables (4)
-- ============================================

CREATE TABLE IF NOT EXISTS prom_alert_rule (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    group_name VARCHAR(50) DEFAULT '',
    expr TEXT NOT NULL,
    duration VARCHAR(10) DEFAULT '5m',
    severity VARCHAR(20) DEFAULT 'warning' COMMENT 'info/warning/error/critical',
    labels JSON,
    annotations JSON,
    status VARCHAR(20) DEFAULT 'enabled' COMMENT 'enabled/disabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_severity (severity),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_alert_history (
    id VARCHAR(36) PRIMARY KEY,
    alert_name VARCHAR(100) NOT NULL,
    severity VARCHAR(20),
    status VARCHAR(20) COMMENT 'firing/resolved/pending',
    instance VARCHAR(100) DEFAULT '',
    value VARCHAR(50) DEFAULT '',
    starts_at DATETIME,
    ends_at DATETIME,
    duration VARCHAR(50) DEFAULT '',
    handled_by VARCHAR(50) DEFAULT '',
    handled_at DATETIME,
    remark TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_starts_at (starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_silence (
    id VARCHAR(36) PRIMARY KEY,
    matchers JSON,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    created_by VARCHAR(50) DEFAULT '',
    comment TEXT,
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/expired/pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_notification_channel (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL COMMENT 'dingtalk/wechat/email/slack/webhook',
    name VARCHAR(100) NOT NULL,
    config JSON,
    enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 6. Cluster Tables (2)
-- ============================================

CREATE TABLE IF NOT EXISTS prom_cluster (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    region VARCHAR(50) DEFAULT '',
    prometheus_url VARCHAR(255) DEFAULT '',
    instance_id VARCHAR(36) DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'healthy' COMMENT 'healthy/warning/critical/offline',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_instance_id (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prom_cluster_node (
    id VARCHAR(36) PRIMARY KEY,
    hostname VARCHAR(100) NOT NULL,
    ip VARCHAR(50) NOT NULL,
    role VARCHAR(20) DEFAULT 'worker' COMMENT 'master/worker/edge',
    cpu JSON,
    memory JSON,
    disk JSON,
    network JSON,
    status VARCHAR(20) DEFAULT 'online' COMMENT 'online/offline/maintenance',
    os VARCHAR(50) DEFAULT '',
    kernel VARCHAR(50) DEFAULT '',
    uptime VARCHAR(50) DEFAULT '',
    labels JSON,
    cluster_id VARCHAR(36),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_cluster_id (cluster_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 7. System Tables (3)
-- ============================================

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    username VARCHAR(50),
    action VARCHAR(50),
    resource VARCHAR(50),
    resource_id VARCHAR(36) DEFAULT '',
    detail TEXT,
    ip VARCHAR(50),
    user_agent VARCHAR(255),
    status VARCHAR(20) DEFAULT 'success' COMMENT 'success/failure',
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_global_settings (
    id VARCHAR(36) PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(200),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_system_log (
    id VARCHAR(36) PRIMARY KEY,
    level VARCHAR(10) NOT NULL COMMENT 'INFO/WARN/ERROR/DEBUG',
    message TEXT,
    source VARCHAR(100),
    stack_trace TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_level (level),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- Seed Data
-- ============================================

-- Users (password: 123456 encrypted with BCrypt $2a$)
-- Frontend login page presets: Super/Admin/User with password 123456
INSERT INTO sys_user (id, username, password, email, phone, avatar, status, last_login, created_at) VALUES
('user-1', 'Super', '$2a$10$J6yUFcxXik/DNHdlQJFmgui2MZq36fxnQ6NzQ5w8BjvyIb3dIN7Vm', 'super@example.com', '13800000001', '', 'active', '2024-06-22 14:30:00', '2024-01-01 00:00:00'),
('user-2', 'Admin', '$2a$10$J6yUFcxXik/DNHdlQJFmgui2MZq36fxnQ6NzQ5w8BjvyIb3dIN7Vm', 'admin@example.com', '13800000002', '', 'active', '2024-06-22 10:00:00', '2024-01-15 09:00:00'),
('user-3', 'User', '$2a$10$J6yUFcxXik/DNHdlQJFmgui2MZq36fxnQ6NzQ5w8BjvyIb3dIN7Vm', 'user@example.com', '13800000003', '', 'active', '2024-06-21 16:00:00', '2024-02-01 09:00:00'),
('user-4', 'sre-li', '$2a$10$J6yUFcxXik/DNHdlQJFmgui2MZq36fxnQ6NzQ5w8BjvyIb3dIN7Vm', 'li@example.com', '13800000004', '', 'active', '2024-06-22 08:00:00', '2024-01-20 09:00:00'),
('user-5', 'dba-chen', '$2a$10$J6yUFcxXik/DNHdlQJFmgui2MZq36fxnQ6NzQ5w8BjvyIb3dIN7Vm', 'chen@example.com', '13800000005', '', 'active', '2024-06-20 14:00:00', '2024-03-01 09:00:00'),
('user-6', 'viewer-liu', '$2a$10$J6yUFcxXik/DNHdlQJFmgui2MZq36fxnQ6NzQ5w8BjvyIb3dIN7Vm', 'liu@example.com', '13800000006', '', 'disabled', '2024-05-01 10:00:00', '2024-04-01 09:00:00');

-- Roles
INSERT INTO sys_role (id, name, code, description, built_in, user_count, created_at) VALUES
('role-1', '超级管理员', 'R_SUPER', '拥有所有权限', 1, 1, '2024-01-01 00:00:00'),
('role-2', '管理员', 'R_ADMIN', '管理数据源、仪表盘、告警等', 1, 2, '2024-01-01 00:00:00'),
('role-3', '普通用户', 'R_USER', '查看仪表盘、查询指标、查看告警', 1, 2, '2024-01-01 00:00:00'),
('role-4', '只读用户', 'R_VIEWER', '仅查看权限', 1, 1, '2024-01-01 00:00:00');

-- User-Role mappings
INSERT INTO sys_user_role (user_id, role_id) VALUES
('user-1', 'role-1'),
('user-2', 'role-2'),
('user-3', 'role-3'),
('user-4', 'role-2'),
('user-5', 'role-3'),
('user-6', 'role-4');

-- Permissions
INSERT INTO sys_permission (id, name, code, type, parent_id, sort_order) VALUES
('perm-1', '数据源管理', 'datasource', 'menu', '', 1),
('perm-1-1', '查看数据源', 'datasource:view', 'button', 'perm-1', 1),
('perm-1-2', '编辑数据源', 'datasource:edit', 'button', 'perm-1', 2),
('perm-1-3', '删除数据源', 'datasource:delete', 'button', 'perm-1', 3),
('perm-2', '仪表盘', 'dashboard', 'menu', '', 2),
('perm-2-1', '查看仪表盘', 'dashboard:view', 'button', 'perm-2', 1),
('perm-2-2', '编辑仪表盘', 'dashboard:edit', 'button', 'perm-2', 2),
('perm-3', '查询', 'query', 'menu', '', 3),
('perm-3-1', '执行查询', 'query:execute', 'button', 'perm-3', 1),
('perm-4', '告警管理', 'alert', 'menu', '', 4),
('perm-4-1', '查看告警', 'alert:view', 'button', 'perm-4', 1),
('perm-4-2', '编辑告警', 'alert:edit', 'button', 'perm-4', 2),
('perm-5', '集群管理', 'cluster', 'menu', '', 5),
('perm-5-1', '查看集群', 'cluster:view', 'button', 'perm-5', 1),
('perm-6', '权限管理', 'permission', 'menu', '', 6),
('perm-6-1', '用户管理', 'permission:user', 'button', 'perm-6', 1),
('perm-6-2', '角色管理', 'permission:role', 'button', 'perm-6', 2),
('perm-7', '系统设置', 'settings', 'menu', '', 7),
('perm-7-1', '全局设置', 'settings:global', 'button', 'perm-7', 1),
('perm-7-2', '系统日志', 'settings:logs', 'button', 'perm-7', 2);

-- Role-Permission mappings (super admin gets all)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 'role-1', id FROM sys_permission;

-- Admin role permissions
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
('role-2', 'perm-1'), ('role-2', 'perm-1-1'), ('role-2', 'perm-1-2'), ('role-2', 'perm-1-3'),
('role-2', 'perm-2'), ('role-2', 'perm-2-1'), ('role-2', 'perm-2-2'),
('role-2', 'perm-3'), ('role-2', 'perm-3-1'),
('role-2', 'perm-4'), ('role-2', 'perm-4-1'), ('role-2', 'perm-4-2'),
('role-2', 'perm-5'), ('role-2', 'perm-5-1');

-- Normal user permissions
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
('role-3', 'perm-1'), ('role-3', 'perm-1-1'),
('role-3', 'perm-2'), ('role-3', 'perm-2-1'),
('role-3', 'perm-3'), ('role-3', 'perm-3-1'),
('role-3', 'perm-4'), ('role-3', 'perm-4-1'),
('role-3', 'perm-5'), ('role-3', 'perm-5-1');

-- Viewer permissions
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
('role-4', 'perm-1'), ('role-4', 'perm-1-1'),
('role-4', 'perm-2'), ('role-4', 'perm-2-1'),
('role-4', 'perm-5'), ('role-4', 'perm-5-1');

-- Prometheus Instances
INSERT INTO prom_instance (id, name, url, status, group_name, labels, scrape_interval, retention_time, version, created_at, updated_at) VALUES
('prom-1', 'Production Prometheus', 'http://prometheus-prod.example.com:9090', 'online', '生产环境', '{"env":"production","dc":"dc-beijing"}', '15s', '30d', '2.47.0', '2024-01-15 10:00:00', '2024-06-20 14:30:00'),
('prom-2', 'Staging Prometheus', 'http://prometheus-staging.example.com:9090', 'online', '预发环境', '{"env":"staging","dc":"dc-beijing"}', '30s', '15d', '2.47.0', '2024-02-10 09:00:00', '2024-06-18 11:00:00'),
('prom-3', 'Development Prometheus', 'http://prometheus-dev.example.com:9090', 'online', '开发环境', '{"env":"development","dc":"dc-shanghai"}', '30s', '7d', '2.45.0', '2024-03-01 08:00:00', '2024-06-15 16:00:00'),
('prom-4', 'Edge Prometheus', 'http://prometheus-edge.example.com:9090', 'offline', '边缘节点', '{"env":"edge","dc":"dc-guangzhou"}', '60s', '3d', '2.44.0', '2024-04-05 12:00:00', '2024-06-10 09:00:00'),
('prom-5', 'K8s Cluster Prometheus', 'http://prometheus-k8s.example.com:9090', 'online', '生产环境', '{"env":"production","dc":"dc-beijing","cluster":"k8s-main"}', '15s', '30d', '2.47.0', '2024-01-20 10:00:00', '2024-06-22 08:00:00');

-- Exporters
INSERT INTO prom_exporter (id, type, name, host, port, `interval`, metrics_path, status, instance_id, labels, created_at) VALUES
('exp-1', 'node', 'Node Exporter - Web Server 01', '10.0.1.10', 9100, '15s', '/metrics', 'running', 'prom-1', '{"role":"web"}', '2024-01-15 10:00:00'),
('exp-2', 'node', 'Node Exporter - Web Server 02', '10.0.1.11', 9100, '15s', '/metrics', 'running', 'prom-1', '{"role":"web"}', '2024-01-15 10:00:00'),
('exp-3', 'node', 'Node Exporter - DB Server 01', '10.0.2.10', 9100, '15s', '/metrics', 'running', 'prom-1', '{"role":"database"}', '2024-01-16 09:00:00'),
('exp-4', 'mysqld', 'MySQL Exporter - Master', '10.0.2.10', 9104, '15s', '/metrics', 'running', 'prom-1', '{"role":"mysql-master"}', '2024-01-16 09:30:00'),
('exp-5', 'redis', 'Redis Exporter - Cache', '10.0.3.10', 9121, '15s', '/metrics', 'running', 'prom-1', '{"role":"cache"}', '2024-01-17 10:00:00'),
('exp-6', 'nginx', 'Nginx Exporter - LB', '10.0.0.10', 9113, '15s', '/metrics', 'running', 'prom-1', '{"role":"lb"}', '2024-01-17 11:00:00'),
('exp-7', 'blackbox', 'Blackbox Exporter', '10.0.1.100', 9115, '30s', '/probe', 'running', 'prom-1', '{"role":"probe"}', '2024-01-18 08:00:00'),
('exp-8', 'cadvisor', 'cAdvisor - K8s Node 01', '10.0.5.10', 8080, '15s', '/metrics', 'running', 'prom-5', '{"role":"k8s-node"}', '2024-01-20 10:00:00'),
('exp-9', 'cadvisor', 'cAdvisor - K8s Node 02', '10.0.5.11', 8080, '15s', '/metrics', 'stopped', 'prom-5', '{"role":"k8s-node"}', '2024-01-20 10:00:00'),
('exp-10', 'process', 'Process Exporter - App Server', '10.0.1.10', 9256, '15s', '/metrics', 'error', 'prom-1', '{"role":"app"}', '2024-02-01 14:00:00');

-- Metric Metadata
INSERT INTO prom_metric_meta (id, name, type, help, labels, exporter, unit, favorite) VALUES
('mm-1', 'node_cpu_seconds_total', 'counter', 'Seconds the CPUs spent in each mode', '["cpu","mode","instance"]', 'node', 'seconds', 1),
('mm-2', 'node_memory_MemTotal_bytes', 'gauge', 'Memory information field MemTotal_bytes', '["instance"]', 'node', 'bytes', 1),
('mm-3', 'node_memory_MemAvailable_bytes', 'gauge', 'Memory information field MemAvailable_bytes', '["instance"]', 'node', 'bytes', 1),
('mm-4', 'node_filesystem_size_bytes', 'gauge', 'Filesystem size in bytes', '["device","fstype","mountpoint","instance"]', 'node', 'bytes', 0),
('mm-5', 'node_filesystem_avail_bytes', 'gauge', 'Filesystem space available in bytes', '["device","fstype","mountpoint","instance"]', 'node', 'bytes', 0),
('mm-6', 'node_network_receive_bytes_total', 'counter', 'Network device statistic receive_bytes', '["device","instance"]', 'node', 'bytes', 0),
('mm-7', 'node_network_transmit_bytes_total', 'counter', 'Network device statistic transmit_bytes', '["device","instance"]', 'node', 'bytes', 0),
('mm-8', 'node_load1', 'gauge', '1m load average', '["instance"]', 'node', '', 0),
('mm-9', 'node_load5', 'gauge', '5m load average', '["instance"]', 'node', '', 0),
('mm-10', 'node_load15', 'gauge', '15m load average', '["instance"]', 'node', '', 0),
('mm-11', 'up', 'gauge', 'Target up', '["instance","job"]', 'prometheus', '', 1),
('mm-12', 'http_requests_total', 'counter', 'Total number of HTTP requests', '["method","handler","code","instance"]', 'custom', '', 1),
('mm-13', 'http_request_duration_seconds', 'histogram', 'HTTP request latency in seconds', '["method","handler","le","instance"]', 'custom', 'seconds', 1),
('mm-14', 'container_cpu_usage_seconds_total', 'counter', 'Cumulative cpu time consumed', '["container","pod","namespace","instance"]', 'cadvisor', 'seconds', 0),
('mm-15', 'container_memory_usage_bytes', 'gauge', 'Current memory usage in bytes', '["container","pod","namespace","instance"]', 'cadvisor', 'bytes', 0),
('mm-16', 'container_spec_memory_limit_bytes', 'gauge', 'Memory limit for the container', '["container","pod","namespace","instance"]', 'cadvisor', 'bytes', 0),
('mm-17', 'mysql_global_status_threads_connected', 'gauge', 'Current number of open connections', '["instance"]', 'mysqld', '', 0),
('mm-18', 'mysql_global_status_queries', 'counter', 'Total number of queries executed', '["instance"]', 'mysqld', '', 0),
('mm-19', 'redis_connected_clients', 'gauge', 'Number of client connections', '["instance"]', 'redis', '', 0),
('mm-20', 'redis_memory_used_bytes', 'gauge', 'Total number of bytes allocated by Redis', '["instance"]', 'redis', 'bytes', 0),
('mm-21', 'nginx_http_requests_total', 'counter', 'Total number of HTTP requests', '["instance"]', 'nginx', '', 0),
('mm-22', 'nginx_connections_active', 'gauge', 'Active client connections', '["instance"]', 'nginx', '', 0),
('mm-23', 'probe_success', 'gauge', 'Displays whether or not the probe was a success', '["instance","job"]', 'blackbox', '', 0),
('mm-24', 'probe_duration_seconds', 'gauge', 'Returns how long the probe took to complete in seconds', '["instance","job"]', 'blackbox', 'seconds', 0);

-- Dashboards
INSERT INTO prom_dashboard (id, name, description, panels, time_range, refresh_interval, tags, folder_id, created_by, favorite, created_at, updated_at) VALUES
('dash-1', '主机概览', '所有主机的CPU、内存、磁盘、网络概览',
 '[{"id":"p1","title":"CPU 使用率","type":"line","promql":"100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\\"idle\\"}[5m])) * 100)","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":80,"color":"#faad14","label":"警告"},{"value":95,"color":"#f5222d","label":"严重"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p2","title":"内存使用率","type":"line","promql":"(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[{"value":85,"color":"#faad14","label":"警告"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"},{"id":"p3","title":"磁盘使用率","type":"bar","promql":"(1 - node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":90,"color":"#f5222d","label":"严重"}],"options":{"unit":"%","decimals":1,"colorMode":"thresholds","fillOpacity":80,"showLegend":false,"stackMode":"none"},"legendFormat":"{{instance}} - {{mountpoint}}"},{"id":"p4","title":"网络流量","type":"line","promql":"irate(node_network_receive_bytes_total{device=\\"eth0\\"}[5m]) * 8","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bps","decimals":0,"colorMode":"fixed","fillOpacity":10,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}} - {{device}}"}]',
 '{"from":"now-1h","to":"now","label":"最近1小时"}', 30, '["主机","基础设施"]', '', 'admin', 1, '2024-01-15 10:00:00', '2024-06-20 14:30:00'),
('dash-2', '应用性能监控', 'HTTP请求率、延迟、错误率监控',
 '[{"id":"p5","title":"HTTP QPS","type":"line","promql":"rate(http_requests_total[5m])","position":{"x":0,"y":0},"size":{"w":12,"h":4},"thresholds":[],"options":{"unit":"req/s","decimals":1,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"normal"},"legendFormat":"{{handler}} {{method}} {{code}}"},{"id":"p6","title":"P99 延迟","type":"line","promql":"histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))","position":{"x":0,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14","label":"警告"}],"options":{"unit":"s","decimals":3,"colorMode":"thresholds","fillOpacity":10,"showLegend":true,"stackMode":"none"},"legendFormat":"{{handler}}"},{"id":"p7","title":"错误率","type":"stat","promql":"rate(http_requests_total{code=~\\"5..\\"}[5m]) / rate(http_requests_total[5m]) * 100","position":{"x":6,"y":4},"size":{"w":6,"h":4},"thresholds":[{"value":1,"color":"#faad14","label":"警告"},{"value":5,"color":"#f5222d","label":"严重"}],"options":{"unit":"%","decimals":2,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""}]',
 '{"from":"now-1h","to":"now","label":"最近1小时"}', 15, '["应用","HTTP"]', '', 'admin', 1, '2024-02-01 14:00:00', '2024-06-20 10:00:00'),
('dash-3', 'K8s 集群监控', 'Kubernetes集群容器资源监控',
 '[{"id":"p8","title":"容器CPU使用率","type":"line","promql":"rate(container_cpu_usage_seconds_total[5m]) * 100","position":{"x":0,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"%","decimals":1,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}"},{"id":"p9","title":"容器内存使用","type":"line","promql":"container_memory_usage_bytes","position":{"x":6,"y":0},"size":{"w":6,"h":4},"thresholds":[],"options":{"unit":"bytes","decimals":0,"colorMode":"fixed","fillOpacity":20,"showLegend":true,"stackMode":"none"},"legendFormat":"{{pod}}"}]',
 '{"from":"now-1h","to":"now","label":"最近1小时"}', 15, '["K8s","容器"]', '', 'admin', 0, '2024-01-20 10:00:00', '2024-06-22 08:00:00'),
('dash-4', 'MySQL 监控', 'MySQL数据库性能监控',
 '[{"id":"p10","title":"连接数","type":"gauge","promql":"mysql_global_status_threads_connected","position":{"x":0,"y":0},"size":{"w":4,"h":4},"thresholds":[{"value":100,"color":"#52c41a","label":"正常"},{"value":200,"color":"#faad14","label":"警告"},{"value":300,"color":"#f5222d","label":"严重"}],"options":{"unit":"","decimals":0,"colorMode":"thresholds","fillOpacity":0,"showLegend":false,"stackMode":"none"},"legendFormat":""},{"id":"p11","title":"QPS","type":"line","promql":"rate(mysql_global_status_queries[5m])","position":{"x":4,"y":0},"size":{"w":8,"h":4},"thresholds":[],"options":{"unit":"ops","decimals":1,"colorMode":"fixed","fillOpacity":15,"showLegend":true,"stackMode":"none"},"legendFormat":"{{instance}}"}]',
 '{"from":"now-1h","to":"now","label":"最近1小时"}', 30, '["MySQL","中间件"]', '', 'admin', 0, '2024-03-01 08:00:00', '2024-06-15 16:00:00');

-- Dashboard Templates
INSERT INTO prom_dashboard_template (id, name, category, description, panels, thumbnail, tags, version) VALUES
('tmpl-1', 'Node Exporter Full', '主机监控', '全面的主机监控仪表盘，包含CPU、内存、磁盘、网络等', '[]', '', '["node","linux"]', '1.0.0'),
('tmpl-2', 'Container Monitoring', '容器监控', 'Docker/K8s容器资源监控', '[]', '', '["container","docker","k8s"]', '1.0.0'),
('tmpl-3', 'Kubernetes Cluster', '容器监控', 'Kubernetes集群全局监控', '[]', '', '["kubernetes","cluster"]', '1.0.0'),
('tmpl-4', 'Redis Dashboard', '中间件监控', 'Redis性能和内存监控', '[]', '', '["redis","cache"]', '1.0.0'),
('tmpl-5', 'MySQL Overview', '中间件监控', 'MySQL数据库监控概览', '[]', '', '["mysql","database"]', '1.0.0'),
('tmpl-6', 'Nginx Monitoring', '中间件监控', 'Nginx负载均衡监控', '[]', '', '["nginx","lb"]', '1.0.0');

-- PromQL Templates
INSERT INTO prom_promql_template (id, name, category, query, description, variables) VALUES
('tpl-1', 'CPU 使用率', '主机监控', '100 - (avg by(instance)(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)', '各实例CPU使用百分比', '["instance"]'),
('tpl-2', '内存使用率', '主机监控', '(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100', '各实例内存使用百分比', '["instance"]'),
('tpl-3', '磁盘使用率', '主机监控', '(1 - node_filesystem_avail_bytes{fstype!~"tmpfs|overlay"} / node_filesystem_size_bytes) * 100', '各实例磁盘使用百分比', '["instance","mountpoint"]'),
('tpl-4', '网络接收流量', '主机监控', 'irate(node_network_receive_bytes_total{device="eth0"}[5m]) * 8', '网络接收比特率', '["instance","device"]'),
('tpl-5', '网络发送流量', '主机监控', 'irate(node_network_transmit_bytes_total{device="eth0"}[5m]) * 8', '网络发送比特率', '["instance","device"]'),
('tpl-6', 'HTTP 请求率', '应用监控', 'rate(http_requests_total[5m])', 'HTTP请求速率(QPS)', '["handler","method","code"]'),
('tpl-7', '接口延迟 P99', '应用监控', 'histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))', 'HTTP请求99分位延迟', '["handler","method"]'),
('tpl-8', '容器CPU使用率', '容器监控', 'rate(container_cpu_usage_seconds_total[5m]) * 100', '容器CPU使用百分比', '["container","pod","namespace"]'),
('tpl-9', '容器内存使用率', '容器监控', 'container_memory_usage_bytes / container_spec_memory_limit_bytes * 100', '容器内存使用百分比', '["container","pod","namespace"]'),
('tpl-10', 'Blackbox 可用性', '拨测监控', 'sum(up{job="blackbox"}) / count(up{job="blackbox"}) * 100', 'Blackbox探针可用性百分比', '[]'),
('tpl-11', 'MySQL 连接数', '中间件监控', 'mysql_global_status_threads_connected', 'MySQL当前连接数', '["instance"]'),
('tpl-12', 'Redis 内存使用', '中间件监控', 'redis_memory_used_bytes', 'Redis内存使用字节数', '["instance"]');

-- Alert Rules
INSERT INTO prom_alert_rule (id, name, group_name, expr, duration, severity, labels, annotations, status, created_at, updated_at) VALUES
('rule-1', '主机CPU使用率过高', '主机告警', '100 - (avg by(instance)(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80', '5m', 'warning', '{"team":"infra"}', '{"summary":"{{ $labels.instance }} CPU使用率超过80%","description":"当前值: {{ $value }}%","runbook":"https://wiki.example.com/runbook/high-cpu"}', 'enabled', '2024-01-15 10:00:00', '2024-06-20 14:30:00'),
('rule-2', '主机CPU使用率严重', '主机告警', '100 - (avg by(instance)(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 95', '3m', 'critical', '{"team":"infra"}', '{"summary":"{{ $labels.instance }} CPU使用率超过95%","description":"当前值: {{ $value }}%"}', 'enabled', '2024-01-15 10:00:00', '2024-06-20 14:30:00'),
('rule-3', '内存使用率过高', '主机告警', '(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100 > 85', '5m', 'warning', '{"team":"infra"}', '{"summary":"{{ $labels.instance }} 内存使用率超过85%","description":"当前值: {{ $value }}%"}', 'enabled', '2024-01-16 09:00:00', '2024-06-18 11:00:00'),
('rule-4', '磁盘空间不足', '主机告警', '(1 - node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100 > 90', '10m', 'error', '{"team":"infra"}', '{"summary":"{{ $labels.instance }} 磁盘使用率超过90%","description":"挂载点: {{ $labels.mountpoint }}, 当前值: {{ $value }}%"}', 'enabled', '2024-01-17 10:00:00', '2024-06-15 16:00:00'),
('rule-5', '服务不可达', '服务告警', 'up == 0', '1m', 'critical', '{"team":"sre"}', '{"summary":"{{ $labels.instance }} 服务不可达","description":"Job: {{ $labels.job }}"}', 'enabled', '2024-01-18 08:00:00', '2024-06-22 08:00:00'),
('rule-6', 'HTTP错误率过高', '应用告警', 'rate(http_requests_total{code=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100 > 5', '3m', 'error', '{"team":"backend"}', '{"summary":"HTTP 5xx错误率超过5%","description":"处理器: {{ $labels.handler }}, 当前值: {{ $value }}%"}', 'enabled', '2024-02-01 14:00:00', '2024-06-20 10:00:00'),
('rule-7', '接口延迟过高', '应用告警', 'histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m])) > 1', '5m', 'warning', '{"team":"backend"}', '{"summary":"接口P99延迟超过1秒","description":"处理器: {{ $labels.handler }}, 当前值: {{ $value }}s"}', 'enabled', '2024-02-01 14:00:00', '2024-06-20 10:00:00'),
('rule-8', 'MySQL连接数过高', '中间件告警', 'mysql_global_status_threads_connected > 200', '5m', 'warning', '{"team":"dba"}', '{"summary":"MySQL连接数超过200","description":"当前值: {{ $value }}"}', 'disabled', '2024-03-01 08:00:00', '2024-06-15 16:00:00');

-- Alert History
INSERT INTO prom_alert_history (id, alert_name, severity, status, instance, value, starts_at, ends_at, duration, handled_by, handled_at, remark) VALUES
('ah-1', '主机CPU使用率过高', 'warning', 'resolved', '10.0.1.10:9100', '82.3%', '2024-06-21 09:30:00', '2024-06-21 09:45:00', '15分钟', 'admin', '2024-06-21 09:35:00', '临时任务导致CPU升高，已结束'),
('ah-2', '磁盘空间不足', 'error', 'resolved', '10.0.2.10:9100', '92.1%', '2024-06-20 16:00:00', '2024-06-20 18:30:00', '2小时30分', 'ops-zhang', '2024-06-20 16:30:00', '清理日志文件释放空间'),
('ah-3', '服务不可达', 'critical', 'resolved', '10.0.5.11:8080', '0', '2024-06-20 10:00:00', '2024-06-20 10:15:00', '15分钟', 'sre-li', '2024-06-20 10:05:00', '节点重启恢复'),
('ah-4', 'HTTP错误率过高', 'error', 'resolved', '', '8.5%', '2024-06-19 14:00:00', '2024-06-19 14:30:00', '30分钟', 'dev-wang', '2024-06-19 14:10:00', '数据库连接池耗尽，扩容后恢复'),
('ah-5', '主机CPU使用率过高', 'warning', 'firing', '10.0.2.10:9100', '87.5%', '2024-06-22 14:30:00', NULL, '持续中', '', NULL, ''),
('ah-6', '内存使用率过高', 'warning', 'resolved', '10.0.1.11:9100', '86.8%', '2024-06-18 08:00:00', '2024-06-18 09:00:00', '1小时', 'admin', '2024-06-18 08:15:00', '内存泄漏，重启应用后恢复'),
('ah-7', '接口延迟过高', 'warning', 'resolved', '', '1.5s', '2024-06-17 20:00:00', '2024-06-17 20:30:00', '30分钟', 'dev-wang', '2024-06-17 20:10:00', '慢查询导致，已优化SQL'),
('ah-8', 'MySQL连接数过高', 'warning', 'resolved', '10.0.2.10:9104', '245', '2024-06-16 11:00:00', '2024-06-16 11:30:00', '30分钟', 'dba-chen', '2024-06-16 11:10:00', '连接池配置不当，调整max_connections');

-- Silences
INSERT INTO prom_silence (id, matchers, starts_at, ends_at, created_by, comment, status) VALUES
('sil-1', '[{"name":"instance","value":"10.0.5.11:8080","isRegex":false,"isEqual":true}]', '2024-06-22 15:00:00', '2024-06-23 09:00:00', 'admin', 'cAdvisor节点维护，临时静默', 'active'),
('sil-2', '[{"name":"severity","value":"info","isRegex":false,"isEqual":true}]', '2024-06-22 00:00:00', '2024-06-22 08:00:00', 'ops-zhang', '夜间维护窗口，静默info级别告警', 'expired');

-- Notification Channels
INSERT INTO prom_notification_channel (id, type, name, config, enabled, created_at) VALUES
('nc-1', 'dingtalk', '运维告警群', '{"webhook":"https://oapi.dingtalk.com/robot/send?access_token=xxx","secret":"SEC***"}', 1, '2024-01-15 10:00:00'),
('nc-2', 'email', '告警邮件组', '{"to":"ops-team@example.com","smtp":"smtp.example.com:465"}', 1, '2024-01-15 10:00:00'),
('nc-3', 'wechat', '企微告警群', '{"webhook":"https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"}', 1, '2024-02-01 09:00:00'),
('nc-4', 'slack', 'Slack #alerts', '{"webhook":"https://hooks.slack.com/services/xxx/yyy/zzz","channel":"#alerts"}', 0, '2024-03-01 08:00:00'),
('nc-5', 'webhook', '自定义 Webhook', '{"url":"https://api.example.com/alert/callback","method":"POST"}', 1, '2024-04-01 10:00:00');

-- Clusters (nodes are auto-discovered from Prometheus via sync)
-- No seed data: clusters and nodes should be created via UI or API

-- Audit Logs
INSERT INTO sys_audit_log (id, user_id, username, action, resource, resource_id, detail, ip, user_agent, status, timestamp) VALUES
('log-1', 'user-1', 'admin', '创建', '告警规则', 'rule-1', '创建告警规则: 主机CPU使用率过高', '192.168.1.100', 'Chrome/125.0', 'success', '2024-06-22 14:30:00'),
('log-2', 'user-2', 'ops-zhang', '修改', '静默规则', 'sil-1', '创建静默规则: cAdvisor节点维护', '192.168.1.101', 'Chrome/125.0', 'success', '2024-06-22 15:00:00'),
('log-3', 'user-3', 'dev-wang', '查询', '指标查询', '', '执行PromQL查询: rate(http_requests_total[5m])', '192.168.1.102', 'Firefox/126.0', 'success', '2024-06-22 11:30:00'),
('log-4', 'user-4', 'sre-li', '处理', '告警', 'ah-3', '确认告警: 服务不可达 10.0.5.11:8080', '192.168.1.103', 'Chrome/125.0', 'success', '2024-06-20 10:05:00'),
('log-5', 'user-1', 'admin', '创建', '仪表盘', 'dash-1', '创建仪表盘: 主机概览', '192.168.1.100', 'Chrome/125.0', 'success', '2024-01-15 10:00:00'),
('log-6', 'user-6', 'viewer-liu', '登录', '系统', '', '用户登录失败: 密码错误', '192.168.1.200', 'Chrome/125.0', 'failure', '2024-06-22 09:00:00'),
('log-7', 'user-5', 'dba-chen', '修改', '告警规则', 'rule-8', '禁用告警规则: MySQL连接数过高', '192.168.1.104', 'Chrome/125.0', 'success', '2024-06-15 16:00:00'),
('log-8', 'user-1', 'admin', '删除', '用户', 'user-old', '删除用户: test-user', '192.168.1.100', 'Chrome/125.0', 'success', '2024-06-10 14:00:00');

-- Global Settings
INSERT INTO sys_global_settings (id, setting_key, setting_value, description) VALUES
('gs-1', 'scrape_interval', '15s', '默认采集间隔'),
('gs-2', 'retention_time', '15d', '默认数据保留时间'),
('gs-3', 'notification_enabled', 'true', '是否启用告警通知'),
('gs-4', 'theme', 'dark', '默认主题'),
('gs-5', 'language', 'zh-CN', '默认语言'),
('gs-6', 'max_query_duration', '30s', '最大查询超时时间'),
('gs-7', 'alert_evaluation_interval', '1m', '告警评估间隔');

-- System Logs
INSERT INTO sys_system_log (id, level, message, source, stack_trace, created_at) VALUES
('syslog-1', 'INFO', '系统启动成功', 'Application', NULL, '2024-06-22 08:00:00'),
('syslog-2', 'INFO', '数据库连接池初始化完成', 'HikariCP', NULL, '2024-06-22 08:00:01'),
('syslog-3', 'WARN', 'Prometheus实例 prom-4 连接超时', 'PrometheusProxy', NULL, '2024-06-22 08:05:00'),
('syslog-4', 'ERROR', 'cAdvisor节点 10.0.5.11 无法连接', 'ExporterMonitor', 'java.net.ConnectException: Connection refused', '2024-06-22 08:10:00'),
('syslog-5', 'INFO', '告警规则评估完成，发现3条告警', 'AlertEvaluator', NULL, '2024-06-22 14:30:00');
