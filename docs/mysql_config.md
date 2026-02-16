# 数据库配置信息

## 连接信息

| 配置项 | 值 |
|--------|-----|
| 数据库类型 | MySQL 8.0+ |
| 主机 | localhost |
| 端口 | 3306 |
| 数据库名 | prometheus_monitor |
| 用户名 | root |
| 密码 | root |

## 初始化

```bash
# 1. 创建数据库并导入表结构和种子数据
mysql -u root -proot < docs/schema.sql

# 2. 或者在 MySQL 客户端中执行
mysql> source docs/schema.sql;
```

## 种子数据账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |
| ops-zhang | admin123 | 管理员 |
| dev-wang | admin123 | 普通用户 |
| sre-li | admin123 | 管理员 |
| dba-chen | admin123 | 普通用户 |
| viewer-liu | admin123 | 只读用户（已禁用） |

## 表结构概览

共 17 张表：

### 认证与权限（5 表）
- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_user_role` - 用户角色关联
- `sys_permission` - 权限树表
- `sys_role_permission` - 角色权限关联

### 数据源管理（3 表）
- `prom_instance` - Prometheus 实例
- `prom_exporter` - Exporter 配置
- `prom_metric_meta` - 指标元数据

### 仪表盘（2 表）
- `prom_dashboard` - 仪表盘
- `prom_dashboard_template` - 仪表盘模板

### 查询（2 表）
- `prom_query_history` - 查询历史
- `prom_promql_template` - PromQL 模板

### 告警（4 表）
- `prom_alert_rule` - 告警规则
- `prom_alert_history` - 告警历史
- `prom_silence` - 静默规则
- `prom_notification_channel` - 通知渠道

### 集群（2 表）
- `prom_cluster` - 集群
- `prom_cluster_node` - 集群节点

### 系统（3 表）
- `sys_audit_log` - 审计日志
- `sys_global_settings` - 全局设置
- `sys_system_log` - 系统日志
