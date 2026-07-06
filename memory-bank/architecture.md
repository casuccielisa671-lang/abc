# Architecture — 当前架构状态

> 本文档记录项目当前的架构状态，由 AI 在每完成一个 Step 后自动更新。
> 包含：模块状态、数据库 Schema、API 接口、部署组件。

> **最后更新**: 2026-07-06  
> **状态**: P0 — 设计阶段，尚未开始编码

---

## 一、当前模块状态

| 模块                        | 状态    | 说明                         |
| --------------------------- | ------- | ---------------------------- |
| occupation-common           | ⏳ 待开发 | 公共模块（P1 Step 1.1-1.4） |
| occupation-auth             | ⏳ 待开发 | 认证授权（P1 Step 1.5）     |
| occupation-crawler          | ⏳ 待开发 | 数据采集（P1 Step 1.7-1.9） |
| occupation-analysis         | ⏳ 待开发 | 数据分析（P2）              |
| occupation-report           | ⏳ 待开发 | 报告生成（P3）              |
| occupation-recommend        | ⏳ 待开发 | 推荐推送（P4）              |
| occupation-api              | ⏳ 待开发 | 对外 API（P5）              |
| occupation-web              | ⏳ 待开发 | 启动模块（P1 Step 1.1）     |
| occupation-web-ui (Vue 3)   | ⏳ 待开发 | 前端（P3 Step 3.1）         |

---

## 二、数据库 Schema（当前版本）

> 初始 Schema 定义见 `implementation-plan.md` Step 1.3。  
> 以下为执行 `init.sql` 后应存在的表清单。

### 已实现表：暂无（尚未开始编码）

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

## 三、API 接口清单

> 完整定义见 `implementation-plan.md` 各步骤。  
> 当前实现数：0

---

## 四、部署组件状态

| 组件       | 版本   | 本地可运行 | 说明                   |
| ---------- | ------ | ---------- | ---------------------- |
| MySQL      | 8.0    | ⏳         | docker-compose 定义完成 |
| Redis      | 6.2    | ⏳         | docker-compose 定义完成 |
| Kafka      | 3.4    | ⏳         | docker-compose 定义完成 |
| Nginx      | 1.24   | ⏳         | docker-compose 定义完成 |
| HDFS       | 3.3    | ⏳         | docker-compose 定义完成 |
| Hive       | 3.1    | ⏳         | docker-compose 定义完成 |
| HBase      | 2.4    | ⏳         | docker-compose 定义完成 |
| Spark      | 3.4    | ⏳         | docker-compose 定义完成 |
| Zookeeper  | 3.8    | ⏳         | docker-compose 定义完成 |

---

## 五、变更日志

| 日期       | 变更内容         | 关联步骤  |
| ---------- | ---------------- | --------- |
| 2026-07-06 | 初始架构骨架创建 | 设计阶段  |
