# 职业能力大数据服务平台 — 团队同步

> **日期**: 2026-07-06  
> **状态**: 设计阶段完成，尚未开始编码

---

## 1. 项目简介

搭建一套基于大数据 + AI 的职业能力分析 SaaS 平台，解决高校人才培养与就业市场之间的信息不对称问题。

**核心能力**: 全量数据采集 → 大数据智能分析 → 自动化报告生成 → 个性化岗位推送 → 对外开放 API。

---

## 2. 文档结构

```
e:\occupation\
├── AGENTS.md                  # AI 编码规范（模块化、分层架构、多租户等硬性规则）
├── vibe-coding-guide.md       # Vibe Coding 工作流指南
└── memory-bank/               # 项目文档唯一来源
    ├── design-document.md     # 产品设计文档
    ├── tech-stack.md          # 技术选型文档
    ├── implementation-plan.md # 实施计划（5 阶段 36 步）
    ├── architecture.md        # 架构状态（Schema、模块、API、部署组件）
    └── progress.md            # AI 开发进度追踪
```

---

## 3. 五层系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                      展示层 / API 层                              │
│  管理后台 │ 学生端 │ 教师端 │ 企业HR端 │ 3D可视化大屏 │ 对外API   │
├──────────────────────────────────────────────────────────────────┤
│                       业务应用层                                  │
│  报告分析  │ 报告自动生成  │ 岗位推送  │ 技能缺口诊断 │ 教学改革助手 │
├──────────────────────────────────────────────────────────────────┤
│                   ⭐ AI 智能引擎层（差异化核心）                    │
│   LLM 自然语言洞察  │ 知识图谱推理  │ NLP 语义匹配  │ 趋势预测模型  │
├──────────────────────────────────────────────────────────────────┤
│                       分析引擎层                                  │
│   Spark 离线分析  │ 实时流分析(Kafka Streaming)  │ 多维OLAP        │
├──────────────────────────────────────────────────────────────────┤
│                    数据处理与存储层                                │
│   HDFS │ Hive │ HBase │ Redis │ MySQL 8.0 │ Neo4j │ Elasticsearch │
├──────────────────────────────────────────────────────────────────┤
│                       数据采集层                                  │
│   BOSS直聘/智联招聘 │ 企业官网 │ 政府公开数据 │ 第三方API │ 课程数据  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. 四大用户角色（多租户 SaaS）

| 角色 | 核心功能 | 典型场景 |
|---:|---|---|
| **学生** | 个人画像、岗位推荐、就业报告查看、投递/收藏 | 了解行业趋势，匹配合适岗位 |
| **教师/辅导员** | 班级就业动态、教学建议报告、学生数据导出 | 掌握学生就业情况，调整教学内容 |
| **管理员** | 系统配置、数据采集调度、报告模板、用户管理 | 平台日常运维与配置管理 |
| **企业 HR** | 发布职位、人才画像浏览（脱敏）、投递管理 | 企业发布岗位，对接院校人才 |

**数据隔离**: 所有业务表通过 `tenant_id` 行级隔离，JWT Token 携带租户标识。

---

## 5. 功能模块总览

### 基础模块（P1-P5）

| 模块 | 职责 |
|---|---|
| **分布式数据采集** | BOSS直聘等主流招聘平台爬虫，反爬策略，Kafka 解耦采集与处理链路 |
| **报告分析系统** | 行业/技能/地域/学历/时间趋势 五维分析，Spark 离线批处理 |
| **报告自动生成** | 模板管理 + Freemarker 渲染，PDF/Word/HTML 导出，定时+手动触发 |
| **岗位推送系统** | 学生画像构建 + 规则/协同/内容推荐，站内通知/邮件/短信推送 |
| **对外公开 API** | OAuth2 鉴权 + 限流，职位查询/报告摘要/技能热度/统计大盘 |
| **系统监控** | Actuator + Prometheus + Grafana，Kafka 延迟/爬虫异常告警 |

### ⭐ 差异化模块（P6）

| 模块 | 核心价值 |
|---|---|
| **技能缺口诊断引擎** | 自动对比"学校教了什么 vs 市场要了什么"，输出课程改革建议 |
| **职业能力知识图谱** | Neo4j + G6 可视化，查询"学完 Java 再学 Spring，匹配岗位增 3 倍" |
| **LLM 智能报告引擎** | AI 生成自然语言洞察，学生/教师/校领导三版个性化报告 |
| **实时市场脉搏大屏** | Kafka Streaming 实时更新，像股市指数一样的"就业热度指数" |
| **NLP 简历-JD 语义匹配** | BGE 向量化 + FAISS 检索，语义级匹配 + 技能差距分析 |
| **就业趋势预测模型** | Prophet 时序预测，3/6 个月后岗位数量/薪资走势预测 |

---

## 6. 技术选型总览

### 后端（Java）

| 技术 | 版本 | 说明 |
|---|---|---|
| SpringBoot | 2.7.x | 模块化单体架构，7 个 Maven Module |
| MyBatis-Plus | 3.5.x | ORM + 多租户插件 |
| Spring Security + JWT | 5.x | 认证鉴权 |
| XXL-Job | 2.x | 分布式定时任务调度 |
| Kafka | 3.4.x | 消息队列（采集↔清洗↔分析解耦） |
| WebMagic | — | 爬虫框架 |

### 前端（Vue 3）

| 技术 | 说明 |
|---|---|
| Vue 3 + Vite 5 | 组合式 API，TypeScript |
| Element Plus | 管理后台 UI 组件库 |
| ECharts 5 | 数据可视化图表 |
| G6 ⭐ | 知识图谱可视化 |
| Three.js ⭐ | 3D 可视化大屏 |
| Pinia + Axios | 状态管理 + HTTP 客户端 |

### 大数据 & AI

| 技术 | 用途 |
|---|---|
| Spark 3.4 + Hive 3.1 + HBase 2.4 | 离线批处理 + 数据仓库 + 明细查询 |
| Kafka + Zookeeper + Yarn | 消息队列 + 协调 + 资源调度 |
| Neo4j 5.x ⭐ | 知识图谱存储与查询 |
| Elasticsearch 8.x ⭐ | 全文检索 + 职位搜索 |
| DeepSeek/通义千问 ⭐ | LLM 智能报告生成 |
| Sentence-Transformers + FAISS ⭐ | NLP 语义匹配 |
| Prophet ⭐ | 就业趋势预测 |

### 基础设施

| 技术 | 说明 |
|---|---|
| MySQL 8.0 | 业务主库 |
| Redis 6.2 | 缓存、Session、实时计数器 |
| Nginx 1.24 | 反向代理 + 静态资源 |
| Docker + Docker Compose | 本地开发环境一键部署 |

---

## 7. 模块划分（Maven 7 模块）

```
occupation-platform (父 POM)
├── occupation-common        # 公共：统一响应、异常、多租户拦截器、工具类
├── occupation-auth          # 认证授权：登录、JWT、Token 校验
├── occupation-crawler       # 采集管理：爬虫调度、采集任务 API
├── occupation-analysis      # 分析服务：Spark Job、数据清洗管道
├── occupation-report        # 报告生成：模板管理、PDF/Word 导出、LLM 引擎
├── occupation-recommend     # 推荐推送：画像匹配、推荐算法、通知推送
├── occupation-api           # 对外开放：OAuth2 鉴权、限流、Swagger 文档
└── occupation-web           # 启动模块：入口类、全局配置
```

> 初期使用模块化单体架构，按需拆分为微服务（Spring Cloud 或 K8s）。

---

## 8. 数据库核心表（12 张）

| 表名 | 模块 | 说明 |
|---|---|---|
| sys_tenant | common | 租户表 |
| sys_user | auth | 用户表（含 role: STUDENT/TEACHER/ADMIN/HR） |
| sys_student_profile | recommend | 学生画像表 |
| crawler_task | crawler | 采集任务表 |
| crawler_log | crawler | 采集日志表 |
| raw_job_data | crawler | 原始职位数据表 |
| job_detail | analysis | 清洗后职位表 |
| analysis_result | analysis | 分析结果表 |
| report_template | report | 报告模板表 |
| report_record | report | 报告记录表 |
| push_record | recommend | 推送记录表 |
| student_behavior | recommend | 学生行为表 |
| sys_alert | web/common | 告警表 |

---

## 9. 实施计划总览

| 阶段 | 内容 | 步骤数 | 状态 |
|---:|---|---|---|
| **P1** | 项目骨架 + JWT 认证 + 多租户 + Kafka + 爬虫采集 | 9 步 | ⏳ 设计完成，待开发 |
| **P2** | 大数据清洗 + 5 维度分析 + 定时调度 + 职位查询 API | 8 步 | ⏳ 待开发 |
| **P3** | Vue 3 管理后台 + 分析看板 + 报告生成/导出 + 用户管理 | 7 步 | ⏳ 待开发 |
| **P4** | 学生端 + 画像匹配 + 推荐引擎 + 推送通知 + 行为闭环 | 6 步 | ⏳ 待开发 |
| **P5** | 对外 API + 教师端 + 企业端 + 监控告警 + 压测 + 文档 | 6 步 | ⏳ 待开发 |
| **P6** | ⭐ 6 大 AI 差异化亮点模块 | — | 远期规划 |

---

## 10. P1 首阶段目标（即将开始）

1. Maven 多模块项目初始化
2. SpringBoot 基础骨架 + 统一响应格式
3. 数据库表设计 + init.sql
4. MyBatis-Plus + 多租户插件
5. JWT 认证 + 登录接口
6. Kafka 生产者/消费者基础配置
7. 爬虫基础框架（WebMagic）
8. 实现一个真实采集源（BOSS直聘等）
9. 采集任务管理接口 + XXL-Job 调度

---

## 11. 开发环境

- **Java**: IntelliJ IDEA + JDK 8+/11+
- **前端**: VS Code + Node 18+
- **Docker Compose**: 一键启动 MySQL + Redis + Kafka + Nginx
- **大数据组件**: HDFS + Hive + HBase + Spark（Docker 部署）
- **AI 组件**: P6 阶段再接入（Neo4j + ES + LLM API）

---

> **下一步**: 启动 P1 Step 1.1 — 初始化 Maven 多模块项目结构。  
> 文档权威来源: `memory-bank/`，AI 编码规范: `AGENTS.md`，工作流: `vibe-coding-guide.md`
