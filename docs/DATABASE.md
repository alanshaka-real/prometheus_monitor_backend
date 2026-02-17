# 数据库文档

> Prometheus 集群监控管理平台 - 数据库设计说明

## 目录

- [概述](#概述)
- [表清单](#表清单)
- [表结构详细说明](#表结构详细说明)
  - [1. 认证与权限 (5 张表)](#1-认证与权限)
  - [2. 数据源管理 (4 张表)](#2-数据源管理)
  - [3. 仪表盘 (5 张表)](#3-仪表盘)
  - [4. 查询 (2 张表)](#4-查询)
  - [5. 告警 (4 张表)](#5-告警)
  - [6. 集群 (2 张表)](#6-集群)
  - [7. 系统 (3 张表)](#7-系统)
  - [8. 分布式部署 (4 张表)](#8-分布式部署)
- [ER 关系](#er-关系)
- [索引策略](#索引策略)
- [数据库迁移](#数据库迁移)
- [约定与规范](#约定与规范)

---

## 概述

| 项目 | 值 |
|------|-----|
| 数据库引擎 | MySQL 8.0+ |
| 字符集 | `utf8mb4` / `utf8mb4_unicode_ci` |
| 存储引擎 | InnoDB |
| 表总数 | 29 |
| 主键策略 | UUID (`VARCHAR(36)`)，部分关联表使用 `BIGINT AUTO_INCREMENT` |
| 逻辑删除 | `deleted INT DEFAULT 0`（0=未删除, 1=已删除） |
| 时间字段 | `DATETIME`，默认 `CURRENT_TIMESTAMP` |

### Schema 文件位置

| 文件 | 说明 |
|------|------|
| `src/main/resources/db/schema.sql` | 主 Schema（建表 + 种子数据） |
| `src/main/resources/db/migration/V7_*` | 面板模板种子数据 (200+ 条) |
| `src/main/resources/db/migration/V8_*` | 仪表盘模板种子数据 |
| `src/main/resources/db/migration/V9__add_performance_indexes.sql` | 性能索引 |
| `src/main/resources/db/migration/V10__add_optimistic_locking.sql` | 乐观锁字段 |
| `src/main/resources/db/migration/V11__add_foreign_keys.sql` | 外键约束 |

---

## 表清单

| # | 表名 | 分组 | 说明 | 行数级别 |
|---|------|------|------|---------|
| 1 | `sys_user` | 认证与权限 | 系统用户 | 十 ~ 百 |
| 2 | `sys_role` | 认证与权限 | 系统角色 | 个位数 |
| 3 | `sys_user_role` | 认证与权限 | 用户-角色关联 | 十 ~ 百 |
| 4 | `sys_permission` | 认证与权限 | 权限定义 | 十 ~ 百 |
| 5 | `sys_role_permission` | 认证与权限 | 角色-权限关联 | 百级 |
| 6 | `prom_instance` | 数据源 | Prometheus 实例 | 十级 |
| 7 | `prom_exporter` | 数据源 | Exporter 采集器 | 百 ~ 千 |
| 8 | `prom_exporter_operation_log` | 数据源 | Exporter 操作日志 | 千 ~ 万 |
| 9 | `prom_metric_meta` | 数据源 | 指标元数据 | 百级 (119+) |
| 10 | `prom_dashboard` | 仪表盘 | 用户仪表盘 | 十 ~ 百 |
| 11 | `prom_dashboard_template` | 仪表盘 | 仪表盘模板 | 十级 |
| 12 | `prom_panel_template` | 仪表盘 | 面板模板 | 百级 (200+) |
| 13 | `prom_workspace` | 仪表盘 | 工作空间/监控项目 | 十级 |
| 14 | `prom_workspace_dashboard` | 仪表盘 | 工作空间-仪表盘关联 | 十 ~ 百 |
| 15 | `prom_query_history` | 查询 | PromQL 查询历史 | 千 ~ 万 |
| 16 | `prom_promql_template` | 查询 | PromQL 模板库 | 百级 (100+) |
| 17 | `prom_alert_rule` | 告警 | 告警规则 | 十 ~ 百 |
| 18 | `prom_alert_history` | 告警 | 告警历史记录 | 千 ~ 万 |
| 19 | `prom_silence` | 告警 | 静默规则 | 十级 |
| 20 | `prom_notification_channel` | 告警 | 通知渠道 | 个位数 |
| 21 | `prom_cluster` | 集群 | 集群定义 | 个位数 |
| 22 | `prom_cluster_node` | 集群 | 集群节点 | 十 ~ 百 |
| 23 | `sys_audit_log` | 系统 | 审计日志 | 万级 |
| 24 | `sys_global_settings` | 系统 | 全局配置 | 个位数 |
| 25 | `sys_system_log` | 系统 | 系统运行日志 | 千 ~ 万 |
| 26 | `prom_distribute_machine` | 部署 | 目标主机 | 十 ~ 百 |
| 27 | `prom_distribute_task` | 部署 | 部署任务 | 百级 |
| 28 | `prom_distribute_task_detail` | 部署 | 任务明细 (per-machine) | 千级 |
| 29 | `prom_distribute_software` | 部署 | 软件包注册 | 十级 |

---

## 表结构详细说明

### 1. 认证与权限

#### sys_user (系统用户)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `username` | VARCHAR(50) | NO | - | 登录用户名 (UNIQUE) |
| `password` | VARCHAR(100) | NO | - | BCrypt 哈希密码 |
| `email` | VARCHAR(100) | YES | NULL | 邮箱 |
| `phone` | VARCHAR(20) | YES | NULL | 手机号 |
| `avatar` | VARCHAR(255) | YES | `''` | 头像 URL |
| `real_name` | VARCHAR(100) | YES | NULL | 真实姓名 |
| `nick_name` | VARCHAR(100) | YES | NULL | 昵称 |
| `gender` | VARCHAR(10) | YES | NULL | 性别 |
| `address` | VARCHAR(500) | YES | NULL | 地址 |
| `description` | TEXT | YES | NULL | 个人简介 |
| `status` | VARCHAR(20) | YES | `'active'` | 状态: active/disabled |
| `last_login` | DATETIME | YES | NULL | 最后登录时间 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_username(username)`, `idx_status(status)`

#### sys_role (系统角色)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(50) | NO | - | 角色名称 |
| `code` | VARCHAR(50) | NO | - | 角色编码 (UNIQUE) |
| `description` | VARCHAR(200) | YES | NULL | 角色描述 |
| `built_in` | TINYINT | YES | `0` | 是否内置角色 |
| `user_count` | INT | YES | `0` | 关联用户数量 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_code(code)`

**内置角色**：`R_SUPER`(超级管理员), `R_ADMIN`(管理员), `R_USER`(普通用户), `R_VIEWER`(只读用户)

#### sys_user_role (用户-角色关联)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键 |
| `user_id` | VARCHAR(36) | NO | - | 用户 ID (FK -> sys_user) |
| `role_id` | VARCHAR(36) | NO | - | 角色 ID (FK -> sys_role) |

**索引**：`idx_user_id(user_id)`, `idx_role_id(role_id)`, `uk_user_role(user_id, role_id)` UNIQUE

**外键**：`fk_user_role_user_id` -> `sys_user(id)` ON DELETE CASCADE, `fk_user_role_role_id` -> `sys_role(id)` ON DELETE CASCADE

#### sys_permission (权限定义)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(50) | NO | - | 权限名称 |
| `code` | VARCHAR(100) | NO | - | 权限编码 (UNIQUE)，如 `datasource:view` |
| `type` | VARCHAR(20) | NO | - | 类型: menu/button/api |
| `parent_id` | VARCHAR(36) | YES | `''` | 父权限 ID（树形结构） |
| `sort_order` | INT | YES | `0` | 排序 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_parent_id(parent_id)`, `idx_code(code)`

#### sys_role_permission (角色-权限关联)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | BIGINT | NO | AUTO_INCREMENT | 主键 |
| `role_id` | VARCHAR(36) | NO | - | 角色 ID (FK -> sys_role) |
| `permission_id` | VARCHAR(36) | NO | - | 权限 ID (FK -> sys_permission) |

**索引**：`idx_role_id(role_id)`, `idx_permission_id(permission_id)`, `uk_role_perm(role_id, permission_id)` UNIQUE

**外键**：`fk_role_perm_role_id` -> `sys_role(id)` ON DELETE CASCADE, `fk_role_perm_perm_id` -> `sys_permission(id)` ON DELETE CASCADE

---

### 2. 数据源管理

#### prom_instance (Prometheus 实例)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 实例名称 |
| `url` | VARCHAR(255) | NO | - | Prometheus URL |
| `status` | VARCHAR(20) | YES | `'unknown'` | 状态: online/offline/unknown |
| `group_name` | VARCHAR(50) | YES | `''` | 分组名（如 "生产环境"） |
| `labels` | JSON | YES | NULL | 标签键值对 |
| `scrape_interval` | VARCHAR(10) | YES | `'15s'` | 采集间隔 |
| `retention_time` | VARCHAR(10) | YES | `'15d'` | 数据保留时间 |
| `version` | VARCHAR(20) | YES | `''` | Prometheus 版本 |
| `machine_id` | VARCHAR(36) | YES | NULL | 关联的分发机器 ID |
| `config_json` | JSON | YES | NULL | Prometheus 配置 (PrometheusConfigDTO) |
| `lifecycle_enabled` | TINYINT | YES | `0` | 是否启用 --web.enable-lifecycle |
| `opt_version` | INT | YES | `0` | 乐观锁版本号 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_status(status)`, `idx_group(group_name)`, `idx_machine_id(machine_id)`

#### prom_exporter (Exporter 采集器)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `type` | VARCHAR(20) | NO | - | 类型: node/blackbox/process/cadvisor/mysqld/redis/nginx/custom |
| `name` | VARCHAR(100) | NO | - | Exporter 名称 |
| `host` | VARCHAR(100) | NO | - | 主机地址 |
| `port` | INT | NO | - | 端口 |
| `interval` | VARCHAR(10) | YES | `'15s'` | 采集间隔 |
| `metrics_path` | VARCHAR(100) | YES | `'/metrics'` | 指标路径 |
| `status` | VARCHAR(20) | YES | `'stopped'` | 状态: running/stopped/error |
| `instance_id` | VARCHAR(36) | YES | NULL | 所属 Prometheus 实例 ID |
| `labels` | JSON | YES | NULL | 标签 |
| `machine_id` | VARCHAR(36) | YES | NULL | 关联分发机器 ID |
| `service_name` | VARCHAR(128) | YES | NULL | systemd 服务名 |
| `install_dir` | VARCHAR(512) | YES | NULL | 安装目录 |
| `binary_path` | VARCHAR(512) | YES | NULL | 二进制文件路径 |
| `cli_flags` | TEXT | YES | NULL | 启动命令行参数 |
| `pid` | INT | YES | NULL | 进程 PID |
| `last_status_check` | DATETIME | YES | NULL | 最近状态检查时间 |
| `version` | INT | YES | `0` | 乐观锁版本号 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_type(type)`, `idx_instance_id(instance_id)`, `idx_machine_id(machine_id)`

#### prom_exporter_operation_log (Exporter 操作日志)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `exporter_id` | VARCHAR(36) | NO | - | Exporter ID |
| `operation` | VARCHAR(20) | NO | - | 操作: start/stop/restart/config_update/status_check |
| `operator` | VARCHAR(50) | YES | NULL | 操作人 |
| `success` | TINYINT | NO | `0` | 是否成功 |
| `message` | TEXT | YES | NULL | 操作消息 |
| `detail` | TEXT | YES | NULL | SSH 命令输出详情 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_exporter_id(exporter_id)`, `idx_created_at(created_at)`

#### prom_metric_meta (指标元数据)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(200) | NO | - | 指标名称 (如 `node_cpu_seconds_total`) |
| `type` | VARCHAR(20) | NO | - | 指标类型: counter/gauge/histogram/summary |
| `help` | TEXT | YES | NULL | 指标说明 |
| `labels` | JSON | YES | NULL | 关联标签列表 |
| `exporter` | VARCHAR(50) | YES | NULL | 来源 Exporter: node/prometheus/cadvisor/mysqld/redis/nginx/blackbox/process/custom |
| `unit` | VARCHAR(20) | YES | `''` | 单位: seconds/bytes/空 |
| `favorite` | TINYINT | YES | `0` | 是否收藏 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_name(name)`, `idx_type(type)`, `idx_exporter(exporter)`

**种子数据**：预置 119 条指标元数据，覆盖 8 种 Exporter 类型。

---

### 3. 仪表盘

#### prom_dashboard (用户仪表盘)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 仪表盘名称 |
| `description` | TEXT | YES | NULL | 描述 |
| `panels` | JSON | YES | NULL | 面板配置 (含 PromQL/图表类型/布局) |
| `time_range` | JSON | YES | NULL | 时间范围 |
| `refresh_interval` | INT | YES | `30` | 自动刷新间隔 (秒) |
| `tags` | JSON | YES | NULL | 标签 |
| `folder_id` | VARCHAR(36) | YES | `''` | 文件夹 ID |
| `created_by` | VARCHAR(50) | YES | `''` | 创建者 |
| `favorite` | TINYINT | YES | `0` | 是否收藏 |
| `version` | INT | YES | `0` | 乐观锁版本号 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_name(name)`

#### prom_dashboard_template (仪表盘模板)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 模板名称 |
| `category` | VARCHAR(50) | YES | NULL | 分类 (如 "主机监控"/"容器监控") |
| `sub_category` | VARCHAR(50) | YES | `''` | 二级分类 |
| `exporter_type` | VARCHAR(50) | YES | `''` | 关联 Exporter 类型 |
| `description` | TEXT | YES | NULL | 描述 |
| `panels` | JSON | YES | NULL | 面板配置 |
| `thumbnail` | VARCHAR(255) | YES | `''` | 缩略图 URL |
| `tags` | JSON | YES | NULL | 标签 |
| `version` | VARCHAR(20) | YES | `'1.0.0'` | 模板版本 |
| `panel_count` | INT | YES | `0` | 面板数量 |
| `panel_template_ids` | JSON | YES | NULL | 引用的面板模板 ID 列表 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_category(category)`, `idx_exporter(exporter_type)`

#### prom_panel_template (面板模板)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 面板名称 |
| `description` | TEXT | YES | NULL | 描述 |
| `category` | VARCHAR(50) | NO | - | 分类 |
| `sub_category` | VARCHAR(50) | YES | `''` | 二级分类 |
| `exporter_type` | VARCHAR(50) | YES | `''` | Exporter 类型 |
| `chart_type` | VARCHAR(20) | NO | `'line'` | 图表类型: line/bar/gauge/stat/pie/table |
| `promql` | VARCHAR(2000) | NO | - | PromQL 表达式 |
| `unit` | VARCHAR(20) | YES | `''` | 单位 |
| `unit_format` | VARCHAR(20) | YES | `'none'` | 格式化策略 |
| `default_width` | INT | YES | `6` | 默认宽度 (Grid 列数) |
| `default_height` | INT | YES | `3` | 默认高度 (Grid 行数) |
| `thresholds` | JSON | YES | NULL | 阈值配置 |
| `options` | JSON | YES | NULL | 面板选项 |
| `tags` | JSON | YES | NULL | 标签 |
| `sort_order` | INT | YES | `0` | 排序 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_category(category)`, `idx_exporter(exporter_type)`, `idx_sub_category(sub_category)`

#### prom_workspace (工作空间)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 工作空间名称 |
| `description` | TEXT | YES | NULL | 描述 |
| `icon` | VARCHAR(50) | YES | `'Monitor'` | 图标名称 |
| `cover_image` | VARCHAR(255) | YES | `''` | 封面图 URL |
| `owner` | VARCHAR(50) | NO | `'admin'` | 所有者 |
| `status` | VARCHAR(20) | YES | `'active'` | 状态 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_owner(owner)`, `idx_status(status)`

#### prom_workspace_dashboard (工作空间-仪表盘关联)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `workspace_id` | VARCHAR(36) | NO | - | 工作空间 ID (FK -> prom_workspace) |
| `dashboard_id` | VARCHAR(36) | NO | - | 仪表盘 ID (FK -> prom_dashboard) |
| `sort_order` | INT | YES | `0` | 排序 |
| `published_at` | DATETIME | YES | CURRENT_TIMESTAMP | 发布时间 |
| `published_by` | VARCHAR(50) | YES | `'admin'` | 发布者 |

**索引**：`idx_workspace(workspace_id)`, `idx_dashboard(dashboard_id)`, `uk_ws_dash(workspace_id, dashboard_id)` UNIQUE

**外键**：`fk_ws_dash_workspace_id` -> `prom_workspace(id)` ON DELETE CASCADE, `fk_ws_dash_dashboard_id` -> `prom_dashboard(id)` ON DELETE CASCADE

---

### 4. 查询

#### prom_query_history (PromQL 查询历史)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `query` | TEXT | NO | - | PromQL 查询语句 |
| `executed_at` | DATETIME | YES | CURRENT_TIMESTAMP | 执行时间 |
| `duration` | DOUBLE | YES | `0` | 执行耗时 (毫秒) |
| `result_count` | INT | YES | `0` | 结果数量 |
| `favorite` | TINYINT | YES | `0` | 是否收藏 |
| `user_id` | VARCHAR(36) | YES | NULL | 执行用户 ID |

**索引**：`idx_executed_at(executed_at)`, `idx_user_id(user_id)`

#### prom_promql_template (PromQL 模板库)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 模板名称 |
| `category` | VARCHAR(50) | YES | NULL | 分类 (如 "主机监控"/"应用监控") |
| `sub_category` | VARCHAR(50) | YES | `''` | 二级分类 |
| `exporter_type` | VARCHAR(20) | YES | `''` | 关联组件类型 |
| `query` | TEXT | NO | - | PromQL 查询语句 |
| `description` | TEXT | YES | NULL | 描述 |
| `variables` | JSON | YES | NULL | 可用变量列表 |
| `unit` | VARCHAR(20) | YES | `'none'` | 语义单位: percent/bytes/seconds/count/ops/bps/none |
| `unit_format` | VARCHAR(20) | YES | `'none'` | 格式化策略: percent/bytes_iec/duration/number/bps/ops/none |
| `sort_order` | INT | YES | `0` | 排序 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_category(category)`, `idx_exporter_type(exporter_type)`, `idx_sub_category(sub_category)`

---

### 5. 告警

#### prom_alert_rule (告警规则)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 规则名称 |
| `group_name` | VARCHAR(50) | YES | `''` | 规则分组 |
| `expr` | TEXT | NO | - | PromQL 表达式 |
| `duration` | VARCHAR(10) | YES | `'5m'` | 持续触发时间 (for 子句) |
| `severity` | VARCHAR(20) | YES | `'warning'` | 严重级别: info/warning/error/critical |
| `labels` | JSON | YES | NULL | 附加标签 |
| `annotations` | JSON | YES | NULL | 注释 (summary/description/runbook) |
| `status` | VARCHAR(20) | YES | `'enabled'` | 状态: enabled/disabled |
| `version` | INT | YES | `0` | 乐观锁版本号 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_severity(severity)`, `idx_status(status)`, `idx_alert_rule_status_severity(status, severity)`

#### prom_alert_history (告警历史)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `alert_name` | VARCHAR(100) | NO | - | 告警名称 |
| `severity` | VARCHAR(20) | YES | NULL | 严重级别 |
| `status` | VARCHAR(20) | YES | NULL | 状态: firing/resolved/pending |
| `instance` | VARCHAR(100) | YES | `''` | 告警实例 |
| `value` | VARCHAR(50) | YES | `''` | 告警值 |
| `starts_at` | DATETIME | YES | NULL | 触发时间 |
| `ends_at` | DATETIME | YES | NULL | 恢复时间 |
| `duration` | VARCHAR(50) | YES | `''` | 持续时间描述 |
| `handled_by` | VARCHAR(50) | YES | `''` | 处理人 |
| `handled_at` | DATETIME | YES | NULL | 处理时间 |
| `remark` | TEXT | YES | NULL | 备注 |
| `silenced` | TINYINT(1) | YES | `0` | 是否被静默规则抑制 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_severity(severity)`, `idx_status(status)`, `idx_starts_at(starts_at)`, `idx_alert_history_severity_starts(severity, starts_at)`

#### prom_silence (静默规则)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `matchers` | JSON | YES | NULL | 匹配条件 (name/value/isRegex/isEqual) |
| `starts_at` | DATETIME | NO | - | 静默开始时间 |
| `ends_at` | DATETIME | NO | - | 静默结束时间 |
| `created_by` | VARCHAR(50) | YES | `''` | 创建者 |
| `comment` | TEXT | YES | NULL | 备注 |
| `status` | VARCHAR(20) | YES | `'active'` | 状态: active/expired/pending |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_status(status)`

#### prom_notification_channel (通知渠道)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `type` | VARCHAR(20) | NO | - | 类型: dingtalk/wechat/email/slack/webhook |
| `name` | VARCHAR(100) | NO | - | 渠道名称 |
| `config` | JSON | YES | NULL | 渠道配置 (webhook URL/SMTP/etc.) |
| `enabled` | TINYINT | YES | `1` | 是否启用 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_type(type)`

---

### 6. 集群

#### prom_cluster (集群)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(100) | NO | - | 集群名称 |
| `description` | TEXT | YES | NULL | 描述 |
| `region` | VARCHAR(50) | YES | `''` | 区域 |
| `prometheus_url` | VARCHAR(255) | YES | `''` | Prometheus URL |
| `instance_id` | VARCHAR(36) | YES | NULL | 关联 Prometheus 实例 ID |
| `status` | VARCHAR(20) | YES | `'healthy'` | 状态: healthy/warning/critical/offline |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_status(status)`, `idx_instance_id(instance_id)`

#### prom_cluster_node (集群节点)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `hostname` | VARCHAR(100) | NO | - | 主机名 |
| `ip` | VARCHAR(50) | NO | - | IP 地址 |
| `role` | VARCHAR(20) | YES | `'worker'` | 角色: master/worker/edge |
| `cpu` | JSON | YES | NULL | CPU 信息 |
| `memory` | JSON | YES | NULL | 内存信息 |
| `disk` | JSON | YES | NULL | 磁盘信息 |
| `network` | JSON | YES | NULL | 网络信息 |
| `status` | VARCHAR(20) | YES | `'online'` | 状态: online/offline/maintenance |
| `os` | VARCHAR(50) | YES | `''` | 操作系统 |
| `kernel` | VARCHAR(50) | YES | `''` | 内核版本 |
| `uptime` | VARCHAR(50) | YES | `''` | 运行时间 |
| `labels` | JSON | YES | NULL | 标签 |
| `cluster_id` | VARCHAR(36) | YES | NULL | 所属集群 ID |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_cluster_id(cluster_id)`, `idx_status(status)`

---

### 7. 系统

#### sys_audit_log (审计日志)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `user_id` | VARCHAR(36) | YES | NULL | 操作用户 ID |
| `username` | VARCHAR(50) | YES | NULL | 操作用户名 |
| `action` | VARCHAR(50) | YES | NULL | 操作类型 (创建/修改/删除/查询/登录/处理) |
| `resource` | VARCHAR(50) | YES | NULL | 资源类型 (告警规则/仪表盘/系统/etc.) |
| `resource_id` | VARCHAR(36) | YES | `''` | 资源 ID |
| `detail` | TEXT | YES | NULL | 操作详情 |
| `ip` | VARCHAR(50) | YES | NULL | 客户端 IP |
| `user_agent` | VARCHAR(255) | YES | NULL | User-Agent |
| `status` | VARCHAR(20) | YES | `'success'` | 结果: success/failure |
| `timestamp` | DATETIME | YES | CURRENT_TIMESTAMP | 操作时间 |

**索引**：`idx_user_id(user_id)`, `idx_action(action)`, `idx_timestamp(timestamp)`

#### sys_global_settings (全局配置)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `setting_key` | VARCHAR(100) | NO | - | 配置键 (UNIQUE) |
| `setting_value` | TEXT | YES | NULL | 配置值 |
| `description` | VARCHAR(200) | YES | NULL | 配置描述 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：`idx_key(setting_key)`

**预置配置**：`scrape_interval`(15s), `retention_time`(15d), `notification_enabled`(true), `theme`(dark), `language`(zh-CN), `max_query_duration`(30s), `alert_evaluation_interval`(1m)

#### sys_system_log (系统运行日志)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `level` | VARCHAR(10) | NO | - | 日志级别: INFO/WARN/ERROR/DEBUG |
| `message` | TEXT | YES | NULL | 日志内容 |
| `source` | VARCHAR(100) | YES | NULL | 来源 (类名/模块名) |
| `stack_trace` | TEXT | YES | NULL | 异常堆栈 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_level(level)`, `idx_created_at(created_at)`

---

### 8. 分布式部署

#### prom_distribute_machine (目标主机)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(128) | NO | - | 机器名称 |
| `ip` | VARCHAR(45) | NO | - | IP 地址 (支持 IPv6) |
| `ssh_port` | INT | YES | `22` | SSH 端口 |
| `ssh_username` | VARCHAR(64) | NO | - | SSH 用户名 |
| `ssh_password` | VARCHAR(512) | YES | NULL | SSH 密码 (AES-256-GCM 加密) |
| `os_type` | VARCHAR(32) | YES | NULL | 操作系统: linux/darwin |
| `os_arch` | VARCHAR(32) | YES | NULL | 架构: amd64/arm64/armv7 |
| `os_distribution` | VARCHAR(64) | YES | NULL | 发行版: Ubuntu 22.04/CentOS 7/etc. |
| `status` | VARCHAR(20) | YES | `'unknown'` | 状态: online/offline/unknown |
| `labels` | JSON | YES | NULL | 标签 |
| `last_checked_at` | DATETIME | YES | NULL | 最后检测时间 |
| `created_by` | VARCHAR(36) | YES | NULL | 创建者 ID |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_status(status)`

#### prom_distribute_task (部署任务)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(255) | NO | - | 任务名称 |
| `mode` | VARCHAR(20) | NO | - | 模式: batch_unified/batch_custom/single |
| `status` | VARCHAR(20) | YES | `'pending'` | 状态: pending/running/success/partial_fail/failed/cancelled |
| `machine_count` | INT | YES | `0` | 目标机器数量 |
| `success_count` | INT | YES | `0` | 成功数量 |
| `fail_count` | INT | YES | `0` | 失败数量 |
| `components` | JSON | NO | - | 组件列表 (如 ["node_exporter","blackbox_exporter"]) |
| `config` | JSON | YES | NULL | 全局配置 (含组件配置参数) |
| `started_at` | DATETIME | YES | NULL | 开始时间 |
| `finished_at` | DATETIME | YES | NULL | 完成时间 |
| `created_by` | VARCHAR(36) | YES | NULL | 创建者 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_status(status)`

#### prom_distribute_task_detail (任务明细)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `task_id` | VARCHAR(36) | NO | - | 任务 ID |
| `machine_id` | VARCHAR(36) | NO | - | 机器 ID |
| `machine_ip` | VARCHAR(45) | NO | - | 机器 IP |
| `components` | JSON | NO | - | 组件列表 |
| `component_config` | JSON | YES | NULL | 机器级组件自定义配置 |
| `status` | VARCHAR(20) | YES | `'pending'` | 状态 |
| `progress` | INT | YES | `0` | 进度 (0-100) |
| `current_step` | VARCHAR(128) | YES | NULL | 当前步骤描述 |
| `log_text` | MEDIUMTEXT | YES | NULL | 安装日志 |
| `error_message` | VARCHAR(1024) | YES | NULL | 错误信息 |
| `started_at` | DATETIME | YES | NULL | 开始时间 |
| `finished_at` | DATETIME | YES | NULL | 完成时间 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**索引**：`idx_task_id(task_id)`, `idx_machine_id(machine_id)`, `idx_distribute_task_detail_task_status(task_id, status)`

#### prom_distribute_software (软件包注册)

| 列名 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | VARCHAR(36) | NO | - | 主键 (UUID) |
| `name` | VARCHAR(64) | NO | - | 组件名 (如 node_exporter) |
| `display_name` | VARCHAR(128) | NO | - | 显示名称 |
| `version` | VARCHAR(32) | NO | - | 版本号 |
| `os_type` | VARCHAR(32) | NO | - | 操作系统 |
| `os_arch` | VARCHAR(32) | NO | - | 架构 |
| `file_name` | VARCHAR(255) | NO | - | 文件名 |
| `file_size` | BIGINT | YES | `0` | 文件大小 (bytes) |
| `file_hash` | VARCHAR(128) | YES | NULL | SHA256 哈希 |
| `default_port` | INT | YES | NULL | 默认端口 |
| `install_script` | TEXT | YES | NULL | 安装脚本模板 |
| `description` | VARCHAR(512) | YES | NULL | 描述 |
| `created_at` | DATETIME | YES | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | YES | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `deleted` | INT | YES | `0` | 逻辑删除 |

**唯一约束**：`uk_name_ver_os(name, version, os_type, os_arch, deleted)`

---

## ER 关系

### 核心关系总结

```
sys_user ──N:M──> sys_role ──N:M──> sys_permission
                                         |
                                    parent_id (自引用树)

prom_instance ──1:N──> prom_exporter
      |                      |
      |                 ──1:N──> prom_exporter_operation_log
      |
      +──N:1──> prom_distribute_machine <──N:1── prom_exporter
      |
      +──N:1──> prom_cluster ──1:N──> prom_cluster_node

prom_workspace ──N:M──> prom_dashboard (via prom_workspace_dashboard)

prom_alert_rule ──1:N──> prom_alert_history

prom_distribute_task ──1:N──> prom_distribute_task_detail ──N:1──> prom_distribute_machine
```

### 外键约束

| 表 | 外键 | 引用表 | 删除策略 |
|-----|------|--------|---------|
| `sys_user_role` | `user_id` | `sys_user(id)` | CASCADE |
| `sys_user_role` | `role_id` | `sys_role(id)` | CASCADE |
| `sys_role_permission` | `role_id` | `sys_role(id)` | CASCADE |
| `sys_role_permission` | `permission_id` | `sys_permission(id)` | CASCADE |
| `prom_workspace_dashboard` | `workspace_id` | `prom_workspace(id)` | CASCADE |
| `prom_workspace_dashboard` | `dashboard_id` | `prom_dashboard(id)` | CASCADE |

> 注：`prom_instance`/`prom_exporter`/`prom_cluster_node` 等表间使用逻辑外键（应用层保证引用完整性），未设置数据库外键约束，以提高灵活性和删除性能。

---

## 索引策略

### 索引设计原则

1. **主键索引**：所有表使用 `VARCHAR(36)` UUID 主键（InnoDB 聚簇索引）
2. **唯一索引**：业务唯一字段添加 UNIQUE 约束（如 `username`, `code`, `setting_key`）
3. **查询索引**：高频查询条件添加二级索引（如 `status`, `type`, `created_at`）
4. **复合索引**：多条件联合查询使用复合索引（如 `(status, severity)`, `(task_id, status)`）
5. **外键索引**：所有外键列自动创建索引

### 性能索引 (V9 迁移)

```sql
-- 告警规则: 按状态+严重级别联合查询
CREATE INDEX idx_alert_rule_status_severity ON prom_alert_rule(status, severity);

-- 任务明细: 按任务ID+状态联合查询
CREATE INDEX idx_distribute_task_detail_task_status ON prom_distribute_task_detail(task_id, status);

-- 告警历史: 按严重级别+触发时间联合查询
CREATE INDEX idx_alert_history_severity_starts ON prom_alert_history(severity, starts_at);

-- 查询历史: 按执行时间排序
CREATE INDEX idx_query_history_executed_at ON prom_query_history(executed_at);
```

### 索引统计

| 表 | 索引数量 | 说明 |
|----|---------|------|
| `sys_user` | 3 | PK + username UNIQUE + status |
| `sys_role` | 2 | PK + code UNIQUE |
| `sys_user_role` | 4 | PK + user_id + role_id + UNIQUE(user_id, role_id) |
| `sys_permission` | 3 | PK + parent_id + code UNIQUE |
| `sys_role_permission` | 4 | PK + role_id + permission_id + UNIQUE(role_id, permission_id) |
| `prom_instance` | 4 | PK + status + group_name + machine_id |
| `prom_exporter` | 4 | PK + type + instance_id + machine_id |
| `prom_alert_rule` | 4 | PK + severity + status + (status, severity) |
| `prom_alert_history` | 5 | PK + severity + status + starts_at + (severity, starts_at) |
| `prom_distribute_task_detail` | 4 | PK + task_id + machine_id + (task_id, status) |

---

## 数据库迁移

### 迁移脚本概览

| 脚本 | 版本 | 说明 |
|------|------|------|
| `schema.sql` | 初始 | 29 张表创建 + 种子数据 |
| `V7_01` ~ `V7_15` | v1.0.5 | 面板模板种子数据 (15 种 Exporter，200+ 条) |
| `V8_01` ~ `V8_04` | v1.0.5 | 仪表盘模板种子数据 |
| `V9__add_performance_indexes.sql` | v1.0.12 | 4 个性能复合索引 |
| `V10__add_optimistic_locking.sql` | v1.0.12 | 乐观锁字段 + 静默标记字段 |
| `V11__add_foreign_keys.sql` | v1.0.12 | 3 张关联表外键约束 |

### 执行迁移

```bash
# 按顺序执行（确保 schema.sql 已导入）
cd src/main/resources/db/migration

# 面板模板 (可选)
for f in V7_*.sql; do mysql -u root -p prometheus_monitor < "$f"; done

# 仪表盘模板 (可选)
for f in V8_*.sql; do mysql -u root -p prometheus_monitor < "$f"; done

# 性能索引 (推荐)
mysql -u root -p prometheus_monitor < V9__add_performance_indexes.sql

# 乐观锁 (必须 - 应用层依赖)
mysql -u root -p prometheus_monitor < V10__add_optimistic_locking.sql

# 外键约束 (推荐)
mysql -u root -p prometheus_monitor < V11__add_foreign_keys.sql
```

### 注意事项

- V10 和 V11 使用 `ALTER TABLE`，在大表上执行可能需要较长时间
- V11 在添加外键前会清理孤儿记录，请确认清理逻辑符合预期
- 迁移脚本不使用 `IF NOT EXISTS`，重复执行会报错

---

## 约定与规范

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | `snake_case`，系统表 `sys_` 前缀，业务表 `prom_` 前缀 | `sys_user`, `prom_instance` |
| 列名 | `snake_case` | `group_name`, `created_at` |
| 主键 | `id` | `VARCHAR(36)` UUID |
| 外键列 | `{关联表}_id` | `instance_id`, `cluster_id` |
| 时间列 | `created_at` / `updated_at` / `{action}_at` | `started_at`, `finished_at` |
| 布尔列 | 形容词或 `is_` 前缀 | `enabled`, `favorite`, `deleted` |
| JSON 列 | 表达复杂结构 | `labels`, `panels`, `config` |
| 索引 | `idx_{表名缩写}_{列名}` | `idx_status`, `idx_cluster_id` |
| 唯一约束 | `uk_{列名组合}` | `uk_user_role`, `uk_name_ver_os` |

### 数据类型规范

| 场景 | 推荐类型 | 说明 |
|------|----------|------|
| 主键 | `VARCHAR(36)` | UUID 字符串 |
| 自增主键 | `BIGINT AUTO_INCREMENT` | 仅关联表使用 |
| 短文本 | `VARCHAR(N)` | N 根据实际需要设定 |
| 长文本 | `TEXT` | PromQL 表达式、日志内容等 |
| 超长文本 | `MEDIUMTEXT` | 部署日志 (可达 16MB) |
| 布尔值 | `TINYINT` | 0=false, 1=true |
| 时间 | `DATETIME` | 统一使用 DATETIME |
| 结构化数据 | `JSON` | 标签、配置、面板等 |
| 枚举值 | `VARCHAR(20)` | 不使用 ENUM 类型，便于扩展 |

### JSON 字段使用规范

本项目广泛使用 MySQL JSON 类型存储半结构化数据：

| 表.列 | JSON 结构 | 用途 |
|--------|----------|------|
| `prom_instance.labels` | `{"env":"prod","dc":"bj"}` | 实例标签 |
| `prom_instance.config_json` | `{global:{},scrapeConfigs:[]}` | Prometheus 配置 |
| `prom_dashboard.panels` | `[{id,title,type,promql,...}]` | 面板定义 |
| `prom_dashboard.time_range` | `{from,to,label}` | 时间范围 |
| `prom_silence.matchers` | `[{name,value,isRegex,isEqual}]` | 匹配规则 |
| `prom_notification_channel.config` | `{webhook,secret,...}` | 渠道配置 |
| `prom_cluster_node.cpu/memory/disk` | `{cores,usage,...}` | 硬件信息 |
| `prom_distribute_task.components` | `["node_exporter",...]` | 组件列表 |
| `prom_distribute_task.config` | `{installDir,port,...}` | 部署配置 |
