# Architecture — 当前架构状态

> 本文档记录项目当前的架构状态，由 AI 在每完成一个 Step 后自动更新。
> 包含：模块状态、数据库 Schema、API 接口、部署组件。

> **最后更新**: 2026-07-07
> **状态**: P1 Step 1.7~1.9 已完成 — 爬虫基础框架 + BOSS直聘采集器 + 采集任务管理 + XXL-Job 调度

---

## 一、当前模块状态

| 模块                        | 状态     | 说明                         |
| --------------------------- | -------- | ---------------------------- |
| occupation-common           | ✅ 已实现 | 统一响应 + 异常处理 + BaseEntity + 多租户 + 分页 + 13张表SQL + Kafka管道 |
| occupation-auth             | ✅ 已实现 | JWT 签发/校验 + 登录接口 + JwtAuthenticationFilter + SecurityConfig |
| occupation-crawler          | ✅ 已实现 | WebMagic 爬虫框架 + CrawlerTask/CrawlerLog + 模拟+真实采集 + XXL-Job Handler |
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
| SQL 脚本 | `occupation-common/.../sql/init.sql` | ✅ 13 张表 + 种子数据（测试学院 / admin） |
| 多租户插件 | `occupation-common/.../config/MyBatisPlusConfig.java` | ✅ 分页 + tenant_id 自动注入 |
| JWT 工具类 | `occupation-auth/.../util/JwtUtil.java` | ✅ 签发/校验/解析 |
| 登录接口 | `occupation-auth/.../controller/AuthController.java` | ✅ POST /api/auth/login |
| Kafka Topic | `occupation-common/.../config/KafkaTopicConfig.java` | ✅ raw-job-data / cleaned-job-data |
| Kafka 生产者 | `occupation-common/.../config/KafkaProducerConfig.java` | ✅ JsonSerializer + 可靠性保障 |
| Kafka 消费者 | `occupation-common/.../config/KafkaConsumerConfig.java` | ✅ data-cleaner-group + JsonDeserializer |
| 消息体 | `occupation-common/.../dto/JobDataMessage.java` | ✅ source / sourceUrl / rawContent / fetchTime |
| 生产者服务 | `occupation-common/.../service/KafkaProducerService.java` | ✅ 异步发送 + 回调日志 |
| 消费者服务 | `occupation-common/.../service/KafkaConsumerService.java` | ✅ @KafkaListener → raw_job_data 入库 |
| 爬虫基础框架 | `occupation-crawler/.../processor/JobPageProcessor.java` | ✅ 抽象基类，UA 轮换，随机延迟，重试3次 |
| 采集管道 | `occupation-crawler/.../processor/JobPipeline.java` | ✅ WebMagic Pipeline → Kafka 发送 |
| 模拟爬虫 | `occupation-crawler/.../processor/MockJobPageProcessor.java` | ✅ 本地 JSON 模拟采集链路 |
| BOSS 采集器 | `occupation-crawler/.../processor/BossJobPageProcessor.java` | ✅ 列表页+详情页解析，薪资格式化 |
| 采集 Service | `occupation-crawler/.../service/impl/CrawlerServiceImpl.java` | ✅ 启停管理 + 多源路由 + 状态跟踪 |
| 采集管理 API | `occupation-crawler/.../controller/CrawlerController.java` | ✅ CRUD + 启停 + 日志查询 |
| 采集 Entity | `occupation-crawler/.../entity/CrawlerTask.java` + `CrawlerLog.java` | ✅ 映射 crawler_task / crawler_log |
| XXL-Job 配置 | `occupation-crawler/.../config/XxlJobConfig.java` | ✅ 调度器自动注册 |
| XXL-Job Handler | `occupation-crawler/.../job/CrawlerJobHandler.java` | ✅ 定时扫描 + 手动触发 + 全停 |
| 模拟数据 | `occupation-crawler/.../mock/mock-jobs.json` | ✅ 20 条多行业职位测试数据 |

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

### MVC 包结构规范（所有模块统一遵循）

```
com.occupation.<模块名>
├── controller/          ← Controller 层
├── service/             ← Service 接口
├── service/impl/        ← Service 实现
├── mapper/              ← MyBatis-Plus Mapper
├── entity/              ← 数据库实体
├── dto/                 ← 请求 DTO
├── vo/                  ← 响应 VO
└── config/              ← 模块配置
```

---

## 四、数据库 Schema（当前版本）

> 脚本位置：`occupation-common/src/main/resources/sql/init.sql`
> 执行后可创建全部 13 张表 + 初始化种子数据。

### 已实现表（13 张，全部在 init.sql 中定义）

| 表名                   | 所属模块        | 说明                 | tenant_id |
| ---------------------- | --------------- | -------------------- | --------- |
| sys_tenant             | common          | 租户表               | ❌（自身不含） |
| sys_user               | auth            | 用户表               | ✅ |
| sys_student_profile    | recommend       | 学生画像表           | ✅ |
| crawler_task           | crawler         | 采集任务表           | ✅ |
| crawler_log            | crawler         | 采集日志表           | ✅ |
| raw_job_data           | crawler         | 原始职位数据表       | ❌（全平台共享） |
| job_detail             | analysis        | 清洗后职位表         | ❌（全平台共享） |
| analysis_result        | analysis        | 分析结果表           | ✅ |
| report_template        | report          | 报告模板表           | ✅ |
| report_record          | report          | 报告记录表           | ✅ |
| push_record            | recommend       | 推送记录表           | ✅ |
| student_behavior       | recommend       | 学生行为记录表       | ✅ |
| sys_alert              | web/common      | 系统告警表           | ✅ |

### 种子数据

| 表 | 数据 |
|---|---|
| sys_tenant | id=1, name="测试学院" |
| sys_user | id=1, username="admin", password="admin123"（BCrypt），role=ADMIN |

---

## 五、API 接口清单

> 完整定义见 `implementation-plan.md` 各步骤。
> 当前已实现：11

| 接口 | 方法 | 说明 | 所属 Step | 状态 |
|---|---|---|---|---|
| GET /api/health | GET | 健康检查 | Step 1.2 | ✅ |
| GET /api/health/error | GET | 异常测试（验证全局异常处理器） | Step 1.2 | ✅ |
| POST /api/auth/login | POST | 用户登录（返回 JWT Token） | Step 1.5 | ✅ |
| POST /api/admin/crawler/task | POST | 创建采集任务 | Step 1.9 | ✅ |
| GET /api/admin/crawler/task | GET | 采集任务列表（分页） | Step 1.9 | ✅ |
| GET /api/admin/crawler/task/{id} | GET | 采集任务详情 | Step 1.9 | ✅ |
| PUT /api/admin/crawler/task/{id} | PUT | 更新采集任务 | Step 1.9 | ✅ |
| DELETE /api/admin/crawler/task/{id} | DELETE | 删除采集任务 | Step 1.9 | ✅ |
| PUT /api/admin/crawler/task/{id}/start | PUT | 手动启动任务 | Step 1.9 | ✅ |
| PUT /api/admin/crawler/task/{id}/stop | PUT | 停止任务 | Step 1.9 | ✅ |
| POST /api/admin/crawler/task/mock | POST | 启动模拟爬虫（便捷测试） | Step 1.7 | ✅ |

---

## 六、部署组件状态

| 组件       | 版本   | 本地可运行 | Docker 定义 |
| ---------- | ------ | ---------- | ----------- |
| MySQL      | 8.0    | ⏳         | ✅ 已完成 |
| Redis      | 6.2    | ⏳         | ✅ 已完成 |
| Kafka      | 7.5.0  | ⏳         | ✅ 已完成（Producer/Consumer 配置 + Topic 定义） |
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
| 2026-07-07 | P1 Step 1.3 完成：init.sql（13 张表 + 种子数据）+ 自动填充 + Druid 数据源配置 | Step 1.3 |
| 2026-07-07 | P1 Step 1.4 完成：多租户插件 + TenantContextHolder + 分页插件 + SysTenant/SysUser Entity+Mapper+Service | Step 1.4 |
| 2026-07-07 | P1 Step 1.5 完成：JWT 签发/校验 + POST /api/auth/login + JwtAuthenticationFilter + SecurityConfig + BCrypt | Step 1.5 |
| 2026-07-07 | P1 Step 1.6 完成：Kafka Spring 集成 — KafkaTopicConfig（raw-job-data/cleaned-job-data）+ Producer/Consumer 配置 + JobDataMessage + RawJobData 实体/Mapper + KafkaProducerService + KafkaConsumerService（@KafkaListener → 入库）| Step 1.6 |
| 2026-07-07 | MVC 包结构调整：所有 7 个业务模块统一 8 层包结构 + package-info 说明 | 架构重构 |
| 2026-07-07 | P1 Step 1.7 完成：WebMagic 爬虫框架集成 — JobPageProcessor 抽象基类（UA 池 + 随机延迟 + 重试）+ JobPipeline（Kafka 管道）+ MockJobPageProcessor（本地 JSON 模拟）+ 20 条测试数据 | Step 1.7 |
| 2026-07-07 | P1 Step 1.8 完成：BossJobPageProcessor 真实采集器 — 列表页解析（标题/公司/薪资/城市/学历/经验）+ 详情页解析（描述/技能标签）+ 分页翻页 + 反爬策略（5-15s 随机延迟）| Step 1.8 |
| 2026-07-07 | P1 Step 1.9 完成：采集任务管理 API（CRUD + 启停 + 日志查询）+ XXL-Job 调度集成（定时扫描/手动触发/全停）| Step 1.9 |
