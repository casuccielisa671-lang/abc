# Progress — 开发进度

> 每完成一个 Step 后由 AI 更新，便于新会话恢复上下文。

---

## P1：项目骨架 + 数据采集模块

| Step | 内容 | 状态 | 完成日期 |
|---|---|---|---|
| 1.1 | 初始化 Maven 多模块项目结构 + docker-compose.yml | ✅ 已完成 | 2026-07-06 |
| 1.2 | 搭建 SpringBoot 基础骨架与统一响应 | ✅ 已完成 | 2026-07-06 |
| 1.3 | 数据库表设计（init.sql） | ✅ 已完成 | 2026-07-07 |
| 1.4 | MyBatis-Plus 基础配置 + 多租户插件 | ✅ 已完成 | 2026-07-07 |
| 1.5 | JWT 认证 + 登录接口 | ✅ 已完成 | 2026-07-07 |
| 1.6 | Kafka 生产者/消费者基础配置 | ✅ 已完成 | 2026-07-07 |
| 1.7 | 爬虫基础框架（WebMagic 集成） | ✅ 已完成 | 2026-07-07 |
| 1.8 | 实现一个真实采集源（BOSS直聘） | ✅ 已完成 | 2026-07-07 |
| 1.9 | 采集任务管理接口 + 调度集成 | ✅ 已完成 | 2026-07-07 |

> **P1 已 100% 完成**：后端骨架 + JWT 认证 + 多租户 + Kafka 链路 + 爬虫采集 🎉

---

## 骨架代码（跨模块基础设施）— 2026-07-07

| 类别 | 文件数 | 说明 |
|---|---|---|
| Entity | 8 | AnalysisResult、JobDetail、ReportTemplate、ReportRecord、SysStudentProfile、PushRecord、StudentBehavior、ApiClient |
| Mapper | 8 | 每个 Entity 对应一个 Mapper |
| Service 接口 | 2 | AnalysisService、JobDetailService（跨模块契约） |
| DTO/VO | 4 | DashboardQueryDTO、DashboardVO、JobQueryDTO、JobDetailVO |
| POM 更新 | 3 模块 | report（+freemarker/poi/flying-saucer/analysis）、recommend（+analysis）、api（+spring-boot-starter-oauth2-resource-server） |
| SQL | +1 表 | api_client 表 + 种子数据 |
| Vue 3 脚手架 | ~27 文件 | Vite + Vue 3 + Element Plus + ECharts + Axios + Pinia + Vue Router，16 个占位页面 + 4 角色路由骨架 |

> 骨架代码已全部通过 `mvn compile` 编译验证 ✅
