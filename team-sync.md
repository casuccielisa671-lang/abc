# 职业能力大数据服务平台 — 团队同步文档

> **版本**: v2.0 — 骨架代码就绪  
> **日期**: 2026-07-07  
> **负责人**: 你  
> **状态**: P1 全部完成 + 骨架代码交付，即日起四人并行开发

---

## 0. 速查：我分到哪个模块？

| 组员 | 模块 | 阶段 | 文件位置 |
|---|---|---|---|
| **A** | 大数据分析 | P2 | `occupation-analysis/` |
| **B** | 报告引擎 + 对外 API | P3 + P5 | `occupation-report/` + `occupation-api/` |
| **C** | 岗位推荐推送 + 教师/HR 后端 | P4 + P5 | `occupation-recommend/` |
| **D** | 前端（全四端） | P3～P5 | `occupation-web-ui/` |

> **前提**：A 先把 `AnalysisService` 和 `JobDetailService` 实现好，B 和 C 才能正常启动。

---

## 1. 项目已就绪的基础设施

### 1.1 可以直接用的（无需修改）

| 基础设施 | 位置 | 说明 |
|---|---|---|
| 多租户架构 | `occupation-common/.../config/MyBatisPlusConfig.java` | `tenant_id` 自动注入，全平台共享表（`job_detail`、`raw_job_data`、`sys_tenant`）已配置忽略 |
| 统一响应 | `com.occupation.common.result.Result<T>` | `Result.ok(data)` / `Result.error(msg)` |
| 全局异常 | `GlobalExceptionHandler.java` | 用 `throw new BizException("原因")` 即可 |
| JWT 认证 | `occupation-auth/` | 登录返回 Token，前端带 `Authorization: Bearer xxx` |
| JWT 过滤器 | `JwtAuthenticationFilter` | 已注册 Security 过滤器链，`@PermitAll`/`@PreAuthorize` 可用 |
| Kafka 链路 | `occupation-common/.../config/Kafka*Config.java` | 生产者/消费者/Topic 全部配好 |
| XXL-Job | `occupation-crawler/.../config/XxlJobConfig.java` | 参考 CrawlerJobHandler 写法即可 |
| 14 张数据库表 | `occupation-common/.../sql/init.sql` | 含种子数据（admin / admin123） |

### 1.2 骨架代码已交付（编译通过）

每个模块的 `entity/`、`mapper/`、`service/`、`dto/`、`vo/` 目录下已有下列文件：

```
occupation-analysis/
├── entity/JobDetail.java          ← 映射 job_detail 表（全平台共享）
├── entity/AnalysisResult.java     ← 映射 analysis_result 表
├── mapper/JobDetailMapper.java
├── mapper/AnalysisResultMapper.java
├── service/AnalysisService.java   ← 接口：getDashboard(DashboardQueryDTO)
├── service/JobDetailService.java  ← 接口：queryJobs(JobQueryDTO) → Page<JobDetailVO>
├── dto/DashboardQueryDTO.java
├── dto/JobQueryDTO.java
├── vo/DashboardVO.java
└── vo/JobDetailVO.java

occupation-report/
├── entity/ReportTemplate.java     ← 继承 BaseEntity
├── entity/ReportRecord.java       ← 继承 BaseEntity
├── mapper/ReportTemplateMapper.java
└── mapper/ReportRecordMapper.java

occupation-recommend/
├── entity/SysStudentProfile.java  ← 继承 BaseEntity
├── entity/PushRecord.java
├── entity/StudentBehavior.java
├── mapper/SysStudentProfileMapper.java
├── mapper/PushRecordMapper.java
└── mapper/StudentBehaviorMapper.java

occupation-api/
├── entity/ApiClient.java          ← 继承 BaseEntity
└── mapper/ApiClientMapper.java

occupation-web-ui/ (Vue 3 脚手架)
├── package.json                   ← Vite + Vue 3 + Element Plus + ECharts + Axios + Pinia
├── src/router/index.js            ← 4 角色路由已配好
├── src/api/request.js             ← Axios 拦截器已写好
├── src/store/user.js              ← Pinia 用户状态
├── src/components/AppLayout.vue   ← 通用布局（侧边栏+头部+内容区）
└── src/views/                     ← 16 个占位页面，路由可跳转
    ├── login/LoginView.vue
    ├── admin/  5个页面 (Dashboard/CrawlerTask/ReportTemplate/ReportList/UserManage)
    ├── student/5个页面 (StudentHome/JobDetail/Profile/Favorites/Reports)
    ├── teacher/3个页面 (TeacherHome/Students/Suggestions)
    └── hr/     3个页面 (HrHome/JobManage/Talents)
```

---

## 2. 跨模块接口契约（所有组员必须遵守）

### 2.1 A 组提供 → B 组调用

```java
// occupation-analysis/.../service/AnalysisService.java
public interface AnalysisService {
    DashboardVO getDashboard(DashboardQueryDTO query);
}
// 返回字段：industryTop、cityDist、skillHot、educationDist、trend
```

### 2.2 A 组提供 → C 组调用

```java
// occupation-analysis/.../service/JobDetailService.java
public interface JobDetailService {
    Page<JobDetailVO> queryJobs(JobQueryDTO query);
}
// 入参：city、industry、salaryMin/Max、education、experience、keyword、pageNum、pageSize
// 返回：分页 JobDetailVO 列表
```

### 2.3 调用方式（B 组 / C 组）

```java
// 在自己的 ServiceImpl 中通过 @Autowired 注入 A 组的 Service 接口：
@Autowired
private AnalysisService analysisService;    // B 组用

@Autowired
private JobDetailService jobDetailService;   // C 组用
```

> **A 组必须优先实现这两个接口的 ServiceImpl**，否则 B、C 启动时会因缺少 Bean 而报错。
> 可以先写一个返回 null/空对象的空实现，让 B、C 通过编译。

---

## 3. 各组开发任务

### A 组 — `occupation-analysis`（大数据分析）

编译即用的文件已在你的模块，**你需要新建**：

| 新建文件 | 说明 |
|---|---|
| `service/impl/AnalysisServiceImpl.java` | 实现 `AnalysisService.getDashboard()`，调用 Mapper 查 analysis_result 表 |
| `service/impl/JobDetailServiceImpl.java` | 实现 `JobDetailService.queryJobs()`，调用 Mapper 查 job_detail 表 |
| `controller/AnalysisController.java` | `GET /api/analysis/dashboard` |
| `config/RedisCacheConfig.java` | Dashboard 数据缓存 |
| `dto/`、`vo/` 可按需新增 | 当前 DTO/VO 不够用可自行扩展 |

**⚠️ 注意**：`job_detail` 表不含 `tenant_id`，查询时不要加租户过滤。`analysis_result` 表含 `tenant_id`，多租户插件会自动注入。

---

### B 组 — `occupation-report` + `occupation-api`（报告 + 对外 API）

**report 模块需新建**：

| 新建文件 | 说明 |
|---|---|
| `service/ReportTemplateService.java` + `impl/` | 模板 CRUD |
| `service/ReportGeneratorService.java` + `impl/` | 调 AnalysisService → 数据填充 → Freemarker 渲染 |
| `service/PdfExporter.java` | HTML → PDF（Flying Saucer） |
| `service/WordExporter.java` | HTML → Word（POI） |
| `controller/ReportTemplateController.java` | 模板管理 API |
| `controller/ReportRecordController.java` | 报告生成/下载 API |
| `dto/`、`vo/` | 按需新建 |

**api 模块需新建**：

| 新建文件 | 说明 |
|---|---|
| `service/impl/ApiAuthServiceImpl.java` | OAuth2 注册/签发 Token |
| `service/impl/OpenApiServiceImpl.java` | 职位查询/报告摘要/技能热度/大盘统计 |
| `controller/OpenAuthController.java` | `POST /api/open/auth/token` |
| `controller/OpenJobController.java` | `GET /api/open/jobs` |
| `controller/OpenStatsController.java` | `GET /api/open/stats/*` |
| `config/RateLimitConfig.java` | Redis 令牌桶限流 |
| `config/SwaggerConfig.java` | Knife4j 文档分组 |
| `config/OAuth2Config.java` | OAuth2 资源服务器配置 |

**依赖说明**：已有 `knife4j-openapi3-spring-boot-starter` 和 `spring-boot-starter-oauth2-resource-server`。

---

### C 组 — `occupation-recommend`（推荐推送 + 教师/HR 后端）

**需新建**：

| 新建文件 | 说明 |
|---|---|
| `service/impl/StudentProfileServiceImpl.java` | 画像 CRUD |
| `service/impl/JobMatchServiceImpl.java` | 调 JobDetailService → 规则打分（技能40%+城市25%+薪资20%+学历15%） |
| `service/impl/PushServiceImpl.java` | 推送通知 + XXL-Job 定时推送 |
| `service/impl/BehaviorServiceImpl.java` | VIEW/FAVORITE/APPLY 行为记录 |
| `controller/StudentProfileController.java` | 画像 API |
| `controller/RecommendController.java` | 推荐/收藏/投递 API |
| `controller/PushController.java` | 推送列表/已读 API |
| `controller/TeacherController.java` | 教师端：学生列表/行为统计/导出 |
| `controller/HrController.java` | 企业端：职位发布/人才浏览/投递管理 |
| `job/RecommendJobHandler.java` | XXL-Job：每日推送 Top5 新匹配 |
| `dto/`、`vo/` | 按需新建 |

---

### D 组 — `occupation-web-ui`（Vue 3 前端，全四端）

脚手架已就绪：`npm install` → `npm run dev` 即可启动。

**需实现**（按优先级）：

| 优先级 | 页面 | 说明 |
|---|---|---|
| P0 | `views/login/LoginView.vue` | 对接 `POST /api/auth/login`，存 token |
| P0 | `views/admin/Dashboard.vue` | 5 个 ECharts 图表（行业/城市/技能/学历/趋势） |
| P1 | `views/admin/CrawlerTask.vue` | 采集任务 CRUD 表格 + 启停 |
| P1 | `views/admin/ReportTemplate.vue` | 模板 CRUD + JSON 编辑器 |
| P1 | `views/admin/ReportList.vue` | 报告生成/下载 |
| P1 | `views/admin/UserManage.vue` | 用户管理表格 |
| P2 | `views/student/*.vue` | 推荐流/详情/画像/收藏/报告 |
| P2 | `views/teacher/*.vue` | 班级概览/学生列表/建议 |
| P2 | `views/hr/*.vue` | 工作台/职位管理/人才浏览 |

**API 封装目录**：`src/api/` 下已提供 `request.js`（Axios 拦截器），参照它创建 `auth.js`、`crawler.js`、`analysis.js` 等模块。

---

## 4. 代码规范速查

### 后端（Java）

| 规范 | 要点 |
|---|---|
| Controller | 只做参数校验 + 调 Service + 封装 `Result<T>`，不写业务逻辑 |
| Service | 接口 + impl 配对，`@Transactional` 放 impl |
| Mapper | 只做 CRUD，继承 `BaseMapper<Entity>` |
| Entity | 有 `tenant_id+deleted+createTime+updateTime` 的表继承 `BaseEntity`，否则 `implements Serializable` |
| DTO | 入参，加 `@NotNull`/`@NotBlank` 校验 |
| VO | 出参，纯字段，无逻辑 |
| 文件长度 | ≤ 500 行（Vue ≤ 300 行） |

### 前端（Vue 3）

| 规范 | 要点 |
|---|---|
| 语法 | `<script setup>` 组合式 API |
| 请求 | 通过 `src/api/` 封装，不直接在 `.vue` 里写 `axios.get` |
| 状态 | 通过 `src/store/` 的 Pinia store，不滥用 `localStorage` |
| 组件拆分 | 页面放 `views/`，复用 UI 放 `components/` |

---

## 5. 开发流程

```
1. git pull (获取骨架代码)
2. 阅读自己的模块文件 → 了解 Entity 字段/Mapper/接口契约
3. A 组先写 AnalysisServiceImpl + JobDetailServiceImpl（返回空对象也可）
4. B、C 组启动依赖 A 的 Bean，先写自己的 ServiceImpl
5. Controller 最后写，联调时逐步实现
6. D 组可以先启动 vue，跑通 login 页面，然后逐步连 B、C 的 API
```

---

## 6. 常用命令

```bash
# 后端编译
cd e:\occupation
mvn compile -DskipTests

# 后端启动（需要先 docker-compose up 启动 MySQL/Redis/Kafka）
cd e:\occupation\occupation-web
mvn spring-boot:run

# 前端
cd e:\occupation\occupation-web-ui
npm install
npm run dev

# 一键启动基础设施
docker-compose up -d
```

---

## 7. 关键文件索引

| 文件 | 用途 |
|---|---|
| `memory-bank/design-document.md` | 产品设计（功能描述、数据流、角色定义） |
| `memory-bank/tech-stack.md` | 技术选型与版本号 |
| `memory-bank/architecture.md` | 当前架构状态（Schema、API、部署组件） |
| `memory-bank/progress.md` | 开发进度追踪 |
| `AGENTS.md` | AI 编码规范（分层架构、多租户、禁止事项） |
| `docker-compose.yml` | 本地基础设施一键启动 |
| `occupation-common/.../sql/init.sql` | 14 张表 DDL + 种子数据 |
