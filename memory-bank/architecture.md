# Architecture — 当前架构状态

> 本文档记录项目当前的架构状态，由 AI 在每完成一个 Step 后自动更新。
> 包含：模块状态、数据库 Schema、API 接口、部署组件。

> **最后更新**: 2026-07-14（推荐算法升级：协同过滤 + 内容推荐）
> **状态**: 四端功能与工具箱已落地 — 数据管道(采集→清洗→统计)贯通 + 中等规模种子数据 + Docker 生产部署链路就绪 + AI 能力全面升级 + 混合推荐引擎(规则+语义+CF+CB)
> **⚠️ 重要**: 本轮起以 `docs/项目开发说明书.md` 为唯一权威开发指南（含架构、分工、待办清单）；
> 技术栈已收敛：HDFS/Hive/HBase/Neo4j/ES 移出必做范围，Spark 以可插拔接口预留（AnalysisJobService），
> 调度默认 Spring @Scheduled；XXL-Job 不再作为当前必需组件。

---

## 一、当前模块状态

| 模块                        | 状态     | 说明                         |
| --------------------------- | -------- | ---------------------------- |
| occupation-common           | ✅ 已实现 | 统一响应 + 异常处理 + BaseEntity + 多租户 + 分页 + 13张表SQL + Kafka管道 |
| occupation-auth             | ✅ 已实现 | JWT 签发/校验 + 登录接口 + JwtAuthenticationFilter + SecurityConfig |
| occupation-crawler          | ✅ 已实现 | WebMagic 爬虫框架 + CrawlerTask/CrawlerLog + MOCK/OFFICIAL_PUBLIC/STACK_OVERFLOW_SURVEY 采集 + 完成状态自动回写；资讯拉取归属 recommend/news 模块 |
| occupation-analysis         | ✅ 已实现 | Dashboard 5维度查询 + 职位分页查询 + Controller API |
| occupation-report           | ✅ 框架落地 | 模板 CRUD + 生成引擎(六步) + AI 摘要(LLM+降级) + PDF/Word 导出器 + 下载接口 |
| occupation-recommend        | ✅ 框架落地 | 画像 CRUD + 混合推荐引擎(规则打分+语义匹配+协同过滤+内容推荐) + 推送 + 行为闭环 + 教师/HR 接口 + 每日推送调度 + SemanticMatchService + CollaborativeFilterService + ContentBasedRecommendService |
| occupation-api              | ✅ 框架落地 | Token 签发(Redis) + 校验/限流拦截器 + 开放数据接口(职位/大盘/技能/行业) |
| occupation-web              | ✅ 已运行 | Application + HealthController + 2 项测试通过 |
| occupation-web-ui (Vue 3)   | ✅ 已实现 | 四角色页面 + 工具箱主界面/悬浮入口 + 资讯/报告/职位/画像等页面 |

---

## 二、关键文件清单

| 文件 | 路径 | 状态 |
|---|---|---|
| 父 POM | `pom.xml` | ✅ SpringBoot 2.7.18，8 模块，版本锁定 |
| 启动入口 | `occupation-web/.../Application.java` | ✅ @SpringBootApplication |
| 应用配置 | `occupation-web/.../application.yml` | ✅ MySQL + Redis + Kafka + MyBatis-Plus |
| Docker 编排 | `docker-compose.yml` / `docker-compose.prod.yml` | ✅ 开发环境 + 生产环境；生产镜像自动构建后端 JAR 与前端 dist |
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
| 官方公开采集器 | `occupation-crawler/.../processor/OfficialPublicJobProcessor.java` | ✅ robots.txt 校验 + 低频采集公开招聘公告 |
| 调查 CSV 采集器 | `occupation-crawler/.../processor/StackOverflowSurveyProcessor.java` | ✅ 公开 CSV 调查导入，转为开发者生态样本岗位 |
| 资讯兼容采集器 | `OfficialReportProcessor` / `InfoQNewsProcessor` / `OsChinaNewsProcessor` / `NewsPageProcessor` | ✅ 历史兼容入口；新建资讯推荐通过 `AdminNewsController.pull-rss` 写入 news 表，不作为数据面板采集任务 |
| 采集 Service | `occupation-crawler/.../service/impl/CrawlerServiceImpl.java` | ✅ 启停管理 + 多源路由 + 状态跟踪 |
| 采集管理 API | `occupation-crawler/.../controller/CrawlerController.java` | ✅ CRUD + 启停 + 日志查询 |
| 采集 Entity | `occupation-crawler/.../entity/CrawlerTask.java` + `CrawlerLog.java` | ✅ 映射 crawler_task / crawler_log |
| 资讯定时任务 | `occupation-crawler/.../job/NewsCrawlerJobHandler.java` | ✅ 历史兼容定时任务；前台行业资讯统一读取 `/api/news` |
| 模拟数据 | `occupation-crawler/.../mock/mock-jobs.json` | ✅ 120 条多行业职位测试数据 |
| 初始化脚本 | `scripts/reset-dev-db.ps1` | ✅ 重新生成数据并重建开发数据库 |
| 部署说明 | `docs/部署与同步说明.md` | ✅ GitHub 同步、初始化、云服务器部署说明 |

---

## 三、模块依赖关系

```
occupation-web (启动入口)
  ├── occupation-common (下层基础)
  ├── occupation-auth → common
  ├── occupation-crawler → common
  ├── occupation-analysis → common
  ├── occupation-report → common, analysis ←跨模块
  ├── occupation-recommend → common, analysis ←跨模块
  ├── occupation-api → common
  └── occupation-web-ui (Vue 3, 独立前端)
```

所有业务模块依赖 common，web 聚合全部模块。
**跨模块依赖**：report 和 recommend 依赖 analysis（通过 Service 接口调用）。

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
> 执行后可创建全部 16 张表 + 初始化种子数据。
> **2026-07-10 之后种子数据大改（职位/行为/投递全部重算），必须 `docker-compose down -v && docker-compose up -d`，增量脚本兜不住。**

### 已实现表（16 张，全部在 init.sql 中定义）

| 表名                   | 所属模块        | 说明                 | tenant_id |
| ---------------------- | --------------- | -------------------- | --------- |
| sys_tenant             | common          | 租户表               | ❌（自身不含） |
| sys_user               | auth            | 用户表               | ✅ |
| sys_student_profile    | recommend       | 学生画像表           | ✅ |
| student_resume         | recommend       | 学生简历表           | ✅ |
| crawler_task           | crawler         | 采集任务表           | ✅ |
| crawler_log            | crawler         | 采集日志表           | ✅ |
| raw_job_data           | crawler         | 原始职位数据表       | ❌（全平台共享） |
| job_detail             | analysis        | 清洗后职位表         | ❌（全平台共享） |
| analysis_result        | analysis        | 分析结果表           | ✅ |
| report_template        | report          | 报告模板表           | ✅ |
| report_record          | report          | 报告记录表           | ✅ |
| push_record            | recommend       | 推送记录表           | ✅ |
| student_behavior       | recommend       | 学生行为记录表（埋点）| ✅ |
| **job_application**    | recommend       | **投递记录表（业务实体）** | ✅ |
| sys_alert              | web/common      | 系统告警表           | ✅ |
| api_client             | api             | API 客户端鉴权表     | ✅ |

**`student_behavior` 与 `job_application` 的分工**（两张表都记录「投递」，语义完全不同）：

- `student_behavior` = **行为埋点**。`VIEW / FAVORITE / APPLY / IGNORE / CONTACT` 一视同仁，只记「谁在什么时候对哪个职位做了什么」。喂给推荐算法做行为加权，只增不改。
- `job_application` = **业务实体**。有状态机（`SUBMITTED → VIEWED → INTERVIEW → OFFER`，或任意非终态直接 `REJECTED`）、HR 备注、状态变更时间。

投递时后端**双写**：`RecommendController.apply()` 同时写两张表。这样推荐算法、行为统计、教师端明细全都零改动。

**职位归属决定行为合法性**（两条硬约束，种子生成器里有自检，违反直接抛异常）：

| 行为 | 只能落在 | 服务端守卫 |
|---|---|---|
| `APPLY` | 站内职位（`publisher_id` 非空） | `apply()` 拒绝无主职位 |
| `CONTACT` | 采集职位（`publisher_id` 为空） | `contact()` 拒绝站内职位 |
| `VIEW / FAVORITE / IGNORE` | 两类都可以 | 无 |

**画像与简历的分工**（两张表都挂在 recommend 模块，别搞混）：

- `sys_student_profile` = 喂给推荐算法的结构化**匹配依据**，字段扁平可索引（专业/技能/意向城市/期望薪资/学历）
- `student_resume` = 给 HR 和大模型读的**自我陈述**，教育/项目/实习三段经历以 JSON 数组存 TEXT 列。
  条目数不定、只读不查，拆子表只会徒增 join。**HTTP 层收发结构化数组**，序列化只在 `ResumeServiceImpl` 里发生

### 种子数据

| 表 | 数据 |
|---|---|
| sys_tenant | id=1, name="测试学院" |
| sys_user | id=1, username="admin", password="admin123"（BCrypt），role=ADMIN |
| student_resume | 12 份，覆盖租户1全部有画像的学生（userId 2、5~15） |
| job_application | 87 条，五种状态铺开（SUBMITTED 41 / VIEWED 12 / INTERVIEW 18 / OFFER 9 / REJECTED 7），与 student_behavior 的 87 条 APPLY 一一对应 |
| student_behavior | 414 条，覆盖 VIEW / FAVORITE / APPLY / IGNORE / CONTACT |
| job_detail | 186 个职位：162 个采集（publisher_id 为空）+ 24 个站内（6 个 HR 各 4 个，分属 3 家公司） |
| 边界用例 | `student12`(userId 16) 有账号、无画像、无简历，却投递了一个站内职位 —— 一次覆盖三个空态 |

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
| GET /api/analysis/dashboard | GET | Dashboard 分析数据（行业/城市/技能/学历/趋势） | P2 | ✅ |
| GET /api/analysis/jobs | GET | 职位分页查询（城市/行业/薪资/学历/经验/关键词） | P2 | ✅ |
| GET /api/map/recommendJobs | GET | 首页推荐职业列表 | P3 | ✅ |
| GET /api/map/getJobCityHeat | GET | 职业城市聚集度热力（gatherValue） | P3 | ✅ |
| POST /api/map/rebuildJobGather | POST | 管理员重算职业聚集度（Spark 流水线） | P3 | ✅ |

### 2026-07-10 新增：简历 + HR 解密 + AI 能力

| 接口 | 方法 | 说明 | 状态 |
|---|---|---|---|
| /api/student/resume | GET | 我的简历。未填写返回 `{exists:false, educations:[], ...}` 空壳，**不是 null** | ✅ |
| /api/student/resume | PUT | 保存简历。三段经历直接收结构化数组，前端不要 `JSON.stringify` | ✅ |
| /api/student/resume/ai-review | POST | AI 简历诊断。`?targetJobId=` 对标岗位，`?refresh=true` 强制重算 | ✅ |
| /api/student/resume/ai-polish | POST | AI 润色一段文字，返回 `{polished}` | ✅ |
| /api/student/advisor/chat | POST | AI 职业顾问对话（服务端无状态，前端回传完整历史） | ✅ |
| /api/student/advisor/explain/{jobId} | POST | 自然语言解读「为什么推荐这个职位」 | ✅ |
| /api/hr/applicants/{userId} | GET | 投递人详情：姓名/联系方式/画像/简历。**带归属校验，非本人职位的投递者返回 403** | ✅ |
| /api/teacher/suggestions/ai | GET | 教学建议的 AI 解读（原 `/api/teacher/suggestions` 不变） | ✅ |

`/api/hr/applications` 返回值**新增**（未删除任何字段）：`userId` / `realName` / `hasResume`。
联系方式刻意不在列表里 —— 列表页不该批量泄露联系方式，要看得单独调 `/api/hr/applicants/{userId}`。

### 2026-07-10 第二轮：幽灵职位 + 投递状态机 + 就业分析 + 爬虫合规

| 接口 | 方法 | 说明 | 状态 |
|---|---|---|---|
| /api/student/job/{id}/contact | POST | 自主联系（对采集职位表达求职意向）。返回 `{sourceUrl, company}`，MOCK 数据的 `sourceUrl` 为 null | ✅ |
| /api/student/applications | GET | 我的投递 + HR 处理进度。**不含 `hrNote`**（HR 内部备注） | ✅ |
| /api/hr/applications/{id}/status | PUT | HR 变更投递状态。带归属校验 + 状态流转合法性校验 | ✅ |
| /api/analysis/employment | GET | 就业分析：投递漏斗 / 供需错配 / 自主求职流向。**限 ADMIN + TEACHER** | ✅ |

`JobDetailVO` 新增派生字段 **`applicable`**（= `publisherId != null`）。它不落库 —— 归属关系的唯一真相是 `publisher_id`，多存一列就多一处可能不同步的地方。前端据此决定显示「投递简历」还是「自主联系」。

`/api/hr/applications` 改读 `job_application`（业务实体），响应**只加字段**：`applicationId` / `status` / `statusLabel` / `terminal` / `hrNote` / `statusChangedAt`。

`/api/teacher/overview` 新增 `totalContacts`；`BehaviorStatsVO` / `StudentVO` / `TalentVO` 新增 `contactCount`。

**基线保证**：改造前后用脚本对 19 个接口做了响应结构快照比对，结果是**只有新增字段，零字段缺失**。

### analysis_result 的新维度（就业分析）

`analysis_result` 表结构 `(dimension, dimension_value, metric_name, metric_value, period)` 足够通用，新增维度直接往里写，**不建新表**：

| dimension | dimension_value | metric_name |
|---|---|---|
| `apply_funnel` | SUBMITTED / VIEWED / INTERVIEW / OFFER / REJECTED / TOTAL | `application_count` |
| `apply_response` | responded / unresponded / median_hours | `application_count` / `hours` |
| `student_city` / `student_industry` / `student_salary` | 城市 / 行业 / 薪资桶 | `student_count` |
| `gap_city` | 城市 | `student_ratio` / `job_ratio` / `gap_ratio` |
| `gap_salary` | overall | `student_median` / `market_median` / `deviation_percent` |
| `contact_city` / `contact_industry` | 城市 / 行业 | `contact_count` |

**架构约束**：这些维度依赖学生画像、行为、投递三张表，它们都在 `recommend` 模块里，而 `recommend` 依赖 `analysis` —— 反过来依赖会成环。解法是 SPI：`analysis` 定义 `AnalysisContributor` 扩展点，`recommend` 的 `EmploymentAnalysisContributor` 实现它，`AnalysisJobService.runAll()` 用 `List<AnalysisContributor>` 注入并在内置维度之后调用（供需错配要读刚写进去的 city 岗位分布）。

**漏斗只统计 `job_application`**，不碰 `student_behavior` 的 `APPLY`。后者可能含历史上投在无主职位的「幽灵投递」，它们连被查看的可能都没有，混进转化率只会让数字难看，而这个难看不反映任何真实问题。

**AI 能力的统一约定**：所有 AI 接口在大模型不可用时都降级为规则化输出，并在响应里带
`aiGenerated: false` 让前端如实告知用户。唯一例外是 `ai-polish` —— 润色没有合理的规则降级
（拿什么改写？），直接抛业务异常。

---

## 六、部署组件状态

| 组件       | 版本   | 用途 | Docker 定义 |
| ---------- | ------ | ---- | ----------- |
| MySQL      | 8.0    | 业务主库 + 分析结果 + 种子数据 | ✅ 开发/生产 |
| Redis      | 6.2    | 缓存、开放 API Token、限流 | ✅ 开发/生产 |
| Kafka      | 7.5.0  | 采集消息链路 raw/cleaned job data | ✅ 开发/生产 |
| Zookeeper  | 7.5.0  | Kafka 协调 | ✅ 开发/生产 |
| Nginx      | 1.24   | 前端静态资源 + API 反向代理 | ✅ 开发/生产 |
| Spring Boot App | JDK 11 | 后端聚合服务 | ✅ 生产镜像自动 Maven 构建 |
| Vue 前端 | Node 20 构建 | 四角色前端页面 | ✅ 生产镜像自动 npm 构建 |
| HDFS/Hive/HBase/Neo4j/ES | 预留 | 当前非必需，避免本地和云服务器部署过重 | ❌ 暂不启用 |

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
| 2026-07-07 | P1 Step 1.7 完成：WebMagic 爬虫框架集成 — JobPageProcessor 抽象基类（UA 池 + 随机延迟 + 重试）+ JobPipeline（Kafka 管道）+ MockJobPageProcessor（本地 JSON 模拟）+ 多行业测试数据 | Step 1.7 |
| 2026-07-07 | P1 Step 1.8 完成：BossJobPageProcessor 真实采集器 — 列表页解析（标题/公司/薪资/城市/学历/经验）+ 详情页解析（描述/技能标签）+ 分页翻页 + 反爬策略（5-15s 随机延迟）| Step 1.8 |
| 2026-07-07 | P1 Step 1.9 完成：采集任务管理 API（CRUD + 启停 + 日志查询）+ XXL-Job 调度集成（定时扫描/手动触发/全停）| Step 1.9 |
| 2026-07-07 | 骨架代码完成：8 Entity + 8 Mapper + 2 跨模块 Service 接口 + 4 DTO/VO + POM 依赖更新 + api_client 表 + Vue 3 脚手架 | 骨架 |
| 2026-07-07 | P2 A组完成：AnalysisServiceImpl + JobDetailServiceImpl + AnalysisController（Dashboard + 职位查询 API） | P2 |
| 2026-07-07 | **深度优化轮**：①基础设施修补（common 补 Redis/Security 依赖、JWT 白名单放行 /api/open+文档、@PreAuthorize 启用、UserContextHolder、PageResult、XXL-Job 条件装配、HealthControllerTest 修复）②analysis：JobDataCleanListener(Kafka 双消费组)+DataCleanService(清洗规则)+AnalysisJobService(5维度统计)+AnalysisScheduler+/rebuild+saveJob/getJobById/removeJob ③report：模板 CRUD+生成引擎+AiSummaryService(LLM+降级)+Pdf/WordExporter+下载 ④recommend：画像+JobMatchService(40/25/20/15打分)+推送+行为闭环+教师/HR 接口+RecommendScheduler ⑤api：OpenAuthService(Redis Token)+双拦截器(鉴权/限流)+OpenDataService ⑥auth：UserController 用户管理 ⑦新增 docs/项目开发说明书.md（权威指南）。全模块 mvn compile + 测试通过 | 深度优化 |
### 2026-07-13 变更补充

- 管理员采集新增 `OFFICIAL_PUBLIC` 官方公开招聘公告来源，复用 `crawler_task.url_pattern` 存储 `url=...&maxItems=...`，不新增数据库表结构。
- `OFFICIAL_PUBLIC` 沿用现有 `robots.txt` 校验、单线程低频抓取、Kafka -> raw_job_data -> job_detail 清洗链路；仅解析公开列表标题和来源 URL，不抓取联系方式、附件或登录后内容。
- 修复真实采集器解析出的岗位未写入 WebMagic `ResultItems` 的问题，确保 `JobPipeline` 能收到并发送到 Kafka。
- HR 端 JD 优化从独立工具页迁移到职位管理发布/编辑弹窗，删除独立前端路由和页面，保留规则分析能力为 `occupation-web-ui/src/utils/jdOptimizer.js`。

### 2026-07-14 变更补充

- Mock 采集源调整为 120 条多行业职位；初始化种子控制在 186 个职位、414 条行为、87 条投递记录，兼顾分析丰富度与运行速度。
- 真实采集任务改为事务提交后异步启动，并在 Spider 完成后自动回写 `crawler_log.end_time / record_count / status`，避免日志长期停留在 `RUNNING + 0`。
- 外部资讯默认源从 Google News 改为开源中国 RSS，并在关键词无命中时回退最新资讯。
- 生产部署改为后端/前端 Docker 镜像内自动构建；新增 `.dockerignore`、`Dockerfile.frontend`、`scripts/reset-dev-db.ps1` 和 `docs/部署与同步说明.md`，降低 GitHub 同步和云服务器部署误差。
- 重新划清"数据采集"和"行业资讯"边界：数据采集任务默认只保留 MOCK 与 `OFFICIAL_PUBLIC` 岗位/公告采集，用于行业热度、城市分布等数据面板；行业资讯由 recommend/news 模块的 `/api/admin/news/pull-rss` 拉取并通过 `/api/news` 展示给四类角色。
- 行业资讯增加空库基础资讯、分类兜底和 RSS 自动分类，避免某个方向筛选无数据时四端首页资讯卡片显示空白。
### 2026-07-14 采集稳定性补充

- `OFFICIAL_PUBLIC` 管理端"立即采集一次"改为同步直采链路：短超时访问公开岗位页，解析后同时写入 `raw_job_data` 并调用 `DataCleanService` 直接清洗到 `job_detail`，不再依赖 WebMagic 异步状态轮询或 Kafka 消费完成度来判断本次任务是否结束。
- `OfficialPublicDirectCollector` 新增自动翻页功能：每次采集自动跟随"下一页"链接，支持 4 种翻页模式（rel=next、class=next、文本"下一页"、pageNo 自增），最大 50 页，翻页间隔 1.5~3.5s 随机延迟，`visitedUrls` 去重防循环。
### 2026-07-14 AI 能力全面升级

- **AiProperties.maxTokens** 从 1200 提升到 3000（全局默认），支持更长的分析输出。
- **新增 SemanticMatchService**（JD-简历语义匹配）：调用 DeepSeek 做 JD 文本与简历文本的语义相似度计算，弥补字符串匹配中 "Java" 和 "Spring Boot" 被视为无关技能的问题。结果缓存 Redis 24 小时，AI 不可用时降级返回空结果。
- **JobMatchServiceImpl 升级**：在原有四维打分（技能40+城市25+薪资20+学历15）基础上增加语义匹配维度（+15分），总分裁剪回 0~100。
- **AiSummaryServiceImpl 升级**：报告摘要从 300 字升级到 600-800 字，按四大板块组织（整体趋势→行业城市热点→技能需求变化→建议），增加薪资分布和月度趋势数据注入。
- **ResumeAiServiceImpl + ResumeReviewVO 升级**：summary 80-120字，strengths/weaknesses 3-5条各50字，suggestions 4-6条增加 priority 和 expectedEffect 字段，新增 marketCompetitiveness 竞争力评估。
- **StudentAiReportServiceImpl 升级**：报告结构从 3 板块升级到 6 大板块（个人画像→市场定位→技能竞争力→薪资合理性→3条求职路线→90天行动计划），温度从 0.7 调为 0.5，增加行业薪资参考数据注入。
- **TeachingAiServiceImpl 升级**：从 350 字升级到 600-800 字，按五大板块组织（整体诊断→3个技能缺口→课程调整→校企合作→预期效果与评估指标）。
- **CareerAdvisorServiceImpl 升级**：字数从 300 升级到 500，新增 2 条行为准则（回答结构要求、学习路径要求），上下文注入匹配分最高的 3 个职位及缺失技能。

### 2026-07-14 推荐算法升级：协同过滤 + 内容推荐

- **新增 ContentBasedRecommendService**（基于内容的推荐）：TF-IDF 技能向量化 + 余弦相似度计算职位间相似度。支持 `similarJobs(jobId, topN)`（看了这个职位的人还看了...）和 `recommendByUserHistory(userId, topN)`（基于用户投递/收藏历史推荐技能相似的职位）。
- **新增 CollaborativeFilterService**（User-Based 协同过滤）：基于学生 APPLY/FAVORITE 行为构建用户-职位交互矩阵，余弦相似度找 Top K 邻居，聚合邻居交互过的职位按相似度加权推荐。冷启动用户自动跳过。
- **新增 HybridRecommendService**（混合推荐引擎）：融合三种策略 — 规则打分+语义匹配（70%）+ 协同过滤（15%）+ 内容推荐（15%），加权合并去重后返回 Top N。
- **RecommendController 升级**：推荐入口从 `JobMatchService` 切换到 `HybridRecommendService`，保留 `JobMatchService.scoreOne()` 供单职位打分使用。

## 变更记录 — 2026-07-14 报告 Markdown 渲染 + 报告截断修复

- 新增 occupation-report/src/main/java/com/occupation/report/export/MarkdownRenderer.java：flexmark-all 封装的 Markdown→XHTML 渲染器（含表格扩展、软换行、think 标签清洗）。
- 改造 StudentAiReportServiceImpl.renderHtml()：调用 MarkdownRenderer 把 AI 输出（/####//* 列表 等）转成合规 XHTML，再嵌入带样式的 HTML 模板（h2 蓝竖条/列表/表格/引用块）。
- AiChatClient 新增重载 chat(messages, temperature, maxTokens)：学生报告调用时显式传 4000 token（全局默认 3000 经常截断六大板块）。
- StudentAiReportServiceImpl.ROLE prompt 追加输出格式硬性要求：Markdown 二级标题/三级、四级/严禁代码块/严禁 <think>/整篇 1800-2400 字/截断必须在完整句子处。
- 前端 occupation-web-ui/src/views/student/Reports.vue 新增 marked 依赖（12.0.2），把 textarea 改为「预览 / 编辑」双模式（默认预览），预览区 CSS 与 PDF 导出样式对齐。
- 根 pom 与 occupation-report/pom.xml 增加 lexmark.version=0.64.8 依赖版本锁定。
