# 职业能力大数据服务平台 — Vibe Coding 工作流指南

> **适配工具**: CodeBuddy  
> **项目类型**: Java + Vue 3 Web 应用（非游戏）  
> **基于**: memory-bank 文档驱动开发

---

## 准备工作：确保 AI 理解一切

- 打开 CodeBuddy，确认工作区为当前项目根目录。
- **提示词**: 读取 `memory-bank/` 下的所有文档（design-document.md、tech-stack.md、implementation-plan.md、architecture.md、progress.md）以及 `AGENTS.md`。`implementation-plan.md` 是否足够清晰？你有哪些问题需要我澄清以保证理解 100% 无歧义？
- AI 通常会提出 9-10 个问题。逐一回答后，让 AI 据此更新 `implementation-plan.md`。

---

## 第一个实施 Prompt

- 在 CodeBuddy 中发送以下提示词：

```
读取 memory-bank/ 下的所有文档（design-document.md、tech-stack.md、implementation-plan.md、
architecture.md、progress.md）以及 AGENTS.md，
然后按照 memory-bank/implementation-plan.md 执行 Step 1。
我会自己运行验证测试。在我验证通过之前，不要开始 Step 2。
每完成一个 Step 后：
  1. 打开 memory-bank/progress.md，记录你完成了什么（方便后续开发者理解进展）。
  2. 打开 memory-bank/architecture.md，补充架构洞察（每个文件的作用、模块关系、新增的 API/Schema 等，按 AGENTS.md 规则4 执行）。
```

---

## 迭代工作流（核心循环）

### 完成一个 Step 后

```
1. 验证当前 Step 的测试是否通过。
2. 如果通过：让 AI 更新 memory-bank/progress.md 和 memory-bank/architecture.md。
3. Git commit（不熟悉 Git？问 AI）。
4. 开启一个新会话（CodeBuddy 点击"新建对话"或 `/new`）。
   ⚠️ 为什么要新会话？LLM 在上下文窗口宽敞时产出质量最高。
5. 新会话中发送：
   "读取 memory-bank/ 下的所有文档以及 AGENTS.md，
    了解已完成的工作，然后执行 memory-bank/implementation-plan.md 的 Step N。
    不要开始 Step N+1 直到我验证通过。"
6. 重复上述过程，直到整个 implementation-plan.md 完成。
```

### 各阶段预计会话数

| 阶段 | 步骤数 | 预计会话数 | 关键产出 |
|------|--------|-----------|----------|
| P1 | 9 步 | 9-12 会话 | Maven 多模块 + 爬虫 + Kafka |
| P2 | 8 步 | 8-10 会话 | Spark 分析 + Hive/HBase |
| P3 | 7 步 | 7-9 会话 | 管理后台 + 报告生成 |
| P4 | 6 步 | 6-8 会话 | 推荐引擎 + 学生端 |
| P5 | 6 步 | 6-8 会话 | API + 教师端 + HR端 + 监控 |
| ⭐P6 | 6 模块 | 12-18 会话 | AI 差异化亮点 |

---

## 添加功能细节

P1-P5 完成后，你有了一个可运行的平台骨架。接下来做差异化：

- 技能缺口诊断、知识图谱、LLM 报告、实时大屏、NLP 匹配、趋势预测...
- 每做一个 P6 模块，先在 `memory-bank/` 下创建 `feature-{模块名}.md`，写上短步骤 + 验证方法。
- 然后按相同工作流逐步实现。

---

## 问题排查

### 代码编译/运行报错

```
将完整错误日志复制粘贴给 CodeBuddy，让它分析并修复。
```

### 前端页面渲染异常

```
描述你在浏览器中看到的现象（截图更好），
CodeBuddy 可以读取 .vue 文件帮你定位问题。
```

### 卡住了怎么办

```
1. Git revert 到上一个正常 commit，换个思路重新来。
2. 把相关文件路径 + 错误信息一起发给 CodeBuddy。
3. 如果某个 Step 实在太复杂，让 AI 把它拆成更小的子步骤。
```

---

## CodeBuddy 使用技巧

| 场景 | 操作 |
|------|------|
| **开始新步骤** | 新建对话，prompt 中让 AI 先读 memory-bank |
| **代码审查** | 让 AI 检查当前 Step 的代码是否符合 AGENTS.md 规范 |
| **Schema 变更** | 完成后让 AI 更新 `memory-bank/architecture.md` |
| **上下文过长** | 新建对话，只保留 memory-bank 文档作为上下文 |
| **复杂调试** | 使用 CodeBuddy 的 `Task` 工具让子代理探索代码库 |
| **多文件重构** | 一次性告诉 AI 所有要改的文件，批量并行处理 |

### 让 AI 思考更深的提示词技巧

```
# 普通 → 较好 → 最好

"实现登录接口"
↓
"按 AGENTS.md 规则实现登录接口，包含 JWT + BCrypt + 多租户"
↓
"仔细阅读 memory-bank/ 全部文档和 AGENTS.md，理解项目架构后，
 实现 memory-bank/implementation-plan.md Step 1.5 的 JWT 登录，
 确保：多租户隔离、统一响应格式、异常处理、日志记录，
 并同步更新 memory-bank/architecture.md"
```

---

## 文件职责速查

### 根目录（root）— 元规则文件

| 文件 | 谁维护 | 何时更新 |
|------|--------|----------|
| `AGENTS.md` | 人工 + AI | 编码规范变更时 |
| `vibe-coding-guide.md` | 人工 + AI | 工作流优化时 |

### Memory Bank（`memory-bank/`）— 项目文档 + AI 运行时状态

| 文件 | 谁维护 | 何时更新 |
|------|--------|----------|
| `design-document.md` | 人工 + AI | 需求变更时 |
| `tech-stack.md` | 人工 + AI | 技术选型变更时 |
| `implementation-plan.md` | 人工 + AI | 步骤细化/调整时 |
| `architecture.md` | 人工 + AI（每 Step 完成后） | Schema/API/模块变更时（AGENTS.md 规则4） |
| `progress.md` | AI（每 Step 完成后） | 记录已完成内容，便于新会话恢复上下文 |

> 📁 **memory-bank/ 是所有项目文档的单一来源**。根目录仅保留 `AGENTS.md` 和 `vibe-coding-guide.md` 两个元规则文件。

---

## 常见问题

**Q: 这是 Web 应用不是游戏，工作流一样吗？**  
**A:** 完全一样。Memory Bank 文档驱动 + 逐步验证 + 新会话迭代，这套方法论适用于任何软件开发。

**Q: 为什么每次要新建会话？**  
**A:** LLM 在上下文窗口使用率超过 50-60% 后，输出质量会下降。新建会话让 AI 只加载必要的 memory-bank 文档，决策更精准。CodeBuddy 会保留 `progress.md` 和 `architecture.md` 作为"记忆"。

**Q: 我可以跳过某些 Step 吗？**  
**A:** P1-P5 的 Step 之间有依赖关系，建议按顺序执行。如果某个模块你不需要（比如不需要企业 HR 端），可以在 `implementation-plan.md` 中标记为 SKIP，并在 `progress.md` 中记录。

**Q: 我需要自己写测试吗？**  
**A:** `implementation-plan.md` 中每个 Step 的"验证"部分就是测试清单。你可以直接按照验证步骤手动测试，也可以让 AI 帮你生成 JUnit 单元测试 / Postman 测试集。

**Q: P6 AI 模块需要 GPU 吗？**  
**A:** 不需要。LLM 用 DeepSeek API（云端），NLP 用轻量模型（CPU 可跑），Neo4j 和 Elasticsearch 用 Docker 部署，Prophet 是纯 CPU 计算。

**Q: 大数据组件（Hadoop/Spark）本地跑不动怎么办？**  
**A:** P1 只需要 MySQL + Redis + Kafka + Nginx 就能跑通。大数据组件到 P2 才需要，可以用 Docker 单节点部署，资源不够也可以先跳过 HDFS/Hive，直接写 MySQL 做分析。

---

> **准备好了？** 打开 CodeBuddy，发送第一句提示词：  
> "读取 memory-bank/ 下的所有文档和 AGENTS.md，implementation-plan.md 是否清晰？有什么需要我澄清的？"
