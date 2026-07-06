# 职业能力大数据服务平台 — 实施计划

> **版本**: v1.0  
> **更新日期**: 2026-07-06  
> **基于**: design-document.md v0.2 + tech-stack.md v1.0  
> **原则**: 每个步骤小而具体，必须包含可执行的验证方法

---

## 阶段总览

| 阶段 | 内容                         | 步骤数 | 预计产出                                     |
| ---- | ---------------------------- | ------ | -------------------------------------------- |
| P1   | 项目骨架 + 数据采集模块       | 9 步   | 可运行的后端 + 爬虫集群，数据入 Kafka + HDFS |
| P2   | 大数据清洗 + 基础分析报表     | 8 步   | Spark 分析任务跑通，MySQL 可查到分析结果      |
| P3   | 报告生成 + 管理后台           | 7 步   | 报告模板配置、PDF/Word 导出、管理员界面       |
| P4   | 岗位推送 + 学生端             | 6 步   | 个性化推荐上线，学生端可见                    |
| P5   | 对外 API + 教师端 + 企业端    | 6 步   | 完整四端 + API 文档 + 监控                   |

---

## P1：项目骨架 + 数据采集模块

### Step 1.1 — 初始化 Maven 多模块项目结构

**指令**:
- 创建父 POM `occupation-platform`（`pom`），SpringBoot 2.7.x 父依赖。
- 创建 7 个子模块：
  - `occupation-common`（公共类：统一响应、工具类、异常定义、多租户拦截器）
  - `occupation-auth`（认证授权模块）
  - `occupation-crawler`（采集管理模块）
  - `occupation-analysis`（分析服务模块）
  - `occupation-report`（报告生成模块）
  - `occupation-recommend`（推荐推送模块）
  - `occupation-api`（对外开放 API 模块）
- 创建启动模块 `occupation-web`，依赖以上全部子模块，包含 `Application.java` 入口类。
- 在根目录创建 `docker-compose.yml`，包含 MySQL、Redis、Kafka、Nginx 服务定义。

**验证**:
- `mvn clean compile` 全部模块编译通过。
- `docker-compose up -d mysql redis kafka nginx` 四个容器正常运行。

---

### Step 1.2 — 搭建 SpringBoot 基础骨架与统一响应

**指令**:
- 在 `occupation-web` 中配置 `application.yml`：MySQL 连接、Redis 连接、Kafka 连接、server.port=8080。
- 在 `occupation-common` 中定义：
  - `Result<T>` 统一响应类（code、message、data）。
  - `BizException` 业务异常类。
  - `GlobalExceptionHandler` 全局异常处理器（`@RestControllerAdvice`）。
- 在 `occupation-web` 中创建 `HealthController`，提供 `GET /api/health` 接口返回 `Result.ok("ok")`。

**验证**:
- 启动应用，`GET http://localhost:8080/api/health` 返回 `{"code":200,"message":"success","data":"ok"}`。
- 抛出一个 `BizException`，返回统一错误格式。

---

### Step 1.3 — 数据库表设计（核心业务表）

**指令**:
- 在 `occupation-common` 的 `sql/` 目录下创建 `init.sql`。
- 设计并创建以下表（所有业务表必须包含 `tenant_id` 字段）：
  - `sys_tenant`（租户表：id, name, status, created_at）
  - `sys_user`（用户表：id, tenant_id, username, password_hash, role[STUDENT/TEACHER/ADMIN/HR], real_name, phone, email, status）
  - `sys_student_profile`（学生画像表：id, user_id, major, skills, expected_city, expected_industry, expected_salary_min, expected_salary_max, education_level）
  - `crawler_task`（采集任务表：id, tenant_id, source_type, source_name, url_pattern, cron_expr, status）
  - `crawler_log`（采集日志表：id, task_id, start_time, end_time, record_count, status）
  - `raw_job_data`（原始职位数据表：id, source, source_url, raw_content[text], fetch_time, status[RAW/CLEANED]）
  - `job_detail`（清洗后职位表：id, title, company, city, industry, salary_min, salary_max, education, experience, skills, description, publish_date, source, source_url）
  - `analysis_result`（分析结果表：id, tenant_id, dimension, dimension_value, metric_name, metric_value, period_type, period_value, calc_time）

**验证**:
- SQL 在 MySQL 8.0 中执行成功，所有表创建完毕。
- 使用 `SHOW CREATE TABLE` 确认每张业务表包含 `tenant_id` 列。

---

### Step 1.4 — 实现 MyBatis-Plus 基础配置 + 多租户插件

**指令**:
- 在 `occupation-common` 中配置 MyBatis-Plus：
  - 配置分页插件 `PaginationInnerInterceptor`。
  - **配置多租户插件** `TenantLineInnerInterceptor`：从当前请求上下文获取 `tenant_id`，自动注入到 SQL WHERE 条件。
  - 配置 `TenantContextHolder`（ThreadLocal 存储当前租户 ID）。
- 创建 `BaseEntity` 父类（id, tenant_id, created_at, updated_at），所有实体类继承它。
- 创建 `sys_tenant` 和 `sys_user` 的 Entity + Mapper + Service。
- 在 `occupation-auth` 模块中创建 `TenantService` 和 `UserService`。

**验证**:
- 插入一条 `sys_tenant` 记录（tenant_id=1，name="测试学院"）。
- 插入一条 `sys_user` 记录（tenant_id=1，role=ADMIN）。
- 不设置 `TenantContextHolder` 时查询用户，返回空。
- 设置 `TenantContextHolder.setTenantId("1")` 后查询，正确返回该用户。

---

### Step 1.5 — JWT 认证 + 登录接口

**指令**:
- 在 `occupation-auth` 中实现：
  - `POST /api/auth/login`：接收 username + password → 验证 → 生成 JWT Token（含 userId、tenantId、role）。
  - `JwtUtil` 工具类：签发、校验、解析 Token。
  - `JwtAuthenticationFilter`：拦截所有 `/api/**` 请求（除 `/api/auth/login` 和 `/api/health`），校验 Token → 设置 `TenantContextHolder`。
- 密码使用 BCrypt 加密存储。
- 前端登录页暂时用一个简单的 HTML 表单（后续在 P3 替换为 Vue 页面）。

**验证**:
- `POST /api/auth/login` 传入正确用户名密码 → 返回 `{"code":200,"data":{"token":"xxx"}}`。
- `GET /api/health` 无 Token → 返回 200（白名单放行）。
- `GET /api/xxx` 无 Token → 返回 401 Unauthorized。
- 带正确 Token 请求 → `TenantContextHolder` 自动设置，后续数据库查询自动带 `tenant_id` 过滤。

---

### Step 1.6 — Kafka 生产者/消费者基础配置

**指令**:
- 在 `occupation-common` 中配置 Kafka：
  - `KafkaProducerConfig`：生产者配置（topic: `raw-job-data`）。
  - `KafkaConsumerConfig`：消费者配置（group-id: `data-cleaner-group`）。
  - 定义消息体 `JobDataMessage`（source, sourceUrl, rawContent, fetchTime）。
- 实现 `KafkaProducerService.send(JobDataMessage)`。
- 实现 `KafkaConsumerService`：消费消息 → 存入 `raw_job_data` 表（status=RAW）。

**验证**:
- 启动 Kafka（docker-compose）。
- 发送一条测试消息到 `raw-job-data` topic。
- 消费者收到消息后，`raw_job_data` 表中出现一条记录（status=RAW）。

---

### Step 1.7 — 爬虫基础框架（WebMagic 集成）

**指令**:
- 在 `occupation-crawler` 中集成 WebMagic：
  - 定义 `JobPageProcessor` 抽象类（子类实现页面解析逻辑）。
  - 定义 `JobPipeline`：解析到的职位数据 → 构造 `JobDataMessage` → 通过 `KafkaProducerService` 发送到 Kafka。
  - 定义 `CrawlerService`：根据 `crawler_task` 表配置创建和启停爬虫实例。
- 先实现一个简单的**模拟爬虫**（从本地 JSON 文件读取职位数据，模拟采集流程），验证整条链路通顺。

**验证**:
- 启动模拟爬虫任务 → Kafka topic `raw-job-data` 收到消息 → `raw_job_data` 表中出现记录。
- 通过 `crawler_task` 表配置任务（status=ON）→ `CrawlerService` 自动启动对应爬虫。

---

### Step 1.8 — 实现一个真实采集源（BOSS 直聘 / 智联招聘）

**指令**:
- 选择其中一个招聘平台（如 BOSS 直聘），实现 `BossJobPageProcessor`：
  - 解析列表页：职位标题、公司名、薪资、城市、学历要求、经验要求。
  - 解析详情页：职位描述、技能标签。
  - 处理分页翻页逻辑。
- 配置反爬策略：
  - User-Agent 池随机轮换。
  - 请求间隔随机化（3-8 秒）。
  - 失败重试 3 次。
- 爬取结果写入 Kafka。

**验证**:
- 启动爬虫 → Kafka topic 持续收到消息。
- 抽样检查 `raw_job_data` 表：标题、公司、薪资、技能标签字段不为空。
- 爬虫连续运行 5 分钟不崩溃，无 IP 封禁报错。

---

### Step 1.9 — 采集任务管理接口 + 调度集成

**指令**:
- 在 `occupation-crawler` 中实现采集管理 API：
  - `POST /api/admin/crawler/task`：创建采集任务（配置采集源、URL、cron 表达式）。
  - `PUT /api/admin/crawler/task/{id}/start`：手动启动任务。
  - `PUT /api/admin/crawler/task/{id}/stop`：停止任务。
  - `GET /api/admin/crawler/task`：任务列表（分页）。
  - `GET /api/admin/crawler/log`：采集日志（分页）。
- 集成 XXL-Job 调度器：
  - 创建 `CrawlerJobHandler`，定时扫描 `crawler_task` 表中 status=ON 且到达执行时间的任务，启动对应爬虫。
- 创建 `crawler_task` 的 Mapper + Service。

**验证**:
- 通过 API 创建一个采集任务（cron="0 0 2 * * ?"）→ `crawler_task` 表中新增记录。
- 手动调用 start → 爬虫启动，`crawler_log` 表新增日志记录。
- 调用 stop → 爬虫停止，日志记录结束时间。
- XXL-Job 调度面板中看到 `CrawlerJobHandler` 已注册，可手动触发。

---

> **P1 里程碑**：后端骨架 + 认证 + 多租户 + Kafka 链路 + 爬虫采集 全部可运行。  
> **P1 集成测试**：创建一个租户 → 创建管理员 → 登录 → 创建采集任务 → 启动爬虫 → Kafka 收到消息 → raw_job_data 表有数据。

---

## P2：大数据清洗 + 基础分析报表

### Step 2.1 — Docker 部署 Hadoop 基础集群

**指令**:
- 完善 `docker-compose.yml`，添加 HDFS（NameNode + DataNode）、Hive（Metastore + Server）、HBase（Master + RegionServer）。
- 实现 `HdfsClient` 工具类（`occupation-common`）：上传/下载/列出文件。
- 实现 `HiveClient` 工具类：执行 HiveQL 查询。

**验证**:
- `docker-compose up -d` 全部大数据组件正常启动。
- 通过 HDFS Web UI（默认 9870 端口）看到 DataNode 在线。
- `HdfsClient` 上传一个测试文件到 `/test/` 路径 → HDFS 上可见。
- `HiveClient` 执行 `SHOW DATABASES;` 返回 default 库。

---

### Step 2.2 — 数据清洗管道（Kafka → HDFS → Hive）

**指令**:
- 在 `occupation-analysis` 中创建 `DataCleanJob`（Spark 批处理任务）：
  - 从 HDFS 读取原始数据（上一阶段 `raw_job_data` 导出到 HDFS 的文件）。
  - 清洗逻辑：去重（按 source_url MD5）、字段标准化（薪资范围转为数值、城市名统一、学历映射枚举）、技能标签提取。
  - 清洗结果写入 Hive 分区表 `dwd_job_detail`（按 `dt` 日期分区）。
- 更新 `KafkaConsumerService`：消费 Kafka 消息后，除了写入 MySQL `raw_job_data`，同时追加写入 HDFS 文件（按小时滚动）。

**验证**:
- Spark 任务执行成功 → Hive 表 `dwd_job_detail` 有数据。
- `SELECT COUNT(*) FROM dwd_job_detail WHERE dt='2026-07-06'` 返回 > 0。
- 检查清洗质量：城市为空或薪资为负的记录数为 0。

---

### Step 2.3 — HBase 职位明细存储

**指令**:
- 在 `occupation-analysis` 中创建 `HBaseWriter`：
  - 将清洗后的职位数据写入 HBase 表 `job_detail`（RowKey = MD5(source_url)，列族 `info`）。
  - 支持按职位 ID 单条查询、按城市+行业范围扫描。
- 在 `Step 2.2` 的清洗管道末尾追加 HBase 写入步骤。

**验证**:
- HBase Shell 中 `scan 'job_detail'` 返回清洗后的职位记录。
- `get 'job_detail', '<rowkey>'` 返回完整的职位信息。

---

### Step 2.4 — Spark 离线分析 Job（行业维度）

**指令**:
- 在 `occupation-analysis` 中创建 `IndustryAnalysisJob`：
  - 读取 Hive `dwd_job_detail` 表（指定日期分区）。
  - 按行业分组统计：岗位数量、平均薪资最低值、平均薪资最高值、企业数量。
  - 结果写入 MySQL `analysis_result` 表（dimension='industry', dimension_value=行业名）。
- 创建对应的 `AnalysisResult` Entity + Mapper + Service。

**验证**:
- 执行 Spark Job → MySQL `analysis_result` 表有行业维度的分析数据。
- `SELECT * FROM analysis_result WHERE dimension='industry'` 返回多条记录。
- 与 Hive 原始数据人工抽样对比：岗位数量、平均薪资 误差 < 1%。

---

### Step 2.5 — Spark 分析 Job（地域、学历、技能维度）

**指令**:
- 依次创建：
  - `CityAnalysisJob`：按城市统计岗位数量和平均薪资（dimension='city'）。
  - `EducationAnalysisJob`：按学历要求统计岗位占比（dimension='education'）。
  - `SkillHotAnalysisJob`：统计技能标签出现频次 Top 100（dimension='skill'）。
- 每个 Job 独立类、独立 main 方法，可单独提交到 Yarn。
- 全部写入 MySQL `analysis_result` 表。

**验证**:
- 三个 Job 分别执行后，`analysis_result` 表中三个维度均有数据。
- Skill 维度：按 metric_value DESC 排序 → 看到 Java、Python 等热门技能在 Top 20。

---

### Step 2.6 — 时间趋势分析 Job

**指令**:
- 创建 `TimeTrendAnalysisJob`：
  - 按周/月聚合岗位数量和平均薪资变化。
  - 写入 `analysis_result` 表（dimension='trend', period_type='WEEK'/'MONTH'）。
- 创建 `AnalysisDashboardService`：
  - 提供 `GET /api/analysis/dashboard`：返回各维度的最新分析数据（从 MySQL `analysis_result` + Redis 缓存）。
  - Redis 缓存 key 前缀 `analysis:dashboard:{tenant_id}`，TTL 1 小时。

**验证**:
- 执行 Job → `analysis_result` 表中 trend 维度有 3+ 个时间点的数据。
- `GET /api/analysis/dashboard` 返回 JSON，包含 industry、city、education、skill、trend 五个维度的 top 数据。
- 首次请求命中 MySQL，第二次请求命中 Redis（响应时间 < 50ms）。

---

### Step 2.7 — 分析任务定时调度

**指令**:
- 在 XXL-Job 中注册以下任务处理器：
  - `IndustryAnalysisJobHandler`：每日凌晨 3:00 执行 `IndustryAnalysisJob`。
  - `CityAnalysisJobHandler`：每日凌晨 3:30 执行。
  - `EducationAnalysisJobHandler`：每日凌晨 4:00 执行。
  - `SkillHotAnalysisJobHandler`：每日凌晨 4:30 执行。
  - `TimeTrendAnalysisJobHandler`：每周一凌晨 5:00 执行。
- 每个 Handler 通过 `spark-submit` 或 Spark Launcher API 提交任务到 Yarn。

**验证**:
- XXL-Job 面板中 5 个 Handler 均已注册。
- 手动触发一个 Handler → Yarn 上看到 Spark Application 提交成功 → MySQL 数据更新 → Redis 缓存失效。

---

### Step 2.8 — 职位查询 API

**指令**:
- 在 `occupation-api` 中实现：
  - `GET /api/jobs`：多条件职位查询（参数：keyword、city、industry、salary_min、education、page、size）。
  - 默认从 HBase 按条件扫描，结果按发布时间倒序。
  - `GET /api/jobs/{id}`：职位详情（从 HBase 按 RowKey 精确查询）。

**验证**:
- `GET /api/jobs?keyword=Java&city=北京&page=1&size=10` 返回 10 条北京地区的 Java 职位。
- `GET /api/jobs/{id}` 返回完整职位信息（含职位描述全文）。

---

> **P2 里程碑**: 数据清洗管道 + 5 维度分析 + 定时调度 + 职位查询 API 全部可运行。  
> **P2 集成测试**: 爬虫采集 → Kafka → HDFS → Spark 清洗 → Hive 入库 → 5 个分析 Job 执行 → MySQL 分析结果表有数据 → API 返回分析数据。

---

## P3：报告生成 + 管理后台

### Step 3.1 — Vue 3 前端项目初始化

**指令**:
- 使用 Vite 创建 Vue 3 项目：`occupation-web-ui`，放在项目根目录。
- 安装依赖：Element Plus、ECharts、Axios、Pinia、Vue Router。
- 配置路由：
  - `/login` → 登录页
  - `/admin/*` → 管理后台（嵌套路由）
  - `/student/*` → 学生端（嵌套路由）
- 配置 Axios 拦截器：请求前注入 Token，响应拦截 401 跳转登录页。
- 实现 `LoginView.vue`：用户名 + 密码表单，调用 `/api/auth/login`，存储 Token 到 localStorage。

**验证**:
- `npm run dev` 启动前端 → 访问 `http://localhost:5173/login` 显示登录页。
- 输入正确账号密码 → 登录成功跳转到 `/admin/dashboard`。
- 输入错误密码 → 显示错误提示。
- Token 失效后访问任何接口 → 自动跳回登录页。

---

### Step 3.2 — 管理后台布局 + 采集管理页面

**指令**:
- 实现 `AdminLayout.vue`：侧边栏菜单（采集管理、分析报告、报告模板、用户管理、系统配置）+ 顶部导航栏（用户名、退出登录）。
- 实现 `CrawlerTaskPage.vue`：
  - 采集任务列表（表格 + 分页），显示任务名、采集源、cron 表达式、状态。
  - 新增/编辑任务对话框。
  - 启动/停止按钮。
  - 采集日志子页面（按任务筛选）。

**验证**:
- 登录管理后台 → 看到侧边栏菜单。
- 采集管理页面 → 表格展示 `crawler_task` 数据。
- 点击"新增"→ 填写表单 → 提交 → 列表中新增一条。
- 点击"启动"→ 任务状态变为运行中。

---

### Step 3.3 — 分析数据看板页面

**指令**:
- 实现 `AnalysisDashboard.vue`（管理后台）：
  - 调用 `/api/analysis/dashboard` 获取数据。
  - 使用 ECharts 渲染：
    - **行业岗位数量柱状图**（Top 10 行业）。
    - **城市热力图**（全国各城市岗位数量分布）。
    - **技能词云**（热门技能 Top 50）。
    - **学历要求饼图**（学历占比分布）。
    - **薪资趋势折线图**（近 12 周平均薪资变化）。
  - 支持按行业/城市/时间范围下拉筛选。

**验证**:
- 页面加载 → 5 个图表全部正常渲染，数据不为空。
- 切换筛选条件 → 图表数据联动更新。
- 图表支持 hover 显示详细数据标签。

---

### Step 3.4 — 报告模板系统

**指令**:
- 在 `occupation-report` 中实现：
  - 数据库表 `report_template`：id, tenant_id, name, industry, type[MONTHLY/QUARTERLY/YEARLY], template_content[LONGTEXT/JSON], status。
  - `POST /api/admin/report/template`：创建/编辑模板。
  - `GET /api/admin/report/template`：模板列表。
  - 模板内容使用 JSON 描述报告结构（章节列表、每章节的分析维度、图表类型）。
- 管理后台页面 `ReportTemplatePage.vue`：模板列表 + 编辑器（JSON 编辑或拖拽式）。

**验证**:
- 创建一个月度就业报告模板 → MySQL 中新增一条记录。
- 模板 JSON 包含：标题、行业分析章节（柱状图）、技能分析章节（词云）、薪资分析章节（折线图）。

---

### Step 3.5 — 报告自动生成引擎

**指令**:
- 在 `occupation-report` 中实现 `ReportGeneratorService`：
  - 输入：模板 ID + 报告参数（月份、行业等）。
  - 流程：读取模板 JSON → 按章节调用 `analysis_result` 表查询数据 → 生成图表数据 → 渲染为 HTML。
  - 使用 Freemarker 模板引擎渲染报告 HTML。
- 实现 `PdfExporter`：HTML → PDF（使用 Flying Saucer 或 wkhtmltopdf）。
- 实现 `WordExporter`：HTML → Word（使用 POI）。
- 数据库表 `report_record`：id, tenant_id, template_id, params, file_url, file_type, status, created_at。

**验证**:
- 调用报告生成接口 → `report_record` 表中新增记录。
- 生成完成后，file_url 指向有效的 PDF 文件。
- 下载 PDF → 内容包含图表、标题、数据表格，格式正确。
- 同样测试 Word 导出。

---

### Step 3.6 — 报告管理页面

**指令**:
- 后端 API：
  - `POST /api/admin/report/generate`：手动触发生成。
  - `GET /api/admin/report/records`：报告列表（按模板/时间筛选）。
  - `GET /api/admin/report/download/{id}`：下载报告文件。
  - `DELETE /api/admin/report/record/{id}`：删除归档。
- XXL-Job 定时任务：每月 1 号自动生成所有启用的月度报告模板。
- 管理后台页面 `ReportListPage.vue`：报告列表 + 生成按钮 + 下载按钮。

**验证**:
- 点击"生成报告"→ 列表新增一条记录，状态变为"已生成"。
- 点击"下载"→ 浏览器下载 PDF/Word 文件。
- 模拟定时任务触发 → 多份报告同时生成。

---

### Step 3.7 — 用户管理页面

**指令**:
- 后端 API：
  - `GET /api/admin/users`：用户列表（分页，按角色筛选）。
  - `POST /api/admin/users`：新增用户（含角色选择、初始化密码）。
  - `PUT /api/admin/users/{id}`：编辑用户信息。
  - `PUT /api/admin/users/{id}/status`：启用/禁用用户。
  - `POST /api/admin/users/batch-import`：批量导入（Excel 上传）。
- 管理后台页面 `UserManagePage.vue`：用户表格 + 新增/编辑/禁用 + 批量导入。

**验证**:
- 新增一个学生用户 → 用户表中新增记录，tenant_id 自动填充。
- 禁用用户 → 该用户 Token 下次请求返回 401。
- 上传 Excel → 批量创建 10 个用户。

---

> **P3 里程碑**: 前端管理后台 + 分析看板 + 报告模板 + 报告生成/导出 + 用户管理 全部可运行。  
> **P3 集成测试**: 登录 → 看板展示数据 → 创建模板 → 生成报告 → 下载 PDF → 内容完整。

---

## P4：岗位推送 + 学生端

### Step 4.1 — 学生画像构建

**指令**:
- 在 `occupation-recommend` 中实现：
  - `POST /api/student/profile`：创建/更新个人画像（专业、技能列表、意向城市、意向行业、期望薪资范围、学历）。
  - `GET /api/student/profile`：查看自己的画像。
- 学生端页面 `StudentProfile.vue`：画像表单（多选技能标签、城市选择器、薪资滑块）。

**验证**:
- 学生登录 → 填写画像 → `sys_student_profile` 表新增/更新记录。
- 再次查看 → 表单回显已保存的数据。

---

### Step 4.2 — 职位标签化与匹配算法（基础版）

**指令**:
- 在 `occupation-recommend` 中实现 `JobMatchService`：
  - 职位标签提取：从 `job_detail` 的 skills 和 description 字段提取技能关键词。
  - 匹配算法（基础版）：基于规则的打分模型。
    - 技能匹配（权重 40%）：学生技能与职位技能标签的 Jaccard 相似度。
    - 城市匹配（权重 25%）：意向城市完全匹配得满分，范围扩大减分。
    - 薪资匹配（权重 20%）：职位薪资与期望薪资重叠度。
    - 学历匹配（权重 15%）：学历匹配或职位要求 ≤ 学生学历得满分。
  - 综合得分排序，取 Top 50。
- `GET /api/student/recommend/jobs`：返回推荐职位列表（含匹配得分和匹配理由）。

**验证**:
- 一个学生画像（技能=Java,Spring,MySQL，城市=北京，薪资=10k-15k，学历=本科）→ 调用推荐接口。
- 返回的 Top 10 职位中，Java 相关职位占比 > 70%。
- 匹配得分在 0-100 之间，且有合理性（完全匹配的职位得分 > 80）。

---

### Step 4.3 — 学生端首页 + 职位详情

**指令**:
- 学生端页面：
  - `StudentHome.vue`：推荐职位瀑布流/卡片列表，顶部筛选栏（城市、行业、薪资）。
  - `JobDetail.vue`：职位详情页（标题、公司、薪资、要求、描述），底部"投递"和"收藏"按钮。
- 后端 API：
  - `POST /api/student/job/{id}/favorite`：收藏职位。
  - `POST /api/student/job/{id}/apply`：投递职位（记录行为）。
  - `GET /api/student/favorites`：收藏列表。

**验证**:
- 学生登录 → 首页展示推荐职位，按匹配得分排序。
- 切换到"全部职位"tab → 按发布时间排序。
- 点击职位 → 进入详情页，信息完整。
- 点击"收藏"→ 收藏列表中出现该职位。
- 点击"投递"→ 提示投递成功。

---

### Step 4.4 — 推送通知系统

**指令**:
- 在 `occupation-recommend` 中实现：
  - 数据库表 `push_record`：id, user_id, type[RECOMMEND/SYSTEM], title, content, is_read, created_at。
  - `PushService.send(userId, title, content)`：写入推送记录。
  - `GET /api/student/pushes`：我的推送列表（未读置顶）。
  - `PUT /api/student/pushes/{id}/read`：标记已读。
- 前端导航栏：消息图标 + 未读红点数字。
- XXL-Job 定时任务：每天 8:00 为新匹配 Top 5 职位生成推送通知。

**验证**:
- 手动触发推送 → 学生端导航栏出现红点。
- 点击进入推送列表 → 看到推送内容。
- 标记已读 → 红点数字减 1。

---

### Step 4.5 — 学生端就业报告查看

**指令**:
- 后端 API：
  - `GET /api/student/reports`：可用报告列表（管理员已生成的、该租户的）。
  - `GET /api/student/reports/download/{id}`：下载报告。
- 前端页面 `StudentReportList.vue`：报告卡片列表，按月份排列，点击下载或在线预览（HTML）。

**验证**:
- 学生登录 → 报告列表展示已生成的月度报告。
- 点击预览 → 浏览器新标签页打开 HTML 报告，图表正常渲染。
- 点击下载 → 下载 PDF。

---

### Step 4.6 — 学生行为反馈闭环

**指令**:
- 在 `occupation-recommend` 中实现：
  - 数据库表 `student_behavior`：id, user_id, job_id, action[VIEW/FAVORITE/APPLY/IGNORE], created_at。
  - 点击职位 → 记录 VIEW。
  - 投递 → 记录 APPLY。
  - 推荐列表中出现但未点击超过 7 天 → 记录 IGNORE。
  - `GET /api/student/stats`：个人求职统计（浏览次数、投递数、收藏数、推荐匹配率）。
- 优化匹配算法：APPLY 的职位技能标签权重 +10%，IGNORE 的职位技能标签权重 -5%。

**验证**:
- 点击 3 个职位 → `student_behavior` 表新增 3 条 VIEW 记录。
- 投递 1 个职位 → 1 条 APPLY 记录。
- `GET /api/student/stats` 返回：浏览 3、投递 1。
- 再次刷新推荐 → 投递过的技能的同类职位排名上升。

---

> **P4 里程碑**: 学生画像 + 匹配推荐 + 职位详情 + 收藏投递 + 推送通知 + 报告查看 + 行为反馈 全部可运行。  
> **P4 集成测试**: 学生登录 → 填写画像 → 首页展示推荐 → 查看职位 → 收藏 → 投递 → 收到推送 → 查看报告 → 行为统计正确。

---

## P5：对外 API + 教师端 + 企业端 + 优化

### Step 5.1 — 对外公开 API（OAuth2 鉴权）

**指令**:
- 在 `occupation-api` 中实现对外 API：
  - `POST /api/open/auth/token`：第三方应用注册获取 API Key + Secret → 返回 Access Token。
  - `GET /api/open/jobs`：职位查询（按条件返回脱敏后的职位数据）。
  - `GET /api/open/reports/summary`：报告摘要（不暴露原始数据，只返回统计数据）。
  - `GET /api/open/skills/hot`：热门技能排行。
  - `GET /api/open/stats/overview`：就业大盘统计。
- 限流：每个 API Key 每分钟最多 100 次请求（Redis 令牌桶）。
- Swagger 接口文档：`/api/open/docs`（Knife4j UI）。

**验证**:
- 注册一个第三方应用 → 获取 API Key + Secret。
- 用 Access Token 调用 `/api/open/jobs` → 返回 200 + 数据。
- 无 Token 调用 → 返回 401。
- 1 分钟内调用 101 次 → 返回 429 Too Many Requests。
- 访问 `/api/open/docs` → Swagger 页面正常展示所有接口。

---

### Step 5.2 — 教师端页面

**指令**:
- 后端 API：
  - `GET /api/teacher/students`：所辖班级学生列表 + 就业状态。
  - `GET /api/teacher/students/{id}/profile`：学生画像详情。
  - `GET /api/teacher/students/{id}/behavior`：学生求职行为统计。
  - `GET /api/teacher/suggestions`：教学建议报告（基于该租户学生技能缺陷分析，由分析模块提供）。
  - `GET /api/teacher/export/students`：导出班级学生就业数据（Excel）。
- 前端页面：
  - `TeacherHome.vue`：班级概览（就业率、平均投递数、热门技能覆盖情况）。
  - `TeacherStudents.vue`：学生列表 + 详情 + 行为统计。
  - `TeacherReport.vue`：教学建议报告查看。

**验证**:
- 教师登录 → 看到班级概览数据。
- 点击某个学生 → 查看其画像和求职行为。
- 点击"导出"→ 下载 Excel，包含学生姓名、专业、技能、投递数、收藏数。
- 教学建议报告 → 显示"以下技能当前班级覆盖率 < 30%：Docker, K8s"等建议。

---

### Step 5.3 — 企业 HR 端页面

**指令**:
- 后端 API：
  - `POST /api/hr/jobs`：发布职位（企业 HR 在平台内发布）。
  - `GET /api/hr/jobs`：HR 发布的职位列表。
  - `PUT /api/hr/jobs/{id}`：编辑职位。
  - `PUT /api/hr/jobs/{id}/status`：下架/重新上架。
  - `GET /api/hr/talents`：人才画像浏览（脱敏：隐藏姓名和联系方式，仅显示技能、学历、专业）。
  - `GET /api/hr/apply/{jobId}`：该职位收到的投递列表。
- 前端页面：
  - `HrHome.vue`：概览（发布职位数、收到投递数、人才匹配推荐）。
  - `HrJobs.vue`：职位管理（列表 + 新增/编辑 + 上架/下架）。
  - `HrTalents.vue`：人才浏览（按技能/学历筛选）。

**验证**:
- HR 登录 → 发布一个职位 → 列表中显示。
- 编辑职位薪资 → 更新成功。
- 下架职位 → 该职位不在学生端展示。
- 浏览人才 → 看到学生技能画像但无姓名/手机号。

---

### Step 5.4 — 系统监控与告警

**指令**:
- 集成 Spring Boot Actuator + Prometheus：
  - 暴露指标：JVM 内存、GC、线程数、HTTP 请求数/延迟、Kafka 消费延迟。
- Grafana Dashboard：导入 JVM 和 Kafka 监控模板。
- 告警规则：
  - Kafka 消费延迟 > 1000 条 → 钉钉/邮件告警。
  - 爬虫连续 3 次失败 → 告警。
  - MySQL 连接池耗尽 → 告警。
- 数据库表 `sys_alert`：id, type, level[INFO/WARN/ERROR], content, is_read, created_at。
- 管理后台告警页面：告警列表 + 标记已读。

**验证**:
- 访问 `http://localhost:8080/actuator/prometheus` → 返回 Prometheus 格式指标。
- Grafana Dashboard → 看到 JVM 堆内存、GC 次数等图表。
- 手动停止 Kafka 消费者 → 等待 1 分钟 → 告警表中新增 ERROR 记录。

---

### Step 5.5 — 性能优化 + 压测

**指令**:
- 优化点：
  - 分析看板数据全部缓存到 Redis（TTL 1 小时），首次加载后不再查 MySQL。
  - 职位查询 API 结果分页缓存（第 1 页缓存 5 分钟）。
  - HBase 查询添加二级索引（或使用 Elasticsearch 替代 HBase 做搜索，后续评估）。
  - 前端路由懒加载、ECharts 按需引入、Gzip 压缩。
- 使用 JMeter 压测：
  - 并发 100 → `/api/jobs?keyword=Java`，平均响应时间 < 200ms。
  - 并发 100 → `/api/analysis/dashboard`（缓存命中），平均响应时间 < 100ms。
  - 并发 50 → `/api/auth/login`，平均响应时间 < 500ms（含 BCrypt）。

**验证**:
- JMeter 压测报告：所有接口在目标并发下通过。
- 99 分位数延迟 < 1000ms。
- 错误率 < 0.1%。

---

### Step 5.6 — 文档与交付

**指令**:
- 输出以下文档（Markdown）：
  - `deployment-guide.md`：部署指南（Docker Compose 一键部署 + 手动部署步骤）。
  - `api-document.md`：完整 API 文档（Swagger 导出 + 人工补充说明）。
  - `user-manual.md`：用户手册（四角色操作指南，含截图）。
  - `architecture.md`：最终架构文档（完整数据库 Schema、模块关联图、部署架构图）。
- Git Tag：`v1.0.0`。

**验证**:
- 按 `deployment-guide.md` 在新环境（干净 Linux）部署 → 全流程可运行。
- 按 `user-manual.md` 操作 → 四角色核心流程无阻塞。

---

> **P5 里程碑**: 对外 API + 教师端 + 企业端 + 监控告警 + 性能优化 + 文档交付。  
> **P5 集成测试**: 第三方 API 调用 → 四端全流程走通 → 压测通过 → 文档齐全。

---

## 附录 A：每步验证方式速查

| 验证方式              | 适用场景                            |
| --------------------- | ----------------------------------- |
| 单元测试 (JUnit)      | Service/Mapper 逻辑                 |
| API 测试 (Postman)    | Controller 接口                     |
| 集成测试              | 跨模块链路（爬虫→Kafka→存储）       |
| 数据库查询验证        | 数据写入正确性                      |
| 前端 E2E (手动/Playwright) | 页面交互流程                    |
| JMeter 压测           | 性能指标                            |
| Docker 部署验证       | 环境一致性                          |

---

## 附录 B：里程碑检查清单

- [ ] P1：后端骨架 + 认证 + 多租户 + Kafka 链路 + 爬虫采集 可运行
- [ ] P2：数据清洗 + 5 维度分析 + 定时调度 + 职位查询 可运行
- [ ] P3：管理后台 + 分析看板 + 报告生成/导出 + 用户管理 可运行
- [ ] P4：学生画像 + 推荐匹配 + 职位详情 + 推送通知 + 行为反馈 可运行
- [ ] P5：对外 API + 教师端 + 企业端 + 监控 + 压测 + 文档 可运行
