#!/usr/bin/env python3
"""
生成新闻种子数据 SQL。
- 5 个分类 (backend/frontend/test/devops/bigdata) × 3 种类型 (DATA_CAST/ARTICLE/EXTERNAL)
- 每个分类下 ARTICLE 20 条，EXTERNAL 15 条，DATA_CAST 5 条，共 40 条 × 5 = 200 条
- 全部自带 cover_image（picsum.photos 通过 seed 锁图），让前端有真实图片
"""

import random
from datetime import datetime, timedelta

random.seed(20260713)

CATS = ['backend', 'frontend', 'test', 'devops', 'bigdata']
CAT_LABEL = {
    'backend': '后端', 'frontend': '前端', 'test': '测试', 'devops': '运维', 'bigdata': '大数据'
}

# picsum 关键词提示（picsum.photos 用 seed 锁图，所以这里只是 seed 字符串，让图片稳定）
# seed 长度 ≤ 30 字符
PICSUM_SEED_PREFIX = 'occ'

# 后端
BACKEND_ARTICLES = [
    "Spring Boot 3.2 正式发布：原生镜像与虚拟线程加持，云原生部署更轻量",
    "深入理解 JVM G1 垃圾回收器：从原理到生产环境调优实战",
    "Kafka 3.7 重磅更新：KRaft 全面替代 ZooKeeper，运维复杂度大幅下降",
    "微服务架构下的分布式事务：Seata AT 模式原理与最佳实践",
    "Redis 7.4 性能优化：从单线程到 I/O 多线程的演进路径",
    "MySQL 8.4 高可用方案：Group Replication 与 MGR 故障切换全流程",
    "Nacos 2.3 配置中心实战：多环境隔离与灰度发布完整方案",
    "Dubbo 3.3 Triple 协议：兼容 gRPC 的跨语言微服务新选择",
    "从单体到微服务：一次真实的领域驱动设计（DDD）拆分复盘",
    "PostgreSQL 16 新特性详解：逻辑复制、SQL/JSON 性能与并行查询",
    "Elasticsearch 8.13 调优实战：从亿级日志检索到 50ms 响应",
    "OpenTelemetry 在 Spring Cloud 中的全链路追踪落地",
    "RocketMQ 5.x 架构演进：存储计算分离与 Pop 消费模式详解",
    "Java 21 虚拟线程（Virtual Threads）落地：千级并发下的性能验证",
    "高并发秒杀系统设计：从前端限流到库存扣减的全链路方案",
    "Service Mesh 实战：Istio 1.21 在生产环境的灰度与流量管理",
    "Spring Cloud Gateway 4.0 响应式编程实践：WebFlux 性能压测对比",
    "Maven 与 Gradle 构建性能对比：大型项目下的依赖解析优化",
    "从零搭建分布式链路监控系统：日志、指标、追踪三位一体",
    "数据库连接池 HikariCP 调优：maxLifetime 与 leakDetectionThreshold 实战",
]
BACKEND_EXTERNALS = [
    "B 站 Java 岗面试题全曝光：从基础到架构的 50 道真题解析",
    "字节跳动内部高并发中间件 KBEngine 开源细节",
    "CrateDB 6.3.5 发布，分布式 SQL 数据库支持内自动检测与一键升级",
    "Nebula 1.2.0 一口气集成三家云：七牛云 Kodo、AWS S3、Cloudflare R2",
    "Memcached 1.6.45 发布，高性能分布式缓存系统",
    "ThingsPanel 物联网平台 v1.2.5 发布：APP 可视化入口与嵌入体验升级",
    "Apache Dubbo 3.3.0 正式发布，Triple 协议成为一等公民",
    "PostgreSQL 17 GA：逻辑复制槽、SQL/JSON 表与 MERGE 增强",
    "Spring 官方：Spring Boot 4.0 路线图首次披露",
    "Rust 1.79 稳定版：内联汇编与 const 泛型迎来新写法",
    "Redis 8.0 路线图公布：向量检索、查询引擎与多租户支持",
    "阿里云开源 Spring Cloud Alibaba 2023 全新发布",
    "Kafka 4.0 将彻底移除 ZooKeeper 依赖",
    "字节跳动开源 CloudWeGo：高性能 Golang 微服务框架家族",
    "Netty 4.2 性能基准：单机 QPS 突破 200 万",
]
BACKEND_DATA_CAST = [
    ("平台在库 Java 后端岗位达 38 个，占技术岗总量 33%",
     "Java 仍是后端招聘主力，Spring Boot 全栈、Spring Cloud 微服务、消息队列、分布式数据库是高频技能关键词。"),
    ("Python 后端招聘量同比上涨 24%，AI/数据方向带动效应明显",
     "Python 岗位多与 AI 推理服务、数据平台结合，要求 FastAPI、异步编程与基础 ML 工程化能力。"),
    ("Go 语言岗位平均薪资领先后端方向 18%",
     "Go 在云原生、基础设施、RPC 框架领域需求旺盛，要求 Kubernetes、Docker、gRPC 相关项目经验。"),
    ("后端实习岗简历通过率仅 7%，项目深度比技术栈更关键",
     "面试官更关注完整业务闭环项目，仅罗列框架名称的简历越来越难通过筛选。"),
    ("资深后端工程师缺口扩大：3 年以上经验岗位占总量 41%",
     "平台数据显示企业对能独立承担模块设计与稳定性保障的中高级工程师需求明显增加。"),
]

# 前端
FRONTEND_ARTICLES = [
    "Vue 3.5 全面解析：响应式系统重写、SSR 性能提升与组合式 API 进阶",
    "React 19 RC 解读：Server Components、Actions 与 use API 一网打尽",
    "TypeScript 5.5 新特性：Inferred Type Predicates 与 Regular Expression Syntax Checking",
    "Vite 6 深度对比 Webpack 5：构建速度、配置复杂度与生产产物体积",
    "微前端实战：qiankun 2.x 落地与跨应用状态管理方案",
    "前端可视化：ECharts 5.5 大数据渲染性能优化实战",
    "从零搭建企业级组件库：Monorepo、Storybook、单元测试全覆盖",
    "Next.js 15 App Router 实战：Server Actions、Streaming 与缓存策略",
    "前端性能优化：Core Web Vitals 指标体系与 LCP/INP/CLS 全方位提升",
    "Web Components 与 Lit 框架：跨技术栈组件复用的终极方案",
    "Node.js 22 性能对比：Bun、Deno 与 Node 全方位 benchmark",
    "前端监控 SDK 自研：行为、性能、错误三大模块的完整实现",
    "构建工具链演进：从 Gulp 到 Turbopack 的十年技术变迁",
    "Tailwind CSS 4.0 全新引擎：CSS-first 配置与零运行时方案",
    "WebGPU 入门：在浏览器中调用 GPU 进行通用计算",
    "前端工程化：Monorepo 工具对比 pnpm workspaces、Turborepo、Nx",
    "Three.js 实战：从零搭建 3D 数据可视化大屏",
    "前端安全：XSS、CSRF、CSP 防御体系完整梳理",
    "Flutter Web 3.24 性能优化：JS Bundle 体积缩减 60%",
    "Electron 32 跨平台桌面应用：性能、内存与原生交互最佳实践",
]
FRONTEND_EXTERNALS = [
    "Vue.js 2025 年度状态报告：90% 团队仍以 Vue 3 为主",
    "React Conf 2024 回顾：Server Components 成为新默认",
    "Vercel 发布 v0 升级版：AI 生成前端 UI 体验再进化",
    "Chrome 130 正式版：CSS Anchor Positioning 与 View Transitions 全面可用",
    "Tailwind CSS 4 正式版发布：性能与 DX 双双提升",
    "字节跳动开源 Modern.js：渐进式 Web 应用框架",
    "Nuxt 4 路线图首次披露：Vue 3.5 + Nitro 2 全栈新体验",
    "Storybook 8 发布：UI 组件开发与文档一体",
    "Svelte 5 GA：Runes 响应式系统正式登场",
    "阿里 Ant Design X 正式开源：AI 场景下的企业级 UI 解决方案",
    "Deno 2.0 正式发布：兼容 npm 与 Node 生态",
    "Bun 1.1.30 更新：HTTP 服务器性能再度提升 30%",
    "W3C 公布 Web Components 1.0 候选推荐标准",
    "Three.js r170 发布：WebGPU 渲染器进入稳定阶段",
    "ESLint 9 flat config 全面推行：传统配置方式弃用",
]
FRONTEND_DATA_CAST = [
    ("前端岗位平均面试轮次 3.2 轮，比后端多 0.8 轮",
     "前端岗普遍包含笔试、算法、框架原理、项目深挖与 HR 面，候选人需准备更系统化。"),
    ("Vue 3 与 React 19 招聘需求基本持平，Vue 略占上风",
     "平台岗位中 Vue 3 占前端方向 46%，React 19 占 41%，其余为 Angular/Svelte 等。"),
    ("TypeScript 在前端岗位中的出现率达 87%",
     "几乎所有一线公司前端岗位都将 TypeScript 列为必备或加分项，纯 JS 岗位持续缩减。"),
    ("大厂前端实习岗竞争比达 1:43，简历差异化是关键",
     "拥有可演示的完整项目（部署上线、有用户数据）的候选人通过率显著更高。"),
    ("Node.js 全栈工程师岗位增长 31%，BFF 架构受青睐",
     "前端工程师承担 BFF 层（Backend for Frontend）成为趋势，Node 工程化能力成为差异化技能。"),
]

# 测试
TEST_ARTICLES = [
    "从零搭建自动化测试平台：pytest + Selenium + Allure 完整方案",
    "接口自动化测试：Requests + Pytest + YAML 数据驱动最佳实践",
    "性能测试工具对比：JMeter、Locust、Gatling 与 k6 全面横评",
    "App 自动化测试：Appium 2.0 + Flutter Driver 跨平台方案",
    "测试左移实践：单元测试覆盖率从 30% 提升到 80% 的完整路径",
    "Playwright 1.45 新特性：Trace Viewer 与组件测试能力",
    "契约测试（Pact）落地：微服务间接口一致性的工程化保障",
    "Mock 平台自研：挡板数据、异常注入与回放一体化",
    "混沌工程在测试中的应用：ChaosBlade 故障演练全流程",
    "AI 辅助测试：用大模型自动生成测试用例与缺陷预测",
    "UI 自动化稳定性提升：Page Object 模式与失败重试机制",
    "测试用例设计方法论：边界值、等价类、状态迁移与场景法",
    "Postman/Newman 与 JMeter 在 CI/CD 中的集成实践",
    "接口测试断言技巧：Schema 校验、数据库校验、上下游校验",
    "前端 E2E 测试：Cypress 14 vs Playwright 谁更适合中后台",
    "性能瓶颈定位：Profiler、火焰图与 APM 工具链组合拳",
    "数据库测试：数据生成、回滚与一致性校验方案",
    "安全测试入门：OWASP Top 10 与 Burp Suite 实战",
    "可观测性测试：从日志、指标、追踪三角度验证系统质量",
    "测试团队转型：从功能测试到测试开发的技能树重构",
]
TEST_EXTERNALS = [
    "Pytest 8 正式发布：性能优化与新版 fixture 机制",
    "Playwright 1.50 发布：组件测试进入稳定版",
    "Selenium 4.25 新增 CDP 协议支持：现代化浏览器自动化更稳定",
    "Postman 11 全面 AI 化：自动生成测试与文档",
    "k6 0.50 发布：云原生压测工具核心升级",
    "Locust 2.31 发布：分布式压测改进",
    "JUnit 5.11 发布：Java 测试框架进入新阶段",
    "Allure 3 测试报告框架：全新 UI 与多语言支持",
    "TestRail 9.0 发布：测试用例管理平台升级",
    "Cypress 14 GA：跨浏览器测试全面支持",
    "Appium 2.11 发布：移动端自动化测试改进",
    "JMeter 5.6.3 维护版本发布",
    "Gatling 3.11：云端压测能力进一步增强",
    "Charles Proxy 5 发布：抓包与 mock 体验升级",
    "OWASP 2024 风险榜更新：API 安全成新焦点",
]
TEST_DATA_CAST = [
    ("测试开发岗位需求增速达 28%，明显高于功能测试",
     "具备自动化、性能与 CI/CD 集成能力的测试开发工程师供不应求，平均薪资比功能测试高 35%。"),
    ("自动化测试在头部互联网公司覆盖率超 85%",
     "接口与 UI 自动化已成为标配，手工测试比例压缩到回归与探索性测试。"),
    ("AI 辅助测试在大厂试点落地：缺陷预测准确率达 73%",
     "基于历史缺陷日志训练模型，可提前 1-2 天预测高风险模块，节省回归成本。"),
    ("性能测试岗位稀缺：P99 指标关注度上升",
     "越来越多公司要求性能测试工程师输出 P99/P999 长尾指标，而非只看平均值。"),
    ("测试平台化趋势明显：自研 Mock 平台、自动化平台成为简历加分项",
     "能从 0 到 1 搭建测试平台（含用例管理、定时执行、报告聚合）的候选人非常受青睐。"),
]

# 运维
DEVOPS_ARTICLES = [
    "Kubernetes 1.31 深度解读：Sidecar 容器、动态资源分配新特性",
    "Prometheus + Grafana 监控体系实战：从指标采集到告警闭环",
    "Docker Buildx 多架构镜像构建：ARM64 与 x86_64 一套搞定",
    "CI/CD 进阶：GitLab CI 与 Argo CD 打造 GitOps 最佳实践",
    "Terraform 1.9 实战：多云资源编排与状态管理",
    "SRE 实践：SLO、错误预算与故障复盘机制建设",
    "Ansible 10 自动化运维：Playbook 编写与角色复用最佳实践",
    "云原生存储：Rook + Ceph 在生产环境的部署与调优",
    "可观测性体系：OpenTelemetry Collector 统一采集实践",
    "服务网格进阶：Linkerd 2.15 与 Istio 1.22 性能对比",
    "容器安全：Trivy + Falco + OPA 构建全链路安全防护",
    "Helm 4 实战：Chart 模板化与多环境发布",
    "云原生 CI：Tekton 与 Argo Workflows 流水线对比",
    "高可用 Kubernetes 集群：etcd 备份、灾备与升级方案",
    "Pulumi vs Terraform：IaC 工具选型与团队协作",
    "蓝绿发布、灰度发布与金丝雀发布：落地策略全解析",
    "日志收集：EFK（Elasticsearch + Fluentd + Kibana）生产部署",
    "云原生网关：Ingress-NGINX 与 Envoy Gateway 选型",
    "Serverless 实战：Knative 与 OpenFunction 在 K8s 上的函数计算",
    "云成本优化：FinOps 实践与资源利用率提升路径",
]
DEVOPS_EXTERNALS = [
    "Kubernetes 1.31 正式发布",
    "Docker Desktop 4.32 发布：性能与开发者体验优化",
    "HashiCorp 宣布 Terraform 转向 BSL 协议引发社区争议",
    "Argo CD 2.13 发布：多集群管理与 GitOps 新特性",
    "Prometheus 3.0 路线图：远程写入、OpenMetrics 改进",
    "Grafana 11 正式发布：Dashboards 与告警体验升级",
    "OpenTelemetry 1.30 GA：可观测性标准全面落地",
    "Rancher 2.9 发布：Kubernetes 管理平台升级",
    "Vault 1.17 发布：Secrets 管理能力提升",
    "Consul 1.20 发布：服务网格与配置中心",
    "Pulumi 3.130 发布：IaC 工具持续迭代",
    "Crossplane 1.16 发布：Kubernetes 风格的云资源管理",
    "Backstage 1.31 发布：开发者门户体验升级",
    "Flux 2.4 发布：GitOps 工具链更新",
    "Falco 0.40 发布：云原生运行时安全",
]
DEVOPS_DATA_CAST = [
    ("Kubernetes 在运维岗位中的出现率达 91%",
     "几乎所有中大型公司的运维岗都要求 K8s 经验，K8s 已成新一代运维的入场券。"),
    ("SRE 工程师平均薪资比传统运维高 42%",
     "平台数据显示 SRE 在稳定性保障、SLO 设计、自动化方面的能力溢价明显。"),
    ("云原生 DevOps 工程师缺口达 30 万",
     "招聘市场对熟悉 K8s、Helm、Argo CD、Prometheus 组合栈的工程师需求持续增长。"),
    ("运维自动化覆盖率超 70%：Shell/Python/Ansible 成为基本功",
     "能编写可复用自动化脚本、规范化日常运维操作，成为简历筛选硬指标。"),
    ("可观测性平台需求增长 53%：OTel 标准化是趋势",
     "OpenTelemetry 在企业中的采用率从 2023 年 35% 提升到 2026 年 78%。"),
]

# 大数据
BIGDATA_ARTICLES = [
    "Apache Flink 1.20 深度解析：流批一体与状态管理新特性",
    "Spark 3.5 性能调优：AQE、Spark UI 与执行计划分析",
    "数据湖架构演进：从 Delta Lake 到 Apache Iceberg 0.5",
    "ClickHouse 24.x 实战：亿级数据秒级聚合查询",
    "Kafka Streams 与 Flink 选型对比：流处理场景最佳实践",
    "Doris 2.1 实时数仓：MySQL 协议兼容与高并发查询",
    "湖仓一体（Lakehouse）：Iceberg + Trino 全链路实践",
    "StarRocks 3.4 性能优化：物化视图与 CBO 优化器",
    "数据治理：元数据管理与数据血缘的工程化落地",
    "Airflow 3 实战：DAG 编排、调度与监控一体化",
    "Hive 4 与 PrestoDB 协同：构建低成本离线数仓",
    "Flink CDC 3.0 实战：MySQL 实时入湖与全增量一体",
    "OLAP 选型对比：Doris、StarRocks、ClickHouse 与 Druid",
    "数据质量监控：Great Expectations 在生产环境的落地",
    "Hudi 0.15 vs Iceberg 0.5 vs Delta 3：数据湖格式选型",
    "实时数仓分层架构：ODS、DWD、DWS、ADS 设计与实践",
    "Elasticsearch + Beats + Logstash：日志分析全链路",
    "DataOps 实践：从需求到数据产品的端到端协作",
    "向量数据库：Milvus 2.4 实战与 RAG 应用集成",
    "数据可视化：Superset 4.0 与自定义 Dashboard 嵌入",
]
BIGDATA_EXTERNALS = [
    "Apache Spark 3.5.2 发布",
    "Apache Flink 1.20 正式发布",
    "ClickHouse 24.8 发布：性能与功能双重提升",
    "Apache Iceberg 1.6 GA：数据湖格式标准走向成熟",
    "StarRocks 3.4 全新发布：湖仓分析能力增强",
    "Apache Doris 2.1.6 发布：实时数仓能力升级",
    "Trino 449 发布：分布式 SQL 查询引擎持续迭代",
    "Apache Kafka 3.8 发布：KRaft 模式进一步成熟",
    "Apache Beam 2.59 发布：统一批流编程模型",
    "Apache Airflow 2.10 发布：DAG 编排平台更新",
    "Milvus 2.4 发布：向量数据库性能与功能升级",
    "Databricks 推出新引擎 Photon：性能对标 ClickHouse",
    "Snowflake 与 Databricks 财报亮眼：湖仓赛道持续火热",
    "OpenSearch 2.18 发布：Elasticsearch 兼容分支",
    "DuckDB 1.1 发布：嵌入式 OLAP 引擎性能突破",
]
BIGDATA_DATA_CAST = [
    ("大数据开发岗位需求集中在 Spark/Flink/Hive 三件套",
     "招聘市场对分布式计算（Spark/Flink）、存储（Hive/Iceberg）、查询（Trino/Presto）能力的需求稳定。"),
    ("实时数仓岗位同比上涨 41%",
     "企业对低延迟数据链路（Flink CDC、Kafka、Doris）需求旺盛，资深工程师尤其稀缺。"),
    ("湖仓一体（Lakehouse）方向工程师平均薪资 32K",
     "熟悉 Iceberg/Hudi + Flink/Spark + OLAP 引擎组合栈的候选人议价能力突出。"),
    ("数据治理方向岗位增长 27%",
     "数据资产化、合规与质量成为企业重点投入方向，元数据与血缘岗位需求增加。"),
    ("AI/大模型方向带动向量数据库岗位激增",
     "Milvus、Weaviate、Qdrant 等向量数据库的运维与应用工程师需求大幅上升。"),
]

# 类型配置：每桶多少条
BUCKET_PLAN = {
    'ARTICLE': 20,
    'EXTERNAL': 15,
    'DATA_CAST': 5,
}

# 来源映射
SOURCES = {
    'DATA_CAST': '平台数据播报',
    'ARTICLE': '就业指导中心',
    'EXTERNAL': '行业资讯站',
}

# 来源 URL 模板（EXTERNAL 才有 link）
EXTERNAL_URLS = [
    'https://www.oschina.net/news',
    'https://www.infoq.cn/',
    'https://news.cnblogs.com/',
    'https://www.ithome.com/',
    'https://www.csdn.net/',
    'https://www.51cto.com/',
    'https://segmentfault.com/',
    'https://juejin.cn/',
    'https://www.jiqizhixin.com/',
    'https://www.36kr.com/',
]

# 站内跳转（DATA_CAST 才用）
LINK_TARGETS = [
    '/admin/dashboard', '/admin/employment', '/student/recommend',
    '/admin/analysis', '/teacher/dashboard',
]


def build_sql():
    rows = []
    next_id = 9  # init.sql 已有 1-8
    base_time = datetime(2026, 7, 1, 8, 0, 0)

    for cat in CATS:
        for typ, count in BUCKET_PLAN.items():
            # 标题池
            if typ == 'ARTICLE':
                title_pool = {
                    'backend': BACKEND_ARTICLES,
                    'frontend': FRONTEND_ARTICLES,
                    'test': TEST_ARTICLES,
                    'devops': DEVOPS_ARTICLES,
                    'bigdata': BIGDATA_ARTICLES,
                }[cat]
            elif typ == 'EXTERNAL':
                title_pool = {
                    'backend': BACKEND_EXTERNALS,
                    'frontend': FRONTEND_EXTERNALS,
                    'test': TEST_EXTERNALS,
                    'devops': DEVOPS_EXTERNALS,
                    'bigdata': BIGDATA_EXTERNALS,
                }[cat]
            else:  # DATA_CAST
                title_pool = {
                    'backend': BACKEND_DATA_CAST,
                    'frontend': FRONTEND_DATA_CAST,
                    'test': TESTEND_DATA_CAST_FIX,
                    'devops': DEVOPS_DATA_CAST,
                    'bigdata': BIGDATA_DATA_CAST,
                }[cat]

            # DATA_CAST 池是 (title, summary) 元组；ARTICLE/EXTERNAL 是字符串
            is_cast = typ == 'DATA_CAST'

            for i in range(count):
                if is_cast:
                    title, summary = title_pool[i]
                else:
                    title = title_pool[i]
                    summary = generate_summary(cat, typ, i)

                # 封面图：picsum.photos 通过 seed 锁图
                # seed 格式：{prefix}-{cat}-{id}
                seed = f"{PICSUM_SEED_PREFIX}-{cat}-{next_id}"
                cover_image = f"https://picsum.photos/seed/{seed}/640/360"

                # 时间分散
                publish_time = base_time + timedelta(
                    days=random.randint(0, 12),
                    hours=random.randint(0, 23),
                    minutes=random.randint(0, 59),
                )
                publish_time_str = publish_time.strftime("%Y-%m-%d %H:%M:%S")

                # view_count / featured
                view_count = random.randint(20, 500)
                featured = 1 if (i == 0 and typ == 'ARTICLE') else 0  # 每分类头条文章设精选

                # source / source_url / link_target / content
                source = SOURCES[typ]
                if typ == 'EXTERNAL':
                    source_url = EXTERNAL_URLS[i % len(EXTERNAL_URLS)]
                    source = random.choice([
                        'OSCHINA', 'InfoQ', '博客园', 'IT之家',
                        'CSDN', '51CTO', 'SegmentFault', '掘金',
                        '机器之心', '36氪',
                    ])
                else:
                    source_url = 'NULL'

                if typ == 'ARTICLE':
                    content = f"<p>{summary}</p><p>本文由{SOURCES[typ]}原创，更多内容请关注后续更新。</p>"
                    content_escaped = content.replace("'", "\\'")
                    content_sql = "'" + content_escaped + "'"
                else:
                    content_sql = 'NULL'

                if typ == 'DATA_CAST':
                    link_target = random.choice(LINK_TARGETS)
                else:
                    link_target = 'NULL'

                # cover_style 选一个兜底
                cover_style = random.choice(['blue', 'green', 'purple', 'amber'])

                # 转义单引号
                title_esc = title.replace("'", "\\'")
                summary_esc = summary.replace("'", "\\'")
                cover_image_esc = cover_image.replace("'", "\\'")
                source_esc = source.replace("'", "\\'")

                # category: 通用资讯（NULL）大概 1/4，剩余按分类（SQL 字面量要加引号）
                if random.random() > 0.25:
                    cat_sql = "'" + cat + "'"
                else:
                    cat_sql = 'NULL'

                # NULL 走字面量；非 NULL 加单引号
                src_sql = 'NULL' if source_url == 'NULL' else "'" + source_url + "'"
                link_sql = 'NULL' if link_target == 'NULL' else "'" + link_target + "'"

                row = (
                    f"({next_id}, 1, {cat_sql}, '{typ}', "
                    f"'{title_esc}', '{summary_esc}', {content_sql}, "
                    f"'{cover_style}', '{cover_image_esc}', "
                    f"'{source_esc}', {src_sql}, "
                    f"{link_sql}, "
                    f"{view_count}, {featured}, 1, '{publish_time_str}')"
                )
                rows.append(row)
                next_id += 1

    return rows


# 给非 DATA_CAST 生成摘要
def generate_summary(cat, typ, idx):
    cat_name = CAT_LABEL[cat]
    if typ == 'ARTICLE':
        return f"本篇聚焦{cat_name}方向的工程实践与最新技术趋势，适合在校生与初中级工程师参考。"
    else:  # EXTERNAL
        return f"来自外部资讯站的{cat_name}领域要闻速览，点击阅读原文查看完整内容。"


# 修正：测试 DATA_CAST 名字
TESTEND_DATA_CAST_FIX = [
    ("平台自动化测试岗位占比 22%，需求增速领跑测试方向",
     "接口自动化、UI 自动化、性能测试岗位需求持续上升，平均薪资比功能测试高 35%。"),
    ("测试开发岗 P6+ 工程师稀缺：5 年以上经验仅占 18%",
     "能独立设计测试平台、主导自动化体系建设的中高级测试开发工程师长期供不应求。"),
    ("AI 辅助测试在大厂试点：缺陷预测准确率达 73%",
     "基于历史缺陷日志训练的模型可提前识别高风险模块，节省回归测试成本。"),
    ("性能测试岗位稀缺：P99 长尾指标关注度上升",
     "头部公司更关注 P99/P999 指标，对性能测试工程师的全链路定位能力要求更高。"),
    ("测试平台化趋势：自研 Mock 与自动化平台成简历加分项",
     "能搭建完整测试平台（用例管理、定时执行、报告聚合）的候选人在面试中表现突出。"),
]


def main():
    rows = build_sql()
    header = """-- ============================================================
-- 资讯数据扩充（200+ 条，覆盖 5 个分类 × 3 种类型，全部附带封面图）
-- 生成日期: 2026-07-13
-- 说明：图片源 picsum.photos（通过 seed 锁图，访问稳定）
-- ============================================================
SET NAMES utf8mb4;

-- 兜底：id 从 9 起
INSERT INTO news (id, tenant_id, category, type, title, summary, content, cover_style, cover_image, source, source_url, link_target, view_count, featured, status, publish_time) VALUES
"""
    body = ',\n'.join(rows) + ';'
    out = header + body + '\n'
    with open('e:/occupation/occupation-common/src/main/resources/sql/upgrade-2026-07-13-news-bulk.sql', 'w', encoding='utf-8') as f:
        f.write(out)
    print(f"Generated {len(rows)} rows.")
    print(f"File: upgrade-2026-07-13-news-bulk.sql")


if __name__ == '__main__':
    main()
