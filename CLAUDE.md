# CLAUDE.md

职业能力大数据服务平台 — 高校团队课程项目。Spring Boot 多模块 Maven 后端 + Vue 3 前端，多租户，四角色（ADMIN / STUDENT / TEACHER / HR）。

相关文档：`AGENTS.md`（AI 编码规则，生成代码前必读）、`docs/项目开发说明书.md`（架构与分工）、`memory-bank/`（设计文档）。

仓库根目录还有三个一次性输入文件，**都已消化完毕，不要再当待办**：
- `error_analysis.md` — 问题排查清单。**其中的 TODO 已全部实现**（代码里已无任何 `TODO(` 与 `UnsupportedOperationException`）。注意它有两处结论是错的：① 它说给 `TeacherHome.vue` 加个 `Array.isArray` 就能「瞬间填满数据」，实际前端是照着一份后端从未实现的契约写的，光改前端只会得到一张全是 `—` 的表；② 它建议把无鉴权租户接口放在 `/api/open/tenants`，那个命名空间会被 `ApiTokenInterceptor` 拦下返回 401
- `DESIGN.md` — 视觉规范，实为 **Family（以太坊钱包）落地页**的扒取产物（组件名里还留着 "Explore Ethereum"、"Watching Wallets"）。有三处不能照搬，见下方设计系统章节
- `mockup_demo_login.html` — 登录页静态稿，已落地为 `LoginView.vue`

## 模块结构

- `occupation-web` — 聚合后端服务，端口 8080；**全项目唯一的数据库配置在这里**（`application.yml`，默认 root/root，支持 `DB_PASSWORD` 环境变量覆盖）
- `occupation-auth / api / analysis / recommend / report / crawler / common` — 业务模块（common 含 SQL 脚本与公共 DTO）
- `occupation-web-ui` — Vue 3 + Element Plus + ECharts 前端，dev 端口 5173（自带 `/api → 8080` 代理）
- `nginx/` — 部署用反向代理配置（日常开发不需要）

## 启动流程

```bash
docker-compose up -d                      # mysql/redis/zookeeper/kafka/nginx
mvn install -DskipTests                   # ⚠️ 改过非 web 模块后必须先装，见下
mvn spring-boot:run -pl occupation-web    # 后端 8080
cd occupation-web-ui && npm run dev       # 前端 5173
```

- **`spring-boot:run -pl occupation-web` 不会重新编译其它模块**：它从本地 `.m2` 取 `occupation-auth` 等依赖的旧 jar。改了 auth/analysis/recommend/report/api/common 里的代码却不先 `mvn install`，跑的还是旧代码 —— 表现为「明明改了却没生效」。只改 occupation-web 或前端时可跳过。
  **这个坑真实咬过人**：曾有人报「学生画像改了学历、教师端不跟着变」「报告下载下来打不开」，两个功能分别在 `occupation-recommend` 和 `occupation-report`（都是非 web 模块），先 `mvn install` 再启动后两者都复现不出来。省事的写法：`mvn install -DskipTests && mvn spring-boot:run -pl occupation-web`。
  另注意 `mvn clean compile` **治不了这个病** —— 它只编到各模块的 `target/classes`，不往 `~/.m2` 装 jar
- **后端跑着的时候，内置定时任务会持续改库**（`app.scheduler.enabled: true`）：`AnalysisScheduler` 会把 `raw_job_data` 里待清洗的数据洗进 `job_detail`，所以 `job_detail` 的行数会从种子的 96 慢慢往上涨。这是设计如此，不是 bug，但排查数据问题时别把它当成异常
- 登录账号（租户 `测试学院`，密码均 `admin123`）：`admin`（管理员）、`student`（学生）、`teacher`（教师）、`hr`（HR）——init.sql 预置
- init.sql 含全量测试数据（由 `scripts/gen-seed-data.js` 确定性生成）：96 个职位、13 份学生画像（租户1 占 12、租户2 占 1）、242 条行为、分析结果已预算好，**开箱即可看到看板/推荐/收藏/投递数据**
- **种子账号的边界用例**（写统计逻辑时留意）：`student12` 有账号无画像（测「请先完善画像」）；`student98` 状态禁用（`status=0`，**仍计入** `countByRole`）；`student99` 逻辑删除（`deleted=1`，**不计入**）。所以教师端「学生总数 14 / 已填画像 12」是对的，别当成 bug。另有 `student01`~`student11`、`teacher01/02`、`hr01/02`；第二租户 `示范大学`（admin/student/teacher）测多租户隔离；`停用学院` 测租户禁用
- **HR 职位归属**：6 个 `HR_PUBLISH` 职位按 `publisher_id` 分给 hr(2 个) / hr01(1 个) / hr02(3 个)，三个账号登录后「职位管理」看到的列表各不相同 —— 这是验证「只看我发布的」是否生效的最快方式。种子里有 11 条投递落在这些职位上，HR 端「收到的投递」开箱有数据
- 登录页有角色选择标签，但仅作入口提示；实际进入哪个端由**账号自身的 role** 决定（选错会提示并按实际角色进入）
- MySQL：root/root，库名 `occupation`
- **职位数据靠「采集管理 → 选中任务 → 启动」生成**（走 Kafka 清洗链路，Kafka 没起就静默失败）；看板数据靠「手动重算分析数据」——种子数据已含两者结果，重跑只会增量/覆盖
- **原来那个「Mock 模拟采集」按钮已删**（2026-07-10）。它与「对一条 `source_type=MOCK` 的任务点启动」完全等价，却每点一次就用 `System.currentTimeMillis()` 当主键新插一条一次性 `crawler_task`、跑完不清理——实测点五次就留下五条垃圾任务。现在唯一入口是 `PUT /api/admin/crawler/task/{id}/start`
- **MOCK 采集不访问外网**：`MockJobPageProcessor` 读的是 classpath 下的 `occupation-crawler/src/main/resources/mock/mock-jobs.json`（20 条），`source_url` 是 `https://mock.local/job/001` 这类不存在的地址。它不走 WebMagic 的 `Spider`，直接同步循环读文件；只有 BOSS/智联走真正的爬虫框架
- **`mock-jobs.json` 那 20 条已全部入库**：清洗按 `source_url` 去重，再点多少次「启动」`job_detail` 都不会涨（`raw_job_data` 会涨，那是原始归档，不去重）。想采到新职位得先把这个 JSON 加厚
- 爬虫默认 MOCK 数据源；`BossJobPageProcessor` / `ZhaopinJobPageProcessor` 是真实爬虫实现但默认不启用（站点改版即失效 + 服务条款风险，勿依赖）

## 数据库协作流程（以 init.sql 为唯一事实来源）

- 脚本位置：`occupation-common/src/main/resources/sql/init.sql`（DROP+CREATE 15 张表 + 种子数据，可重复执行）
- MySQL 容器**首次启动（数据卷为空）时自动执行**它
- **不想 `down -v` 丢数据时**，用同目录的增量脚本给现有库补表：
  ```bash
  docker exec -i occupation-mysql mysql -uroot -proot occupation < occupation-common/src/main/resources/sql/upgrade-2026-07-10-student-resume.sql
  ```
  它是 `CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，可重复执行、不 DROP、不覆盖已有行。内容由脚本从 init.sql 抽取，两处同源不会漂移

**日常同步（最常用，就两步）：**
```bash
git pull && docker-compose down -v && docker-compose up -d
```
`-v` 必须带 —— 不带的话旧数据卷还在，MySQL 认为已初始化过，不会再执行 init.sql。

**要改种子数据时（加账号 / 改职位数量 / 调行为分布）：**
```bash
vim scripts/gen-seed-data.js              # 1. 改生成器
node scripts/gen-seed-data.js             # 2. 重写 init.sql 的种子段（DDL 部分手写维护，不受影响）
docker-compose down -v && docker-compose up -d   # 3. 本机重建验证
git add init.sql gen-seed-data.js && ...  # 4. 提交，队友同样 down -v
```
生成器用固定随机种子，确定性输出 —— 同样的输入永远产出同样的 init.sql，不会出现「你我各跑一次得到两份数据」。

**⚠️ 两条禁忌：**
- **不要用 `mysqldump > init.sql` 覆盖**（旧文档里的做法）。它会冲掉文件头的 `SET NAMES utf8mb4;` 与 `CREATE DATABASE`，也会把 `gen-seed-data.js` 维护的结构与注释全部抹平；`file_url` 之类的本机绝对路径还会被带进仓库
- **`init.sql` 第 8–12 行有 `CREATE DATABASE occupation;` + `USE occupation;`**。这意味着 `mysql -uroot -p 别的库名 < init.sql` 里指定的库名**会被脚本自己的 `USE` 覆盖**，DROP TABLE 直接打在 `occupation` 上。想导进临时库验证，必须先 `sed` 掉这两行

## 前端设计系统（2026-07 全站改版，DESIGN.md 版）

设计方向：奶油画布 + 墨黑主操作 + **内描边代替阴影**、Inter、深浅双主题。设计系统样品页（含配色实测与组件规范）：https://claude.ai/code/artifact/681fbf62-a0ee-4224-a551-f7348b1a6914

**DESIGN.md 是从 Family（以太坊钱包）落地页扒的规范，有三处不能照搬，theme.css 顶部注释里写了原因：**
1. 它的强调色作正文字色全部不达标（Grass Green 2.09:1、Gold 2.61:1、连它自己的 Muted Gray 也只有 3.90:1，正文需 4.5:1）。**保留色相压暗**成 `--app-money / --app-score / --app-link / --app-ember / --app-danger`；原始亮色只用于 chip 背景和图表填充
2. DESIGN.md 标注 `Theme: light` 没给深色色值，深色令牌是按同一骨架推演的（暖近黑画布、卡片比画布亮、主操作翻转为奶油）
3. 展示字体 Family 是商用定制字，拿不到；标题用 Inter 600，正文 13–14px（不是 DESIGN.md 的 17px）

- **主题令牌**：`src/styles/theme.css` — 全部颜色走 `--app-*` 变量（浅色 `:root`，深色 `html.dark`），并覆写 Element Plus 的 `--el-*`。主操作 `--app-action`（浅色墨黑 `#121212` / 深色奶油 `#f4f1ec`），配 `--app-action-ink` 作其上的文字色
- **高度**：卡片一律 `box-shadow: var(--app-hairline)`（内描边），**禁止投影**；只有浮层（dialog / dropdown / tooltip）能用 `--app-overlay-shadow`
- **深色模式**：`useAppStore().toggleTheme()` 切 `html.dark`，持久化在 localStorage `theme`。深色下 `--el-color-primary` 接近白色，Element 会把勾/文字也画成白的 —— theme.css 已显式覆盖 checkbox / radio / switch，新增控件注意同样问题
- **图表主题**：`src/styles/chartTheme.js` 注册 `app-light` / `app-dark`。八槽分类色板已过色盲校验（浅色相邻 ΔE 24.2，深色对比度全 ≥3:1）。规则：**槽位固定顺序不循环**；单系列图表用 `primarySeriesColor()`；薪资用 `moneySeriesColor()`；**禁止双 Y 轴**（数量与薪资拆两张图，参考 Dashboard.vue）；图表页必须 `watch(() => appStore.dark)` 重绘
- **共享样式类**（theme.css 内，全局可用）：`.page-head / .page-title / .page-sub`、`.page-head.with-actions + .page-actions`、`.stat-grid / .stat-card / .stat-value / .stat-label`、`.chip`（中性标签，`.chip.learn` 琥珀）、`.job-grid / .job-card` 系列、`.salary-text`
- **新页面约定**：装饰性信息（城市/学历/技能）用 `.chip` 而不是彩色 el-tag；el-tag 只留给状态语义（成功/警告/危险）；分页不需要手写对齐样式（全局已右对齐）
- **共享工具**：`utils/list.js` 的 `toList/toTotal`（**所有列表响应必须走它**）、`utils/skills.js` 的 `parseSkills`、`utils/format.js` 的 `salaryRange/formatTime`、`utils/download.js` 的 `saveBlob`（带鉴权下载，**不要用 window.open**）

## 前后端契约（2026-07-09 大修的重灾区，写新页面前必读）

改版前几乎所有「页面空白 / 列全是 —— / 按钮点了没反应」都出在这三类不一致上。全部已修，但**新增接口时极易重蹈覆辙**。

### 1. 列表响应有两种形态，前端必须用 `toList()`

- 分页接口 → `PageResult`：`{ total, pageNum, pageSize, list }`
- 非分页接口 → **直接一个数组**

历史代码写 `data.records || data.list || []`：`records` 是 MyBatis `Page` 的字段名，`PageResult` 序列化后**根本不存在**（死代码）；后端返回纯数组时 `.list` 取不到 → 渲染成空；`data` 为 null 时直接抛 `TypeError`。
**一律用 `utils/list.js` 的 `toList(data)` / `toTotal(data, list)`**，三种情况都兜住。

### 2. 分页参数名不统一 —— 两套并存，传错就永远只返回第一页

| 参数名 | 用在哪些接口 |
|---|---|
| `pageNum` / `pageSize` | `/api/admin/users`、`/api/report/records`、`/api/admin/report/template`、`/api/push/list`，以及所有吃 `JobQueryDTO` 的（`/api/hr/jobs`、`/api/open/jobs`…） |
| `page` / `size` | `/api/hr/talents`、`/api/teacher/students`、`/api/admin/crawler/task`、`/api/admin/crawler/log` |

传错不会报错，Spring 用默认值 `1 / 10`，表现为「翻页没反应」。**加新接口请统一用 `pageNum/pageSize`**。

### 3. 字段名 / 枚举值必须照抄后端，别凭直觉

这些都真实踩过（左边是前端曾经写的、右边是后端实际的）：

| 前端曾写 | 后端实际 | 后果 |
|---|---|---|
| `education` | `educationLevel`（学生画像） | 学历列全是 `—` |
| `intendedCity` | `expectedCity` | 期望城市全是 `—` |
| `status === 'COMPLETED'` | `'SUCCESS'`（报告） | 下载按钮永不出现 |
| `format` | `fileType`（生成报告入参） | 选 PDF 却导出 HTML（DTO 默认值） |
| `content` | `templateContent`（模板入参） | 校验失败 400 |
| type: `STUDENT/CLASS/…` | `MONTHLY/QUARTERLY/YEARLY` | `@Pattern` 校验失败 400 |
| `skills: []`（数组） | `String`（JSON 数组字符串） | 绑定失败，发布职位报错 |
| 学历「大专」 | 「专科」 | `JobMatchServiceImpl.EDU_LEVEL` 取不到 → 学历维度 0 分，匹配分莫名偏低 |
| `job.status`（在招/已下架） | **`job_detail` 没有 status 列** | 状态列永远显示「已下架」 |

**注意 `job_detail` 也没有 `deleted` 列**，HR 下架职位是物理删除。

### 4. 带鉴权的文件下载不能用 `window.open`

`window.open('/api/report/download/1')` 不带 `Authorization` 头 → 401 → 浏览器静默跳走，表现为「点了没反应」。
走 `utils/download.js` 的 `saveBlob(axios请求, 兜底文件名)`：axios 拦截器注入 Token，拿到 Blob 再用 `<a download>` 触发保存，并从 `Content-Disposition` 解析 RFC 5987 编码的中文文件名。
`api/request.js` 的响应拦截器已对 `responseType === 'blob'` 放行（否则会去读 `data.code` 而报错）。

## 采集链路的几个隐坑（2026-07-10 修）

- **MOCK 任务的 `url_pattern` 必须是 `mock/` 目录下的真实文件名**：`CrawlerServiceImpl` 拼的是 `"mock/" + url_pattern`。种子里原来是 `NULL`，会拼出 `mock/null` 静默采到 0 条——因为大家一直点的是 Mock 按钮（它硬编码了文件名），这个坑藏了很久。现在有默认值兜底，种子也已填好
- **不支持的采集源不再返回 500**：`COMPANY_OFFICIAL` 只有表结构没有实现，原先 `createProcessor` 抛 `IllegalArgumentException` 被兜底处理器吞成「系统内部错误」。现在抛 `BizException`，提示「暂不支持的采集源类型」
- **用错 HTTP 方法不再返回 500**：`GlobalExceptionHandler` 补了 `HttpRequestMethodNotSupportedException`(405) 与 `MethodArgumentTypeMismatchException`(400)。原先对 `/task/{id}` 发 POST 会得到一句毫无信息量的「系统内部错误」
- **种子里的 `crawler_task.status` 一律为 0**：原来任务 1/4 写成 `1`（运行中），但根本没在跑，页面显示「运行中」而点「停止」会报「任务未在运行」

## 三个只有跑起来才会暴露的坑（都已修，改相关代码前务必先读）

- **无鉴权接口要在两处放行**：`JwtAuthenticationFilter.WHITE_LIST` 跑在 Spring Security 之前，只在 `SecurityConfig.permitAll()` 里加是不够的，请求会先被过滤器挡下返回 401。`/api/auth/tenants` 就是这么踩的
- **多租户插件：`getTenantId()` 返回 null 不等于「跳过隔离」**。MP 会照样拼出 `AND tenant_id = null`，而 SQL 里 `= null` 恒不成立 → 查询静默返回 0 行。想跳过隔离只能把表加进 `ignoreTable()`。开放 API 的 `/api/open/auth/token` 一直换不到 Token 就是这个原因（`api_client` 已加入 ignoreTable，`getTenantId()` 现在返回 `0` 而非 null）
- **开放 API 没有 JWT，租户上下文由 `ApiTokenInterceptor` 建立**。少了这一步，涉及租户表的查询（如报告摘要）会把所有学校的数据返回给任意第三方客户端。新增 `/api/open/**` 接口时确认租户上下文已就绪

## 2026-07-10 新增：简历 / HR 解密 / DeepSeek

### 简历（`student_resume`）

- **画像 ≠ 简历**：`sys_student_profile` 是喂给推荐算法的结构化匹配依据（扁平、可索引）；`student_resume` 是给 HR 和大模型读的自我陈述（教育/项目/实习三段经历存 JSON 数组）
- **HTTP 层收发结构化数组**，序列化只在 `ResumeServiceImpl` 里发生一次。前端 **不要** `JSON.stringify`（画像的 `skills` 就是那么踩的坑）
- 未填写时 `GET /api/student/resume` 返回 `{exists:false, educations:[], …}` 空壳而不是 `null`，前端不必判空
- 种子里 8 份简历；`userId=14` 投递过 HR 职位但没简历，用来测 HR 端「未填写简历」空态

### HR 可见性边界（改动了原来的「全脱敏」设计）

| 页面 | 能看到什么 | 为什么 |
|---|---|---|
| 人才浏览 `/api/hr/talents` | **全脱敏**，无 userId / 姓名 / 联系方式 | 面向没有投递关系的全校学生 |
| 收到的投递 `/api/hr/applications` | userId / 姓名 / `hasResume`，**无联系方式** | 学生主动投递 = 授权该 HR 查看身份；但列表页不该批量泄露联系方式 |
| 投递人详情 `/api/hr/applicants/{userId}` | 姓名 / 手机 / 邮箱 / 画像 / 简历全文 | 单独拉取，**带归属校验** |

**`/api/hr/applicants/{userId}` 的归属校验是这个接口的全部安全性所在**：只有该学生投递过**本 HR 发布的**职位才放行，否则 403。少了这一步，任何 HR 把 userId 从 1 枚举到 N 就能拖走全校学生的手机号。已有回归测试覆盖（hr01 查 hr 的投递人 → 403）。

`ApplicationVO` 是**只加字段不删字段**，原有前端读法全部保留。

### AI（DeepSeek）

- **全平台唯一 LLM 出口是 `AiChatClient`**（`occupation-common/ai`）。新增 AI 能力请复用它，不要再各写一份 RestTemplate
- **密钥严禁进 git**。走 `occupation-web/src/main/resources/application-local.yml`（已 gitignore），仓库里只留 `.example`。`spring.profiles.active: dev,local`，文件不存在时 Spring 静默跳过 —— 队友不配也能正常启动，AI 自动降级
- **失败一律降级，不阻断主流程**。所有 AI 接口返回 `aiGenerated: false` 表示「这是规则化兜底文字」，前端必须如实告知用户，不能把模板文案冒充成 AI 输出。唯一例外是 `ai-polish`：润色没有合理的规则降级（拿什么改写？），直接抛业务异常
- **前端调 AI 接口必须显式加 `timeout: 90000`**：`api/request.js` 的默认超时是 15 秒，大模型首字延迟常达数秒、长回复能到数十秒
- **`chat()` 会丢弃前端传来的 `role: "system"`**（只接受 user/assistant）。不然任何人都能覆盖角色设定，这是最基础的 prompt 注入。已有测试守着
- 结构化输出走 `askJson()`（DeepSeek 的 `json_object` 模式）。**提示词里必须出现 "json" 字样**，否则 DeepSeek 拒绝该模式
- 诊断/报告类温度 0.3（要稳定可复现），顾问对话用 `chat(messages, 0.7)` 覆盖
- 推荐列表**不批量调大模型**：20 条逐条调既慢又贵。列表只给规则分，点开某条才调 `/api/student/advisor/explain/{jobId}`

**怎么让 AI 效果变好（这是 prompt 工程，不是训练，也不需要联网）**：
1. **真正起作用的是往 prompt 里塞真实上下文**。`CareerAdvisorServiceImpl.buildContext()` 注入了这名学生的画像、简历摘要，以及本平台真实的技能热度/城市/行业岗位数。去掉这段，模型只会输出「建议多学习多实践」这种正确的废话
2. **角色设定与数据分离**：约束放 `system`，数据放 `user`。模型更不容易跑题
3. **禁止编造**要写进 system（「数据里没有的信息，直言不知道」「不许编造原文没有的数字」），否则模型会替学生瞎编项目成果
4. **要结构化就用 JSON 模式**，别让模型输出 Markdown 再正则去抠
5. 换 `deepseek-reasoner`（R1）会更强但慢 3~5 倍且更贵，本项目默认 `deepseek-chat`（V3）
6. **模型偶尔会串维度**（比如把「杭州岗位数」和「互联网行业岗位数」混着说）。要更准就把每个维度的数字标注得更明确，或减少一次喂进去的维度数

## 上一轮新增/改动的接口

| 接口 | 说明 |
|---|---|
| `GET /api/auth/tenants` | 无鉴权，登录页学校下拉。只返回启用中的租户。**刻意不放在 `/api/open/`** —— 那是第三方 OpenAPI 命名空间，被 `ApiTokenInterceptor` + 限流保护，无 Token 会 401 |
| `GET /api/hr/applications` | HR 收到的投递（脱敏，无 userId/姓名/联系方式） |
| `GET /api/hr/jobs` | 加 `publisher_id` 过滤，只看自己发布的 |
| `PUT/DELETE /api/hr/jobs/{id}` | **补了归属校验**（原来任何 HR 都能改别人的职位，403 拦截） |
| `GET /api/teacher/overview` | 班级概览四个统计卡（原来前端读 `data.stats`，后端根本没有） |
| `GET /api/teacher/students` | 分页 + 连 `sys_user` 补真实姓名/学号 + 行为计数 |
| `GET /api/teacher/students/{id}/stats` | 改返回 `BehaviorStatsVO`（原来是 `Map<String,Long>`，键是 `VIEW/APPLY`，前端按 `viewCount` 读永远 0） |
| `GET /api/teacher/students/{id}/behaviors` | 补 `jobTitle/jobCompany/jobCity`（原来只有 jobId，表格两列全空） |
| `GET /api/teacher/suggestions` | **技能缺口诊断**。原来前端 `Suggestions.vue` 里掌握率是 `Math.random()`，每次刷新都变 |
| `GET /api/teacher/export` | 导出学生就业数据 Excel |
| `POST /api/admin/users/batch-import` | Excel 批量导入（全量校验通过才写库，返回逐行错误） |
| `GET /api/admin/users/import-template` | 下载导入模板 |
| `GET /api/open/reports/summary` | 最新报告摘要（读 `report_record.ai_summary`，不重复调大模型） |
| `GET /api/report/records` | 改返回 `ReportRecordVO`，补 `templateName/type` |
| `GET /api/report/download/{id}` | 文件名补扩展名（原来是 `report-1`，双击打不开） |

## 后端要点

- **技能字段解析统一走 `SkillUtils`**（common/utils）：库里标准格式是 JSON 数组 `["Java","MySQL"]`，但存在逗号/顿号分隔的旧数据。前端对应 `utils/skills.js`
- **技能词库提取**（`SkillDictionary`，analysis 模块）：从职位标题+描述中补全技能标签。ASCII 技能用 lookaround 手写词边界，否则 `Django` 会命中 `Go`、`JavaScript` 会命中 `Java`（`\b` 处理不了 `C++`/`C#` 结尾的符号）。中文技能直接子串匹配。有 9 个单测守着，**改词库前先跑 `SkillDictionaryTest`**
- **推荐打分 = 基础规则 0~100 + 行为加权 ±10**：技能 40 / 城市 25 / 薪资 20 / 学历 15，再按历史 APPLY(+2) / FAVORITE(+1) / IGNORE(-2) 反推技能偏好加减分；已投递、已忽略的职位不再出现在推荐里。最终分裁剪回 0~100
- **教学建议是真算的**：`TeachingSuggestionServiceImpl` 对比 `analysis_result`(dimension=skill) 的市场热度与学生画像的掌握率，输出「市场热但掌握率 <30%」的技能，每条附岗位数与掌握人数作证据。`marketDemand` 是**相对热度**（该技能岗位数 ÷ 最热技能岗位数），所以排第一的恒为 100
- **批量查询避免 N+1**：`BehaviorService.countByActionGroupedByUser` / `UserService.mapByIds` / `JobDetailService.listByIds` 都是一次取回。加新的「列表 + 关联信息」接口时沿用这个模式
- **限流是 Redis ZSET 滑动窗口 + Lua**（`RateLimitInterceptor`）：剔除→计数→写入必须原子完成，否则并发下超发。固定窗口计数在窗口边界会瞬间放过 2 倍流量
- **PDF 中文字体不随仓库分发**（simsun.ttc 18MB 且受微软授权）：`PdfExporter` 按「`app.report.pdf.font-path` → classpath `fonts/` → 系统字体目录」自动探测，以 `EMBEDDED` 方式嵌入；找不到会打明确告警。HTML 模板的 `font-family` 要列多个候选，只写 `SimSun` 在 Linux 容器里仍是方块字
- **AI 默认关闭**：建 `occupation-web/src/main/resources/application-local.yml`（见同目录 `.example`）填密钥并 `enabled: true` 才会调大模型；关闭时所有 AI 能力降级为规则化文字（不影响功能）
- **Excel 用 Hutool `ExcelReader/ExcelWriter`**（依赖 `poi-ooxml` 运行时），没引 EasyExcel。中文文件名走 RFC 5987 `filename*=UTF-8''`，否则下载后乱码

## 已知事项

- **数据源必须用 `127.0.0.1` 而非 `localhost`**（application.yml 已改）：Windows 上 localhost 可能解析为 IPv6 `::1`，连到 WSL 残留的 MySQL 而不是 Docker 的
- **init.sql 开头必须保留 `SET NAMES utf8mb4;`**：否则 docker-entrypoint 执行时中文种子数据（测试学院等）会以乱码入库，登录报「租户不存在」
- **2026-07-09 schema 有变更**（`job_detail.publisher_id`、`report_record.ai_summary`、`api_client.api_secret` 改存 BCrypt）：队友必须 `git pull && docker-compose down -v && docker-compose up -d`（`-v` 不带的话旧数据卷还在，新表结构不会生效，HR 端会 403）
- 开放 API 的 `api_secret` 现在是 BCrypt 存储，调用 `/api/open/auth/token` 时传**明文**（init.sql 的注释里列了四个客户端各自的明文）
- **报告落盘目录随进程工作目录变化**：`mvn spring-boot:run -pl occupation-web` 会写到 `occupation-web/data/reports/`，不是仓库根的 `data/reports/`。.gitignore 已加 `**/data/reports/` 覆盖两处
- `.mybatis/`、`data/reports/` 是生成物，不入库

## 验证状态（2026-07-10）

- `mvn test`（18 个单测）与 `npm run build` 均通过
- **AI 开启时**已用真实 HTTP + 真实 DeepSeek 调用验证：简历 CRUD 与校验 / AI 诊断（score+4 条建议）/ AI 润色 / 顾问对话（引用了真实岗位数）/ 匹配理由解读 / 教学建议解读 / **prompt 注入被丢弃**
- **AI 关闭时**（队友的默认状态）验证全部降级路径：诊断退规则、顾问给兜底文案、匹配理由退规则、教学建议退规则、润色明确报错；`aiGenerated` 均为 `false`
- HR：投递列表含身份不含联系方式 / 详情含简历全文 / **越权 403**（枚举 userId 与跨 HR 查看都被拦）/ 人才浏览仍全脱敏
- 早前已验证：租户下拉 / 五个角色登录 / 教师看板统计 / 技能缺口诊断非随机 / HR「只看我发布的」(2/1/3) / Excel 导出 / PDF 内嵌中文字体 / Word 结构化表格 / 开放 API BCrypt 校验
- **报告下载已重新验证无误**：用真实数据库拉取，PDF/Word 下载字节与磁盘文件 sha256 完全一致；PDF 为 3 页、内嵌 SimSun 子集、51 个文字块。此前「下载打不开」的报告应为旧 jar 所致（见启动流程那节）
- **未做**：浏览器里的人工点验（深色模式配色、图表重绘、简历表单与顾问对话的交互手感）；`error_analysis.md` 里提的 XXL-Job 分布式调度未跑通
