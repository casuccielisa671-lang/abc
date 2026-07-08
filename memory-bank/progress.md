# Progress — 开发进度

> 每完成一个 Step 后由 AI 更新，便于新会话恢复上下文。
> **最后更新**: 2026-07-08（全模块深度审计：API 清单 + 数据库 Schema + Service 实现度 + 前端页面状态）

---

## 一、项目整体完成度

| 阶段 | 内容 | 后端 Service | 前端页面 | 状态 |
|---|---|---|---|---|
| 先启阶段 | 4 份管理/技术文档 | — | — | ✅ 100% |
| P1 | 项目骨架 + 数据采集 | 8/8 方法完整 | 登录+采集管理 | ✅ 100% |
| P2 | 大数据分析模块 | 16/16 方法完整 | Dashboard+职位查询 | ✅ 100% |
| P3 | 报告自动生成系统 | 10/10 方法完整 | 模板/列表页(占位) | ✅ 后端完整，前端待开发 |
| P4 | 岗位推送 + 学生端 | 16/16 方法完整 | 首页/详情/画像/收藏 | ✅ 后端完整，学生端已实现 |
| P5 | 对外 API + 教师/HR端 | 5/5 方法完整 | 教师/HR页(占位) | ✅ 后端完整，前端待开发 |
| P6 | AI 差异化升级 | 0% | 0% | ⏳ 规划中 |

> **后端 Service 总计：16 个实现类，62 个方法，61 个完整实现，仅 1 个部分实现（智联招聘采集器）。**

---

## 二、已实现功能模块一览

### 2.1 认证与多租户（occupation-auth）

| 功能 | 说明 | 状态 |
|---|---|---|
| JWT 登录 | `POST /api/auth/login` — 用户名+密码+租户名 → JWT Token | ✅ 已验证 |
| 多租户隔离 | MyBatis-Plus 插件自动注入 `tenant_id`，TenantContextHolder 线程级传递 | ✅ |
| 角色权限 | ADMIN / TEACHER / STUDENT / HR 四角色，`@PreAuthorize` 注解鉴权 | ✅ |
| BCrypt 密码加密 | 用户密码 BCrypt 哈希存储 | ✅ |
| 用户管理 | 管理员 CRUD 用户（分页/搜索/启用禁用），编辑时密码留空不修改 | ✅ |
| JWT 过滤器 | 自动解析 Token → SecurityContext，白名单放行 `/api/auth/**`、`/api/health/**`、`/api/open/**` | ✅ |

### 2.2 数据采集（occupation-crawler）

| 功能 | 说明 | 状态 |
|---|---|---|
| 采集任务管理 | 创建/编辑/删除/列表/详情 + 手动启停 | ✅ 已验证 |
| Mock 模拟采集 | `POST /api/admin/crawler/task/mock` — 从本地 JSON 读取 20 条测试数据 | ✅ 已验证 |
| BOSS 直聘采集 | 真实爬虫：列表页解析 + 详情页解析 + 分页翻页 + 反爬策略 | ✅ 代码就绪 |
| 智联招聘采集 | ZHAOPIN 采集源 | ⚠️ 未实现（抛出 UnsupportedOperationException） |
| WebMagic 集成 | JobPageProcessor 抽象基类（UA 轮换/随机延迟/重试3次）+ JobPipeline → Kafka | ✅ |
| 采集日志 | 按任务查询采集日志（分页），记录成功/失败数 | ✅ |
| 定时调度 | `@Scheduled` 定时扫描 + XXL-Job 条件装配（默认关闭） | ✅ |

### 2.3 数据分析（occupation-analysis）

| 功能 | 说明 | 状态 |
|---|---|---|
| Dashboard 大盘 | `GET /api/analysis/dashboard` — 5 维度：行业 Top10、城市分布、技能 Top20、学历分布、薪资趋势 | ✅ 已验证 |
| 职位查询 | `GET /api/analysis/jobs` — 分页 + 7 种筛选（城市/行业/薪资/学历/经验/关键词/排序） | ✅ 已验证 |
| 数据清洗 | `POST /api/analysis/clean` — raw_job_data → job_detail（去重/标准化/校验） | ✅ 已验证 |
| 统计重算 | `POST /api/analysis/rebuild` — job_detail → analysis_result（5 维度聚合） | ✅ 已验证 |
| 完整流水线 | `POST /api/analysis/pipeline` — 一键清洗+重算 | ✅ 已验证 |
| Kafka 实时清洗 | JobDataCleanListener（job-clean-group 消费组）实时消费清洗 | ✅ |
| 定时清洗 | AnalysisScheduler — 每小时自动清洗 + 每日凌晨 2 点自动重算 | ✅ |
| 职位写入 | HR 发布/编辑/下架职位（仅允许操作 HR_PUBLISH 来源） | ✅ |
| 清洗规则 | 城市标准化（去"市/省"后缀）、薪资异常过滤、学历五档映射、技能 JSON 解析 | ✅ |
| 存量补偿 | `cleanPendingRawData()` 批量扫描 status=RAW 记录（每批 500 条） | ✅ |

### 2.4 报告系统（occupation-report）

| 功能 | 说明 | 状态 |
|---|---|---|
| 报告模板管理 | 模板 CRUD（名称/类型/行业/内容模板），多租户隔离 | ✅ 完整实现 |
| 报告生成流水线 | 六步流程：读模板 → 取分析数据 → AI 摘要 → Freemarker 渲染 → 格式导出 → 归档落盘 | ✅ 完整实现 |
| AI 摘要 | 调用 OpenAI 兼容 LLM 接口生成就业洞察，失败时自动降级为规则模板摘要 | ✅ 完整实现 |
| PDF 导出 | Flying Saucer + Freemarker 模板引擎生成 PDF | ✅ 完整实现 |
| Word 导出 | Apache POI 生成 .docx | ✅ 基础实现（结构化表格导出标记为 TODO） |
| HTML 导出 | Freemarker 直接渲染 HTML 输出 | ✅ 完整实现 |
| 报告下载 | `GET /api/report/download/{id}` — 从磁盘读取并返回报告文件字节流 | ✅ 完整实现 |
| 报告管理 | 分页查询报告记录 + 删除（含物理文件清理） | ✅ 完整实现 |

### 2.5 岗位推荐（occupation-recommend）

| 功能 | 说明 | 状态 |
|---|---|---|
| 学生画像 | 填写/查看/更新个人画像（专业/技能/学历/意向城市/期望薪资），含默认值处理 | ✅ 完整实现 |
| 个性化推荐 | `GET /api/student/recommend` — 四维打分模型（技能40%+学历25%+城市20%+薪资15%），含匹配理由和缺失技能 | ✅ 完整实现 |
| 候选集初筛 | 按意向城市筛选，不足时自动放开城市限制 | ✅ 完整实现 |
| 技能匹配 | JSON 数组解析（兼容逗号分隔旧格式），技能覆盖比例计算 | ✅ 完整实现 |
| 学历匹配 | 五档学历等级比较（专科/本科/硕士/博士/不限） | ✅ 完整实现 |
| 薪资匹配 | 期望薪资区间与职位薪资区间的交集判断 | ✅ 完整实现 |
| 职位收藏 | 收藏/取消收藏 + 收藏列表 | ✅ 完整实现 |
| 职位投递 | 投递职位 → 记录行为 + 自动生成推送通知 | ✅ 完整实现 |
| 行为追踪 | 自动记录 VIEW / FAVORITE / APPLY 行为（VIEW 可重复，其他幂等） | ✅ 完整实现 |
| 推送通知 | 投递成功推送 + 未读红点计数 + 标记已读（含归属校验） | ✅ 完整实现 |
| 求职统计 | `GET /api/student/profile/stats` — 浏览/收藏/投递计数 | ✅ 完整实现 |
| 教师端 | 查看本校学生画像列表 + 行为统计 + 行为明细 | ✅ 完整实现 |
| HR 端 | 发布/编辑/下架职位 + 脱敏人才浏览 | ✅ 完整实现 |

### 2.6 对外开放 API（occupation-api）

| 功能 | 说明 | 状态 |
|---|---|---|
| Token 鉴权 | `POST /api/open/auth/token` — apiKey + apiSecret → accessToken（Redis 存储，30 分钟过期） | ✅ 完整实现 |
| Token 校验 | 拦截器从 Redis 校验 Token 有效性 | ✅ 完整实现 |
| 限流拦截器 | 基于 Token 的请求频率限制 | ✅ 完整实现 |
| 职位查询 | `GET /api/open/jobs` — 开放职位检索 | ✅ 完整实现 |
| 就业大盘 | `GET /api/open/stats/overview` — 总岗位数/平均薪资/城市数/行业数（Redis 缓存 10 分钟） | ✅ 完整实现 |
| 热门技能 | `GET /api/open/stats/skills/hot` — Java 内存词频统计 TopN（Redis 缓存 10 分钟） | ✅ 完整实现 |
| 行业分布 | `GET /api/open/stats/industries` — SQL GROUP BY 行业岗位分布 | ✅ 完整实现 |

### 2.7 前端（occupation-web-ui / Vue 3）

| 功能 | 说明 | 状态 |
|---|---|---|
| 项目脚手架 | Vite + Vue 3 + Element Plus + ECharts + Axios + Pinia + Vue Router | ✅ |
| 登录页面 | 租户选择 + 用户名密码登录 + Token 持久化 | ✅ 已实现 |
| 角色路由 | 4 套路由（Admin/Student/Teacher/HR），含路由守卫 + 角色校验 | ✅ 已实现 |
| 管理员 Dashboard | ECharts 图表展示 5 维度分析数据 | ✅ 已实现 |
| 管理员采集管理 | 采集任务 CRUD + 启停 + Mock 采集 + 日志查看 | ✅ 已实现 |
| 学生端首页 | 推荐职位列表 + 搜索筛选 | ✅ 已实现 |
| 学生端职位详情 | 职位信息展示 + 收藏/投递 | ✅ 已实现 |
| 学生端个人画像 | 画像填写/编辑表单 | ✅ 已实现 |
| 学生端收藏夹 | 收藏列表管理 | ✅ 已实现 |
| AppLayout | 侧边栏导航 + 顶部用户信息 + 退出登录 | ✅ 已实现 |
| 管理员用户管理 | UserManage.vue | ⚠️ 占位组件（231B） |
| 管理员报告模板 | ReportTemplate.vue | ⚠️ 占位组件（243B） |
| 管理员报告列表 | ReportList.vue | ⚠️ 占位组件（278B） |
| 学生端报告 | Reports.vue | ⚠️ 占位组件（238B） |
| 教师端首页 | TeacherHome.vue | ⚠️ 占位组件（244B） |
| 教师端学生管理 | Students.vue | ⚠️ 占位组件（242B） |
| 教师端建议 | Suggestions.vue | ⚠️ 占位组件（251B） |
| HR 端首页 | HrHome.vue | ⚠️ 占位组件（228B） |
| HR 端职位管理 | JobManage.vue | ⚠️ 占位组件（209B） |
| HR 端人才库 | Talents.vue | ⚠️ 占位组件（218B） |

---

## 三、完整 API 接口清单（59 个端点）

### 3.1 认证与用户（5 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/auth/login` | 用户登录，返回 JWT Token | 公开 |
| GET | `/api/admin/users` | 用户分页列表（按角色/关键词筛选） | ADMIN |
| POST | `/api/admin/users` | 新增用户（密码 BCrypt 加密） | ADMIN |
| PUT | `/api/admin/users/{id}` | 编辑用户（密码留空不修改） | ADMIN |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用用户 | ADMIN |

### 3.2 健康检查（3 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/health` | 健康检查 | 公开 |
| GET | `/api/health/db` | 数据库连接测试 | 公开 |
| GET | `/api/health/error` | 异常测试（验证全局异常处理器） | 公开 |

### 3.3 采集管理（10 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/admin/crawler/task` | 创建采集任务 | ADMIN |
| GET | `/api/admin/crawler/task` | 任务列表（分页） | ADMIN |
| GET | `/api/admin/crawler/task/{id}` | 任务详情 | ADMIN |
| PUT | `/api/admin/crawler/task/{id}` | 更新任务 | ADMIN |
| DELETE | `/api/admin/crawler/task/{id}` | 删除任务 | ADMIN |
| PUT | `/api/admin/crawler/task/{id}/start` | 启动任务（自动选处理器：MOCK/BOSS_ZHIPIN） | ADMIN |
| PUT | `/api/admin/crawler/task/{id}/stop` | 停止任务 | ADMIN |
| POST | `/api/admin/crawler/task/mock` | 启动模拟爬虫（强制使用 Mock） | ADMIN |
| GET | `/api/admin/crawler/log` | 采集日志列表（分页，按任务筛选） | ADMIN |
| GET | `/api/admin/crawler/log/{id}` | 日志详情 | ADMIN |

### 3.4 数据分析（5 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/analysis/dashboard` | Dashboard 5 维度（行业Top10/城市/技能Top20/学历/趋势） | 登录用户 |
| GET | `/api/analysis/jobs` | 职位分页查询（7 种筛选：城市/行业/薪资/学历/经验/关键词/排序） | 登录用户 |
| POST | `/api/analysis/clean` | 手动触发数据清洗（raw_job_data → job_detail） | ADMIN |
| POST | `/api/analysis/rebuild` | 手动触发统计重算（job_detail → analysis_result） | ADMIN |
| POST | `/api/analysis/pipeline` | 一键清洗+重算流水线 | ADMIN |

### 3.5 报告系统（9 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/report/generate` | 触发生成报告（六步流水线：数据→渲染→AI摘要→导出→归档） | ADMIN |
| GET | `/api/report/records` | 报告记录列表（分页） | 登录用户 |
| GET | `/api/report/download/{id}` | 下载报告文件（PDF/Word/HTML） | 登录用户 |
| DELETE | `/api/report/records/{id}` | 删除报告（含物理文件清理） | ADMIN |
| GET | `/api/admin/report/template` | 模板分页列表 | ADMIN |
| GET | `/api/admin/report/template/{id}` | 模板详情 | ADMIN |
| POST | `/api/admin/report/template` | 新增模板 | ADMIN |
| PUT | `/api/admin/report/template/{id}` | 编辑模板 | ADMIN |
| DELETE | `/api/admin/report/template/{id}` | 删除模板 | ADMIN |

### 3.6 岗位推荐 — 学生端（9 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/student/recommend` | 个性化推荐（四维打分：技能40%+学历25%+城市20%+薪资15%） | STUDENT |
| GET | `/api/student/job/{jobId}` | 职位详情（自动记录 VIEW 行为） | STUDENT |
| POST | `/api/student/job/{jobId}/favorite` | 收藏职位（幂等） | STUDENT |
| DELETE | `/api/student/job/{jobId}/favorite` | 取消收藏 | STUDENT |
| GET | `/api/student/favorites` | 收藏列表 | STUDENT |
| POST | `/api/student/job/{jobId}/apply` | 投递职位（记录行为 + 生成推送通知） | STUDENT |
| GET | `/api/student/profile` | 查看个人画像 | STUDENT |
| PUT | `/api/student/profile` | 保存/更新画像（含技能默认值处理） | STUDENT |
| GET | `/api/student/profile/stats` | 个人求职统计（浏览/收藏/投递计数） | STUDENT |

### 3.7 岗位推荐 — 教师端（3 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/teacher/students` | 本校学生画像列表 | TEACHER/ADMIN |
| GET | `/api/teacher/students/{userId}/stats` | 学生求职行为统计 | TEACHER/ADMIN |
| GET | `/api/teacher/students/{userId}/behaviors` | 学生行为明细（最近行为记录） | TEACHER/ADMIN |

### 3.8 岗位推荐 — HR 端（5 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/hr/jobs` | 发布职位 | HR |
| PUT | `/api/hr/jobs/{id}` | 编辑职位（仅允许编辑 HR_PUBLISH 来源） | HR |
| DELETE | `/api/hr/jobs/{id}` | 下架职位（仅允许下架 HR_PUBLISH 来源） | HR |
| GET | `/api/hr/jobs` | 职位列表 | HR |
| GET | `/api/hr/talents` | 人才浏览（脱敏） | HR |

### 3.9 推送通知（3 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/push/list` | 我的推送列表（分页，未读优先） | 登录用户 |
| PUT | `/api/push/{id}/read` | 标记已读（含归属校验） | 登录用户 |
| GET | `/api/push/unread/count` | 未读数量 | 登录用户 |

### 3.10 对外开放 API（6 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/open/auth/token` | apiKey+apiSecret 换取 accessToken（Redis 存储，30分钟过期） | 公开 |
| GET | `/api/open/jobs` | 职位查询 | Bearer Token |
| GET | `/api/open/jobs/{id}` | 职位详情 | Bearer Token |
| GET | `/api/open/stats/overview` | 就业大盘（总岗位数/平均薪资/城市数/行业数，缓存10分钟） | Bearer Token |
| GET | `/api/open/stats/skills/hot` | 热门技能排行（词频统计 TopN，缓存10分钟） | Bearer Token |
| GET | `/api/open/stats/industries` | 行业岗位分布（SQL GROUP BY） | Bearer Token |

### 3.11 测试接口（1 个）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/test/kafka/send` | 发送测试数据到 Kafka | 开发用 |

---

## 四、数据库设计（14 张表）

### 4.1 表结构总览

| # | 表名 | 所属模块 | 多租户 | 逻辑删除 | 说明 |
|---|---|---|---|---|---|
| 1 | `sys_tenant` | common | ❌（自身是租户表） | ❌ | 租户表 |
| 2 | `sys_user` | auth | ✅ | ✅ | 用户表 |
| 3 | `sys_student_profile` | recommend | ✅ | ✅ | 学生画像表 |
| 4 | `crawler_task` | crawler | ✅ | ✅ | 采集任务表 |
| 5 | `crawler_log` | crawler | ✅ | ❌ | 采集日志表 |
| 6 | `raw_job_data` | common | ❌（全平台共享） | ❌ | 原始职位数据表 |
| 7 | `job_detail` | analysis | ❌（全平台共享） | ❌ | 清洗后职位详情表 |
| 8 | `analysis_result` | analysis | ✅ | ❌ | 分析结果表 |
| 9 | `report_template` | report | ✅ | ✅ | 报告模板表 |
| 10 | `report_record` | report | ✅ | ✅ | 报告记录表 |
| 11 | `push_record` | recommend | ✅ | ❌ | 推送记录表 |
| 12 | `student_behavior` | recommend | ✅ | ❌ | 学生行为记录表 |
| 13 | `api_client` | api | ✅ | ✅ | API 客户端表 |
| 14 | `sys_alert` | — | ✅ | ❌ | ⚠️ 系统告警表（无对应 Entity 类） |

### 4.2 关键表字段

**`job_detail`**：title, company, city, industry, salary_min, salary_max, education, experience, skills(JSON), description, publish_date, source, source_url — 联合索引 `idx_salary`

**`analysis_result`**：dimension(industry/city/skill/education/trend), dimension_value, metric_name(job_count/avg_salary_min/avg_salary_max), metric_value, period_type, period_value, calc_time

**`student_behavior`**：user_id, job_id, action(VIEW/FAVORITE/APPLY/IGNORE) — VIEW 允许重复，其他幂等

### 4.3 种子数据（init.sql）

| 表 | 数据 | 说明 |
|---|---|---|
| `sys_tenant` | id=1, name='测试学院' | 默认租户 |
| `sys_user` | admin / admin123（BCrypt）/ ADMIN 角色 | 默认管理员 |
| `api_client` | apiKey=`occ_test_2026`, apiSecret=`demo_secret_key_for_dev` | 测试 API 客户端 |

### 4.4 数据库现状（2026-07-08）

| 表名 | 数据量 | 说明 |
|---|---|---|
| `sys_tenant` | 1 | 测试学院 |
| `sys_user` | 1 | admin / ADMIN 角色 |
| `crawler_task` | 若干 | 采集任务记录 |
| `crawler_log` | 若干 | 采集日志 |
| `raw_job_data` | 40 条 | 原始采集数据（状态：CLEANED） |
| `job_detail` | 20 条 | 清洗后职位数据（腾讯/阿里/字节/百度等） |
| `analysis_result` | 119 条 | 5 维度分析结果 |
| `api_client` | 1 | 测试客户端（occ_test_2026） |
| `sys_student_profile` | 0 | 待学生注册填写 |
| `report_template` | 0 | 待管理员配置 |
| `report_record` | 0 | 待触发生成 |
| `push_record` | 0 | 待推荐推送产生 |
| `student_behavior` | 0 | 待学生操作产生 |

---

## 五、后端 Service 实现完整度审计

> 共审计 **16 个 ServiceImpl 类，62 个方法**。61 个完整实现，1 个部分实现。

### 5.1 occupation-auth（2 类 8 方法）

| 类 | 关键方法 | 状态 |
|---|---|---|
| `TenantServiceImpl` | getById, getByName, create | ✅ 全部完整 |
| `UserServiceImpl` | getByUsername, pageUsers(分页/角色/关键词筛选), saveUser(BCrypt加密), updateStatus, toVO(脱敏) | ✅ 全部完整 |

### 5.2 occupation-crawler（1 类 7 方法）

| 类 | 关键方法 | 状态 |
|---|---|---|
| `CrawlerServiceImpl` | startCrawl(停旧→创日志→选处理器→启动), stopCrawl, startMockCrawl, listRunningTasks, getLogsByTaskId | ✅ 全部完整 |
| | createProcessor | ⚠️ MOCK/BOSS_ZHIPIN 已实现，ZHAOPIN 抛出 UnsupportedOperationException |

### 5.3 occupation-analysis（4 类 16 方法）

| 类 | 关键方法 | 状态 |
|---|---|---|
| `AnalysisServiceImpl` | getDashboard(5维度), queryDimension, queryTrend, applyDateFilter | ✅ 全部完整 |
| `JobDetailServiceImpl` | queryJobs(7种筛选+薪资交集), saveJob/removeJob(仅HR_PUBLISH) | ✅ 全部完整 |
| `DataCleanServiceImpl` | cleanAndSave(去重/校验/标准化), cleanPendingRawData(存量补偿500条/批), normalizeCity/Salary/Education, extractSkills | ✅ 全部完整 |
| `AnalysisJobServiceImpl` | runAll, analyzeIndustry/City/Education( SQL GROUP BY), analyzeSkill(词频统计), analyzeTrend(按月聚合) | ✅ 全部完整 |

### 5.4 occupation-report（3 类 10 方法）

| 类 | 关键方法 | 状态 |
|---|---|---|
| `ReportTemplateServiceImpl` | pageTemplates, getById, saveTemplate, deleteTemplate | ✅ 全部完整 |
| `ReportGeneratorServiceImpl` | generate(六步流水线), pageRecords, loadReportFile(磁盘读取), deleteRecord(含物理文件), renderHtml(Freemarker) | ✅ 全部完整 |
| `AiSummaryServiceImpl` | summarize(LLM调用), buildPrompt, callLlm(RestTemplate), fallbackSummary(规则降级) | ✅ 全部完整 |

### 5.5 occupation-recommend（4 类 16 方法）

| 类 | 关键方法 | 状态 |
|---|---|---|
| `StudentProfileServiceImpl` | getByUserId, saveProfile(含默认值), listAll | ✅ 全部完整 |
| `JobMatchServiceImpl` | match(四维打分), queryCandidates(城市初筛+放开限制), score(技能40%+学历25%+城市20%+薪资15%), parseSkills | ✅ 全部完整 |
| `BehaviorServiceImpl` | record(VIEW可重复/其他幂等), removeFavorite, countByAction, listByUser | ✅ 全部完整 |
| `PushServiceImpl` | createPush, pageMyPushes(未读优先), markAsRead(归属校验), countUnread | ✅ 全部完整 |

### 5.6 occupation-api（2 类 5 方法）

| 类 | 关键方法 | 状态 |
|---|---|---|
| `OpenAuthServiceImpl` | issueToken(apiKey+apiSecret校验→Redis存Token), validateToken | ✅ 全部完整 |
| `OpenDataServiceImpl` | getStatsOverview, getSkillHot(词频统计), getIndustryDist — 均含 Redis 缓存10分钟 | ✅ 全部完整 |

---

## 六、前端页面实现状态

### 6.1 架构概览

- **技术栈**：Vite + Vue 3（`<script setup>`）+ Element Plus + ECharts + Axios + Pinia + Vue Router
- **路由**：HTML5 History 模式，4 角色模块（Admin/Student/Teacher/HR），含 Token+角色双重守卫
- **API 封装**：`auth.js`(1个)、`admin.js`(14个)、`student.js`(13个)，Axios 拦截器自动注入 Bearer Token
- **状态管理**：`app.js`(侧边栏/loading)、`user.js`(token/用户信息/登录登出)

### 6.2 页面实现状态（17 个 .vue 文件）

| 页面 | 文件 | 大小 | 状态 |
|---|---|---|---|
| 登录页 | `views/login/LoginView.vue` | 2.88 KB | ✅ 已实现 |
| 管理员 Dashboard | `views/admin/Dashboard.vue` | 6.23 KB | ✅ 已实现 |
| 管理员采集管理 | `views/admin/CrawlerTask.vue` | 8.81 KB | ✅ 已实现 |
| 学生端首页 | `views/student/StudentHome.vue` | 3.05 KB | ✅ 已实现 |
| 学生端职位详情 | `views/student/JobDetail.vue` | 3.72 KB | ✅ 已实现 |
| 学生端个人画像 | `views/student/Profile.vue` | 4.72 KB | ✅ 已实现 |
| 学生端收藏夹 | `views/student/Favorites.vue` | 2.49 KB | ✅ 已实现 |
| 管理员报告模板 | `views/admin/ReportTemplate.vue` | 243 B | ⚠️ 占位 |
| 管理员报告列表 | `views/admin/ReportList.vue` | 278 B | ⚠️ 占位 |
| 管理员用户管理 | `views/admin/UserManage.vue` | 231 B | ⚠️ 占位 |
| 学生端报告 | `views/student/Reports.vue` | 238 B | ⚠️ 占位 |
| 教师端首页 | `views/teacher/TeacherHome.vue` | 244 B | ⚠️ 占位 |
| 教师端学生管理 | `views/teacher/Students.vue` | 242 B | ⚠️ 占位 |
| 教师端建议 | `views/teacher/Suggestions.vue` | 251 B | ⚠️ 占位 |
| HR 端首页 | `views/hr/HrHome.vue` | 228 B | ⚠️ 占位 |
| HR 端职位管理 | `views/hr/JobManage.vue` | 209 B | ⚠️ 占位 |
| HR 端人才库 | `views/hr/Talents.vue` | 218 B | ⚠️ 占位 |

> **已实现 7 个页面，10 个占位页面待开发。**

---

## 七、已验证的端到端链路

### 7.1 数据采集 → 分析全链路 ✅

```
Mock 采集 (POST /api/admin/crawler/task/mock)
  → WebMagic 解析 mock-jobs.json (20条)
  → JobPipeline → Kafka (raw-job-data topic)
  → KafkaConsumerService → raw_job_data 入库 (40条)
  → DataCleanService.cleanPendingRawData() → job_detail (20条清洗后)
  → AnalysisJobService.runAll() → analysis_result (119条聚合)
  → GET /api/analysis/dashboard → 前端图表展示
```

### 7.2 登录认证链路 ✅

```
POST /api/auth/login {username, password, tenantName}
  → TenantServiceImpl.getByName() 校验租户
  → UserServiceImpl.getByUsername() 查用户
  → BCryptPasswordEncoder.matches() 验密码
  → JwtUtil.generate() 签发 Token
  → 返回 {token, role, username, realName, tenantName}
```

### 7.3 已验证的 API

| API | 测试结果 |
|---|---|
| `GET /api/health` | ✅ 200 OK |
| `GET /api/health/db` | ✅ 200 OK |
| `POST /api/auth/login` | ✅ 200 OK（JWT Token） |
| `POST /api/admin/crawler/task/mock` | ✅ 200 OK |
| `GET /api/admin/crawler/task` | ✅ 200 OK |
| `POST /api/analysis/clean` | ✅ 200 OK（清洗 20 条） |
| `POST /api/analysis/rebuild` | ✅ 200 OK（重算 119 条） |
| `POST /api/analysis/pipeline` | ✅ 200 OK |
| `GET /api/analysis/jobs` | ✅ 200 OK（20 条职位） |
| `GET /api/analysis/dashboard` | ✅ 200 OK（5 维度数据） |

---

## 八、Bug 修复记录

| # | Bug | 根因 | 修复 | 状态 |
|---|---|---|---|---|
| 1 | 登录 500 错误 | DB 密码不匹配，Druid 连接池关闭 | `application.yml` 密码改为 `root` | ✅ |
| 2 | 残留 Java 进程端口占用 | 多次启动未清理旧进程 | 重启前 `Stop-Process -Name java -Force` | ✅ |
| 3 | 清洗数据不自动入库 | Kafka 消费者静默失败 | 新增 `POST /api/analysis/clean` 和 `/api/analysis/pipeline` | ✅ |
| 4 | Dashboard 无数据 | 缺少手动分析重算链路 | 调用 pipeline 端到端填充数据 | ✅ |

---

## 九、待完成事项

> **总体评估**：后端 62 个方法中 61 个已完整实现（98.4%），前端 17 个页面中 7 个已实现（41.2%），**最大短板在前端占位页面**。

### 🔴 P0 — 验证类（3 项）

| # | 事项 | 说明 | 涉及模块 |
|---|------|------|----------|
| 1 | 创建学生测试账号 | 验证学生端完整流程（画像→推荐→收藏→投递），目前 `sys_student_profile`/`student_behavior`/`push_record` 表均为空 | auth + recommend |
| 2 | 前端与后端 API 联调 | 确认字段名匹配、分页参数一致，7 个已实现页面需逐一对接验证 | web-ui + 全模块 |
| 3 | 报告生成端到端验证 | 模板创建 → 触发生成 → 下载 PDF/Word/HTML，目前 `report_template`/`report_record` 表均为空 | report |

### 🟡 P1 — 前端占位页面（10 个页面 + 2 项优化）

| # | 页面 | 文件 | 当前大小 | 对应后端 API |
|---|------|------|----------|-------------|
| 1 | 管理员用户管理 | `views/admin/UserManage.vue` | 231B | `/api/admin/users` CRUD（5 个端点） |
| 2 | 管理员报告模板 | `views/admin/ReportTemplate.vue` | 243B | `/api/admin/report/template` CRUD（5 个端点） |
| 3 | 管理员报告列表 | `views/admin/ReportList.vue` | 278B | `/api/report/records` + `/api/report/download/{id}` |
| 4 | 学生端报告 | `views/student/Reports.vue` | 238B | `/api/report/records` + `/api/report/download/{id}` |
| 5 | 教师端首页 | `views/teacher/TeacherHome.vue` | 244B | `/api/teacher/students` + Dashboard 数据 |
| 6 | 教师端学生管理 | `views/teacher/Students.vue` | 242B | `/api/teacher/students` + stats + behaviors |
| 7 | 教师端建议 | `views/teacher/Suggestions.vue` | 251B | 暂无专属 API（P6 技能缺口诊断引擎） |
| 8 | HR 端首页 | `views/hr/HrHome.vue` | 228B | `/api/hr/jobs` + `/api/hr/talents` |
| 9 | HR 端职位管理 | `views/hr/JobManage.vue` | 209B | `/api/hr/jobs` CRUD（5 个端点） |
| 10 | HR 端人才库 | `views/hr/Talents.vue` | 218B | `/api/hr/talents` |

| # | 优化项 | 说明 |
|---|--------|------|
| 11 | 前端 ECharts 图表联调 | Dashboard 页面图表与 `/api/analysis/dashboard` 5 维度数据对接 |
| 12 | Redis 缓存集成 | Dashboard 查询结果缓存（当前每次实时查库） |

### 🟢 P2 — 后端补全（5 项）

| # | 事项 | 说明 | 当前状态 |
|---|------|------|----------|
| 1 | 智联招聘采集器 | `CrawlerServiceImpl.createProcessor()` ZHAOPIN 分支直接抛 `UnsupportedOperationException` | ⚠️ 未实现 |
| 2 | `sys_alert` 表配套代码 | 14 张表中唯一没有 Entity/Mapper/Service 的表 | ❌ 缺失 |
| 3 | BOSS 直聘反爬调优 | 已有基础反爬策略（UA 轮换/随机延迟/重试），需实际跑通调优 | ⚠️ 未实战验证 |
| 4 | Word 结构化表格导出 | `ReportGeneratorServiceImpl` 中 WordExporter 标记为 TODO | ⚠️ 基础实现 |
| 5 | 技能提取增强 | `DataCleanServiceImpl.extractSkills()` 仅解析 JSON，未从 description 关键词匹配补全 | ⚠️ 基础实现 |

### 🔵 P6 — AI 差异化升级（6 大模块，0%）

| # | 模块 | 核心能力 | 技术方案 | 设计文档章节 |
|---|------|----------|----------|-------------|
| 1 | 技能缺口诊断引擎 | 课程体系 vs 市场需求差距自动诊断 | Spark 共现矩阵 + 课程-技能映射 + LLM 建议 | §3.5+ A |
| 2 | 职业能力知识图谱 | 技能→职位→行业→公司关联推理 | Neo4j + Spark 批量构建关系 + G6 可视化 | §3.5+ B |
| 3 | LLM 智能报告引擎 | 自然语言洞察替代模板式报告（学生版/教师版/校领导版） | LLM API + Prompt 工程 + 数据注入 | §3.5+ C |
| 4 | 实时市场脉搏大屏 | 每分钟更新市场热度指数 + 异常告警 | Kafka Streaming + Redis 计数器 + WebSocket | §3.5+ D |
| 5 | NLP 简历-JD 语义匹配 | 向量化语义匹配 + 差距分析 | Sentence-Transformers + FAISS/Milvus | §3.5+ E |
| 6 | 就业趋势预测模型 | 3/6 个月岗位数量/技能需求/薪资走势预测 | Prophet/ARIMA + Spark 历史数据训练 | §3.5+ F |

### 📝 代码中已标注的 TODO（4 个）

| # | 位置 | 内容 | 优先级 |
|---|------|------|--------|
| 1 | `OpenAuthServiceImpl.issueToken()` | apiSecret 明文比对 → 改为 BCrypt | P5 |
| 2 | `DataCleanServiceImpl.extractSkills()` | 从 description 关键词匹配补全技能 | P2 |
| 3 | `JobMatchServiceImpl` | 行为反馈加权 + 语义匹配向量化 | P4 |
| 4 | `ReportGeneratorServiceImpl` | Word 结构化表格导出（WordExporter） | P3 |

### 📊 工作量估算

| 优先级 | 事项数 | 预估工作量 | 说明 |
|--------|--------|-----------|------|
| P0 验证 | 3 | 0.5 天 | 创建账号 + curl 测试 + 报告生成验证 |
| P1 前端 | 12 | 3-5 天 | 10 个页面开发 + 图表联调 + 缓存集成 |
| P2 后端 | 5 | 2-3 天 | 采集器 + sys_alert + 爬虫调优 + 技能提取 |
| P6 AI | 6 | 15-20 天 | 6 大差异化模块，需引入 Neo4j/Python/LLM 等新组件 |
| **合计** | **26** | **约 21-29 天** | — |

---

> **提示**：本文档由 AI 在每次重要里程碑后更新。新会话可通过读取本文档快速恢复上下文。
