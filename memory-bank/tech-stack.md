# 职业能力大数据服务平台 — 技术选型文档

> **版本**: v2.0（差异化升级，新增 AI 智能引擎层）  
> **更新日期**: 2026-07-06  
> **原则**: 最简可行、稳健可扩展（Simplest yet most robust）

---

## 1. 选型总览

```
┌──────────────────────────────────────────────────────┐
│ 前端层           Vue 3 + Element Plus + ECharts       │
│                  + G6 图可视化 + Three.js(3D大屏)     │
├──────────────────────────────────────────────────────┤
│ 网关层           Nginx（反向代理 + 静态资源）          │
├──────────────────────────────────────────────────────┤
│ 业务服务层       SpringBoot 2.7.x 模块化单体           │
│                  ├─ common（公共模块）                 │
│                  ├─ auth（认证授权）                   │
│                  ├─ crawler（采集管理）                │
│                  ├─ analysis（报告分析）               │
│                  ├─ report（报告生成）                 │
│                  ├─ recommend（岗位推送）              │
│                  ├─ skillgap（⭐技能缺口诊断）          │
│                  └─ api（对外开放接口）                │
├──────────────────────────────────────────────────────┤
│ AI 智能引擎层 ⭐                                       │
│  LLM 服务(DeepSeek/通义千问) │ NLP 语义匹配(Sentence-  │
│  Transformers) │ 知识图谱(Neo4j) │ 时序预测(Prophet)    │
├──────────────────────────────────────────────────────┤
│ 分析服务层       Flask + Pandas + scikit-learn         │
├──────────────────────────────────────────────────────┤
│ 消息中间件       Kafka（采集↔清洗↔分析 解耦）          │
├──────────────────────────────────────────────────────┤
│ 缓存层           Redis 6.x                             │
├──────────────────────────────────────────────────────┤
│ 存储层           MySQL 8.0 │ Neo4j │ Elasticsearch      │
│ 大数据存储       HDFS 3.x + Hive 3.x + HBase 2.x       │
├──────────────────────────────────────────────────────┤
│ 大数据计算       Spark 3.x（批处理 + 流处理）           │
├──────────────────────────────────────────────────────┤
│ 协调与调度       Zookeeper 3.x + Yarn                   │
├──────────────────────────────────────────────────────┤
│ 容器化           Docker + Docker Compose（开发/测试）   │
│                  K8s（生产环境，后续引入）              │
└──────────────────────────────────────────────────────┘
```

---

## 2. 各层选型详解

### 2.1 前端

| 选择               | 版本    | 理由                                                         |
| ------------------ | ------- | ------------------------------------------------------------ |
| **Vue 3**          | 3.3+    | 组合式 API，TypeScript 原生支持，生态成熟                    |
| **Element Plus**   | 2.x     | 国内最成熟的 Vue 3 组件库，管理后台开箱即用                   |
| **ECharts 5**      | 5.x     | 数据可视化首选，支持地图/热力图/词云等就业分析所需图表        |
| **G6** ⭐           | 5.x     | 蚂蚁金服图可视化引擎，渲染知识图谱（技能-职位-行业关系网）    |
| **Three.js** ⭐     | r160+   | 3D 可视化大屏：地球光柱热力图、城市粒子特效                  |
| **Vite**           | 5.x     | 开发构建速度极快，替代 Webpack                                |
| **Pinia**          | 2.x     | Vue 3 官方状态管理，比 Vuex 更简洁                            |
| **Axios**          | 1.x     | HTTP 客户端，拦截器统一处理 JWT Token                         |

> **App 扩展（P5 后考虑）**: uni-app（一套代码多端编译）或 Flutter（高性能）。

### 2.2 业务后端（Java）

| 选择               | 版本      | 理由                                                         |
| ------------------ | --------- | ------------------------------------------------------------ |
| **SpringBoot**     | 2.7.x     | 稳定 LTS 线，生态最完善                                      |
| **Spring Security** | 5.x      | 认证授权，配合 JWT 实现无状态鉴权                            |
| **MyBatis-Plus**   | 3.5.x     | 简化 CRUD，多租户插件原生支持 `tenant_id` 行级隔离           |
| **Swagger/OpenAPI** | 3.x     | 自动生成 API 文档，Knife4j 增强 UI                           |
| **XXL-Job**        | 2.x       | 分布式定时任务调度（替代 Quartz），管理采集/分析定时任务      |
| **Hutool**         | 5.x       | Java 工具集，减少重复造轮子                                  |

**模块化单体架构说明**：

- 初期不拆微服务，避免分布式事务、服务发现等复杂度。
- 按功能拆分为 7 个 Maven Module，同进程内调用。
- 模块间通过接口隔离（API 模块定义接口，Impl 模块实现）。
- 当某个模块需要独立扩缩容时，再拆分为独立微服务（Spring Cloud 或 K8s Native）。

**⭐ MVC 分层架构 — 所有模块统一遵循**：

每个业务模块内部采用标准 MVC（Model-View-Controller）分层：

```
com.occupation.<模块名>
├── controller/          ← Controller 层：接收请求、参数校验、调用 Service、封装 Result<T>
├── service/             ← Service 接口：定义业务逻辑契约
├── service/impl/        ← Service 实现：业务逻辑实现 + 事务管理
├── mapper/              ← Mapper 接口：继承 MyBatis-Plus BaseMapper，数据访问
├── entity/              ← 数据库实体：@TableName 映射，纯数据对象
├── dto/                 ← 请求体：前端入参，含 JSR-303 校验注解
├── vo/                  ← 响应体：出参组装，不含业务逻辑
└── config/              ← 模块配置：如多租户插件、拦截器等
```

**调用链路（不可越级）：**
`Controller → Service(接口) → ServiceImpl → Mapper → DB`

> 详细规范见 `AGENTS.md` 规则5。

### 2.3 分析辅助服务（Python）

| 选择       | 版本  | 理由                                         |
| ---------- | ----- | -------------------------------------------- |
| **Flask**  | 2.3.x | 轻量，适合小规模分析 API 服务                |
| **Pandas** | 2.x   | 数据清洗与预处理                              |
| **NumPy**  | 1.x   | 数值计算                                     |

> 使用场景：生成词云数据、技能关联分析、简单的 NLP 处理（JD 文本分词）。  
> 与 Java 后端通过 HTTP API 或 Kafka 通信。

### 2.4 ⭐ AI 智能引擎层（差异化核心）

| 选择                       | 版本            | 用途                           | 必要性 |
| -------------------------- | --------------- | ------------------------------ | ------ |
| **DeepSeek / 通义千问 API** | —               | LLM 智能报告生成、教学建议文本 | 亮点   |
| **Sentence-Transformers**  | 2.x（Python）   | JD 与简历/技能的语义向量化     | 亮点   |
| **Milvus / FAISS**         | 2.x / 1.7       | 向量检索，NLP 语义匹配加速     | 亮点   |
| **Neo4j** ⭐                | 5.x             | 职业能力知识图谱存储与查询     | 亮点   |
| **Prophet**                | 1.x（Python/R） | 就业趋势时序预测               | 亮点   |
| **scikit-learn**           | 1.x             | 传统 ML（聚类、分类辅助分析）  | 辅助   |

> **分层部署策略**：
> - LLM API 调用：通过 Java `occupation-report` 模块调用，Prompt 模板化，支持多模型切换。
> - NLP 语义服务：独立 Python Flask 微服务，加载 Sentence-Transformers 模型（本地或 GPU）。
> - 知识图谱：Neo4j 独立部署，Java 通过 Spring Data Neo4j 或 Cypher HTTP API 访问。
> - 时序预测：Python 脚本 + Spark 批处理输入数据，定时输出预测结果到 MySQL。

### 2.5 大数据生态

| 组件           | 版本  | 用途                   | 必要性 |
| -------------- | ----- | ---------------------- | ------ |
| **HDFS**       | 3.3.x | 原始数据持久化层        | 核心   |
| **Hive**       | 3.1.x | 结构化数据仓库，SQL 查询 | 核心   |
| **HBase**      | 2.4.x | 职位明细实时查询         | 重要   |
| **Spark**      | 3.4.x | 离线批处理 + 流处理     | 核心   |
| **Kafka**      | 3.4.x | 采集↔清洗↔分析消息解耦  | 核心   |
| **Zookeeper**  | 3.8.x | Kafka/HBase 协调        | 依赖   |
| **Yarn**       | 3.3.x | 资源调度                | 依赖   |
| **MapReduce**  | —     | 批处理兜底，非首选      | 备选   |

### 2.6 基础设施

| 组件               | 版本  | 用途                               |
| ------------------ | ----- | ---------------------------------- |
| **MySQL**          | 8.0   | 业务主库                           |
| **Neo4j** ⭐        | 5.x   | 知识图谱（技能-职位-行业关系网）    |
| **Elasticsearch** ⭐ | 8.x   | 全文搜索 + 职位检索加速            |
| **Redis**          | 6.2+  | 缓存、Session、实时计数器、排行榜  |
| **Nginx**          | 1.24+ | 反向代理、负载均衡、静态资源       |
| **Docker**         | 24+   | 容器化部署                         |

---

## 3. 为什么选择这些技术？

### 3.1 简化原则

| 可能会过度设计的点         | 我们的简化方案                               |
| -------------------------- | -------------------------------------------- |
| 微服务架构（Spring Cloud） | **模块化单体**，7 个 Maven Module，按需拆分  |
| K8s 编排                   | **Docker Compose** 本地开发，K8s 生产后再引入 |
| 多个数据库                 | **仅 MySQL 8.0** 作为业务主库                |
| 复杂 NLP/ML 引擎           | **Flask + Pandas** 轻量分析，后续按需增强    |
| 自研爬虫框架               | **WebMagic / Crawler4j** 成熟开源方案         |

### 3.2 稳健原则

- **SpringBoot 2.7.x**：LTS 版本，社区活跃，踩坑资料丰富。
- **MyBatis-Plus 多租户插件**：原生支持 `tenant_id` 拦截，业务代码零侵入。
- **Kafka 消息队列**：采集数据天然异步，削峰填谷，采集端宕机不丢数据。
- **Spark 批处理**：业界标准，分析任务可线性扩展。
- **Docker Compose 一键启动**：Hadoop 全家桶 + MySQL + Redis + Nginx，一条命令启动开发环境。

---

## 4. 开发环境搭建（Docker Compose 方案）

```yaml
# 核心服务 docker-compose.yml（概念示意，非最终配置）
services:
  mysql:         # MySQL 8.0 业务库
  redis:         # Redis 6.2 缓存
  nginx:         # Nginx 反向代理
  kafka:         # Kafka + Zookeeper
  namenode:      # HDFS NameNode
  datanode:      # HDFS DataNode（可多实例）
  hive:          # Hive Metastore + Server
  hbase:         # HBase Master + RegionServer
  spark:         # Spark Master + Worker
  neo4j:         # ⭐ Neo4j 知识图谱
  elasticsearch: # ⭐ Elasticsearch 全文检索
```

> 本地开发最少只需要 `mysql + redis + kafka + nginx`。  
> P6 差异化阶段再接入 `neo4j + elasticsearch + LLM API`。

---

## 5. 技术风险与应对

| 风险                              | 应对措施                                                |
| --------------------------------- | ------------------------------------------------------- |
| 爬虫被招聘平台封禁                | IP 代理池 + 请求频率控制 + 验证码识别服务                |
| 大数据组件学习成本高              | Docker Compose 一键部署，降低环境搭建门槛                |
| 单体应用后期膨胀                  | Maven Module 边界清晰，拆微服务成本可控                  |
| SaaS 多租户数据量增长             | MySQL 分库分表（ShardingSphere），按 tenant 拆分          |
| ⭐ LLM API 调用成本过高            | 缓存常见查询结果，Prompt 精简，仅报告生成时调用           |
| ⭐ 知识图谱数据量膨胀               | Neo4j 分区 + 定期归档历史数据，仅保留近 2 年活跃关系      |
| ⭐ NLP 模型推理延迟高              | 离线批量计算向量存入 FAISS，在线仅做检索，延迟 < 50ms     |

---

## 6. 选型决策记录

| 决策点             | 选择         | 备选                  | 决策理由                                       |
| ------------------ | ------------ | --------------------- | ---------------------------------------------- |
| 前端框架           | Vue 3        | React                 | Vue 3 学习成本低，Element Plus 成熟            |
| 图可视化 ⭐         | G6           | D3.js/Cytoscape       | 国产，中文文档，与 AntV 生态一致               |
| 3D 大屏 ⭐          | Three.js     | ECharts GL/Deck.gl    | 最灵活的 WebGL 方案，特效上限高                |
| 知识图谱 ⭐         | Neo4j        | JanusGraph/ArangoDB   | 生态最成熟，Cypher 查询语法直观                |
| LLM ⭐              | DeepSeek API | 本地部署/OpenAI       | 国内访问稳定，性价比高，中文能力强             |
| NLP 语义 ⭐         | BGE + FAISS  | text2vec/bert-base    | BGE 中文 Embedding SOTA，FAISS 轻量高性能      |
| 搜索引擎 ⭐         | ES 8.x       | Solr                  | 与 Kibana/Grafana 集成好，全文检索性能优       |
| 时序预测 ⭐         | Prophet      | ARIMA/LSTM            | Facebook 开源，自动处理节假日/趋势，调参成本低 |
| 后端架构           | 模块化单体   | 微服务                | 初期简单，按需拆分                             |
| 数据库             | MySQL 8.0    | Oracle/PostgreSQL     | 团队熟悉，社区资源丰富                         |
| 定时任务           | XXL-Job      | Quartz/Elastic-Job    | 可视化管理界面，分片支持                       |
| ORM                | MyBatis-Plus | JPA/Hibernate         | 国内主流，多租户插件成熟                       |
| 爬虫框架           | WebMagic     | 自研                  | 开源稳定，省去大量基础开发                     |
| 报告图表           | ECharts 5    | D3.js/G2              | 文档中文，就业类图表模板丰富                   |

---

> **下一步**: 基于本文档和 `design-document.md`，生成 `implementation-plan.md`。
