# CLAUDE.md

职业能力大数据服务平台 — 高校团队课程项目。Spring Boot 多模块 Maven 后端 + Vue 3 前端，多租户，四角色（ADMIN / STUDENT / TEACHER / HR）。

相关文档：`AGENTS.md`（AI 编码规则，生成代码前必读）、`docs/项目开发说明书.md`（架构与分工）、`memory-bank/`（设计文档）。

仓库根目录保留一个一次性输入文件，**已消化完毕，不要再当待办**：
- `DESIGN.md` — 视觉规范，实为 **Family（以太坊钱包）落地页**的扒取产物（组件名里还留着 "Explore Ethereum"、"Watching Wallets"）。有三处不能照搬，见下方设计系统章节

（另有 `error_analysis.md` 排查清单与 `mockup_demo_login.html` 登录页静态稿，均已消化并于 2026-07-12 结构清理时删除：前者 TODO 已全部实现、结论已过时；后者已落地为 `LoginView.vue`。）

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
- init.sql 含全量测试数据（由 `scripts/gen-seed-data.js` 确定性生成）：**114 个职位**（90 采集 + 24 站内）、13 份学生画像（租户1 占 12、租户2 占 1）、12 份简历、**391 条行为**（VIEW 212 / APPLY 73 / FAVORITE 63 / CONTACT 32 / IGNORE 11）、**73 条投递**（五种状态铺开）、分析结果已预算好，**开箱即可看到看板/推荐/投递漏斗/供需错配**
- **种子账号的边界用例**（写统计逻辑时留意）：`student12` 有账号、无画像、无简历，却投递了一个站内职位 —— 一次覆盖三个空态（学生端「请先完善画像」+ HR 端「未完善画像 / 未填写简历」）；`student98` 状态禁用（`status=0`，**仍计入** `countByRole`）；`student99` 逻辑删除（`deleted=1`，**不计入**）。所以**管理员**看到的教师端「学生总数 14 / 已填画像 12」是对的，别当成 bug。另有 `student01`~`student11`、`teacher01/02`、`hr01`~`hr05`；第二租户 `示范大学`（admin/student/teacher）测多租户隔离；`停用学院` 测租户禁用。
  **注意（2026-07-12 后）**：教师端数字随登录教师的可见范围变化——`teacher01`（班主任软工班）看到 3 人 / 2 有画像、`teacher02`（专业老师计科）2/2、`teacher`（届老师 2022 级）10/9；只有 ADMIN 看整租户 14/12。见下方「教师可见范围」章节
- **HR 职位归属**：24 个 `HR_PUBLISH` 职位平均分给 6 个 HR（`hr`/`hr01`~`hr05`，各 4 个），分属 3 家公司。六个账号登录后「职位管理」看到的列表各不相同 —— 这是验证「只看我发布的」是否生效的最快方式。每个 HR 都收到 10~13 条投递，且五种处理状态都有样本，HR 端与「就业分析」的漏斗开箱就有形状
- 登录页有角色选择标签，但仅作入口提示；实际进入哪个端由**账号自身的 role** 决定（选错会提示并按实际角色进入）
- MySQL：root/root，库名 `occupation`
- **职位数据靠「采集管理 → 选中任务 → 启动」生成**（走 Kafka 清洗链路，Kafka 没起就静默失败）；看板数据靠「手动重算分析数据」——种子数据已含两者结果，重跑只会增量/覆盖
- **种子数据（2026-07-10 版）**：114 个职位（90 采集 + 24 站内）、6 个 HR、13 份画像、12 份简历、391 条行为（含 32 条 CONTACT）、73 条投递（五种状态铺开）。`mock-jobs.json` 有 60 条，点一次「启动」全部入库，再点因 `source_url` 去重不会增加
- **原来那个「Mock 模拟采集」按钮已删**（2026-07-10）。它与「对一条 `source_type=MOCK` 的任务点启动」完全等价，却每点一次就用 `System.currentTimeMillis()` 当主键新插一条一次性 `crawler_task`、跑完不清理——实测点五次就留下五条垃圾任务。现在唯一入口是 `PUT /api/admin/crawler/task/{id}/start`
- **MOCK 采集不访问外网**：`MockJobPageProcessor` 读的是 classpath 下的 `occupation-crawler/src/main/resources/mock/mock-jobs.json`（20 条），`source_url` 是 `https://mock.local/job/001` 这类不存在的地址。它不走 WebMagic 的 `Spider`，直接同步循环读文件；只有 BOSS/智联走真正的爬虫框架
- **`mock-jobs.json` 那 20 条已全部入库**：清洗按 `source_url` 去重，再点多少次「启动」`job_detail` 都不会涨（`raw_job_data` 会涨，那是原始归档，不去重）。想采到新职位得先把这个 JSON 加厚
- 爬虫默认 MOCK 数据源；`BossJobPageProcessor` / `ZhaopinJobPageProcessor` 是真实爬虫实现但默认不启用（站点改版即失效 + 服务条款风险，勿依赖）

## 数据库协作流程（以 init.sql 为唯一事实来源）

- 脚本位置：`occupation-common/src/main/resources/sql/init.sql`（DROP+CREATE 19 张表 + 种子数据，可重复执行）
- **资讯表 `news`（2026-07-12 新增，首页资讯板块）**：`type` = DATA_CAST（数据播报，点击去图表 `link_target`）/ ARTICLE（精选文章，有 `content`）/ EXTERNAL（外部资讯，跳 `source_url`）；`category` = 技术方向（backend/frontend/…，null=通用）；封面用 `cover_style` 色块占位。后端在 **occupation-recommend**（`News`/`NewsService`/`NewsController`，`GET /api/news`、`/api/news/latest`、`/api/news/{id}`，任意登录角色可读）。增量脚本 `upgrade-2026-07-12-news.sql`。种子 8 条（5 播报 + 2 文章 + 1 外部占位）。**RSS 拉取(Google News)与管理端资讯 CRUD、数据播报自动生成 = 待做**
- **首页改造已完成（2026-07-13，各角色 Bento 工作台）**：各角色首页从"整屏大地图落地页"改为**角色工作台**。学生 `views/student/StudentDashboard.vue`、教师 `views/teacher/TeacherDashboard.vue`、HR `views/hr/HrDashboard.vue`（Bento 网格：欢迎条 + 地图 hero + 角色 KPI + 主内容 + 资讯格子），**管理员** `/admin` 首页直接指现成的 `views/admin/Dashboard.vue`（方案A，不套 Bento）。共享组件 `components/home/`：`MapHeroTile.vue`（地图 hero 静态预览，**点击跳独立地图页 `/{role}/map`**）、`NewsTile.vue`（资讯格子）、`NewsDetailDialog.vue`。共享页 `views/common/`：`NewsPage.vue`（资讯全览，四端 `/{role}/news`）、`MapExplore.vue`（3D 地图页，四端 `/{role}/map`）。**旧 `views/Home/`（HomeIndex/JobNews/jobNewsData）已整目录删除。** 注意 `views/student/StudentHome.vue` 是**职位推荐列表**（路由 `/student/jobs`），命名易误解，别和首页 `StudentDashboard.vue` 搞混
- **3D 地图已换 echarts-gl（2026-07-13，弃用自研 three.js）**：`MapExplore.vue` 用 **echarts-gl `geo3D` + `bar3D`/`scatter3D`**（柱状/光点两模式），装了 `echarts-gl`，中国底图从 Aliyun DataV geojson 外链注册。**图层**：`岗位数`/`平均薪资`（市场，全角色）+ `学生意向`/`投递去向`（学生侧，仅教师/ADMIN，按可见范围过滤）。后端接口：`GET /api/map/cityDistribution`（analysis，全量城市岗位数+平均薪资+坐标）、`GET /api/teacher/map/intent-cities`、`/application-cities`（recommend `TeacherMapService`，按 `TeacherScopeService` 范围过滤）；城市坐标走 `common/CityGeoUtil`。**旧 `components/visualization/China3DMap.vue` + `lib/chinaMap3d/` 已无引用（孤儿），待清理。** 地图默认展示全量分布、自动旋转、hover tooltip、visualMap 图例、城市排行侧栏。**echarts-gl 3D 视觉（柱高/配色/光照）待浏览器微调。**
- **学院内组织结构（2026-07-12 新增）**：`sys_class`（班级：专业-入学年级-班级）、`sys_user.class_id`（学生班级归属，仅学生非空）、`teacher_scope`（教师可见范围：CLASS=班主任 / MAJOR=专业老师 / GRADE=届老师，一教师可多行）。种子：11 个班级 + 4 条教师范围，演示三种可见范围（班主任软工班 3 人 / 专业老师计科 2 人 / 届老师 2022 级 10 人——**均为当前租户内计数**，跨租户裸 SQL 会多算租户2的 2022 班而得 11，别被误导）。增量升级脚本 `upgrade-2026-07-12-class-org.sql`。**注意班级归属放 `sys_user` 不放选填的画像；MAJOR/GRADE 范围经 class 解析，`sys_class.major` 为组织权威专业，与 `sys_student_profile.major`（喂推荐）分工**
- MySQL 容器**首次启动（数据卷为空）时自动执行**它
- **不想 `down -v` 丢数据时**，用同目录的增量脚本给现有库补表：
  ```bash
  docker exec -i occupation-mysql mysql -uroot -proot occupation < occupation-common/src/main/resources/sql/upgrade-2026-07-10-student-resume.sql
  ```
  它是 `CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，可重复执行、不 DROP、不覆盖已有行。内容由脚本从 init.sql 抽取，两处同源不会漂移。
  **`sql/` 下现有三份增量脚本**（`upgrade-2026-07-10-student-resume`、`upgrade-2026-07-10-job-application`、`upgrade-2026-07-12-class-org`）：不 `down -v` 的库要**按日期顺序都跑一遍**才与 init.sql 对齐；每份都幂等，重复跑无副作用

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

## 前端设计系统

> **⚠️ 现状（2026-07-12）**：全站实际生效的是**科技蓝主题** —— 入口 `main.js` 引入 `styles/tech-colors.css`（`--color-*` 调色板，含 `:root` 浅色 + `html.dark` 深色）、`styles/theme-tech.css`（共享类 + Element Plus 覆写）、`styles/interactions.css`。
> 下面「奶油画布 + 墨黑」那套是**上一版 `theme.css` 的设计规范，该文件已删除**（它早已不被任何地方 import）。存量组件仍以 `--app-*` 变量引用颜色，现已在 `tech-colors.css` 末尾以「`--app-*` 兼容层」别名到 `--color-*`（用 `var()` 引用，深色自动跟随）。**新代码请直接用 `--color-*`，`--app-*` 仅为兼容保留。** 下方历史规范中关于图表主题、共享类、深色模式的约定仍然适用。

以下为历史设计规范（奶油/墨黑，theme.css 版，保留作背景）。设计方向：奶油画布 + 墨黑主操作 + **内描边代替阴影**、Inter、深浅双主题。设计系统样品页：https://claude.ai/code/artifact/681fbf62-a0ee-4224-a551-f7348b1a6914

**DESIGN.md 是从 Family（以太坊钱包）落地页扒的规范，有三处不能照搬（原因原写在已删的 theme.css 顶部，要点保留于此）：**
1. 它的强调色作正文字色全部不达标（Grass Green 2.09:1、Gold 2.61:1、连它自己的 Muted Gray 也只有 3.90:1，正文需 4.5:1）。**保留色相压暗**成 `--app-money / --app-score / --app-link / --app-ember / --app-danger`；原始亮色只用于 chip 背景和图表填充
2. DESIGN.md 标注 `Theme: light` 没给深色色值，深色令牌是按同一骨架推演的（暖近黑画布、卡片比画布亮、主操作翻转为奶油）
3. 展示字体 Family 是商用定制字，拿不到；标题用 Inter 600，正文 13–14px（不是 DESIGN.md 的 17px）

- **主题令牌（历史，theme.css 已删）**：原 `theme.css` 的颜色走 `--app-*` 变量（浅色墨黑 `#121212` / 深色奶油 `#f4f1ec`）。**现状**：实际令牌是 `tech-colors.css` 的 `--color-*`（科技蓝，浅色 `:root` + 深色 `html.dark`），`--app-*` 已在该文件末尾别名到 `--color-*` 作兼容层
- **高度**：卡片一律 `box-shadow: var(--app-hairline)`（内描边），**禁止投影**；只有浮层（dialog / dropdown / tooltip）能用 `--app-overlay-shadow`
- **深色模式**：`useAppStore().toggleTheme()` 切 `html.dark`，持久化在 localStorage `theme`。深色下 `--el-color-primary` 接近白色，Element 会把勾/文字也画成白的 —— `theme-tech.css` 已显式覆盖 checkbox / radio / switch，新增控件注意同样问题
- **图表主题**：`src/styles/chartTheme.js` 注册 `app-light` / `app-dark`。八槽分类色板已过色盲校验（浅色相邻 ΔE 24.2，深色对比度全 ≥3:1）。规则：**槽位固定顺序不循环**；单系列图表用 `primarySeriesColor()`；薪资用 `moneySeriesColor()`；**禁止双 Y 轴**（数量与薪资拆两张图，参考 Dashboard.vue）；图表页必须 `watch(() => appStore.dark)` 重绘
- **共享样式类**（现定义在 `theme-tech.css`，全局可用）：`.page-head / .page-title / .page-sub`、`.page-head.with-actions + .page-actions`、`.stat-grid / .stat-card / .stat-value / .stat-label`、`.chip`（中性标签，`.chip.learn` 琥珀）、`.job-grid / .job-card` 系列、`.salary-text`
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
| `pageNum` / `pageSize` | `/api/admin/users`、`/api/report/records`、`/api/push/list`，以及所有吃 `JobQueryDTO` 的（`/api/hr/jobs`、`/api/open/jobs`…） |
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
`saveBlob` 还会**先检查 `blob.type` 是否含 `application/json`**：下载失败时后端返回的是 JSON 错误体（HTTP 200 + json），若不识别就会把 `{"code":..}` 当文件存盘。识别到则弹 `ElMessage.error` 并抛错，不存假文件。

### 5. 雪花 id 必须序列化成字符串，否则前端下载/删除/详情按 id 找不到（2026-07-13 修，全站性）

主键用雪花算法（`BaseEntity` 的 `@TableId(ASSIGN_ID)`），**运行时生成**的 id 是 19 位大整数（约 2e18），超过 JS `Number` 的精确上限 2^53（约 9e15）。后端若把 Long 当**裸数字**返回，浏览器 `JSON.parse` 后**尾数被静默改写**（`2076547053897310210` → `...310200`），前端拿这个变了的 id 去 `/download/{id}`、`/records/{id}` 等，后端按 id 查不到 → 「报告不存在」「点了没反应」。**种子数据 id 小（1/2/5…）不触发**，所以潜伏到下载运行时生成的报告才暴露（曾被误诊为"旧报告/文件丢失"）。
- **修法**：`occupation-common/config/JacksonConfig` 注册 `BeanSerializerModifier`，**只把名为 `id` 或以 `Id` 结尾的 `Long` 字段转字符串**（id/userId/jobId/classId/reportId/tenantId…）。
- **为什么不是全局 Long→String**：会误伤计数字段——`DashboardVO.DimensionItem.count`（Long）喂图表，而 `Dashboard.vue` 有 `sum + (i.count||0)`，字符串会变拼接、把饼图聚合弄坏。所以 `count/jobCount` 保持数字；`value` 本就是 BigDecimal（数字）。`PageResult.total/pageNum/pageSize` 是基本类型 `long`、不匹配包装类，天然不受影响（前端 `toTotal` 用 `typeof==='number'` 判断，靠这个才没坏）。
- 验证：`id` 出参带引号、`count/jobCount/value/total` 不带引号；`JSON.parse` 出的字符串 id 原样下载 → 200 合法 PDF。**加新的按 id 操作的接口时不用管，全局已生效**。

## 采集链路的几个隐坑（2026-07-10 修）

- **MOCK 任务的 `url_pattern` 必须是 `mock/` 目录下的真实文件名**：`CrawlerServiceImpl` 拼的是 `"mock/" + url_pattern`。种子里原来是 `NULL`，会拼出 `mock/null` 静默采到 0 条——因为大家一直点的是 Mock 按钮（它硬编码了文件名），这个坑藏了很久。现在有默认值兜底，种子也已填好
- **不支持的采集源不再返回 500**：`COMPANY_OFFICIAL` 只有表结构没有实现，原先 `createProcessor` 抛 `IllegalArgumentException` 被兜底处理器吞成「系统内部错误」。现在抛 `BizException`，提示「暂不支持的采集源类型」。`BOSS_ZHIPIN` 同样保留分支，但直接告知「robots.txt 明确禁止」
- **种子里的采集任务**：1=MOCK（本地样例）、2/3=ZHAOPIN（真实采集，`url_pattern` 是参数串）、4=COMPANY_OFFICIAL（未实现，用来演示错误提示）、5=MOCK（租户2）
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
- 种子里 12 份简历，覆盖租户1全部有画像的学生。「未填写简历」的空态由 `student12`（userId 16）覆盖 —— 他没画像、没简历，却投了一个站内职位

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

## 2026-07-10 第二轮：职位归属 / 投递状态机 / 就业分析 / 爬虫合规

### 职位分两类，这是理解整个系统的关键

| | 站内职位（24 个） | 采集职位（90 个） |
|---|---|---|
| `source` | `HR_PUBLISH` | `MOCK` / `ZHAOPIN` |
| `publisher_id` | 指向某个 HR | **NULL** |
| 学生能做什么 | **投递简历**（HR 端能看到、能处理） | **自主联系**（跳出平台，只记录意向） |
| 用途 | 真实招聘关系 | 市场标尺：看板、技能热度、教师端诊断、推荐候选池、AI 顾问引用的数字 |

`JobDetailVO.applicable`（= `publisherId != null`）是**唯一的可投递判据**，派生字段不落库。

**为什么采集职位不能投递**：真正的招聘方不知道这个平台存在。放行只会制造「幽灵投递」——学生以为投出去了，HR 端一条也看不到。改造前种子里 30 条投递有 19 条是幽灵。

### student_behavior vs job_application

两张表都记录「投递」，但语义完全不同：

- **`student_behavior`** = 行为埋点。`VIEW/FAVORITE/APPLY/IGNORE/CONTACT`，只增不改，喂推荐算法
- **`job_application`** = 业务实体。五状态机 + HR 备注 + 状态变更时间

`apply()` **双写**两张表。所以推荐算法的行为加权、投递计数、教师端行为明细**一行都没改**。

**行为与归属的硬约束**（生成器里有自检，违反直接抛异常）：`APPLY` 只能落站内职位，`CONTACT` 只能落采集职位。服务端两处守卫互为镜像。

**状态流转**：只能向前推进或直接 `REJECTED`，终态（`OFFER`/`REJECTED`）不可再改。HR 打开投递人详情会自动把 `SUBMITTED` 推进到 `VIEWED`。学生看得到状态，**看不到 `hrNote`**。

### 就业分析：复用 analysis_result，用 SPI 解循环依赖

`analysis` 模块只看得到 `job_detail`（市场供给侧）。学生画像/行为/投递在 `recommend` 里，而 `recommend` 依赖 `analysis`——反过来依赖会成环。

解法：`analysis` 定义 `AnalysisContributor` 扩展点，`recommend` 的 `EmploymentAnalysisContributor` 实现它。`runAll()` 用 `List<AnalysisContributor>` 注入，**在内置维度之后**调用（供需错配要读刚写进去的 city 岗位分布）。新增维度直接写进 `analysis_result`，**不建新表**。

- 漏斗**只统计 `job_application`**，幽灵投递不混入
- `gap_ratio` = 学生意向占比 ÷ 岗位供给占比。>1 学生扎堆，999 是「该城市几乎没岗位」的哨兵值
- **种子里预置了这些维度**（否则新库打开是空图表）。JS（`gen-seed-data.js`）和 Java（`EmploymentAnalysisContributor`）两份实现，取整口径必须一致——**已用脚本逐条比对过 68 条指标，完全一致**。改任何一边都要重新验

### 爬虫：Boss 已删，改抓智联

- **`BossJobPageProcessor` 已删除**。它拼的 `?query=...&city=...` 踩在 `www.zhipin.com/robots.txt` 的 `Disallow: /*?query=*` 上。这不是技术问题，是人家写在门口的。何况页面已是 JS 渲染 + 行为验证
- **`ZhaopinJobPageProcessor` 重写**：解析列表页里服务端注入的 `window.__INITIAL_STATE__.positionList`，不再写死 XPath。字段齐全（`industryName` / `publishTime` / `jobSkillTags` / `salaryReal`），页面换皮不影响解析
- **必须先解 301**：`sou.zhaopin.com/?kw=X&jl=Y` → `www.zhaopin.com/sou/jl653/kwXXXX`。目标域的 robots 有 `Disallow: /*?*`（禁一切带查询串的 URL），而跳转后的路径式地址不带查询串，才是被允许的那个
- **新增 `RobotsRules`**：抓取前校验 robots.txt。WebMagic 不会自动遵守，本项目原先完全没有这道关。实现保守——只解析 `User-agent: *` 段的 `Disallow`，拉不到 robots.txt 视为无限制，解析异常按禁止处理。`?` 在 robots 里是**普通字符**不是正则元字符（有单测守着）
- **只抓列表页，不进详情页**：详情页可能带出 HR 手机号，抓下来就进了 `raw_job_data.raw_content`
- 真实采集改为**单线程 + 5~10 秒随机间隔**（原来是 `thread(3)`）
- `crawler_task.url_pattern` 对 ZHAOPIN 存的是**参数串**而非 URL：`kw=Java&jl=653&maxPages=2`（`jl` 是智联城市编码，653=杭州）
- robots 校验不通过时**不抛异常**，而是把日志标成 FAILED 后返回。`startCrawl` 带 `@Transactional(rollbackFor = Exception.class)`，抛 `BizException`（RuntimeException）会把刚写的 FAILED 日志一起回滚

### 学生首页分两栏

「可投递岗位」和「市场参考」两栏。**没有改推荐打分**——只是展示层按 `applicable` 分组。改打分的话，匹配分的语义就变了，教师端和 AI 顾问引用的数字也跟着变。

`CONTACT` 行为的推荐权重是 +2（与 `APPLY` 同级），已联系过的职位不再出现在推荐里。**现有数据里一条 `CONTACT` 都没有，所以加它对既有推荐结果零影响**——改造前后 20 条推荐的职位 ID 与分数完全一致，有脚本验过。

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

## 学院组织结构与教师可见范围（2026-07-12，功能①前后端已完成）

学院（租户）**内部**加了 `专业 → 入学年级 → 班级` 三层结构，教师按范围分三种可见性。DDL/种子见上方「数据库协作流程」的组织结构说明。

- **数据模型**：`sys_class`（班级）、`sys_user.class_id`（学生班级归属）、`teacher_scope`（教师范围）。实体/服务在 **occupation-auth**（`SysClass`/`TeacherScope`/`ClassService`/`TeacherScopeService`），recommend 依赖 auth 调用它。两张新表都带 `tenant_id`，多租户插件自动隔离。
- **教师 = TEACHER + 范围**，不是新角色。`teacher_scope.scope_type`：`CLASS`（班主任，值=班级id）/ `MAJOR`（专业老师，值=专业名）/ `GRADE`（届老师，值=入学年级）。一个教师可多条。
- **范围强制是安全核心**：`TeacherScopeService.visibleStudentIds(teacherId, role)` 解析 scope→班级id→学生userId 集合（**null=ADMIN 不受限；空集=看不到；否则为可见集**）。`TeacherController` 全部学生接口都经它过滤；`/students/{userId}/*` 带**归属校验**（不在可见集→`BizException(403)`，防枚举越权，镜像 HR 端）。`class.major` 是组织权威专业（scope 解析走它），`sys_student_profile.major` 仍是喂推荐的自填专业，二者分工。
- **新增接口**：
  - `GET /api/teacher/students`：加 `major` / `enrollYear` 筛选参数（在可见范围内二次收窄）；StudentVO 增 `classId/classCode/enrollYear`（**只加字段**）；概览 `overview` 数字按范围计
  - `GET /api/teacher/filters`：可见范围内的专业/年级下拉
  - `GET/POST/DELETE /api/admin/classes`、`POST /api/admin/classes/{id}/students`（分配学生）、`GET /api/admin/classes/filters`：**ADMIN** 班级管理
  - `GET/POST/DELETE /api/admin/teacher-scopes`：**ADMIN** 配置教师范围
- **前端**（`api/student.js` 加 `getTeacherFilters`；`api/admin.js` 加班级 CRUD/分配 + 教师范围函数）：
  - 教师端 `views/teacher/Students.vue`：加班级列（`classCode`）+ 专业/年级筛选下拉
  - 管理端 `views/admin/ClassManage.vue`（**新页面**，路由 `/admin/class`，ADMIN 菜单「班级管理」）：Tab「班级」CRUD + 分配学生；Tab「教师范围」按教师配置班主任/专业老师/届老师
- **验证过**（真实 HTTP + `npm run build`，2026-07-12）：teacher01=3/2、teacher02=2/2、teacher(届2022)=10/9、admin=14/12；越权 403；租户隔离（届老师只见本租户 2022）；班级管理、筛选、班级展示、专业/年级筛选均正确。**注意中文 query 参数**（major）前端 axios 会 UTF-8 编码，命令行 curl 测要手工百分号编码，否则误判为 0。**浏览器肉眼点验尚未做**（留待统一测试）

## 报告：两大类、无模板、数据驱动（2026-07-13，已简化并验证）

**⚠️ 2026-07-13 大简化：彻底去掉了「模板」层。** 报告不再"先建模板、再从模板生成"，而是**直接选 大类 + 范围 + 格式** 一步生成。`report_template` 表、`ReportTemplate` 实体/Service/Controller/Mapper、`TemplateSaveDTO`、前端 `ReportTemplate.vue` 及模板 API、菜单「模板管理」**全部删除**。增量脚本 `upgrade-2026-07-13-report-simplify.sql`（`report_record` 加 `name`、**去掉 `template_id`**——原列 NOT NULL 会挡新插入、务必去掉、删 `report_template`）。

- **两大类**（`report_record.category`）：
  - **MARKET 市场行业**：`AnalysisService.getDashboard()` → 智能摘要 + 行业/技能/城市/学历/趋势（PDF/HTML/Word **统一 6 节**）。
  - **EMPLOYMENT 学生就业**：按 scope（专业/年级/班级，均可空→全校）聚合。`EmploymentReportService`（recommend，`report` 直接依赖 recommend、无环）解析 scope→学生 userId（走 `ClassService`），产出 `EmploymentReportData`（投递漏斗/意向城市行业/薪资分桶/技能掌握，投递**只统计 job_application**）。摘要为**规则化文字**（无 AI 依赖）。
- **生成**：`GenerateReportDTO` = `category` + `fileType` + `major/enrollYear/classId`（就业类范围，无 templateId）。`generate()` 按 `dto.category` 分支，用**固定内置模板**（`DEFAULT_TEMPLATE` / `DEFAULT_EMPLOYMENT_TEMPLATE`，不再有自定义模板内容）。报告名 `report_record.name` 按 大类+范围**自动生成**（如「学生就业数据报告（软件工程-2022-1班）」）；scope 存进 `params`。`ReportRecordVO` = `name` + `category`（不再有 templateName/type）。
- **前端**：`views/admin/ReportList.vue`（报告中心，菜单「报告中心」直达）生成弹窗 = 选大类 → 就业类再选范围（班级优先，否则专业/年级，不选=全校）→ 选格式；列表显示 报告名 + 大类。
- **验证过**（真实 HTTP，2026-07-13）：市场报告 → SUCCESS；就业报告(班级1) → 名「学生就业数据报告（软件工程-2022-1班）」SUCCESS。`mvn install` + `npm build` 通过。
- **学生个人 AI 分析报告（2026-07-13，已完成并验证）**：`report_record` 加 `user_id`（归属；**NULL=管理员租户级报告**，有值=学生个人报告）+ 新大类 `STUDENT_AI`（升级脚本 `upgrade-2026-07-13-report-user.sql`，已灌真库）。后端在 **report 模块**（`StudentAiReportService`/`StudentReportController`，`@PreAuthorize STUDENT`）：`POST /api/student/ai-report/preview`（综合本人 画像+简历+推荐匹配+技能缺口+市场热点 → AI 写个性化求职分析，AI 关则规则化兜底；支持前端持多轮历史"让AI改"，**不落库**）、`POST /api/student/ai-report/save`（定稿落库为个人报告 + 导出 PDF/HTML）、`GET /api/student/reports`（只列本人）。**归属校验**：`loadReportFile` 对 `user_id` 非空的报告校验只有本人/ADMIN 可下载（防枚举越权，已验证 teacher 下学生报告→403）。租户级列表 `pageRecords`/`latestSuccess` 加了 `user_id IS NULL` 过滤，学生个人报告不混入。前端 `views/student/Reports.vue` 重做（修好字段+只显示自己的+生成/多轮改/保存弹窗）。**AI 对话/润色现状**：均不存库、后端无状态、前端持多轮历史；唯一持久化的 AI 输出是 `report_record.ai_summary`。验证过（真实 HTTP，AI 已启用）：预览返回真 AI 内容、保存 STUDENT_AI 记录、我的报告只本人、越权下载 403。
- **报告分发：管理员发送 → 学生接收（2026-07-13，已验证）**：新增 **`report_delivery` 表**（init.sql 升到 **19 张表**；增量脚本 `upgrade-2026-07-13-report-delivery.sql`，已灌真库）。产品决策（用户选定）：**①按范围批量发送（ALL/MAJOR/GRADE/CLASS）；②市场报告发布即全体可见（广播、不落 delivery），就业报告按范围定向发送（每接收学生落一行，`uk_report_user` 防重）。** 后端（report 模块）：`ReportDeliveryService`（`deliver` 经 auth 的 `ClassService` 解析范围→userId；`receivedFor` 广播市场+定向下发内存合并分页；`canStudentAccess`；`markRead`）；`ClassService` 加 `allStudentIds()`。接口：`POST /api/report/{id}/deliver`、`GET /api/report/{id}/delivery-count`（ADMIN，**下发市场报告会被拒**）；`GET /api/student/received-reports`、`POST /api/student/received-reports/{id}/read`（STUDENT）。**`loadReportFile` 归属校验扩展**：租户级报告 **STUDENT 仅能下广播市场 或 已下发给自己的就业报告（否则 403）**，ADMIN/TEACHER/HR 不受限。前端：admin `ReportList.vue` 加两类报告生成依据说明 + 就业报告「发送」范围弹窗；student `Reports.vue` 拆两 Tab（我的 AI 报告 / 收到的报告，未读角标）。验证：市场下发被拒；就业发软工班=3 人；student01(软工)收到广播市场+定向就业并下 200 合法 PDF、student02(数据科学)只收市场、下就业**403**、下广播市场 200；mark-read 翻转成功。
- **下载健壮性（`utils/download.js` 的 `saveBlob`）**：后端下载失败返回的是 JSON 错误体（HTTP 200 + `application/json`），但 `responseType:'blob'` 会把它也变成 Blob。`saveBlob` **必须先检查 `blob.type` 含 `application/json`**——是则读文本、`ElMessage.error(message)` 并抛错，**不能当文件存盘**（否则用户打开「PDF」看到的是 `{"code":500,...}`，约 67B）。已修。

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

### 第二轮（职位归属 / 投递状态机 / 就业分析 / 爬虫）

- `mvn test`（**25 个单测**：原 18 + `RobotsRulesTest` 7 个）与 `npm run build` 均通过
- **零破坏保证**：改造前后对 19 个接口做响应结构快照比对，三次都是「**只有新增字段，零字段缺失**」
- **推荐算法未受影响**：加 `CONTACT` 前后，同一学生的 20 条推荐职位 ID 与分数**逐条完全一致**
- **口径一致性**：种子预置的 68 条就业分析指标（JS 算的）与后端「重算分析数据」（Java 算的）**逐条完全一致**
- 幽灵职位：无主职位投递被拒；种子里 19 条幽灵投递已清零（改成 VIEW/FAVORITE/CONTACT）
- 投递状态机：五状态流转、终态不可回退、`hr01` 改 `hr` 的投递被 403、HR 打开详情自动 `SUBMITTED→VIEWED`、学生看不到 `hrNote`
- 就业分析：漏斗总数 = `job_application` 行数（幽灵投递未混入）、错配比 = 学生占比÷岗位占比、薪资分桶按金额而非人数排序、`/employment` 限 ADMIN+TEACHER（学生与 HR 均 403）
- 种子数据自洽性：在**临时库** `occupation_verify` 里跑（`init.sql` 开头有 `USE occupation;`，直接导会打到真库并 DROP 表，必须先 `sed` 掉那两行）——规模、行为归属硬约束、`APPLY↔job_application` 一一对应、每个 HR 都有职位且都收到投递、边界用例仍在
- 采集链路：真实调用「启动任务」验证 `raw_job_data +20 / job_detail +0（去重生效）/ crawler_log +1 / crawler_task 不增`
- **未做**：真实抓取智联的端到端验证（只验证了 robots 规则、URL 解析、`__INITIAL_STATE__` 的结构，没有真的跑一次 Spider）

### 第一轮（简历 / HR 解密 / DeepSeek）

- `mvn test`（18 个单测）与 `npm run build` 均通过
- **AI 开启时**已用真实 HTTP + 真实 DeepSeek 调用验证：简历 CRUD 与校验 / AI 诊断（score+4 条建议）/ AI 润色 / 顾问对话（引用了真实岗位数）/ 匹配理由解读 / 教学建议解读 / **prompt 注入被丢弃**
- **AI 关闭时**（队友的默认状态）验证全部降级路径：诊断退规则、顾问给兜底文案、匹配理由退规则、教学建议退规则、润色明确报错；`aiGenerated` 均为 `false`
- HR：投递列表含身份不含联系方式 / 详情含简历全文 / **越权 403**（枚举 userId 与跨 HR 查看都被拦）/ 人才浏览仍全脱敏
- 早前已验证：租户下拉 / 五个角色登录 / 教师看板统计 / 技能缺口诊断非随机 / HR「只看我发布的」(2/1/3) / Excel 导出 / PDF 内嵌中文字体 / Word 结构化表格 / 开放 API BCrypt 校验
- **报告下载已重新验证无误**：用真实数据库拉取，PDF/Word 下载字节与磁盘文件 sha256 完全一致；PDF 为 3 页、内嵌 SimSun 子集、51 个文字块。此前「下载打不开」的报告应为旧 jar 所致（见启动流程那节）
- **未做**：浏览器里的人工点验（深色模式配色、图表重绘、简历表单与顾问对话的交互手感）；XXL-Job 分布式调度未跑通

## 下次接手前必读的三条

1. **改了非 web 模块，先 `mvn install -DskipTests`**。`spring-boot:run -pl occupation-web` 从 `.m2` 取旧 jar。真实咬过人：曾有两个「bug」（学历不同步、报告下载打不开）在同一份代码上复现不出来，根因就是这个。`mvn clean compile` 治不了——它不往 `.m2` 装 jar
2. **写针对数据库的验证脚本时，清理逻辑必须适配当前种子**。本轮有个脚本的清理语句是 `UPDATE job_application SET status='SUBMITTED' WHERE status='VIEWED'`——它是照着旧种子（全部 SUBMITTED）写的，在新种子上跑了一次，把 18 条 VIEWED 全改坏了。`init.sql` 没受影响（程序从不回写它），`down -v` 重建即可，但排查时会一头雾水
3. **验证种子数据别在真库上做**。`init.sql` 第 8–12 行有 `CREATE DATABASE occupation;` + `USE occupation;`，`mysql -uroot -p 别的库名 < init.sql` 里指定的库名**会被脚本自己的 `USE` 覆盖**，DROP TABLE 直接打在 `occupation` 上。先 `sed` 掉那两行再导进临时库
