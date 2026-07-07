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

## 3. 各组开发任务（超详细版）

> **阅读指南**: 每个文件的职责、输入/输出、接口签名、涉及表都已写明。
> 组员拿到后按编号顺序新建文件即可，**不必从头理解整个项目**。

---

### 🔵 A 组 — `occupation-analysis`（大数据分析 + 职位查询）

> **阶段**: P2 · **模块**: `occupation-analysis/`  
> **依赖**: occupation-common（已就绪）  
> **被依赖**: B 组（调 AnalysisService）、C 组（调 JobDetailService）  
> **⚠️ A 组必须优先实现 ServiceImpl**，B/C 组才能通过编译。

#### 任务清单（按顺序）

| # | 新建文件 | 职责 | 行数预估 |
|---|---|---|---|
| A1 | `service/impl/AnalysisServiceImpl.java` | Dashboard 分析数据查询 | ~80 |
| A2 | `service/impl/JobDetailServiceImpl.java` | 职位分页查询 | ~50 |
| A3 | `controller/AnalysisController.java` | Dashboard + 职位查询 REST API | ~60 |
| A4 | `config/RedisCacheConfig.java` | Dashboard 数据 Redis 缓存（可选） | ~40 |
| A5 | 按需新增 `dto/`、`vo/` | 扩展查询参数、扩展出参 | 按需 |

---

#### A1 — `AnalysisServiceImpl.java`

```
路径: occupation-analysis/.../service/impl/AnalysisServiceImpl.java
实现: AnalysisService 接口
依赖: AnalysisResultMapper（已就绪）
```

**核心逻辑**：

```java
@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Autowired
    private AnalysisResultMapper analysisResultMapper;

    @Override
    public DashboardVO getDashboard(DashboardQueryDTO query) {
        DashboardVO vo = new DashboardVO();

        // 1. 查 industry 维度 → 按 metric_value DESC，取 Top 10
        //    SELECT dimension_value, SUM(metric_value) FROM analysis_result
        //    WHERE tenant_id = ? AND dimension = 'industry'
        //    AND period_type = 'MONTH' AND period_value = 当前月
        //    GROUP BY dimension_value ORDER BY SUM(metric_value) DESC LIMIT 10
        //    → vo.setIndustryTop(list)

        // 2. 查 city 维度 → 同上，dimension = 'city'
        //    → vo.setCityDist(list)

        // 3. 查 skill 维度 → 同上，dimension = 'skill'，只取 metric_name = 'job_count'
        //    → vo.setSkillHot(list)

        // 4. 查 education 维度 → dimension = 'education'
        //    → vo.setEducationDist(list)

        // 5. 查 trend 维度 → dimension = 'trend'，按 period_value ASC
        //    → vo.setTrend(list)

        return vo;
    }
}
```

**涉及的 SQL 模式**（用 MyBatis-Plus QueryWrapper 写）：

| 查询目的 | 条件 |
|---|---|
| 行业 Top 10 | `dimension = "industry"`, `metric_name = "job_count"`, 按 `metric_value` 降序，LIMIT 10 |
| 城市分布 | `dimension = "city"`, `metric_name = "job_count"`, 按 `metric_value` 降序 |
| 技能热度 | `dimension = "skill"`, `metric_name = "job_count"`, 按 `metric_value` 降序，LIMIT 20 |
| 学历分布 | `dimension = "education"`, `metric_name = "job_count"` |
| 趋势 | `dimension = "trend"`, 按 `period_value` 升序 |

**⚠️ 注意事项**：
- `analysis_result` 表含 `tenant_id`，多租户插件会自动注入，不需手动拼接。
- `DimentionItem` 的 `name` = `dimension_value`，`value` = `metric_value`，`count` = 从 `metric_name='job_count'` 的行取。
- 如果查询维度数据时使用了 `startDate/endDate`，按 `calc_time` 过滤。

---

#### A2 — `JobDetailServiceImpl.java`

```
路径: occupation-analysis/.../service/impl/JobDetailServiceImpl.java
实现: JobDetailService 接口
依赖: JobDetailMapper（已就绪）
```

**核心逻辑**：

```java
@Service
public class JobDetailServiceImpl implements JobDetailService {

    @Autowired
    private JobDetailMapper jobDetailMapper;

    @Override
    public Page<JobDetailVO> queryJobs(JobQueryDTO query) {
        // 1. 构建 QueryWrapper<JobDetail>
        //    - 如果 city != null → eq("city", query.getCity())
        //    - 如果 industry != null → eq("industry", query.getIndustry())
        //    - 如果 salaryMin != null → ge("salary_max", query.getSalaryMin())
        //    - 如果 salaryMax != null → le("salary_min", query.getSalaryMax())
        //    - 如果 education != null → eq("education", query.getEducation())
        //    - 如果 experience != null → eq("experience", query.getExperience())
        //    - 如果 keyword != null → like("title", keyword).or().like("company", keyword)
        //    - 按 publish_date 降序排列

        // 2. 用 MyBatis-Plus Page 分页插件查询
        //    Page<JobDetail> page = new Page<>(query.getPageNum(), query.getPageSize());
        //    jobDetailMapper.selectPage(page, wrapper);

        // 3. 将 Page<JobDetail> 转换为 Page<JobDetailVO>
        //    字段一一映射即可

        // 4. 返回 Page<JobDetailVO>
    }
}
```

**⚠️ 注意事项**：
- `job_detail` 表**不含 `tenant_id`**（全平台共享），查询时**不要加租户过滤**。
- 多租户插件已配置 `job_detail` 为忽略表（在 `MyBatisPlusConfig.java` 中），自动生效。
- `salaryMin/salaryMax` 是用户期望的薪资范围，需和 `job_detail.salary_min/salary_max` 做**区间交集判断**。

---

#### A3 — `AnalysisController.java`

```
路径: occupation-analysis/.../controller/AnalysisController.java
```

**接口清单**：

| 方法 | URL | 入参 | 出参 | 权限 |
|---|---|---|---|---|
| GET | `/api/analysis/dashboard` | `DashboardQueryDTO`（Query String） | `Result<DashboardVO>` | 管理员/教师 |
| GET | `/api/analysis/jobs` | `JobQueryDTO`（Query String） | `Result<PageResult<JobDetailVO>>` | 全员 |

**Controller 骨架**：

```java
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private JobDetailService jobDetailService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(@Valid DashboardQueryDTO query) {
        return Result.ok(analysisService.getDashboard(query));
    }

    @GetMapping("/jobs")
    public Result<?> queryJobs(@Valid JobQueryDTO query) {
        // 需要包装分页结果：PageResult 含 total + list
        Page<JobDetailVO> page = jobDetailService.queryJobs(query);
        return Result.ok(Map.of("total", page.getTotal(), "list", page.getRecords()));
    }
}
```

---

#### A4 — `RedisCacheConfig.java`（可选但推荐）

```
路径: occupation-analysis/.../config/RedisCacheConfig.java
```

**职责**：为 Dashboard 数据加 Redis 缓存，TTL = 1 小时，减少重复计算。

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    // 配置 CacheManager，默认 TTL 3600s
}
```

同时在 `AnalysisServiceImpl.getDashboard()` 上添加 `@Cacheable(value = "dashboard", key = "#query.tenantId")`。

---

#### 📊 A 组验收标准

- [x] `mvn compile` 通过（A1、A2 必须编译通过，否则阻塞 B/C 组）
- [ ] `GET /api/analysis/dashboard?tenantId=1` 返回包含 5 个维度的 JSON
- [ ] `GET /api/analysis/jobs?city=北京&industry=互联网` 返回分页结果
- [ ] `job_detail` 查询不带 tenant_id 条件

---

### 🟢 B 组 — `occupation-report` + `occupation-api`（报告引擎 + 对外 API）

> **阶段**: P3 + P5 · **模块**: `occupation-report/` + `occupation-api/`  
> **依赖**: occupation-common（已就绪）、occupation-analysis（调 AnalysisService）  
> **提供**: 报告模板管理、自动生成、多格式导出、OAuth2 对外接口

---

#### 📝 Part 1 — `occupation-report/`（报告模块）

| # | 新建文件 | 职责 | 行数预估 |
|---|---|---|---|
| B1 | `service/ReportTemplateService.java` + `impl/ReportTemplateServiceImpl.java` | 模板表 CRUD | ~60 |
| B2 | `service/ReportGeneratorService.java` + `impl/ReportGeneratorServiceImpl.java` | 调 AnalysisService → 填充 Freemarker → 生成报告 | ~100 |
| B3 | `service/PdfExporter.java` | HTML → PDF（Flying Saucer） | ~40 |
| B4 | `service/WordExporter.java` | HTML → Word（Apache POI） | ~40 |
| B5 | `controller/ReportTemplateController.java` | 模板增删改查 API | ~50 |
| B6 | `controller/ReportRecordController.java` | 报告生成 + 下载 + 列表 API | ~70 |
| B7 | 按需 `dto/`、`vo/` | 模板表单 DTO、报告列表 VO 等 | 按需 |

---

**B1 — ReportTemplateService**

```
路径: occupation-report/.../service/impl/ReportTemplateServiceImpl.java
依赖: ReportTemplateMapper（已就绪）
```

**接口**：

```java
public interface ReportTemplateService {
    List<ReportTemplate> listAll(Long tenantId);           // 按租户查所有模板
    ReportTemplate getById(Long id, Long tenantId);        // 查单个 + 租户校验
    void create(CreateTemplateDTO dto);                    // 新建模板
    void update(UpdateTemplateDTO dto);                    // 更新模板
    void delete(Long id, Long tenantId);                   // 逻辑删除
}
```

**涉及表**: `report_template`（含 `tenant_id`，自动注入）

---

**B2 — ReportGeneratorService**

```
路径: occupation-report/.../service/impl/ReportGeneratorServiceImpl.java
依赖: AnalysisService（跨模块 @Autowired）、Freemarker 模板引擎
```

**核心流程**（这是本模块最核心的逻辑）：

```java
@Service
public class ReportGeneratorServiceImpl implements ReportGeneratorService {

    @Autowired
    private ReportTemplateMapper templateMapper;

    @Autowired
    private ReportRecordMapper recordMapper;

    @Autowired
    private AnalysisService analysisService;  // 跨模块调 A 组！

    @Override
    public String generateReport(Long templateId, Long tenantId, String fileType) {
        // Step 1: 查模板 → ReportTemplate
        ReportTemplate template = templateMapper.selectById(templateId);

        // Step 2: 调 A 组的 AnalysisService 获取分析数据
        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setTenantId(tenantId);
        DashboardVO dashboard = analysisService.getDashboard(query);

        // Step 3: 将 dashboard 数据放入 Freemarker Model（Map<String, Object>）
        Map<String, Object> model = new HashMap<>();
        model.put("dashboard", dashboard);
        model.put("templateName", template.getName());
        model.put("generateTime", LocalDateTime.now());

        // Step 4: Freemarker 渲染 → 得到 HTML 字符串
        //    Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        //    Template tmpl = new Template("report", template.getTemplateContent(), cfg);
        //    StringWriter writer = new StringWriter();
        //    tmpl.process(model, writer);

        // Step 5: 根据 fileType 导出
        //    PDF  → PdfExporter.export(html)
        //    WORD → WordExporter.export(html)
        //    HTML → 直接存 html

        // Step 6: 保存到文件系统（如 /data/reports/{tenantId}/{uuid}.pdf）
        // Step 7: 写入 report_record 表（templateId、fileUrl、fileType、status=SUCCESS）

        return fileUrl;
    }
}
```

**涉及表**: `report_template`（读）、`report_record`（写）

---

**B3 — PdfExporter**

```
路径: occupation-report/.../service/PdfExporter.java
依赖: org.xhtmlrenderer:flying-saucer-pdf-itext5（已在 POM 中声明）
```

```java
public class PdfExporter {
    public static byte[] export(String html) {
        // ITextRenderer renderer = new ITextRenderer();
        // renderer.setDocumentFromString(html);
        // renderer.layout();
        // ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // renderer.createPDF(baos);
        // return baos.toByteArray();
    }
}
```

**⚠️ 注意**: 中文需要字体支持，把 `simsun.ttf` 放入 `resources/fonts/`，Flying Saucer 配置添加字体路径。

---

**B4 — WordExporter**

```
路径: occupation-report/.../service/WordExporter.java
依赖: org.apache.poi:poi-ooxml（已在 POM 中声明）
```

```java
public class WordExporter {
    public static byte[] export(String html) {
        // Apache POI 的 XWPFDocument 写入 HTML 内容
        // 或直接用 POI 构建文档对象（推荐：生成结构化的 Word 表格）
    }
}
```

---

**B5 — ReportTemplateController**

```
路径: occupation-report/.../controller/ReportTemplateController.java
```

| 方法 | URL | 入参 | 出参 |
|---|---|---|---|
| GET | `/api/admin/report/template` | pageNum, pageSize | 模板分页列表 |
| GET | `/api/admin/report/template/{id}` | — | 模板详情 |
| POST | `/api/admin/report/template` | `CreateTemplateDTO` | 创建的模板 |
| PUT | `/api/admin/report/template/{id}` | `UpdateTemplateDTO` | 更新后模板 |
| DELETE | `/api/admin/report/template/{id}` | — | 操作结果 |

---

**B6 — ReportRecordController**

```
路径: occupation-report/.../controller/ReportRecordController.java
```

| 方法 | URL | 入参 | 出参 | 说明 |
|---|---|---|---|---|
| POST | `/api/admin/report/generate` | `{ templateId, fileType }` | `{ reportId, fileUrl }` | 触发生成（异步） |
| GET | `/api/admin/report/record` | pageNum, pageSize | 报告记录分页 | 历史列表 |
| GET | `/api/admin/report/download/{id}` | — | 文件流 | 下载文件 |
| DELETE | `/api/admin/report/record/{id}` | — | 操作结果 | 删除记录 |
| GET | `/api/student/report/list` | pageNum, pageSize | 报告列表 | 学生端只读 |

---

#### 🔌 Part 2 — `occupation-api/`（对外 API 模块）

| # | 新建文件 | 职责 | 行数预估 |
|---|---|---|---|
| B8 | `service/ApiAuthService.java` + `impl/ApiAuthServiceImpl.java` | 验证 apiKey + apiSecret → 签发/校验 JWT | ~80 |
| B9 | `service/OpenApiService.java` + `impl/OpenApiServiceImpl.java` | 对外职位查询/报告/技能/大盘 | ~100 |
| B10 | `controller/OpenAuthController.java` | `POST /api/open/auth/token` | ~40 |
| B11 | `controller/OpenJobController.java` | `GET /api/open/jobs` + 详情 | ~50 |
| B12 | `controller/OpenStatsController.java` | `GET /api/open/stats/*` | ~50 |
| B13 | `config/RateLimitConfig.java` | Redis 令牌桶限流 | ~50 |
| B14 | `config/SwaggerConfig.java` | Knife4j API 文档分组 | ~30 |
| B15 | `config/OpenApiSecurityConfig.java` | OAuth2 资源服务器拦截 `/api/open/**` | ~40 |

---

**B8 — ApiAuthService**

```
路径: occupation-api/.../service/impl/ApiAuthServiceImpl.java
依赖: ApiClientMapper（已就绪）
```

**核心流程**：

```java
@Service
public class ApiAuthServiceImpl implements ApiAuthService {

    @Autowired
    private ApiClientMapper apiClientMapper;

    // 1. 签发 Token
    //    输入: apiKey, apiSecret
    //    → 查 api_client 表，匹配 apiKey + apiSecret
    //    → 用 JWT 签发 access_token（有效期 2 小时，payload 含 clientId + scopes）
    //    → 返回 { access_token, expires_in, scope }

    // 2. 校验 Token（供 Security Filter 调用）
    //    输入: token 字符串
    //    → JWT 解密 → 拿到 clientId
    //    → 确认 api_client 表中 status = 1（启用）
    //    → 返回认证主体对象
}
```

**涉及表**: `api_client`（读）

---

**B9 — OpenApiService**

```
路径: occupation-api/.../service/impl/OpenApiServiceImpl.java
依赖: JobDetailMapper（跨模块）、AnalysisResultMapper（跨模块）、ReportRecordMapper（跨模块）
```

**对外暴露的数据接口**（需跨模块调 A 组和本模块的 Mapper）：

| 方法 | 说明 | 数据来源 |
|---|---|---|
| `queryJobs(JobQueryDTO)` | 职位查询 | `job_detail` 表 |
| `getJobDetail(Long jobId)` | 职位详情 | `job_detail` 表 |
| `getReportSummary(Long tenantId)` | 最新报告摘要 | `report_record` 表 |
| `getSkillHot(Integer topN)` | 技能热度 Top N | `analysis_result` 表（dimension=skill） |
| `getStatsOverview()` | 大盘统计（岗位总量/平均薪资/活跃城市数） | `job_detail` 表聚合查询 |

**⚠️ 注意**：对外 API 模块的 Mapper 调用需要**跨模块注入**，或直接在自己的模块声明 Mapper。当前 `occupation-api` 已依赖 `occupation-common`，可通过 common 中的 Mapper 查询共享表。

---

**B10 — OpenAuthController**

```
POST /api/open/auth/token
入参: { "apiKey": "xxx", "apiSecret": "xxx" }
出参: { "code": 200, "data": { "accessToken": "xxx", "expiresIn": 7200, "scope": "..." } }
错误: { "code": 401, "message": "Invalid credentials" }
```

---

**B11 — OpenJobController**

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/open/jobs` | 分页查询职位（参数同 JobQueryDTO） |
| GET | `/api/open/jobs/{id}` | 职位详情 |

需要带 `Authorization: Bearer {accessToken}` 请求头。

---

**B12 — OpenStatsController**

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/open/stats/overview` | 大盘统计 |
| GET | `/api/open/stats/skills/hot` | 技能热度排行 |
| GET | `/api/open/stats/industries` | 行业分布 |

---

**B13 — RateLimitConfig**

```
路径: occupation-api/.../config/RateLimitConfig.java
```

**职责**：用 Redis + 拦截器实现令牌桶限流，**每个 apiKey 每分钟最多 60 次**，超限返回 `429 Too Many Requests`。

---

**B14 — SwaggerConfig**

```
路径: occupation-api/.../config/SwaggerConfig.java
```

**职责**：配置 Knife4j OpenAPI3 文档分组，区分"对外 API"和"管理后台 API"两个分组。

---

**B15 — OpenApiSecurityConfig**

```
路径: occupation-api/.../config/OpenApiSecurityConfig.java
```

**职责**：配置 Spring Security 对 `/api/open/**` 路径的 OAuth2 资源服务器拦截，验证 JWT Bearer Token。

---

#### 📊 B 组验收标准

- [ ] `mvn compile` 通过
- [ ] `POST /api/admin/report/template` 创建模板成功
- [ ] `POST /api/admin/report/generate` 生成 PDF 报告，可下载
- [ ] `POST /api/open/auth/token` 返回 access_token
- [ ] `GET /api/open/jobs?city=深圳` 带 token 返回 200
- [ ] 限流拦截器超频返回 429
- [ ] Knife4j 页面 `http://localhost:8080/doc.html` 显示两个分组

---

### 🟠 C 组 — `occupation-recommend`（岗位推送 + 学生画像 + 教师/HR 后端）

> **阶段**: P4 · **模块**: `occupation-recommend/`  
> **依赖**: occupation-common（已就绪）、occupation-analysis（调 JobDetailService）  
> **提供**: 学生画像、智能匹配推荐、推送系统、教师/HR 功能后端

---

| # | 新建文件 | 职责 | 行数预估 |
|---|---|---|---|
| C1 | `service/StudentProfileService.java` + `impl/StudentProfileServiceImpl.java` | 学生画像读写 | ~60 |
| C2 | `service/impl/JobMatchServiceImpl.java` | 规则打分匹配（核心算法） | ~120 |
| C3 | `service/impl/PushServiceImpl.java` | 推送记录创建/查询/已读 | ~60 |
| C4 | `service/impl/BehaviorServiceImpl.java` | VIEW/FAVORITE/APPLY 行为记录 | ~50 |
| C5 | `controller/StudentProfileController.java` | 画像管理 API | ~40 |
| C6 | `controller/RecommendController.java` | 推荐流/详情/收藏/投递 API | ~80 |
| C7 | `controller/PushController.java` | 推送列表/已读标记 API | ~40 |
| C8 | `controller/TeacherController.java` | 教师端：学生列表/行为统计/导出 | ~70 |
| C9 | `controller/HrController.java` | HR 端：职位管理/人才浏览/投递处理 | ~70 |
| C10 | `job/RecommendJobHandler.java` | XXL-Job：每日推送 Top5 | ~50 |
| C11 | 按需 `dto/`、`vo/` | 画像表单、推荐列表 VO、统计 VO 等 | 按需 |

---

**C1 — StudentProfileService**

```
路径: occupation-recommend/.../service/impl/StudentProfileServiceImpl.java
依赖: SysStudentProfileMapper（已就绪）
```

```java
public interface StudentProfileService {
    SysStudentProfile getByUserId(Long userId);            // 查画像，新建默认值
    void saveOrUpdate(ProfileFormDTO dto);                 // 保存/更新画像
    void updateSkills(Long userId, String skills);          // 更新技能
}
```

**涉及表**: `sys_student_profile`（含 `tenant_id`，自动注入）

---

**C2 — JobMatchServiceImpl（⭐核心）**

```
路径: occupation-recommend/.../service/impl/JobMatchServiceImpl.java
依赖: JobDetailService（跨模块 @Autowired）、SysStudentProfileMapper
```

**匹配算法——规则打分**：

```java
@Service
public class JobMatchServiceImpl implements JobMatchService {

    @Autowired
    private JobDetailService jobDetailService;  // 跨模块调 A 组！

    @Override
    public List<MatchResultVO> match(Long userId, int topN) {
        // Step 1: 获取学生画像
        SysStudentProfile profile = profileService.getByUserId(userId);

        // Step 2: 调 A 组的 JobDetailService 获取候选职位（用画像的意向城市/行业做初筛）
        JobQueryDTO query = new JobQueryDTO();
        query.setCity(profile.getExpectedCity());
        query.setIndustry(profile.getExpectedIndustry());
        query.setPageSize(200);
        List<JobDetailVO> candidates = jobDetailService.queryJobs(query).getRecords();

        // Step 3: 逐条打分（权重：技能匹配 40% + 城市匹配 25% + 薪资匹配 20% + 学历匹配 15%）
        for (JobDetailVO job : candidates) {
            int score = 0;

            // 技能匹配（40分）：比较 profile.skills 和 job.skills（JSON 数组交集/包含）
            List<String> profileSkills = parseSkills(profile.getSkills());
            List<String> jobSkills = parseSkills(job.getSkills());
            double skillRate = intersectionRate(profileSkills, jobSkills);
            score += (int)(skillRate * 40);

            // 城市匹配（25分）：完全匹配 = 25，否则 0
            if (job.getCity().equals(profile.getExpectedCity())) {
                score += 25;
            }

            // 薪资匹配（20分）：期望薪资在职位薪资范围内
            if (profile.getExpectedSalaryMin() != null && job.getSalaryMax() != null) {
                if (profile.getExpectedSalaryMin() <= job.getSalaryMax()
                    && (profile.getExpectedSalaryMax() == null || profile.getExpectedSalaryMax() >= job.getSalaryMin())) {
                    score += 20;
                } else {
                    // 部分匹配：按重叠比例给分
                    // ...
                }
            }

            // 学历匹配（15分）：学历等于要求 = 15，低于要求 = 0
            if (job.getEducation() != null && profile.getEducationLevel() != null) {
                if (eduLevelMatch(profile.getEducationLevel(), job.getEducation())) {
                    score += 15;
                }
            }

            // Step 4: 按分数降序，取 Top N
        }

        // Step 5: 返回 MatchResultVO 列表（含 job 信息 + score + 差距分析文本）
    }
}
```

**涉及表**: `sys_student_profile`（读）、`job_detail`（读）

---

**C3 — PushServiceImpl**

```
路径: occupation-recommend/.../service/impl/PushServiceImpl.java
依赖: PushRecordMapper（已就绪）
```

**接口**：

```java
public interface PushService {
    void createPush(Long userId, String type, String title, String content);  // 创建推送
    List<PushRecord> getMyPushes(Long userId, int pageNum, int pageSize);     // 我的推送列表
    void markAsRead(Long pushId, Long userId);                                // 标记已读
    int getUnreadCount(Long userId);                                          // 未读数
}
```

**涉及表**: `push_record`（含 `tenant_id`，自动注入）

---

**C4 — BehaviorServiceImpl**

```
路径: occupation-recommend/.../service/impl/BehaviorServiceImpl.java
依赖: StudentBehaviorMapper（已就绪）
```

```java
public interface BehaviorService {
    void record(Long userId, Long jobId, String action);  // 记录 VIEW/FAVORITE/APPLY/IGNORE
    List<StudentBehavior> getByUser(Long userId, int days);  // 近 N 天行为
    Map<String, Long> countByAction(Long userId);           // 各行为计数
}
```

**涉及表**: `student_behavior`（含 `tenant_id`，自动注入）

---

**C5 — StudentProfileController**

```
路径: occupation-recommend/.../controller/StudentProfileController.java
```

| 方法 | URL | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/student/profile` | 查自己的画像 | 学生 |
| PUT | `/api/student/profile` | 更新画像 | 学生 |
| GET | `/api/student/profile/skills` | 查自己的技能 | 学生 |

---

**C6 — RecommendController**

```
路径: occupation-recommend/.../controller/RecommendController.java
```

| 方法 | URL | 入参 | 说明 | 权限 |
|---|---|---|---|---|
| GET | `/api/student/recommend` | `topN=20` | 获取个性化推荐列表（按匹配分降序） | 学生 |
| GET | `/api/student/job/{jobId}` | — | 职位详情 + 自动记录 VIEW 行为 | 学生 |
| POST | `/api/student/job/{jobId}/favorite` | — | 收藏职位 | 学生 |
| DELETE | `/api/student/job/{jobId}/favorite` | — | 取消收藏 | 学生 |
| GET | `/api/student/favorites` | pageNum, pageSize | 收藏列表（关联 job_detail） | 学生 |
| POST | `/api/student/job/{jobId}/apply` | — | 投递职位 | 学生 |

**⚠️ 注意**：收藏/投递需写 `student_behavior` 表。投递成功同时生成 `push_record`。

---

**C7 — PushController**

```
路径: occupation-recommend/.../controller/PushController.java
```

| 方法 | URL | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/student/push/list` | 我的推送列表 | 学生 |
| PUT | `/api/student/push/{id}/read` | 标记已读 | 学生 |
| GET | `/api/student/push/unread/count` | 未读数量 | 学生 |

---

**C8 — TeacherController**

```
路径: occupation-recommend/.../controller/TeacherController.java
```

| 方法 | URL | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/teacher/students` | 学生列表（分页，含画像概览） | 教师 |
| GET | `/api/teacher/students/{userId}` | 学生详情（画像+行为统计） | 教师 |
| GET | `/api/teacher/students/{userId}/behaviors` | 学生行为明细 | 教师 |
| GET | `/api/teacher/stats` | 班级整体统计（画像完成率/投递数/匹配率） | 教师 |
| GET | `/api/teacher/export` | 导出学生数据 Excel | 教师 |

**涉及表**: `sys_user`（读学生角色）、`sys_student_profile`（读）、`student_behavior`（读）

---

**C9 — HrController**

```
路径: occupation-recommend/.../controller/HrController.java
```

| 方法 | URL | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/hr/jobs` | HR 发布新职位 | HR |
| PUT | `/api/hr/jobs/{id}` | 编辑职位 | HR |
| DELETE | `/api/hr/jobs/{id}` | 下架职位 | HR |
| GET | `/api/hr/jobs` | 我发布的职位列表 | HR |
| GET | `/api/hr/talents` | 浏览学生人才卡片（脱敏：只展示技能/学历/专业，隐去姓名手机号） | HR |
| GET | `/api/hr/applications` | 收到的投递列表 | HR |
| PUT | `/api/hr/applications/{id}/status` | 更新投递状态（筛选/面试/录用） | HR |

**涉及表**: `job_detail`（写）、`sys_student_profile`（读）、`student_behavior`（读）

---

**C10 — RecommendJobHandler**

```
路径: occupation-recommend/.../job/RecommendJobHandler.java
```

**职责**：XXL-Job 定时任务，每天 8:00 执行：
1. 查所有学生画像
2. 对每个学生调用 `JobMatchService.match(userId, 5)`
3. 将 Top 5 匹配结果写入 `push_record` 表
4. 推送标题："今日为你推荐 5 个高匹配职位"

---

#### 📊 C 组验收标准

- [ ] `mvn compile` 通过
- [ ] `GET /api/student/recommend` 返回按匹配分降序的职位列表
- [ ] `POST /api/student/job/{id}/favorite` 收藏成功
- [ ] `POST /api/student/job/{id}/apply` 投递成功 → 生成推送记录
- [ ] `GET /api/teacher/students` 返回学生画像列表
- [ ] `POST /api/hr/jobs` HR 发布职位成功
- [ ] RecommendJobHandler 每日 8:00 自动推送

---

### 🔴 D 组 — `occupation-web-ui`（Vue 3 前端，全四端）

> **阶段**: P3～P5 · **模块**: `occupation-web-ui/`  
> **依赖**: 后端 API（A/B/C 组）  
> **技术栈**: Vite + Vue 3 + Element Plus + ECharts + Axios + Pinia + Vue Router

**启动命令**：`cd occupation-web-ui && npm install && npm run dev`

---

#### 📋 任务清单（分三批，按依赖顺序）

**第 1 批 — 基础设施 + 登录（不依赖任何后端接口）**

| # | 文件 | 具体要写什么 | 预估行数 |
|---|---|---|---|
| D1 | `src/api/auth.js` | 封装 `login(username, password)` → POST /api/auth/login | ~20 |
| D2 | `src/api/request.js` | **已就绪**，但需完善响应拦截器（401 → 跳登录） | ~10 |
| D3 | `src/views/login/LoginView.vue` | 登录表单（用户名+密码+角色选择）、调 auth.js、存 token、路由跳转 | ~100 |

**D3 — LoginView 详细**：
- 表单：用户名、密码、角色下拉（学生/教师/管理员/HR）
- 成功后：token 存入 Pinia `user.js` → `userStore.login(token, role)`
- 路由跳转：根据 role → `/student`、`/teacher`、`/admin`、`/hr`
- 失败：Element Plus Message 报错提示

---

**第 2 批 — 管理后台 5 页（依赖 A/B 组的 Controller）**

| # | 文件 | 具体要写什么 | 预估行数 |
|---|---|---|---|
| D4 | `src/api/analysis.js` | `getDashboard()`、`queryJobs(params)` | ~20 |
| D5 | `src/api/crawler.js` | `getTaskList()`、`createTask()`、`startTask(id)`、`stopTask(id)` | ~30 |
| D6 | `src/api/report.js` | `getTemplateList()`、`createTemplate()`、`generateReport()`、`getRecordList()`、`download(id)` | ~30 |
| D7 | `src/api/user.js` | `getUserList()`、`createUser()`、`updateUser()`、`deleteUser(id)` | ~20 |
| D8 | `src/views/admin/Dashboard.vue` | 5 个 ECharts 图表 + 统计卡片行 | ~250 |
| D9 | `src/views/admin/CrawlerTask.vue` | El-Table CRUD + 启停按钮 + 搜索栏 | ~180 |
| D10 | `src/views/admin/ReportTemplate.vue` | El-Table + 新增/编辑 Dialog + JSON 编辑器 | ~200 |
| D11 | `src/views/admin/ReportList.vue` | 报告记录表 + 生成按钮 + 下载链接 + 状态标签 | ~150 |
| D12 | `src/views/admin/UserManage.vue` | 用户 El-Table + 新增 Dialog（角色选择） | ~150 |

**D8 — Dashboard 页面详细**：
```
5 个 ECharts 图表：
1. 行业 Top 10 柱状图（横轴=行业，纵轴=岗位数）
2. 城市分布饼图（城市占比）
3. 技能热度横向柱状图（Top 20 技能）
4. 学历分布玫瑰饼图
5. 岗位趋势折线图（横轴=月份，纵轴=岗位数，双线=数量+均薪）

顶部 4 个统计卡片：
- 总岗位数
- 本月新增
- 平均薪资
- 活跃城市数
```

**D9 — CrawlerTask 页面详细**：
- 表格列：任务名称、数据源类型、URL 模式、Cron 表达式、状态、操作
- 操作按钮：编辑、启动/停止、删除、查看日志
- 新增/编辑 Dialog：表单含 sourceType 下拉、sourceName、urlPattern、cronExpr
- 状态列用 Element Plus 的 `<el-tag>` 标签（运行中=绿色、停止=灰色）

**D10 — ReportTemplate 页面详细**：
- 表格列：模板名称、行业、类型（月度/季度/年度）、状态、操作
- 新增/编辑 Dialog：名称、行业下拉、类型下拉、模板内容（**JSON 编辑器**——可用 CodeMirror 或简单的 `<el-input type="textarea">`）
- 操作：编辑、启用/禁用、删除

**D11 — ReportList 页面详细**：
- 表格列：报告名称、模板、类型（PDF/Word/HTML）、生成时间、状态、操作
- 顶部操作栏：选择模板（Select）+ 选择文件类型（Radio）+ "生成报告"按钮
- 状态标签：生成中=橙色 spinning、已完成=绿色、失败=红色
- 下载按钮

**D12 — UserManage 页面详细**：
- 表格列：用户名、真实姓名、角色、手机号、状态、操作
- 新增/编辑 Dialog：用户名、密码（新增时必填）、真实姓名、角色下拉、手机号
- 操作：编辑、启用/禁用、重置密码

---

**第 3 批 — 学生端/教师端/HR 端（依赖 C 组的 Controller）**

| # | 文件 | 具体要写什么 | 预估行数 |
|---|---|---|---|
| D13 | `src/api/recommend.js` | `getRecommend()`、`getJobDetail(id)`、`favorite(id)`、`unfavorite(id)`、`apply(id)`、`getFavorites()` | ~40 |
| D14 | `src/api/profile.js` | `getProfile()`、`updateProfile()`、`getBehaviors()`、`getStats()` | ~20 |
| D15 | `src/api/push.js` | `getPushList()`、`markRead(id)`、`getUnreadCount()` | ~15 |
| D16 | `src/api/hr.js` | `postJob()`、`getMyJobs()`、`getTalents()`、`getApplications()`、`updateAppStatus()` | ~30 |
| D17 | `src/views/student/StudentHome.vue` | 推荐流卡片列表（职务卡片+匹配分+收藏按钮） | ~200 |
| D18 | `src/views/student/JobDetail.vue` | 职位详情页（标题/公司/薪资/技能标签/描述/投递按钮） | ~180 |
| D19 | `src/views/student/Profile.vue` | 画像表单（专业/技能编辑器/意向城市/意向行业/薪资/学历） | ~200 |
| D20 | `src/views/student/Favorites.vue` | 收藏列表（表格+取消收藏） | ~100 |
| D21 | `src/views/student/Reports.vue` | 报告列表（表格+查看/下载） | ~100 |
| D22 | `src/views/teacher/TeacherHome.vue` | 班级概览（统计卡片+最近行为表格） | ~150 |
| D23 | `src/views/teacher/Students.vue` | 学生列表（表格+详情抽屉） | ~150 |
| D24 | `src/views/teacher/Suggestions.vue` | 教学建议列表（基于技能缺口的展示页） | ~120 |
| D25 | `src/views/hr/HrHome.vue` | HR 工作台（统计卡片+最近投递列表） | ~150 |
| D26 | `src/views/hr/JobManage.vue` | 职位管理（表格+新增/编辑 Dialog） | ~180 |
| D27 | `src/views/hr/Talents.vue` | 人才卡片网格（脱敏：技能标签+专业+学历，无姓名） | ~150 |

**D17 — StudentHome（推荐流）**：
```
- 顶部：推送未读角标（Badge），点击下拉推送列表
- 主体：职位卡片网格（el-card），每张卡片展示：
  - 职位标题（加粗）
  - 公司名称 + 城市（灰色小字）
  - 薪资范围（橙色高亮）
  - 技能标签（el-tag 横向排列，最多 4 个）
  - 匹配度进度条（匹配分/100）
  - 底部：收藏按钮（星标图标 空心/实心切换）
- 点击卡片 → 跳转 JobDetail
- 支持下拉刷新 / 上拉加载更多（el-infinite-scroll）
```

**D18 — JobDetail**：
```
- 顶部：职位标题 + 公司名称 + 城市 + 发布日期
- 薪资卡片：薪资范围 + 学历要求 + 经验要求（el-descriptions）
- 技能标签区：el-tag 列表
- 职位描述区：富文本渲染（v-html）
- 底部操作栏：投递按钮（el-button type="primary"）+ 收藏按钮
```

**D19 — Profile**：
```
- 表单字段：专业（el-input）、技能（el-select 多选 + 自定义输入）、意向城市（el-cascader 省/市）、意向行业（el-select）、期望薪资区间（el-slider 双滑块）、学历（el-radio-group）
- 提交后更新画像 + 刷新推荐列表
```

**D26 — JobManage（HR 职位管理）**：
```
- 表格：职位标题、公司、城市、薪资、学历、经验、状态、操作
- 新增/编辑 Dialog：标题、公司（预填）、城市、行业、薪资范围、学历要求、经验要求、技能标签、职位描述（富文本或 textarea）
```

---

#### 🔧 前端配套工作

| # | 文件 | 具体要写什么 |
|---|---|---|
| D28 | `src/store/user.js` | **已就绪**，无需修改 |
| D29 | `src/store/app.js` | **已就绪**，无需修改 |
| D30 | `src/components/AppLayout.vue` | **已就绪**，但如果需要调整侧边栏菜单项，按需修改 |

---

#### 📊 D 组验收标准

- [ ] `npm run dev` 启动无报错
- [ ] 登录页可以输入用户名密码，跳转到对应角色首页
- [ ] Dashboard 页面展示 5 个图表 + 4 个统计卡片
- [ ] CrawlerTask 页面可新增/编辑/启停任务
- [ ] 学生端推荐流展示职位卡片，可收藏+投递
- [ ] 教师端学生列表可查看画像详情
- [ ] HR 端可发布/编辑职位

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

---

## 8. 每人自检清单

> 每完成一个文件就打勾 ☑️，Git 提交前跑一遍编译。

### 🔵 A 组（大数据分析）— 5 个文件

```
☐ A1  occupation-analysis/.../service/impl/AnalysisServiceImpl.java
      → 实现 getDashboard()，5 个维度各查一次 analysis_result 表
☐ A2  occupation-analysis/.../service/impl/JobDetailServiceImpl.java
      → 实现 queryJobs()，city/industry/salary/education/experience/keyword 筛选
      → ⚠️ job_detail 无 tenant_id，不加租户过滤
☐ A3  occupation-analysis/.../controller/AnalysisController.java
      → GET /api/analysis/dashboard  +  GET /api/analysis/jobs
☐ A4  occupation-analysis/.../config/RedisCacheConfig.java  （可选）
      → Redis 缓存 Dashboard，TTL=1h
☐ A5  按需 DTO/VO（如需要更多查询字段）

完成后执行: mvn compile -DskipTests
```

### 🟢 B 组（报告引擎 + 对外 API）— 15 个文件

```
Report 模块 (7 个):
☐ B1  occupation-report/.../service/ReportTemplateService.java + impl
      → 模板 CRUD：listAll / getById / create / update / delete
☐ B2  occupation-report/.../service/ReportGeneratorService.java + impl
      → 核心：调 AnalysisService → Freemarker 渲染 → Pdf/Word 导出 → 入库
☐ B3  occupation-report/.../service/PdfExporter.java
      → HTML → PDF（Flying Saucer），注意中文字体
☐ B4  occupation-report/.../service/WordExporter.java
      → HTML → Word（Apache POI）
☐ B5  occupation-report/.../controller/ReportTemplateController.java
      → GET/POST/PUT/DELETE /api/admin/report/template
☐ B6  occupation-report/.../controller/ReportRecordController.java
      → POST /api/admin/report/generate + GET download/{id} + list
☐ B7  按需 DTO/VO

API 模块 (8 个):
☐ B8  occupation-api/.../service/impl/ApiAuthServiceImpl.java
      → apiKey+apiSecret 校验 → JWT 签发
☐ B9  occupation-api/.../service/impl/OpenApiServiceImpl.java
      → 职位查询/报告摘要/技能热度/大盘统计
☐ B10 occupation-api/.../controller/OpenAuthController.java
      → POST /api/open/auth/token
☐ B11 occupation-api/.../controller/OpenJobController.java
      → GET /api/open/jobs + GET /api/open/jobs/{id}
☐ B12 occupation-api/.../controller/OpenStatsController.java
      → GET /api/open/stats/overview|skills/hot|industries
☐ B13 occupation-api/.../config/RateLimitConfig.java
      → Redis 令牌桶，60次/分钟/apiKey
☐ B14 occupation-api/.../config/SwaggerConfig.java
      → Knife4j 分组：对外API / 管理后台API
☐ B15 occupation-api/.../config/OpenApiSecurityConfig.java
      → Security 拦截 /api/open/**，JWT Bearer 校验

完成后执行: mvn compile -DskipTests
```

### 🟠 C 组（推荐推送 + 教师/HR）— 11 个文件

```
☐ C1  occupation-recommend/.../service/impl/StudentProfileServiceImpl.java
      → getByUserId / saveOrUpdate / updateSkills
☐ C2  occupation-recommend/.../service/impl/JobMatchServiceImpl.java  ⭐核心
      → 调 JobDetailService → 规则打分（技能40+城市25+薪资20+学历15）
☐ C3  occupation-recommend/.../service/impl/PushServiceImpl.java
      → createPush / getMyPushes / markAsRead / getUnreadCount
☐ C4  occupation-recommend/.../service/impl/BehaviorServiceImpl.java
      → record(VIEW/FAVORITE/APPLY) / getByUser / countByAction
☐ C5  occupation-recommend/.../controller/StudentProfileController.java
      → GET/PUT /api/student/profile
☐ C6  occupation-recommend/.../controller/RecommendController.java
      → GET /api/student/recommend + favorite/apply 全套
☐ C7  occupation-recommend/.../controller/PushController.java
      → GET push/list + PUT read + GET unread/count
☐ C8  occupation-recommend/.../controller/TeacherController.java
      → GET students/stats/export
☐ C9  occupation-recommend/.../controller/HrController.java
      → POST jobs + GET talents/applications
☐ C10 occupation-recommend/.../job/RecommendJobHandler.java
      → XXL-Job 每日 8:00 推送 Top5
☐ C11 按需 DTO/VO

完成后执行: mvn compile -DskipTests
```

### 🔴 D 组（Vue 3 前端）— 30 个文件（分 3 批）

```
第1批：基础设施 + 登录 (3 个，不依赖后端)
☐ D1  src/api/auth.js            → login()
☐ D2  src/api/request.js          → 完善 401 跳登录拦截
☐ D3  src/views/login/LoginView.vue → 表单 + token 存储 + 角色路由

第2批：管理后台 (9 个，依赖 A/B 组)
☐ D4  src/api/analysis.js         → getDashboard / queryJobs
☐ D5  src/api/crawler.js          → CRUD + start/stop
☐ D6  src/api/report.js           → CRUD + generate + download
☐ D7  src/api/user.js             → CRUD
☐ D8  src/views/admin/Dashboard.vue     → 5图表 + 4卡片
☐ D9  src/views/admin/CrawlerTask.vue   → El-Table + 启停
☐ D10 src/views/admin/ReportTemplate.vue → El-Table + JSON编辑器
☐ D11 src/views/admin/ReportList.vue     → 生成+下载+状态
☐ D12 src/views/admin/UserManage.vue     → El-Table + 新增Dialog

第3批：学生端/教师端/HR端 (15 个，依赖 C 组)
☐ D13 src/api/recommend.js            → getRecommend / favorite / apply
☐ D14 src/api/profile.js              → getProfile / updateProfile
☐ D15 src/api/push.js                 → pushList / markRead / unreadCount
☐ D16 src/api/hr.js                   → postJob / getTalents / applications
☐ D17 src/views/student/StudentHome.vue  → 推荐卡片流
☐ D18 src/views/student/JobDetail.vue    → 职位详情
☐ D19 src/views/student/Profile.vue      → 画像表单
☐ D20 src/views/student/Favorites.vue    → 收藏列表
☐ D21 src/views/student/Reports.vue      → 报告列表
☐ D22 src/views/teacher/TeacherHome.vue  → 班级概览
☐ D23 src/views/teacher/Students.vue     → 学生列表
☐ D24 src/views/teacher/Suggestions.vue  → 教学建议
☐ D25 src/views/hr/HrHome.vue            → HR工作台
☐ D26 src/views/hr/JobManage.vue         → 职位管理
☐ D27 src/views/hr/Talents.vue           → 人才浏览
☐ D28 src/store/user.js      → 已就绪，无需修改
☐ D29 src/store/app.js       → 已就绪，无需修改
☐ D30 src/components/AppLayout.vue → 已就绪，按需改菜单

完成后执行: npm run dev → 无报错，所有页面可路由跳转
```

---

## 9. 协作时间线

```
第1天  → A组 写完 A1+A2（必须），B/C 组可以编译通过
         D组 写完 D1~D3（登录页），可自测

第2天  → A组 完成 A3+A4
         B组 开始写 B1~B7（报告模块）
         C组 开始写 C1~C4（Service 层）
         D组 开始写 D4~D8（Dashboard 图表）—— 需 A 组 /api/analysis/dashboard 可用

第3天  → B组 完成 B8~B15（API 模块）
         C组 完成 C5~C10（Controller + Job）
         D组 继续 D9~D12（管理后台），开始 D13~D16（API 封装）

第4天  → B/C 组联调修正
         D组 完成 D17~D27（学生/教师/HR 端页面）

第5天  → 全员联调 + Bug 修复 + 文档更新
```
