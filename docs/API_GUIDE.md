# API 使用指南

> Prometheus 集群监控管理平台 - 后端 RESTful API 完整指南

## 目录

- [概述](#概述)
- [认证流程](#认证流程)
- [统一响应格式](#统一响应格式)
- [错误码](#错误码)
- [分页规范](#分页规范)
- [限流策略](#限流策略)
- [WebSocket 协议](#websocket-协议)
- [API 端点详细说明](#api-端点详细说明)
  - [1. 认证模块](#1-认证模块-apiauth)
  - [2. 实例管理](#2-实例管理-apiprometheusinstances)
  - [3. Exporter 管理](#3-exporter-管理-apiprometheusexporters)
  - [4. PromQL 查询](#4-promql-查询-apiprometheusquery)
  - [5. 仪表盘](#5-仪表盘-apiprometheusdashboards)
  - [6. 告警管理](#6-告警管理-apiprometheusalert)
  - [7. 集群管理](#7-集群管理-apiprometheusclusters)
  - [8. 权限管理](#8-权限管理-apiprometheus)
  - [9. 分布式部署](#9-分布式部署-apiprometheusdistribute)
  - [10. 系统设置](#10-系统设置-apiprometheussettings)
  - [11. 指标元数据](#11-指标元数据-apiprometheusmetrics)
  - [12. 工作空间](#12-工作空间-apiprometheusworkspaces)
  - [13. 面板模板](#13-面板模板-apiprometheuspanels)

---

## 概述

| 项目 | 值 |
|------|-----|
| Base URL | `http://localhost:8080` |
| API 前缀 | `/api/auth/*` (认证), `/api/prometheus/*` (业务) |
| 认证方式 | Bearer Token (JWT) 或 httpOnly Cookie |
| 请求格式 | `application/json` (UTF-8) |
| 响应格式 | 统一 JSON `{code, msg, data}` |
| 在线文档 | http://localhost:8080/doc.html (Knife4j) |
| 文件上传限制 | 500 MB |

---

## 认证流程

### 1. 登录获取 Token

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "Admin",
  "password": "123456"
}
```

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

同时，响应头中包含 httpOnly Cookie：

```
Set-Cookie: access_token=eyJ...; HttpOnly; Secure; SameSite=Lax; Path=/api; Max-Age=7200
```

### 2. 携带 Token 请求 API

**方式一：Authorization Header**（推荐）

```
GET /api/prometheus/instances
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**方式二：httpOnly Cookie**（自动携带，浏览器环境）

浏览器会自动在同源请求中携带 `access_token` Cookie，无需手动处理。

### 3. 刷新 Token

当 Access Token 过期（默认 2 小时）时，使用 Refresh Token 获取新 Token：

```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 4. 登出

```
POST /api/auth/logout
```

清除 httpOnly Cookie 中的 `access_token`。

### Token 生命周期

| Token 类型 | 有效期 | 用途 |
|-----------|--------|------|
| Access Token | 2 小时 (7200s) | API 请求认证 |
| Refresh Token | 7 天 (604800s) | 刷新 Access Token |

### 免认证端点

以下端点无需携带 Token：

| 端点 | 说明 |
|------|------|
| `POST /api/auth/login` | 用户登录 |
| `POST /api/auth/refresh` | 刷新 Token |
| `POST /api/auth/logout` | 用户登出 |
| `POST /api/prometheus/alert/webhook` | Alertmanager Webhook |
| `/doc.html`, `/v3/api-docs/**` | API 文档 |
| `/ws/**` | WebSocket 端点（消息级认证） |

---

## 统一响应格式

所有 API 均返回统一的 JSON 结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": <T>
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `int` | 状态码 |
| `msg` | `string` | 消息描述 |
| `data` | `T` | 数据载荷，可为对象、数组、字符串或 null |

### 成功响应示例

**单个对象：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "prom-1",
    "name": "Production Prometheus",
    "url": "http://prometheus-prod.example.com:9090",
    "status": "online"
  }
}
```

**分页列表：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [...],
    "list": [...],
    "total": 100,
    "current": 1,
    "pageNum": 1,
    "size": 10,
    "pageSize": 10
  }
}
```

> 注：`records`/`list`、`current`/`pageNum`、`size`/`pageSize` 为同一数据的双别名，兼容不同前端库。

**无数据操作：**

```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 错误响应示例

```json
{
  "code": 400,
  "msg": "告警规则名称不能为空",
  "data": null
}
```

---

## 错误码

| 码值 | 常量 | 说明 | 触发场景 |
|------|------|------|---------|
| `200` | SUCCESS | 请求成功 | 正常响应 |
| `400` | ERROR | 请求失败 | 参数校验失败、业务逻辑错误 |
| `401` | UNAUTHORIZED | 未授权 | Token 缺失、过期或无效 |
| `403` | FORBIDDEN | 禁止访问 | 无权限访问该资源 |
| `404` | NOT_FOUND | 资源不存在 | 查询的资源 ID 不存在 |
| `429` | - | 请求过于频繁 | 触发登录限流 |
| `500` | INTERNAL_ERROR | 服务器内部错误 | 未捕获的异常 |

### 参数校验错误

当请求体校验失败时（`@Valid` 注解），返回具体的字段错误信息：

```json
{
  "code": 400,
  "msg": "username: 用户名不能为空; password: 密码长度必须在6-20之间",
  "data": null
}
```

---

## 分页规范

### 请求参数

所有分页接口统一使用以下查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `pageNum` | int | 否 | 1 | 页码（从 1 开始） |
| `pageSize` | int | 否 | 10 | 每页大小（最大 100） |

**示例**：

```
GET /api/prometheus/instances?pageNum=1&pageSize=20&keyword=prod&status=online
```

### 响应结构

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      { "id": "prom-1", "name": "Production Prometheus", ... },
      { "id": "prom-2", "name": "Staging Prometheus", ... }
    ],
    "list": [...],          // records 的别名
    "total": 5,             // 总记录数
    "current": 1,           // 当前页码
    "pageNum": 1,           // current 的别名
    "size": 20,             // 每页大小
    "pageSize": 20          // size 的别名
  }
}
```

### 非分页列表

某些接口返回完整列表（非分页），包装为 `{list, total}` 结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [...],
    "total": 15
  }
}
```

---

## 限流策略

### 登录限流

采用基于内存的滑动窗口算法，按客户端 IP 维度限流：

| 规则 | 值 |
|------|-----|
| 窗口大小 | 5 分钟 |
| 最大尝试次数 | 5 次/窗口 |
| 锁定时间 | 15 分钟 |

**超限响应**：

```json
{
  "code": 429,
  "msg": "登录尝试过于频繁，请15分钟后重试",
  "data": null
}
```

### 其他接口

当前版本未对业务接口实施速率限制。建议在生产环境通过 Nginx 或 API Gateway 实施全局限流。

---

## WebSocket 协议

### 连接端点

| 路径 | 用途 | 推送频率 |
|------|------|---------|
| `ws://localhost:8080/ws/distribute` | 部署任务日志实时推送 | 实时 |
| `ws://localhost:8080/ws/message` | 系统消息通知 | 事件驱动 |
| `ws://localhost:8080/ws/cluster-topology` | 集群拓扑更新 | 每 30 秒 |

### 认证流程

WebSocket 使用消息级认证（非 URL 参数，避免 Token 泄露到日志）：

**步骤 1**：建立 WebSocket 连接（无需 Token）

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/distribute');
```

**步骤 2**：发送认证消息

```json
{
  "type": "auth",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**步骤 3**：认证结果

成功：
```json
{
  "type": "auth_success",
  "userId": "user-1"
}
```

失败：
```json
{
  "type": "auth_failed",
  "message": "Token 无效或已过期"
}
```

### 部署日志消息格式

```json
{
  "taskId": "task-xxx",
  "detailId": "detail-xxx",
  "machineIp": "10.0.0.1",
  "status": "running",
  "message": "正在安装 node_exporter...",
  "progress": 50,
  "timestamp": "2026-02-17T10:30:00"
}
```

| 字段 | 说明 |
|------|------|
| `taskId` | 部署任务 ID |
| `detailId` | 任务明细 ID |
| `machineIp` | 目标机器 IP |
| `status` | 状态: pending/running/success/failed |
| `message` | 日志消息 |
| `progress` | 进度百分比 (0-100) |

### 集群拓扑消息格式

**订阅**：

```json
{
  "type": "subscribe"
}
```

**接收拓扑更新**：

```json
{
  "type": "topology_update",
  "data": {
    "clusters": [...],
    "nodes": [...],
    "edges": [...]
  }
}
```

---

## API 端点详细说明

### 1. 认证模块 (`/api/auth`)

#### POST /api/auth/login - 用户登录

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "Admin",
  "password": "123456"
}
```

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

#### GET /api/auth/userinfo - 获取当前用户信息

```
GET /api/auth/userinfo
Authorization: Bearer eyJ...
```

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "user-2",
    "username": "Admin",
    "email": "admin@example.com",
    "phone": "13800000002",
    "avatar": "",
    "realName": null,
    "nickName": null,
    "roles": ["R_ADMIN"],
    "permissions": ["datasource:view", "datasource:edit", ...]
  }
}
```

#### PUT /api/auth/profile - 更新个人信息

```
PUT /api/auth/profile
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "email": "newemail@example.com",
  "phone": "13900000001",
  "realName": "管理员",
  "nickName": "Admin"
}
```

#### POST /api/auth/change-password - 修改密码

```
POST /api/auth/change-password
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "oldPassword": "123456",
  "newPassword": "newpassword123"
}
```

#### POST /api/auth/refresh - 刷新 Token

```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJ..."
}
```

#### POST /api/auth/logout - 用户登出

```
POST /api/auth/logout
```

> 清除 httpOnly Cookie，无请求体和响应数据。

---

### 2. 实例管理 (`/api/prometheus/instances`)

#### GET /api/prometheus/instances - 实例列表

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `keyword` | string | 名称/URL 模糊搜索 |
| `status` | string | 状态过滤: online/offline/unknown |
| `pageNum` | int | 页码 |
| `pageSize` | int | 每页大小 |

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "prom-1",
        "name": "Production Prometheus",
        "url": "http://prometheus-prod.example.com:9090",
        "status": "online",
        "group": "生产环境",
        "labels": {"env": "production"},
        "scrapeInterval": "15s",
        "retentionTime": "30d",
        "version": "2.47.0",
        "machineId": null,
        "lifecycleEnabled": false,
        "createdAt": "2024-01-15 10:00:00"
      }
    ],
    "total": 5,
    "current": 1,
    "size": 10
  }
}
```

#### POST /api/prometheus/instances - 创建实例

```json
{
  "name": "新建 Prometheus",
  "url": "http://10.0.0.1:9090",
  "description": "生产环境监控",
  "scrapeInterval": "15s",
  "retentionTime": "15d",
  "groupName": "生产环境",
  "labels": {"env": "prod"}
}
```

#### PUT /api/prometheus/instances/{id} - 更新实例

#### DELETE /api/prometheus/instances/{id} - 删除实例

#### POST /api/prometheus/instances/{id}/test - 测试连接

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "success": true,
    "version": "2.47.0",
    "message": "连接成功"
  }
}
```

#### GET /api/prometheus/instances/{id}/config - 获取远程配置

读取 Prometheus 实例的 prometheus.yml 配置，优先从数据库缓存获取，否则通过 SSH 读取。

#### POST /api/prometheus/instances/{id}/config/push - 推送配置

将结构化配置生成 YAML 并通过 SSH 写入远程 Prometheus 实例，可选触发热加载。

```json
{
  "global": {
    "scrapeInterval": "15s",
    "evaluationInterval": "15s"
  },
  "scrapeConfigs": [
    {
      "jobName": "node",
      "staticConfigs": [
        {
          "targets": ["10.0.0.1:9100", "10.0.0.2:9100"]
        }
      ]
    }
  ],
  "reload": true
}
```

#### POST /api/prometheus/instances/{id}/reload - 热加载

触发 `POST /-/reload`（需要 Prometheus 启用 `--web.enable-lifecycle`）。

#### POST /api/prometheus/instances/{id}/link-machine - 关联分发机器

```json
{
  "machineId": "machine-1"
}
```

#### POST /api/prometheus/instances/{id}/unlink-machine - 解绑分发机器

---

### 3. Exporter 管理 (`/api/prometheus/exporters`)

#### GET /api/prometheus/exporters - Exporter 列表

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `keyword` | string | 名称/主机模糊搜索 |
| `type` | string | 类型过滤: node/blackbox/process/cadvisor/mysqld/redis/nginx |
| `status` | string | 状态过滤: running/stopped/error |
| `instanceId` | string | Prometheus 实例过滤 |
| `pageNum` | int | 页码 |
| `pageSize` | int | 每页大小 |

#### POST /api/prometheus/exporters - 创建 Exporter

```json
{
  "type": "node_exporter",
  "name": "Node Exporter - 10.0.0.1",
  "host": "10.0.0.1",
  "port": 9100,
  "interval": "15s",
  "metricsPath": "/metrics",
  "instanceId": "prom-1"
}
```

#### POST /api/prometheus/exporters/batch - 批量导入 Exporter

```json
{
  "instanceId": "prom-1",
  "exporters": [
    {"type": "node_exporter", "name": "Node-1", "host": "10.0.0.1", "port": 9100},
    {"type": "node_exporter", "name": "Node-2", "host": "10.0.0.2", "port": 9100},
    {"type": "redis_exporter", "name": "Redis-1", "host": "10.0.0.1", "port": 9121}
  ]
}
```

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 3,
    "created": 2,
    "skipped": 1,
    "skippedTargets": ["10.0.0.1:9100"]
  }
}
```

#### POST /api/prometheus/exporters/{id}/start - 启动 Exporter

通过 SSH 执行 `systemctl start {service_name}`。

#### POST /api/prometheus/exporters/{id}/stop - 停止 Exporter

#### POST /api/prometheus/exporters/{id}/restart - 重启 Exporter

#### GET /api/prometheus/exporters/{id}/status - 状态检查

---

### 4. PromQL 查询 (`/api/prometheus/query`)

#### GET /api/prometheus/query - 即时查询

| 查询参数 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| `instanceId` | string | 是 | Prometheus 实例 ID |
| `query` | string | 是 | PromQL 表达式 |
| `time` | string | 否 | 查询时间点 (RFC3339 或 Unix timestamp) |

**示例**：

```
GET /api/prometheus/query?instanceId=prom-1&query=up
```

#### GET /api/prometheus/query_range - 范围查询

| 查询参数 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| `instanceId` | string | 是 | Prometheus 实例 ID |
| `query` | string | 是 | PromQL 表达式 |
| `start` | string | 是 | 起始时间 |
| `end` | string | 是 | 结束时间 |
| `step` | string | 是 | 步长 (如 "15s", "1m", "5m") |

**示例**：

```
GET /api/prometheus/query_range?instanceId=prom-1&query=rate(node_cpu_seconds_total{mode="idle"}[5m])&start=2026-02-17T00:00:00Z&end=2026-02-17T12:00:00Z&step=60s
```

#### GET /api/prometheus/labels/__name__/values - 指标名称列表

```
GET /api/prometheus/labels/__name__/values?instanceId=prom-1
```

#### GET /api/prometheus/labels/{label}/values - Label 值列表

```
GET /api/prometheus/labels/instance/values?instanceId=prom-1
```

#### GET /api/prometheus/query/history - 查询历史列表

#### POST /api/prometheus/query/history - 保存查询历史

```json
{
  "query": "rate(http_requests_total[5m])",
  "duration": 125.6,
  "resultCount": 42
}
```

#### GET /api/prometheus/query/templates - PromQL 模板列表

#### GET /api/prometheus/query/templates/tree - PromQL 模板树

返回按 `category` -> `sub_category` 分组的树形结构。

---

### 5. 仪表盘 (`/api/prometheus/dashboards`)

#### GET /api/prometheus/dashboards - 仪表盘列表

#### POST /api/prometheus/dashboards - 创建仪表盘

```json
{
  "name": "主机概览",
  "description": "所有主机的 CPU、内存、磁盘、网络概览",
  "panels": [
    {
      "id": "p1",
      "title": "CPU 使用率",
      "type": "line",
      "promql": "100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
      "position": {"x": 0, "y": 0},
      "size": {"w": 6, "h": 4},
      "thresholds": [{"value": 80, "color": "#faad14", "label": "警告"}],
      "options": {"unit": "%", "decimals": 1}
    }
  ],
  "timeRange": {"from": "now-1h", "to": "now"},
  "refreshInterval": 30,
  "tags": ["主机", "基础设施"]
}
```

#### GET /api/prometheus/dashboards/{id} - 获取仪表盘详情

#### PUT /api/prometheus/dashboards/{id} - 更新仪表盘

#### DELETE /api/prometheus/dashboards/{id} - 删除仪表盘

#### POST /api/prometheus/dashboards/import - 导入 Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "Node Exporter Full",
    "panels": [
      {
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {"expr": "100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)"}
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      }
    ]
  }
}
```

> 支持 `{"dashboard": {...}}` 包装格式或裸 JSON。

#### GET /api/prometheus/dashboards/{id}/export?format=grafana - 导出为 Grafana 格式

不带 `format` 参数时返回内部格式。

#### GET /api/prometheus/dashboard/templates - 仪表盘模板列表

#### GET /api/prometheus/dashboard/templates/tree - 仪表盘模板树

#### GET /api/prometheus/dashboard/templates/{id} - 模板详情

#### POST /api/prometheus/dashboard/templates/{id}/import - 从模板创建仪表盘

---

### 6. 告警管理 (`/api/prometheus/alert`)

#### GET /api/prometheus/alert/rules - 告警规则列表

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `keyword` | string | 名称模糊搜索 |
| `severity` | string | 严重级别: info/warning/error/critical |
| `status` | string | 状态: enabled/disabled |
| `pageNum` | int | 页码 |
| `pageSize` | int | 每页大小 |

#### POST /api/prometheus/alert/rules - 创建告警规则

```json
{
  "name": "主机 CPU 使用率过高",
  "groupName": "主机告警",
  "expr": "100 - (avg by(instance)(irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100) > 80",
  "duration": "5m",
  "severity": "warning",
  "labels": {"team": "infra"},
  "annotations": {
    "summary": "{{ $labels.instance }} CPU 使用率超过 80%",
    "description": "当前值: {{ $value }}%",
    "runbook": "https://wiki.example.com/runbook/high-cpu"
  }
}
```

#### PUT /api/prometheus/alert/rules/{id} - 更新告警规则

#### DELETE /api/prometheus/alert/rules/{id} - 删除告警规则

#### POST /api/prometheus/alert/rules/{id}/toggle - 启用/禁用告警规则

#### GET /api/prometheus/alert/history - 告警历史列表

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `severity` | string | 严重级别过滤 |
| `status` | string | 状态过滤: firing/resolved/pending |
| `startTime` | string | 起始时间 |
| `endTime` | string | 结束时间 |
| `pageNum` | int | 页码 |
| `pageSize` | int | 每页大小 |

#### POST /api/prometheus/alert/history/{id}/acknowledge - 确认告警

```json
{
  "handledBy": "sre-li",
  "remark": "已检查，临时负载高峰导致"
}
```

#### GET /api/prometheus/alert/silences - 静默规则列表

#### POST /api/prometheus/alert/silences - 创建静默规则

```json
{
  "matchers": [
    {"name": "instance", "value": "10.0.5.11:8080", "isRegex": false, "isEqual": true}
  ],
  "startsAt": "2026-02-17T15:00:00",
  "endsAt": "2026-02-18T09:00:00",
  "comment": "计划维护窗口"
}
```

#### DELETE /api/prometheus/alert/silences/{id} - 删除静默规则

#### GET /api/prometheus/alert/notifications - 通知渠道列表

#### POST /api/prometheus/alert/notifications - 创建通知渠道

**钉钉机器人**：

```json
{
  "type": "dingtalk",
  "name": "运维告警群",
  "config": {
    "webhook": "https://oapi.dingtalk.com/robot/send?access_token=xxx",
    "secret": "SECxxx"
  }
}
```

**邮件**：

```json
{
  "type": "email",
  "name": "告警邮件组",
  "config": {
    "to": "ops-team@example.com",
    "smtp": "smtp.example.com:465"
  }
}
```

**企业微信**：

```json
{
  "type": "wechat",
  "name": "企微告警群",
  "config": {
    "webhook": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"
  }
}
```

**Slack**：

```json
{
  "type": "slack",
  "name": "Slack #alerts",
  "config": {
    "webhook": "https://hooks.slack.com/services/xxx/yyy/zzz",
    "channel": "#alerts"
  }
}
```

**通用 Webhook**：

```json
{
  "type": "webhook",
  "name": "自定义 Webhook",
  "config": {
    "url": "https://api.example.com/alert/callback",
    "method": "POST"
  }
}
```

#### PUT /api/prometheus/alert/notifications/{id} - 更新通知渠道

#### DELETE /api/prometheus/alert/notifications/{id} - 删除通知渠道

#### POST /api/prometheus/alert/notifications/{id}/test - 测试通知渠道

#### POST /api/prometheus/alert/webhook - Alertmanager Webhook

接收 Alertmanager 标准 Webhook 通知格式，自动创建告警历史记录。

```json
{
  "status": "firing",
  "alerts": [
    {
      "status": "firing",
      "labels": {
        "alertname": "HighCPU",
        "instance": "10.0.0.1:9090",
        "severity": "warning"
      },
      "annotations": {
        "summary": "CPU usage high"
      },
      "startsAt": "2026-02-17T10:00:00Z",
      "endsAt": "0001-01-01T00:00:00Z"
    }
  ]
}
```

---

### 7. 集群管理 (`/api/prometheus/clusters`)

#### GET /api/prometheus/clusters - 集群列表

#### POST /api/prometheus/clusters - 创建集群

```json
{
  "name": "生产 K8s 集群",
  "description": "北京机房 K8s 集群",
  "region": "cn-beijing",
  "instanceId": "prom-1"
}
```

#### GET /api/prometheus/clusters/{id} - 集群详情

#### PUT /api/prometheus/clusters/{id} - 更新集群

#### DELETE /api/prometheus/clusters/{id} - 删除集群

#### GET /api/prometheus/clusters/topology - 集群拓扑

返回所有集群的拓扑关系图数据。

#### GET /api/prometheus/clusters/health - 集群健康概览

#### GET /api/prometheus/clusters/{id}/nodes - 集群节点列表

#### POST /api/prometheus/clusters/{id}/nodes - 添加节点

```json
{
  "hostname": "node-1",
  "ip": "10.0.0.1",
  "role": "worker"
}
```

#### DELETE /api/prometheus/clusters/{id}/nodes/{nodeId} - 移除节点

#### GET /api/prometheus/clusters/{id}/discover - 自动发现节点

从 Prometheus 实例自动发现集群节点。

#### POST /api/prometheus/clusters/{id}/sync - 同步集群状态

---

### 8. 权限管理 (`/api/prometheus`)

#### 用户管理

```
GET    /api/prometheus/users              # 用户列表 (支持分页)
POST   /api/prometheus/users              # 创建用户
PUT    /api/prometheus/users/{id}         # 更新用户
DELETE /api/prometheus/users/{id}         # 删除用户
POST   /api/prometheus/users/{id}/toggle  # 启用/禁用用户
```

**创建用户**：

```json
{
  "username": "new-user",
  "password": "password123",
  "email": "user@example.com",
  "roleIds": ["role-3"]
}
```

#### 角色管理

```
GET    /api/prometheus/roles              # 角色列表
POST   /api/prometheus/roles              # 创建角色
PUT    /api/prometheus/roles/{id}         # 更新角色
DELETE /api/prometheus/roles/{id}         # 删除角色 (内置角色不可删)
```

#### 权限管理

```
GET    /api/prometheus/permissions        # 权限树
POST   /api/prometheus/permissions        # 创建权限
PUT    /api/prometheus/permissions/{id}   # 更新权限
DELETE /api/prometheus/permissions/{id}   # 删除权限
```

#### 审计日志

```
GET /api/prometheus/audit-logs
```

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `username` | string | 操作用户过滤 |
| `action` | string | 操作类型过滤 |
| `startTime` | string | 起始时间 |
| `endTime` | string | 结束时间 |
| `pageNum` | int | 页码 |
| `pageSize` | int | 每页大小 |

---

### 9. 分布式部署 (`/api/prometheus/distribute`)

#### 主机管理

```
GET    /api/prometheus/distribute/machines              # 主机列表
POST   /api/prometheus/distribute/machines              # 添加主机
PUT    /api/prometheus/distribute/machines/{id}         # 更新主机
DELETE /api/prometheus/distribute/machines/{id}         # 删除主机
POST   /api/prometheus/distribute/machines/{id}/test    # SSH 连接测试
POST   /api/prometheus/distribute/machines/{id}/detect  # 单机检测
POST   /api/prometheus/distribute/machines/batch-detect # 批量检测
```

**添加主机**：

```json
{
  "name": "生产服务器-1",
  "ip": "10.0.0.1",
  "sshPort": 22,
  "sshUsername": "root",
  "sshPassword": "password123"
}
```

> SSH 密码在入库前使用 AES-256-GCM 加密。

**批量检测响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [
      {
        "machineId": "machine-1",
        "machineName": "生产服务器-1",
        "machineIp": "10.0.0.1",
        "osType": "linux",
        "osArch": "amd64",
        "osDistribution": "Ubuntu 22.04",
        "components": [
          {"name": "prometheus", "version": "2.47.0", "status": "running", "port": 9090, "installPath": "/usr/local/prometheus"},
          {"name": "node_exporter", "version": "1.7.0", "status": "running", "port": 9100, "installPath": "/usr/local/node_exporter"}
        ]
      }
    ],
    "total": 1
  }
}
```

#### 部署任务

```
POST /api/prometheus/distribute/tasks                        # 创建部署任务
GET  /api/prometheus/distribute/tasks                        # 任务列表
GET  /api/prometheus/distribute/tasks/{id}                   # 任务详情
GET  /api/prometheus/distribute/tasks/{id}/details           # 任务明细
POST /api/prometheus/distribute/tasks/{id}/cancel            # 取消任务
POST /api/prometheus/distribute/tasks/{id}/details/{detailId}/retry  # 重试失败明细
```

**创建部署任务**：

```json
{
  "name": "部署 Node Exporter 到所有生产服务器",
  "mode": "batch_unified",
  "machineIds": ["machine-1", "machine-2", "machine-3"],
  "components": ["node_exporter", "blackbox_exporter"],
  "config": {
    "installDir": "/usr/local",
    "node_exporter": {
      "port": 9100,
      "logLevel": "info"
    },
    "blackbox_exporter": {
      "port": 9115
    }
  }
}
```

#### 软件包管理

```
GET  /api/prometheus/distribute/software                     # 软件包列表
POST /api/prometheus/distribute/software/scan                # 扫描本地软件
POST /api/prometheus/distribute/software/download            # 在线下载 (GitHub)
GET  /api/prometheus/distribute/software/download/{downloadId} # 下载进度
POST /api/prometheus/distribute/software/upload              # 离线上传
```

**在线下载**：

```json
{
  "components": ["node_exporter", "blackbox_exporter"],
  "osType": "linux",
  "osArch": "amd64"
}
```

**离线上传**：

```
POST /api/prometheus/distribute/software/upload
Content-Type: multipart/form-data

file: node_exporter-1.7.0.linux-amd64.tar.gz
```

#### 配置验证

```
POST /api/prometheus/distribute/prometheus/validate-config
Content-Type: application/json

{
  "yamlContent": "global:\n  scrape_interval: 15s\n..."
}
```

---

### 10. 系统设置 (`/api/prometheus/settings`)

#### GET /api/prometheus/settings/global - 获取全局设置

**响应**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {"key": "scrape_interval", "value": "15s", "description": "默认采集间隔"},
    {"key": "retention_time", "value": "15d", "description": "默认数据保留时间"},
    {"key": "notification_enabled", "value": "true", "description": "是否启用告警通知"},
    {"key": "theme", "value": "dark", "description": "默认主题"},
    {"key": "language", "value": "zh-CN", "description": "默认语言"},
    {"key": "max_query_duration", "value": "30s", "description": "最大查询超时时间"},
    {"key": "alert_evaluation_interval", "value": "1m", "description": "告警评估间隔"}
  ]
}
```

#### PUT /api/prometheus/settings/global - 更新全局设置

```json
{
  "scrape_interval": "30s",
  "retention_time": "30d"
}
```

#### GET /api/prometheus/settings/logs - 系统日志列表

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `level` | string | 日志级别: INFO/WARN/ERROR/DEBUG |
| `keyword` | string | 消息模糊搜索 |
| `pageNum` | int | 页码 |
| `pageSize` | int | 每页大小 |

#### GET /api/prometheus/settings/logs/export - 导出系统日志

#### POST /api/prometheus/settings/logs/clear - 清理系统日志

---

### 11. 指标元数据 (`/api/prometheus/metrics`)

#### GET /api/prometheus/metrics/metadata - 指标元数据列表

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `keyword` | string | 指标名称模糊搜索 |
| `exporter` | string | Exporter 类型过滤 |
| `type` | string | 指标类型: counter/gauge/histogram/summary |
| `pageNum` | int | 页码 |
| `pageSize` | int | 每页大小 |

#### POST /api/prometheus/metrics/metadata/{id}/favorite - 切换收藏状态

切换指标的 `favorite` 布尔状态（已收藏则取消，未收藏则添加）。

---

### 12. 工作空间 (`/api/prometheus/workspaces`)

```
GET    /api/prometheus/workspaces              # 工作空间列表
POST   /api/prometheus/workspaces              # 创建工作空间
PUT    /api/prometheus/workspaces/{id}         # 更新工作空间
DELETE /api/prometheus/workspaces/{id}         # 删除工作空间
```

**创建工作空间**：

```json
{
  "name": "生产监控中心",
  "description": "生产环境核心监控仪表盘集合",
  "icon": "Monitor"
}
```

**发布仪表盘到工作空间**：

```
POST /api/prometheus/workspaces/{workspaceId}/dashboards/{dashboardId}
```

**移除仪表盘**：

```
DELETE /api/prometheus/workspaces/{workspaceId}/dashboards/{dashboardId}
```

---

### 13. 面板模板 (`/api/prometheus/panels`)

```
GET /api/prometheus/panels/templates           # 面板模板列表
GET /api/prometheus/panels/templates/tree       # 面板模板树 (按分类)
GET /api/prometheus/panels/templates/{id}       # 模板详情
```

| 查询参数 | 类型 | 说明 |
|----------|------|------|
| `category` | string | 分类过滤 |
| `exporterType` | string | Exporter 类型过滤 |
| `chartType` | string | 图表类型过滤: line/bar/gauge/stat/pie/table |

> 面板模板为只读数据，由迁移脚本预置 200+ 条，覆盖 15 种 Exporter 类型。
