# AGENTS.md — 职业能力大数据服务平台 AI 开发规则

> 本文档为 AI 编码助手的核心规则集，**每次生成代码前必须遵守**。

---

## 🔴 Always 规则（始终生效，不可跳过）

```yaml
规则1:
  名称: "读取设计文档"
  触发: 始终
  内容: >
    在编写任何代码之前，必须先读取以下文件：
    - memory-bank/design-document.md（系统架构、功能模块、角色设计）
    - memory-bank/tech-stack.md（技术选型、版本、架构决策记录）
    这确保每次生成的代码与项目的整体架构保持一致。

规则2:
  名称: "读取架构文件"
  触发: 始终
  内容: >
    如果 memory-bank/architecture.md 已存在，必须在写代码前读取它。
    该文件包含最新的数据库 Schema、模块划分和接口定义。
    代码必须和 memory-bank/architecture.md 中的 Schema 保持100%一致。

规则3:
  名称: "模块化优先"
  触发: 始终
  内容: >
    绝对禁止创建单个巨型文件（超过 500 行）。
    每个 Java 类/前端组件应职责单一。
    Maven 模块按功能拆分：common、auth、crawler、analysis、report、recommend、api。
    前端按页面/组件拆分：views/、components/、api/、store/。

规则4:
  名称: "更新架构文档"
  触发: 重要里程碑完成后
  内容: >
    以下场景必须在任务完成后更新 memory-bank/architecture.md：
    - 新增或修改了数据库表结构
    - 新增了模块或服务
    - 新增了对外 API 接口
    - 变更了模块间的调用关系
```

---

## 🟡 代码规范规则

### 后端（Java / SpringBoot）

```
规则5:
  名称: "分层架构"
  触发: 编写后端代码时
  内容: >
    Controller → Service → Mapper 严格分层。
    Controller 只做参数校验和响应封装，不写业务逻辑。
    Service 层处理业务逻辑，通过接口定义（如 JobService），实现类放在 impl 包下。
    使用 MyBatis-Plus 的 BaseMapper，减少手写 SQL。

规则6:
  名称: "多租户数据隔离"
  触发: 涉及数据库操作时
  内容: >
    所有业务 SQL 必须包含 tenant_id 过滤条件。
    优先使用 MyBatis-Plus 的多租户插件自动注入，而非手动拼接。
    分析类查询（跨租户汇总）需显式注释说明为何不隔离。

规则7:
  名称: "统一响应格式"
  触发: 编写 Controller 时
  内容: >
    所有 API 返回统一格式：
    { "code": 200, "message": "success", "data": {...} }
    使用全局异常处理器（@RestControllerAdvice）统一捕获异常并转换。
```

### 前端（Vue 3）

```
规则8:
  名称: "组件拆分粒度"
  触发: 编写 Vue 组件时
  内容: >
    每个 .vue 文件不超过 300 行。
    复用 UI 片段抽为独立组件放在 components/ 目录。
    页面级组件放在 views/ 目录。
    使用 <script setup> 语法糖（组合式 API）。

规则9:
  名称: "API 调用封装"
  触发: 前端调用后端接口时
  内容: >
    所有 HTTP 请求统一通过 api/ 目录下的模块封装。
    使用 Axios 拦截器统一处理 Token 注入和错误提示。
    不要在 .vue 组件中直接写 axios.get/post。
```

### 大数据 / Python

```
规则10:
  名称: "Spark 作业规范"
  触发: 编写 Spark 任务时
  内容: >
    Spark 作业必须可配置输入/输出路径，不硬编码。
    每个分析维度拆为独立的 Job 类，不混在一个 main 方法中。
    结果写入 MySQL 分析结果表，而非直接生成报告文件。

规则11:
  名称: "Flask 分析服务"
  触发: 编写 Python 分析代码时
  内容: >
    Flask 仅用于轻量分析 API（词云、技能关联等）。
    不要将 Flask 作为主要业务后端。
    与 Java 后端通过 HTTP API 通信，JWT Token 校验调用方身份。
```

---

## 🟢 质量规则

```
规则12:
  名称: "配置外置"
  触发: 涉及配置参数时
  内容: >
    所有环境相关配置（数据库连接、Kafka 地址、爬虫频率等）
    必须写在 application.yml 或 .env 中，不得硬编码。
    敏感信息（密码、Token）使用环境变量或配置中心。

规则13:
  名称: "日志规范"
  触发: 编写核心逻辑时
  内容: >
    关键节点（采集开始/结束、分析任务启停、API 异常）必须记录日志。
    使用 SLF4J（Java）和 logging（Python），不直接 print。
    日志级别：INFO（关键流程）、WARN（异常但可恢复）、ERROR（需要人工介入）。

规则14:
  名称: "禁止超大文件"
  触发: 始终
  内容: >
    任何单个文件不得超过 500 行。
    超过即视为需要拆分的信号。
    前端 .vue 文件不超过 300 行。
```

---

## 📋 项目文件索引（AI 必须知道的关键文件）

| 文件                      | 用途                     | 读取时机     |
| ------------------------- | ------------------------ | ------------ |
| `memory-bank/design-document.md`      | 产品设计文档              | Always       |
| `memory-bank/tech-stack.md`           | 技术选型与架构决策        | Always       |
| `memory-bank/architecture.md`         | 当前架构状态（Schema等）  | Always       |
| `memory-bank/implementation-plan.md`  | 实施计划                  | 按阶段读取   |

---

## ⚠️ 禁止事项

- ❌ 不要创建超过 500 行的单个文件。
- ❌ 不要在 Controller 中写业务逻辑。
- ❌ 不要硬编码数据库连接、API Key、密码。
- ❌ 不要跳过 `tenant_id` 隔离（除非是跨租户分析查询）。
- ❌ 不要在前端组件中直接操作数据库（必须通过后端 API）。
- ❌ 不要重复造轮子（优先使用 Hutool、MyBatis-Plus 已有能力）。
