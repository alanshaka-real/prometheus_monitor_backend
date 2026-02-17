# Changelog

本项目所有重要变更均记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [v1.0.12] - 2026-02-17

### Added
- **告警规则评估调度器** (`AlertEvaluationScheduler`)：`@Scheduled` 每 60 秒定时查询所有启用规则，执行 PromQL 即时查询，触发告警并创建 `PromAlertHistory` 记录
- **告警收敛/降噪** (`AlertGroupManager`)：按 `groupName:severity` 分组，实现 `group_wait(30s)` / `group_interval(5min)` / `repeat_interval(4h)` 三级时间窗控制
- **PromQL 远程验证**：`AlertServiceImpl.validateOnPrometheus()` 调用 Prometheus 即时查询验证语法，不可用时降级为本地检查
- **Alertmanager Webhook 接收**：`POST /api/prometheus/alert/webhook` 接收 Alertmanager 通知，自动创建告警历史记录
- **静默规则执行**：告警评估时检查活跃静默规则，匹配则抑制通知并标记 `silenced=true`；自动过期已结束的静默规则
- **实例健康检查调度器** (`InstanceHealthScheduler`)：每 5 分钟对每个实例调用 `GET /-/healthy`，更新 status 为 `online`/`offline`
- **指标元数据同步调度器** (`MetadataScheduler`)：每 30 分钟从在线 Prometheus 实例拉取 `/api/v1/metadata`，upsert 到 `prom_metric_meta` 表
- **Spring Cache + Caffeine 缓存** (`CacheConfig`)：`@Cacheable` 应用于权限树 (5min)、仪表盘模板树 (10min)、PromQL 模板树 (10min)
- **SSH 连接池** (`SshConnectionPool`)：commons-pool2 `KeyedObjectPool`，按 `host:port:username` 分组，`maxTotal=20`、`maxPerKey=3`、`minEvictableIdle=5min`
- **Grafana 仪表盘导入**：`POST /api/prometheus/dashboards/import` 接受 Grafana JSON，自动转换面板类型和 PromQL targets
- **Grafana 仪表盘导出**：`GET /api/prometheus/dashboards/{id}/export?format=grafana` 导出 Grafana 兼容 JSON（含 `__inputs`/`__requires`/`targets`/`gridPos`）
- **集群拓扑 WebSocket**：`ClusterTopologyWebSocketHandler` 注册 `/ws/cluster-topology`，默认每 30 秒广播拓扑更新
- **用户登出端点**：`POST /api/auth/logout` 清除 httpOnly Cookie 中的 `access_token`
- **数据库迁移 V10**：`prom_dashboard`/`alert_rule`/`exporter` 添加 `version INT DEFAULT 0`；`prom_instance` 添加 `opt_version`；`prom_alert_history` 添加 `silenced` 字段
- **数据库迁移 V11**：清理孤儿记录，为 `sys_user_role`/`sys_role_permission`/`prom_workspace_dashboard` 添加 CASCADE 外键

### Changed
- **乐观锁**：`prom_dashboard`/`alert_rule`/`instance`/`exporter` 添加 `@Version` 字段 + `OptimisticLockerInnerInterceptor`
- **事务隔离级别**：`PermissionServiceImpl` 的 `updateUser`/`updateRole` 使用 `REPEATABLE_READ` 隔离级别
- **响应格式统一**：`PageResult` 添加 `@JsonProperty("list"/"pageNum"/"pageSize")` 别名，兼容前端字段命名

### Security
- **JWT httpOnly Cookie**：`AuthController.login()` 设置 `Set-Cookie: access_token=xxx; HttpOnly; Secure; SameSite=Lax; Path=/api`
- **双模式认证**：`JwtAuthenticationFilter` 同时支持 Authorization Header 和 httpOnly Cookie
- **WebSocket Token 清理**：`WebSocketAuthInterceptor` 移除 URL 参数 token 接受，仅支持消息认证

## [v1.0.11] - 2026-02-16

### Added
- **组件配置 DTO** (`ComponentConfigDTO`)：扁平 DTO，包含所有组件通用字段（`installDir`/`port`/`logLevel`）和组件特有字段
- **组件配置生成器** (`ComponentConfigGenerator`)：
  - `buildExecStart()` — 按组件类型拼接 CLI 参数（node_exporter 的 `--no-collector`、cadvisor 的 `--port=`、redis_exporter 的 `--redis.addr` 等）
  - `generateBlackboxYaml()` — 生成默认 blackbox.yml（http_2xx / http_post_2xx / tcp_connect / icmp 四个探测模块）
  - `generateMyCnf()` — MySQL `.my.cnf` 凭据文件
  - `generateSystemdService()` — 完整 systemd unit 文件（MySQL 额外含 `Environment=DATA_SOURCE_NAME`）

### Changed
- **DistributeTaskExecutor 集成**：`extractComponentConfigs()` 从 task.config 提取 `Map<String, ComponentConfigDTO>`
- `installDir`/`port` 优先从组件配置获取
- blackbox_exporter 自动写入 blackbox.yml，mysql_exporter 自动写入 `~/.my.cnf`（chmod 600）
- totalSteps 动态计算：blackbox/mysql 额外 +1 步
- 移除旧的硬编码 `generateSystemdService()` 方法

## [v1.0.10] - 2026-02-16

### Fixed
- **数据库 Schema 漂移**：`prom_promql_template` 表补齐 `sub_category`/`exporter_type`/`unit`/`unit_format`/`sort_order` 5 个字段，与 Entity 对齐
- **EmailSender 实现真实邮件发送**：从 TODO 桩代码升级为接入 `JavaMailSender`
  - 使用 `@Autowired(required = false)` 注入，未配置 SMTP 时降级为打日志
  - 已配置时使用 `MimeMessageHelper` 发送 HTML 格式邮件
  - `application.yml` 新增 Spring Mail 配置项（默认注释状态）

## [v1.0.9] - 2026-02-16

### Added
- **GitHub Releases 在线下载**：`POST /api/prometheus/distribute/software/download`，支持 8 个组件（Prometheus、Node Exporter、Blackbox Exporter、Process Exporter、MySQL Exporter、Redis Exporter、Nginx Exporter、cAdvisor）
  - 自动获取最新版本，统一命名为 `{name}-{version}.{os}-{arch}.tar.gz`
  - 下载完成后自动清理旧版本文件和数据库记录
  - 支持可选 GitHub Token 配置以提升 API 限额（60/h -> 5000/h）
- **下载进度查询**：`GET /api/prometheus/distribute/software/download/{downloadId}`，轮询获取下载状态作为 WebSocket 兜底
- **下载进度 WebSocket 推送**：通过 `/ws/distribute` 实时推送下载进度
- **离线上传**：`POST /api/prometheus/distribute/software/upload`，支持手动上传 tar.gz 文件，自动解析文件名、校验组件名、保存文件并注册

### Changed
- **文件上传限制**：`application.yml` 配置 `multipart max-file-size` / `max-request-size` 为 500MB

## [v1.0.8] - 2026-02-15

### Added
- **指标元数据扩充 (24 -> 119 条)**：`schema.sql` 新增 95 条 `INSERT IGNORE` 语句，覆盖 8 种 Exporter 类型
  - Node Exporter (+25)：磁盘 I/O、网络错误/丢包、内存细节、文件描述符、inode、系统时间
  - Prometheus (+11)：TSDB 指标、Scrape 指标、Engine 查询耗时、配置重载状态
  - cAdvisor (+9)：CPU 细分、working_set 内存、文件系统使用、容器网络
  - MySQL (+13)：线程运行数、最大连接数、慢查询、InnoDB buffer pool、主从延迟
  - Redis (+13)：命令处理 OPS、keyspace 命中率、内存上限/RSS、RDB、阻塞/拒绝/驱逐
  - Nginx (+8)：连接状态、HTTP 响应、上游延迟
  - Blackbox (+8)：HTTP 状态码/内容、SSL 证书过期、分阶段延迟、DNS、ICMP
  - Process (+8)：CPU、内存、文件描述符、进程数、线程数、读/写字节
- **收藏切换端点**：`POST /api/prometheus/metrics/metadata/{id}/favorite`，切换指标的 `favorite` 布尔状态

## [v1.0.7] - 2026-02-15

### Added
- **Exporter 批量导入**：`POST /api/prometheus/exporters/batch`，接收 `BatchCreateExporterDTO`（含 `instanceId` + `exporters` 列表），返回 `BatchCreateResultVO`（total/created/skipped/skippedTargets）
- 以 `host:port` 为唯一键 O(1) 查重，同时防止批内重复
- 默认值填充：interval 默认 `"15s"`，metricsPath 默认 `"/metrics"`，status 固定 `"stopped"`
- `@Transactional` 保证批量插入原子性

## [v1.0.6] - 2026-02-15

### Fixed
- **rawYaml 优先级 Bug**：`PrometheusYamlGenerator.generate()` 优先使用 `rawYaml` 字段导致结构化表单修改被忽略
  - 修复：`pushInstanceConfig()` 生成 YAML 前清除 `rawYaml`；保存 configJson 时始终清除 `rawYaml`
- **writeRemoteFile 不检查退出码**：SSH 写文件命令失败时被静默吞掉
  - 修复：添加退出码检查和 stderr 读取，失败时抛出异常
- **空 target 过滤**：scrapeConfigs 中的空字符串 target 在生成 YAML 前被自动过滤

## [v1.0.5] - 2026-02-15

### Added
- **Prometheus 实例配置管理**：
  - 数据库扩展：`prom_instance` 新增 `machine_id`/`config_json`/`lifecycle_enabled` 3 个字段
  - Service 层新增 5 个方法：`getInstanceConfig()`、`pushInstanceConfig()`、`uploadInstanceConfig()`、`reloadInstance()`、`linkMachine()`
  - Controller 新增 6 个端点：获取配置、推送配置、上传 YAML、热加载、关联/解绑机器
  - SSH 工具方法：复用分发模块的 SSH 连接、命令执行、密码解密模式

## [v1.0.4] - 2026-02-15

### Added
- **组件检测**：`detectComponents(SSHClient)` 检测 6 个组件（Prometheus、Node Exporter、Alertmanager、Blackbox Exporter、Pushgateway、Grafana）的安装路径、版本、运行状态、端口
- `MachineDetectVO` 新增 `machineId`/`machineName`/`machineIp` 机器标识 + `components` 组件列表

### Fixed
- **批量检测返回格式不匹配**：Controller 返回裸数组 `{data: [...]}`，前端期望 `{data: {list: [...], total: N}}`
  - 修复：包装为 `Map<String, Object>` 含 `list` 和 `total` 键
- **execCmd() join 顺序**：`cmd.join()` 移到 `readFully()` 之前

## [v1.0.3] - 2026-02-15

### Fixed
- **实例管理字段映射**：`PromInstance.groupName` 添加 `@JsonProperty("group")` 使 JSON 字段名与前端一致
- **连接测试错误信息**：`ConnectionTestVO` 新增 `message` 字段，连接失败时返回具体异常信息

## [v1.0.2] - 2026-02-15

### Fixed
- **SSH 命令执行顺序**：`execCmd()`/`execCmdChecked()` 中 `cmd.join()` 移到 `readFully()` 之前，确保命令完成后再读取输出
- **超时处理**：`execCmdChecked()` null 退出码（超时）现在抛异常而非静默通过
- **systemd 服务文件写入**：新增 `writeSudoFile()` 替代有缺陷的 `sudo -S tee` 管道写法，使用临时文件 + `sudo cp`
- **自定义超时**：`execCmdWithSudo()` 支持自定义超时参数，tar 解压使用 120 秒超时
- 解压后新增文件列表验证

## [v1.0.1] - 2026-02-15

### Fixed
- **@Async 自调用失效**：`createTask()` 在同一 Bean 内调用 `executeTaskAsync()`，Spring AOP 代理被绕过
  - 修复：拆分为独立的 `DistributeTaskExecutor` Bean
- **dataDir 路径 ~ 未解析**：systemd 不展开 `~`，Prometheus 启动失败
  - 修复：运行时解析为绝对路径
- **systemd 服务文件格式错误**：非 Prometheus 组件使用 `\\n` 字面量
  - 修复：使用正确的换行符
- **配置验证命令**：`--check` 改为 `promtool check config`
- **安装日志不显示**：`updateProgress()` 未调用 `sendLog()`，前端 WebSocket 日志面板无数据
  - 修复：`updateProgress()` 同步调用 `sendLog()` 推送日志

## [v1.0.0] - 2026-02-14

### Added
- Spring Boot 3.2.5 + Java 17 基础框架搭建
- MyBatis-Plus 3.5.5 ORM 集成，支持逻辑删除与 UUID 主键
- Spring Security + JWT (HMAC256) 认证体系，含双令牌（Access Token / Refresh Token）
- 9 个功能模块：auth、datasource、dashboard、query、alert、cluster、permission、settings、distribute
- RBAC 权限模型：4 个内置角色（超级管理员 / 管理员 / 普通用户 / 只读用户）
- 统一响应格式 `R<T>`（code/msg/data）+ 全局异常处理
- 分页工具 `PageResult<T>`，基于 MyBatis-Plus `IPage`
- WebSocket 实时通信（部署日志推送、消息通知）
- Knife4j (OpenAPI 3) API 在线文档
- CORS 跨域配置
- 17 张核心数据库表 + 种子数据
- BCrypt 密码哈希 + AES256 SSH 凭据加密
- @AuditLog 注解驱动的审计日志 (AOP)
- 通知渠道工厂模式：支持钉钉、企业微信、邮件、Slack、Webhook 5 种渠道

[v1.0.12]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.11...v1.0.12
[v1.0.11]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.10...v1.0.11
[v1.0.10]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.9...v1.0.10
[v1.0.9]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.8...v1.0.9
[v1.0.8]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.7...v1.0.8
[v1.0.7]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.6...v1.0.7
[v1.0.6]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.5...v1.0.6
[v1.0.5]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.4...v1.0.5
[v1.0.4]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.3...v1.0.4
[v1.0.3]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.2...v1.0.3
[v1.0.2]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.1...v1.0.2
[v1.0.1]: https://github.com/user/prometheus-monitor-backend/compare/v1.0.0...v1.0.1
[v1.0.0]: https://github.com/user/prometheus-monitor-backend/releases/tag/v1.0.0
