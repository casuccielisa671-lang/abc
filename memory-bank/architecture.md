# Architecture — 当前架构状态

> 本文档记录项目当前的架构状态，由 AI 在每完成一个 Step 后自动更新。
> 包含：模块状态、数据库 Schema、API 接口、部署组件。

> **最后更新**: 2026-07-06
> **状态**: P1 Step 1.2 已完成 — 统一响应 + 异常处理 + 健康检查

---

## 一、当前模块状态

| 模块                        | 状态     | 说明                         |
| --------------------------- | -------- | ---------------------------- |
| occupation-common           | ✅ 已实现 | Result<T> + BizException + GlobalExceptionHandler |
| occupation-auth             | ✅ 已创建 | POM + 包结构就绪，待 Step 1.5 实现认证 |
| occupation-crawler          | ✅ 已创建 | POM + 包结构就绪，待 Step 1.7-1.9 实现采集 |
| occupation-analysis         | ✅ 已创建 | POM + 包结构就绪，待 P2 实现分析 |
| occupation-report           | ✅ 已创建 | POM + 包结构就绪，待 P3 实现报告 |
| occupation-recommend        | ✅ 已创建 | POM + 包结构就绪，待 P4 实现推荐 |
| occupation-api              | ✅ 已创建 | POM + 包结构就绪，待 P5 实现对外 API |
| occupation-web              | ✅ 已运行 | Application + HealthController + 2 项测试通过 |
| occupation-web-ui (Vue 3)   | ⏳ 待开发 | P3 Step 3.1 |

---

## 二、关键文件清单

| 文件 | 路径 | 状态 |
|---|---|---|
| 父 POM | `pom.xml` | ✅ SpringBoot 2.7.18，8 模块，版本锁定 |
| 启动入口 | `occupation-web/.../Application.java` | ✅ @SpringBootApplication |
| 应用配置 | `occupation-web/.../application.yml` | ✅ MySQL + Redis + Kafka + MyBatis-Plus |
| Docker 编排 | `docker-compose.yml` | ✅ MySQL 8.0 + Redis 6.2 + Kafka + Zookeeper + Nginx |
| Nginx 配置 | `nginx/nginx.conf` | ✅ API 代理 + 静态资源 |
| SQL 脚本 | `occupation-common/.../sql/init.sql` | ✅ 建库占位，待 Step 1.3 补全建表 |

---

## 三、模块依赖关系

```
occupation-web (启动入口)
  ├── occupation-common (下层基础)
  ├── occupation-auth → common
  ├── occupation-crawler → common
  ├── occupation-analysis → common
  ├── occupation-report → common
  ├── occupation-recommend → common
  └── occupation-api → common
```

所有业务模块依赖 common，web 聚合全部模块。

---

## 四、数据库 Schema（当前版本）

> 初始 Schema 定义见 `implementation-plan.md` Step 1.3。
> 以下为执行 `init.sql` 后应存在的表清单。

### 已实现表：暂无

### 待实现表：

| 表名                   | 所属模块        | 说明                 |
| ---------------------- | --------------- | -------------------- |
| sys_tenant             | common          | 租户表               |
| sys_user               | common/auth     | 用户表               |
| sys_student_profile    | recommend       | 学生画像表           |
| crawler_task           | crawler         | 采集任务表           |
| crawler_log            | crawler         | 采集日志表           |
| raw_job_data           | crawler         | 原始职位数据表       |
| job_detail             | analysis        | 清洗后职位表         |
| analysis_result        | analysis        | 分析结果表           |
| report_template        | report          | 报告模板表           |
| report_record          | report          | 报告记录表           |
| push_record            | recommend       | 推送记录表           |
| student_behavior       | recommend       | 学生行为表           |
| sys_alert              | web/common      | 告警表               |

---

## 五、API 接口清单

> 完整定义见 `implementation-plan.md` 各步骤。
> 当前已实现：2

| 接口 | 方法 | 说明 | 所属 Step | 状态 |
|---|---|---|---|---|
| GET /api/health | GET | 健康检查 | Step 1.2 | ✅ |
| GET /api/health/error | GET | 异常测试（验证全局异常处理器） | Step 1.2 | ✅ |

---

## 六、部署组件状态

| 组件       | 版本   | 本地可运行 | Docker 定义 |
| ---------- | ------ | ---------- | ----------- |
| MySQL      | 8.0    | ⏳         | ✅ 已完成 |
| Redis      | 6.2    | ⏳         | ✅ 已完成 |
| Kafka      | 7.5.0  | ⏳         | ✅ 已完成 |
| Nginx      | 1.24   | ⏳         | ✅ 已完成 |
| Zookeeper  | 7.5.0  | ⏳         | ✅ 已完成 |
| HDFS       | 3.3    | ⏳         | P2 补充 |
| Hive       | 3.1    | ⏳         | P2 补充 |
| HBase      | 2.4    | ⏳         | P2 补充 |
| Spark      | 3.4    | ⏳         | P2 补充 |

---

## 七、变更日志

| 日期       | 变更内容         | 关联步骤  |
| ---------- | ---------------- | --------- |
| 2026-07-06 | 初始架构骨架创建 | 设计阶段  |
| 2026-07-06 | P1 Step 1.1 完成：8 模块 Maven 项目 + docker-compose + nginx + application.yml | Step 1.1 |
| 2026-07-06 | P1 Step 1.2 完成：Result<T> + BizException + GlobalExceptionHandler + HealthController + 2 项测试通过 | Step 1.2 |
