# 部署指南

> Prometheus 集群监控管理平台 - 后端部署与运维手册

## 目录

- [环境要求](#环境要求)
- [数据库初始化](#数据库初始化)
- [环境配置](#环境配置)
- [本地开发](#本地开发)
- [生产部署](#生产部署)
- [Docker 部署](#docker-部署)
- [JVM 调优建议](#jvm-调优建议)
- [CI/CD Pipeline](#cicd-pipeline)
- [监控与运维](#监控与运维)
- [故障排查](#故障排查)

---

## 环境要求

### 必要软件

| 软件 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 17 | 17 LTS (Eclipse Temurin) | 仅支持 Java 17+，不兼容 Java 8/11 |
| Maven | 3.6 | 3.9+ | 构建工具 |
| MySQL | 8.0 | 8.0.35+ | 主数据库，需启用 utf8mb4 |
| Git | 2.x | 最新 | 源码管理 |

### 可选软件

| 软件 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 容器化部署 |
| Docker Compose | 2.x | 多容器编排 |
| Nginx | 1.20+ | 反向代理（生产环境推荐） |
| Prometheus | 2.47+ | 监控数据源 |
| Node.js | 20.x LTS | 前端构建（联合部署时需要） |

### 硬件建议

| 规模 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| 开发/测试 | 2 核 | 2 GB | 20 GB | 单机开发环境 |
| 小型生产 (<50 实例) | 4 核 | 4 GB | 50 GB | 适合小团队 |
| 中型生产 (50-200 实例) | 8 核 | 8 GB | 100 GB | 建议独立 MySQL |
| 大型生产 (200+ 实例) | 16 核 | 16 GB | 200 GB+ | 建议高可用部署 |

---

## 数据库初始化

### 1. 创建数据库

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库（必须使用 utf8mb4 字符集）
CREATE DATABASE prometheus_monitor
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 2. 导入 Schema

```bash
# 导入主 Schema（包含所有 29 张表 + 种子数据）
mysql -u root -p prometheus_monitor < src/main/resources/db/schema.sql
```

### 3. 执行迁移脚本（可选）

迁移脚本位于 `src/main/resources/db/migration/`，按版本号顺序执行：

```bash
# 面板模板数据 (200+ 条)
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_01_panel_templates_node.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_02_panel_templates_cadvisor.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_03_panel_templates_kube.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_04_panel_templates_mysql.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_05_panel_templates_redis.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_06_panel_templates_nginx.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_07_panel_templates_blackbox.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_08_panel_templates_process.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_09_panel_templates_jvm.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_10_panel_templates_kafka.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_11_panel_templates_postgres.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_12_panel_templates_mongodb.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_13_panel_templates_es.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_14_panel_templates_rabbitmq.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V7_15_panel_templates_haproxy.sql

# 仪表盘模板数据
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V8_01_dashboard_templates_part1.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V8_02_dashboard_templates_part2.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V8_03_dashboard_templates_part3.sql
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V8_04_dashboard_templates_part4.sql

# 性能索引
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V9__add_performance_indexes.sql

# 乐观锁字段
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V10__add_optimistic_locking.sql

# 外键约束
mysql -u root -p prometheus_monitor < src/main/resources/db/migration/V11__add_foreign_keys.sql
```

### 4. 验证初始化

```sql
-- 确认表数量
SELECT COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'prometheus_monitor';
-- 预期: 29

-- 确认种子用户
SELECT username, status FROM sys_user;
-- 预期: Super/Admin/User/sre-li/dba-chen/viewer-liu
```

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| Super | 123456 | 超级管理员 |
| Admin | 123456 | 管理员 |
| User | 123456 | 普通用户 |

---

## 环境配置

### 配置文件结构

```
src/main/resources/
├── application.yml          # 公共配置
├── application-dev.yml      # 开发环境配置
└── application-prod.yml     # 生产环境配置（需自行创建）
```

### 核心配置项

#### 数据库连接

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/prometheus_monitor?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
    hikari:
      maximum-pool-size: 20        # 最大连接数
      minimum-idle: 5              # 最小空闲连接
      idle-timeout: 300000         # 空闲超时 (5min)
      connection-timeout: 30000    # 连接超时 (30s)
      leak-detection-threshold: 30000  # 泄漏检测阈值
```

#### JWT 认证

```yaml
jwt:
  secret: <至少64字符的随机字符串>     # 生产环境必须更换！
  access-token-expire: 7200000      # Access Token 有效期 (2h)
  refresh-token-expire: 604800000   # Refresh Token 有效期 (7d)
```

#### 分布式部署

```yaml
distribute:
  software-path: /opt/prometheus-monitor/software  # 软件包存储路径
  encryption-key: <32字符AES256密钥>               # SSH 密码加密密钥
  github-token: <可选GitHub Token>                 # 提升 API 限额
  task-timeout-minutes: 30                          # 任务超时时间
  ssh:
    strict-host-key-checking: false                 # 生产环境建议 true
```

#### 调度器配置

```yaml
alert:
  evaluation:
    enabled: true
    interval-ms: 60000           # 告警评估间隔 (60s)

instance:
  health-check:
    enabled: true
    interval-ms: 300000          # 实例健康检查 (5min)

metadata:
  sync:
    enabled: true
    interval-ms: 1800000         # 元数据同步 (30min)

cluster:
  topology:
    push-interval-ms: 30000      # 拓扑推送 (30s)
```

#### 邮件通知（可选）

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 465
    username: alert@example.com
    password: <邮箱密码或应用专用密码>
    properties:
      mail:
        smtp:
          ssl:
            enable: true
```

### 生产环境配置模板

创建 `src/main/resources/application-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/prometheus_monitor?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:prometheus_user}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:30}
      minimum-idle: 10

jwt:
  secret: ${JWT_SECRET}

distribute:
  software-path: ${SOFTWARE_PATH:/opt/prometheus-monitor/software}
  encryption-key: ${AES_KEY}
  ssh:
    strict-host-key-checking: true

cors:
  allowed-origins: ${CORS_ORIGINS:https://monitor.example.com}

logging:
  level:
    com.wenmin.prometheus: info
    org.apache.ibatis.logging: warn

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

### 环境变量

| 变量名 | 必填 | 说明 | 示例 |
|--------|------|------|------|
| `DB_HOST` | 是 | 数据库主机 | `db.example.com` |
| `DB_PORT` | 否 | 数据库端口，默认 3306 | `3306` |
| `DB_USERNAME` | 是 | 数据库用户名 | `prometheus_user` |
| `DB_PASSWORD` | 是 | 数据库密码 | `secure_password` |
| `DB_POOL_SIZE` | 否 | 连接池大小，默认 30 | `50` |
| `JWT_SECRET` | 是 | JWT 签名密钥 (>=64字符) | `随机字符串` |
| `AES_KEY` | 是 | AES256 加密密钥 (32字符) | `随机字符串` |
| `SOFTWARE_PATH` | 否 | 软件包路径 | `/opt/prometheus-monitor/software` |
| `CORS_ORIGINS` | 否 | CORS 允许的来源 | `https://monitor.example.com` |

---

## 本地开发

### 快速启动

```bash
# 1. 克隆代码
git clone <repo-url>
cd prometheus_monitor_backend

# 2. 初始化数据库（参见上方"数据库初始化"部分）

# 3. 启动开发环境
mvn spring-boot:run

# 或使用 IDE 运行 PrometheusMonitorApplication.main()
```

### 开发工具

- **API 文档**：启动后访问 http://localhost:8080/doc.html (Knife4j)
- **SQL 日志**：开发环境默认开启 MyBatis SQL 日志（StdOutImpl）
- **热重载**：支持 IDE 的热加载功能

---

## 生产部署

### 方式一：JAR 包直接部署

#### 1. 构建

```bash
cd prometheus_monitor_backend
mvn clean package -DskipTests
# 产物: target/prometheus-monitor-1.0.0.jar
```

#### 2. 前台运行（调试）

```bash
java -jar target/prometheus-monitor-1.0.0.jar \
  --spring.profiles.active=prod
```

#### 3. 后台运行

```bash
nohup java -jar target/prometheus-monitor-1.0.0.jar \
  --spring.profiles.active=prod \
  -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  > /var/log/prometheus-monitor/app.log 2>&1 &
```

#### 4. systemd 服务管理（推荐）

创建 `/etc/systemd/system/prometheus-monitor.service`：

```ini
[Unit]
Description=Prometheus Monitor Backend
Documentation=https://github.com/user/prometheus-monitor-backend
After=network.target mysql.service
Requires=network.target

[Service]
Type=simple
User=prometheus-monitor
Group=prometheus-monitor
WorkingDirectory=/opt/prometheus-monitor

# 环境变量
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=JAVA_HOME=/usr/lib/jvm/java-17-temurin
EnvironmentFile=-/opt/prometheus-monitor/.env

ExecStart=/usr/bin/java \
  -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/opt/prometheus-monitor/heapdump/ \
  -Djava.security.egd=file:/dev/./urandom \
  -jar prometheus-monitor-1.0.0.jar

Restart=always
RestartSec=10
StandardOutput=append:/var/log/prometheus-monitor/app.log
StandardError=append:/var/log/prometheus-monitor/error.log

# 安全加固
NoNewPrivileges=true
ProtectSystem=strict
ReadWritePaths=/opt/prometheus-monitor /var/log/prometheus-monitor /tmp

[Install]
WantedBy=multi-user.target
```

创建环境变量文件 `/opt/prometheus-monitor/.env`：

```bash
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=prometheus_user
DB_PASSWORD=your_secure_password
JWT_SECRET=your-production-jwt-secret-key-at-least-64-characters-long-random
AES_KEY=your-aes256-encryption-key-32ch!
```

启动服务：

```bash
# 创建用户
sudo useradd -r -s /bin/false prometheus-monitor
sudo mkdir -p /opt/prometheus-monitor/heapdump /var/log/prometheus-monitor
sudo chown -R prometheus-monitor:prometheus-monitor /opt/prometheus-monitor /var/log/prometheus-monitor

# 加载并启动
sudo systemctl daemon-reload
sudo systemctl enable prometheus-monitor
sudo systemctl start prometheus-monitor

# 查看状态
sudo systemctl status prometheus-monitor
sudo journalctl -u prometheus-monitor -f
```

### 方式二：Nginx 反向代理

推荐在生产环境使用 Nginx 作为反向代理，提供 SSL 终止、静态资源服务和 WebSocket 代理。

```nginx
upstream backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name monitor.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name monitor.example.com;

    # SSL 证书
    ssl_certificate     /etc/ssl/certs/monitor.example.com.pem;
    ssl_certificate_key /etc/ssl/private/monitor.example.com.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # 安全头
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 前端静态资源
    root /var/www/prometheus-monitor;
    index index.html;

    # Vue Router history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 120s;
        proxy_send_timeout 30s;

        # 文件上传限制 (软件包上传)
        client_max_body_size 500M;
    }

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://backend/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain application/javascript text/css application/json
               application/xml text/xml image/svg+xml;
}
```

---

## Docker 部署

### Dockerfile

```dockerfile
# ==================== 构建阶段 ====================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre-alpine

# 安全: 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 复制 JAR
COPY --from=builder /build/target/prometheus-monitor-1.0.0.jar app.jar

# 复制数据库脚本 (用于容器初始化)
COPY --from=builder /build/src/main/resources/db/ /app/db/

# 软件包存储目录
RUN mkdir -p /app/software /app/heapdump && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/auth/login || exit 1

ENTRYPOINT ["java", \
  "-Xms512m", "-Xmx1024m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/heapdump/", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  # ==================== MySQL ====================
  mysql:
    image: mysql:8.0
    container_name: prometheus-monitor-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-root}
      MYSQL_DATABASE: prometheus_monitor
      MYSQL_USER: prometheus_user
      MYSQL_PASSWORD: ${DB_PASSWORD:-prometheus_pass}
      TZ: Asia/Shanghai
    ports:
      - "${DB_PORT:-3306}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-authentication-plugin=mysql_native_password
      --max-connections=200
      --innodb-buffer-pool-size=256M
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD:-root}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - monitor-net

  # ==================== Backend ====================
  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: prometheus-monitor-backend
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/prometheus_monitor?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: prometheus_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-prometheus_pass}
      JWT_SECRET: ${JWT_SECRET:-change-me-to-a-very-long-random-string-at-least-64-characters}
      DISTRIBUTE_ENCRYPTION_KEY: ${AES_KEY:-change-me-32-char-aes-key-here!}
      DISTRIBUTE_SOFTWARE_PATH: /app/software
      CORS_ALLOWED_ORIGINS: ${CORS_ORIGINS:-http://localhost:3006}
    volumes:
      - software-data:/app/software
      - heapdump-data:/app/heapdump
    networks:
      - monitor-net

volumes:
  mysql-data:
    driver: local
  software-data:
    driver: local
  heapdump-data:
    driver: local

networks:
  monitor-net:
    driver: bridge
```

### Docker 操作命令

```bash
# 构建并启动
docker-compose up -d --build

# 查看日志
docker-compose logs -f backend

# 停止
docker-compose down

# 停止并清除数据
docker-compose down -v

# 仅重建后端
docker-compose up -d --build backend
```

### Docker 环境变量文件

创建 `.env` 文件：

```bash
# Database
DB_ROOT_PASSWORD=root_secure_password
DB_PASSWORD=prometheus_secure_password
DB_PORT=3306

# Backend
BACKEND_PORT=8080
JWT_SECRET=your-production-jwt-secret-key-at-least-64-characters-long-and-random
AES_KEY=your-aes256-encryption-key-32ch!
CORS_ORIGINS=https://monitor.example.com
```

---

## JVM 调优建议

### 基础配置

```bash
# 小型部署 (< 50 实例)
java -Xms512m -Xmx1024m -XX:+UseG1GC ...

# 中型部署 (50-200 实例)
java -Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 ...

# 大型部署 (200+ 实例)
java -Xms2048m -Xmx4096m -XX:+UseG1GC -XX:MaxGCPauseMillis=150 \
     -XX:G1HeapRegionSize=8m -XX:InitiatingHeapOccupancyPercent=45 ...
```

### 推荐 JVM 参数

```bash
# GC 配置
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=4m

# OOM 诊断
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/opt/prometheus-monitor/heapdump/

# GC 日志 (Java 17)
-Xlog:gc*:file=/var/log/prometheus-monitor/gc.log:time,uptime,level,tags:filecount=5,filesize=10m

# 安全
-Djava.security.egd=file:/dev/./urandom

# 网络
-Djava.net.preferIPv4Stack=true
-Dsun.net.inetaddr.ttl=60
```

### 连接池调优

| 参数 | 开发 | 小型生产 | 中型生产 | 大型生产 |
|------|------|---------|---------|---------|
| HikariCP `max-pool-size` | 20 | 30 | 50 | 100 |
| HikariCP `minimum-idle` | 5 | 10 | 20 | 30 |
| SSH 连接池 `maxTotal` | 20 | 30 | 50 | 100 |
| SSH 连接池 `maxPerKey` | 3 | 5 | 8 | 10 |
| Async 线程池 `maxPoolSize` | 20 | 30 | 50 | 100 |

---

## CI/CD Pipeline

### GitHub Actions 示例

```yaml
name: Build and Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '17'
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # ==================== 构建与测试 ====================
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and Test
        run: mvn clean verify -B

      - name: Upload JAR
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/prometheus-monitor-1.0.0.jar
          retention-days: 7

  # ==================== Docker 镜像 ====================
  docker:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build JAR
        run: mvn clean package -DskipTests -B

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

  # ==================== 部署 ====================
  deploy:
    needs: docker
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production

    steps:
      - name: Deploy to Server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_KEY }}
          script: |
            cd /opt/prometheus-monitor
            docker-compose pull backend
            docker-compose up -d backend
            docker image prune -f
```

---

## 监控与运维

### 健康检查

```bash
# 基本检查 - 通过 HTTP 状态码判断服务是否存活
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/doc.html
# 预期: 200

# 数据库连接检查
curl -s http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"healthcheck","password":"invalid"}' | jq .code
# 预期: 400 (请求失败但服务正常)
```

### 日志管理

```bash
# 应用日志位置
/var/log/prometheus-monitor/app.log    # 主日志
/var/log/prometheus-monitor/error.log  # 错误日志

# Logback 内置按日滚动，无需额外配置 logrotate
# 如需自定义，可在 application.yml 中覆盖:
logging:
  file:
    name: /var/log/prometheus-monitor/app.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

### 数据备份

```bash
# MySQL 备份脚本
#!/bin/bash
BACKUP_DIR=/opt/backup/mysql
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u root -p prometheus_monitor | gzip > ${BACKUP_DIR}/prometheus_monitor_${DATE}.sql.gz

# 保留最近 7 天备份
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +7 -delete
```

---

## 故障排查

### 常见问题

#### Q: 启动报错 `Communications link failure`

**原因**：MySQL 连接失败

**解决**：
1. 确认 MySQL 服务已启动：`systemctl status mysql`
2. 确认连接参数正确：URL、用户名、密码
3. 确认 MySQL 允许远程连接：`GRANT ALL ON prometheus_monitor.* TO 'user'@'%';`
4. 确认防火墙开放 3306 端口

#### Q: JWT Token 验证失败 (401)

**原因**：Token 过期或密钥不匹配

**解决**：
1. 确认 `jwt.secret` 配置一致（所有实例使用相同密钥）
2. 确认客户端时钟同步
3. 检查 Token 是否过期（Access Token 默认 2 小时）
4. 使用 Refresh Token 刷新：`POST /api/auth/refresh`

#### Q: WebSocket 连接失败

**原因**：Nginx 未正确配置 WebSocket 代理

**解决**：确保 Nginx 配置包含：
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_read_timeout 3600s;
```

#### Q: SSH 远程操作失败

**原因**：SSH 连接或权限问题

**解决**：
1. 确认目标主机 SSH 服务正常
2. 确认用户名/密码正确（密码使用 AES256 加密存储）
3. 确认用户有 sudo 权限（安装组件需要）
4. 检查防火墙是否开放 SSH 端口

#### Q: 文件上传报 413 错误

**原因**：Nginx 默认限制 1MB

**解决**：在 Nginx 配置中添加：
```nginx
client_max_body_size 500M;
```

#### Q: OutOfMemoryError

**原因**：JVM 堆内存不足

**解决**：
1. 增加堆内存：`-Xmx2048m`
2. 检查 heap dump 文件分析内存泄漏
3. 检查 SSH 连接池是否正常释放连接
4. 检查 WebSocket Session 是否正常清理
