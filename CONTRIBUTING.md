# 贡献指南

感谢你对 Prometheus Monitor 后端项目的关注！欢迎提交 Issue 和 Pull Request。

## 开发环境搭建

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- IDE 推荐：IntelliJ IDEA

### 快速启动

```bash
# 1. Fork 并克隆项目
git clone https://github.com/<your-username>/prometheus_monitor_backend.git
cd prometheus_monitor_backend

# 2. 初始化数据库
mysql -u root -p -e "CREATE DATABASE prometheus_monitor DEFAULT CHARSET utf8mb4;"
mysql -u root -p prometheus_monitor < src/main/resources/db/schema.sql

# 3. 修改数据库配置
# 编辑 src/main/resources/application-dev.yml

# 4. 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 项目规范

### 模块结构

每个业务模块遵循统一目录结构：

```
module/<module-name>/
├── controller/    # REST 控制器
├── service/       # 业务接口
│   └── impl/     # 业务实现
├── mapper/        # MyBatis-Plus Mapper
├── entity/        # 数据库实体
├── dto/           # 请求数据传输对象
└── vo/            # 响应视图对象
```

### 编码规范

- 使用 Lombok 简化实体/DTO 代码
- Controller 只做参数校验和结果包装，业务逻辑放 Service 层
- 使用 `@AuditLog` 注解记录关键操作
- 所有 API 返回统一 `R<T>` 响应格式
- 数据库实体使用 `@TableLogic` 实现逻辑删除

### 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
feat: 新增 Exporter 批量导入功能
fix: 修复仪表盘查询超时问题
docs: 更新 API 文档
refactor: 重构告警通知发送逻辑
```

## Pull Request

1. 确保代码可以编译：`mvn clean compile`
2. 确保能正常打包：`mvn clean package -DskipTests`
3. 新增接口需在 Controller 上添加 Knife4j 注解
4. 数据库变更需提供 SQL 迁移脚本
5. 在 PR 中说明改动内容和测试方式
