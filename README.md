<h1 align="center">Prometheus Monitor - Backend</h1>

<p align="center">
  <strong>企业级 Prometheus 集群监控管理平台 — 后端服务</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk" alt="Java" />
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.5-blue" alt="MyBatis-Plus" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL" />
  <img src="https://img.shields.io/badge/JWT-Auth-000000?logo=jsonwebtokens" alt="JWT" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License" />
</p>

---

## 项目简介

本仓库是 **Prometheus 集群监控管理平台** 的后端服务，基于 Spring Boot 3 构建，提供完整的 RESTful API 支持。涵盖 Prometheus 多实例管理、PromQL 查询代理、仪表盘可视化、告警全流程、RBAC 权限控制及 SSH 远程分布式部署等核心功能。

### 核心亮点

- **模块化架构** — 11 个功能模块，职责清晰，易于扩展
- **多实例 Prometheus 管理** — 代理查询请求，远程配置推送，热加载
- **SSH 远程管控** — 通过 SSHJ 实现目标主机的远程命令执行和文件传输
- **WebSocket 实时通信** — 部署任务日志实时推送
- **五种告警通知渠道** — 钉钉、企微、邮件、Slack、Webhook
- **JWT 无状态认证** — Access Token + Refresh Token 双令牌机制
- **完善的审计日志** — 基于 AOP 注解的全量操作记录

---

## 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **框架** | Spring Boot | 3.2.5 | Web + Security + AOP + Validation + Mail + WebSocket |
| **语言** | Java | 17 | LTS 版本 |
| **ORM** | MyBatis-Plus | 3.5.5 | 自动 CRUD、分页、逻辑删除 |
| **数据库** | MySQL | 8.0+ | 主数据存储 |
| **认证** | java-jwt | 4.4.0 | HMAC256 JWT 签发/验证 |
| **API 文档** | Knife4j (OpenAPI 3.0) | 4.3.0 | 自动生成在线接口文档 |
| **SSH 客户端** | SSHJ | 0.38.0 | 远程命令执行、文件上传 |
| **加密** | Bouncy Castle | 1.77 | SSH 密钥加密支持 |
| **构建** | Maven | 3.9+ | 依赖管理、打包 |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser / 前端                          │
└────────────────────┬─────────────────┬──────────────────────────┘
                     │ HTTP/REST       │ WebSocket
                     ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐  │
│  │ Security │  │   REST   │  │ WebSocket│  │  Knife4j Doc   │  │
│  │  Filter  │  │ Controllers│ │ Handler  │  │  /doc.html     │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────────────────┘  │
│       │              │              │                             │
│  ┌────▼──────────────▼──────────────▼────────────────────────┐  │
│  │                   Service Layer                            │  │
│  │  Auth │ Datasource │ Dashboard │ Query │ Alert │ Cluster  │  │
│  │  Permission │ Workspace │ Distribute │ Panel │ Settings   │  │
│  └────┬──────────────┬──────────────┬────────────────────────┘  │
│       │              │              │                             │
│  ┌────▼────┐    ┌────▼────┐   ┌────▼──────┐                    │
│  │ MySQL   │    │Prometheus│   │ SSH/SSHJ  │                    │
│  │ (MyBatis│    │  HTTP    │   │ (Remote   │                    │
│  │  -Plus) │    │  Client  │   │  Control) │                    │
│  └─────────┘    └─────────┘   └───────────┘                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 功能模块（11 个）

### 1. 认证模块 (`module/auth`)

- JWT 双令牌：Access Token（2h）+ Refresh Token（7d）
- BCrypt 密码加密
- Spring Security 集成
- 用户信息管理、密码修改

### 2. 数据源管理 (`module/datasource`)

- **Prometheus 实例管理** — 多实例 CRUD、连接测试、远程配置读写、热加载（`/-/reload`）
- **Exporter 管理** — 13+ 类型支持（node/mysql/redis/nginx/cadvisor/blackbox/process 等）
- **远程控制** — SSH 启停、状态检查、配置编辑、操作日志
- **批量导入** — 从 Prometheus scrape 配置自动解析并导入 Exporter
- **指标元数据** — 元数据管理、搜索、收藏

### 3. 仪表盘管理 (`module/dashboard`)

- 自定义仪表盘 CRUD
- 15+ 预置模板（Node/MySQL/Redis/Nginx/K8s/Kafka/ES 等）
- 模板导入/导出
- 模板分类树形结构

### 4. PromQL 查询 (`module/query`)

- Prometheus 查询代理（instant / range query）
- 指标名称和 Label 值获取
- 查询历史持久化
- PromQL 模板库（100+ 模板，按 Exporter 分类）
- Exporter 实例列表查询

### 5. 告警管理 (`module/alert`)

- **告警规则** — 基于 PromQL 表达式，多级严重度（critical/warning/info）
- **告警历史** — 触发记录，确认/标记
- **静默管理** — 标签匹配器，时间范围静默
- **通知渠道** — 五种渠道（工厂模式）：
  - 邮件（JavaMailSender）
  - 钉钉（Webhook）
  - 企业微信（Webhook）
  - Slack（Webhook）
  - 通用 Webhook

### 6. 集群管理 (`module/cluster`)

- 集群注册与节点管理
- 拓扑映射
- 节点发现与同步
- 健康评分（从 Prometheus 指标计算）

### 7. 权限管理 (`module/permission`)

- **用户管理** — CRUD，启用/禁用
- **角色管理** — 内置角色（admin/user/viewer）
- **权限树** — 层级权限结构
- **审计日志** — AOP 注解驱动的全量操作记录

### 8. 工作空间 (`module/workspace`)

- 工作空间 CRUD
- 仪表盘发布到工作空间
- 排序管理

### 9. 分布式部署 (`module/distribute`)

- **主机管理** — SSH 凭据管理、连接测试、OS/架构检测
- **软件包管理** — 本地扫描、GitHub Releases 下载、离线上传
- **部署任务** — 异步执行、WebSocket 日志实时推送、任务重试/取消
- **组件配置生成** — 自动生成 CLI 参数、配置文件、systemd 服务文件
- 支持 8 种组件：Prometheus/node_exporter/blackbox_exporter/process_exporter/mysql_exporter/redis_exporter/nginx_exporter/cadvisor

### 10. 面板模板 (`module/panel`)

- 预置面板模板（15+ 分类）
- 模板分类树形结构
- 按 Exporter 类型过滤

### 11. 系统设置 (`module/settings`)

- 全局配置（键值对存储）
- 系统日志管理、导出、清理

---

## 数据库设计

共 **30 张数据表**，涵盖所有业务模块：

```
认证与权限（5 表）
├── sys_user                    # 用户表
├── sys_role                    # 角色表
├── sys_user_role               # 用户-角色关联
├── sys_permission              # 权限表（树形结构）
└── sys_role_permission         # 角色-权限关联

数据源管理（4 表）
├── prom_instance               # Prometheus 实例
├── prom_exporter               # Exporter 配置
├── prom_exporter_operation_log # Exporter 操作日志
└── prom_metric_meta            # 指标元数据

仪表盘与可视化（3 表）
├── prom_dashboard              # 自定义仪表盘
├── prom_dashboard_template     # 仪表盘模板
└── prom_panel_template         # 面板模板

查询与模板（2 表）
├── prom_query_history          # 查询历史
└── prom_promql_template        # PromQL 模板

告警管理（4 表）
├── prom_alert_rule             # 告警规则
├── prom_alert_history          # 告警历史
├── prom_silence                # 静默规则
└── prom_notification_channel   # 通知渠道

集群管理（2 表）
├── prom_cluster                # 集群定义
└── prom_cluster_node           # 集群节点

工作空间（2 表）
├── prom_workspace              # 工作空间
└── prom_workspace_dashboard    # 工作空间-仪表盘关联

系统管理（3 表）
├── sys_audit_log               # 审计日志
├── sys_global_settings         # 全局设置
└── sys_system_log              # 系统日志

分布式部署（4 表）
├── prom_distribute_machine     # 目标主机
├── prom_distribute_task        # 部署任务
├── prom_distribute_task_detail # 任务执行明细
└── prom_distribute_software    # 软件包管理
```

---

## API 接口概览

所有 API 以 `/api` 为前缀，认证方式为 `Bearer Token`（JWT）。

| 模块 | 前缀 | 端点数 |
|------|------|--------|
| 认证 | `/api/auth` | 4 |
| 实例管理 | `/api/prometheus/instances` | 8 |
| Exporter 管理 | `/api/prometheus/exporters` | 12 |
| 指标元数据 | `/api/prometheus/metrics` | 3 |
| 仪表盘 | `/api/prometheus/dashboards` | 5 |
| 仪表盘模板 | `/api/prometheus/dashboard/templates` | 4 |
| PromQL 查询 | `/api/prometheus/query` | 8 |
| 告警规则 | `/api/prometheus/alert/rules` | 5 |
| 告警历史 | `/api/prometheus/alert/history` | 2 |
| 静默管理 | `/api/prometheus/alert/silences` | 3 |
| 通知渠道 | `/api/prometheus/alert/notifications` | 5 |
| 集群管理 | `/api/prometheus/clusters` | 10 |
| 权限管理 | `/api/prometheus/users,roles,permissions` | 12 |
| 审计日志 | `/api/prometheus/audit-logs` | 1 |
| 工作空间 | `/api/prometheus/workspaces` | 6 |
| 分布式部署 | `/api/prometheus/distribute` | 15 |
| 面板模板 | `/api/prometheus/panel-templates` | 3 |
| 系统设置 | `/api/prometheus/settings` | 5 |

> 启动后访问 `http://localhost:8080/doc.html` 查看完整 Knife4j 在线接口文档。

---

## 项目结构

```
src/main/java/com/wenmin/prometheus/
├── PrometheusMonitorApplication.java  # 主启动类
├── annotation/                        # 自定义注解
│   └── AuditLog.java                 # 审计日志注解
├── aspect/                            # AOP 切面
│   └── AuditLogAspect.java           # 审计日志切面
├── common/                            # 通用模块
│   ├── result/                       # 统一响应（R<T>, ResultCode）
│   ├── exception/                    # 全局异常处理
│   └── page/                         # 分页工具（PageQuery, PageResult）
├── config/                            # 配置类（8 个）
│   ├── SecurityConfig.java           # Spring Security + JWT 过滤器
│   ├── WebSocketConfig.java          # WebSocket 配置
│   ├── MybatisPlusConfig.java        # MyBatis-Plus 配置
│   ├── SwaggerConfig.java            # Knife4j API 文档配置
│   └── ...                           # Jackson, RestTemplate, Async, WebMvc
├── security/                          # 安全模块
│   ├── JwtTokenProvider.java         # JWT 签发/验证
│   ├── JwtAuthenticationFilter.java  # JWT 过滤器
│   └── SecurityUser.java             # 认证用户详情
├── util/                              # 工具类
│   ├── EncryptionUtil.java           # AES256 加密
│   ├── IpUtil.java                   # IP 解析
│   └── UUIDUtil.java                 # UUID 生成
├── websocket/                         # WebSocket
│   ├── DistributeWebSocketHandler.java  # 部署日志实时推送
│   └── WebSocketAuthInterceptor.java    # WebSocket JWT 认证
└── module/                            # 业务模块（11 个）
    ├── auth/                         # 认证
    ├── datasource/                   # 数据源
    ├── dashboard/                    # 仪表盘
    ├── query/                        # 查询
    ├── alert/                        # 告警
    ├── cluster/                      # 集群
    ├── permission/                   # 权限
    ├── workspace/                    # 工作空间
    ├── distribute/                   # 分布式部署
    ├── panel/                        # 面板模板
    └── settings/                     # 系统设置

src/main/resources/
├── application.yml                    # 基础配置
├── application-dev.yml                # 开发环境配置
├── application-prod.yml               # 生产环境配置
├── logback-spring.xml                 # 日志配置
└── db/
    ├── schema.sql                     # 数据库建表脚本（30 表）
    └── migration/                     # 数据迁移脚本（模板数据）
```

---

## 快速开始

### 环境要求

| 软件 | 版本 |
|------|------|
| JDK | >= 17 |
| Maven | >= 3.9 |
| MySQL | >= 8.0 |
| Prometheus | >= 2.x（可选，用于实际监控） |

### 1. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE prometheus_monitor DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;"

# 导入建表脚本
mysql -u root -p prometheus_monitor < src/main/resources/db/schema.sql

# 导入 PromQL 模板数据（可选）
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/migration_promql_templates.sql
```

### 2. 修改配置

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/prometheus_monitor
    username: root
    password: your_password
```

### 3. 启动服务

```bash
# Maven 编译并运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或打包后运行
mvn clean package -DskipTests
java -jar target/prometheus-monitor-1.0.0.jar --spring.profiles.active=dev
```

### 4. 验证

- API 服务：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html
- 默认账号：`admin` / `admin123`

---

## 统一响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": { }
}
```

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

---

## 部署

### Docker 部署（推荐）

```bash
# 构建镜像
docker build -t prometheus-monitor-backend:latest .

# 运行容器
docker run -d \
  --name prometheus-monitor-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/prometheus_monitor \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  prometheus-monitor-backend:latest
```

### 传统部署

```bash
# 打包
mvn clean package -DskipTests

# 后台运行
nohup java -jar target/prometheus-monitor-1.0.0.jar \
  --spring.profiles.active=prod \
  > /var/log/prometheus-monitor.log 2>&1 &
```

---

## 相关仓库

- [prometheus_monitor](https://github.com/alanshaka-real/prometheus_monitor) — 前端工程（Vue 3 + TypeScript + Element Plus）

---

## License

[MIT License](./LICENSE)
