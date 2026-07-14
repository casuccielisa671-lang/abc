-- ============================================================
-- 增量升级脚本：资讯表 news（首页资讯板块）
-- 生成日期: 2026-07-12
-- ============================================================
--
-- 用途：给「已有数据、不想 down -v 重建」的库补上 news 表与种子。
-- 全新的库不需要执行 —— init.sql 已含同样 DDL 与种子。
-- 可重复执行：CREATE TABLE IF NOT EXISTS + INSERT IGNORE。
--
-- ⚠️ 不写 `USE occupation;`。命令行显式指定库：
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < upgrade-2026-07-12-news.sql
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS news (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
    tenant_id    BIGINT       NOT NULL COMMENT '所属租户ID',
    category     VARCHAR(20)  DEFAULT NULL COMMENT '技术方向：backend/frontend/test/devops/bigdata；NULL=通用',
    type         VARCHAR(20)  NOT NULL COMMENT '类型：DATA_CAST=数据播报 ARTICLE=精选文章 EXTERNAL=外部资讯',
    title        VARCHAR(300) NOT NULL COMMENT '标题',
    summary      VARCHAR(600) DEFAULT NULL COMMENT '摘要',
    content      LONGTEXT     DEFAULT NULL COMMENT '正文（仅精选文章）',
    cover_style  VARCHAR(20)  DEFAULT 'blue' COMMENT '封面色块样式：blue/green/purple/amber',
    source       VARCHAR(100) DEFAULT NULL COMMENT '来源',
    source_url   VARCHAR(600) DEFAULT NULL COMMENT '外部原文链接',
    link_target  VARCHAR(200) DEFAULT NULL COMMENT '站内跳转（DATA_CAST 点击去对应图表）',
    view_count   INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
    featured     TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶/精选',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=上架 0=下架',
    publish_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_type (tenant_id, type),
    KEY idx_tenant_cat (tenant_id, category),
    KEY idx_publish (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯表';

INSERT IGNORE INTO news (id, tenant_id, category, type, title, summary, content, cover_style, source, source_url, link_target, view_count, featured, status, publish_time) VALUES
(1, 1, NULL, 'DATA_CAST', '本平台在库岗位达 114 个，数据分析已就绪', '涵盖 90 个采集岗位与 24 个站内职位，覆盖 10 座主要城市、8 大技术方向，看板与推荐均基于此。', NULL, 'blue', '平台数据播报', NULL, '/admin/dashboard', 128, 1, 1, '2026-07-09 08:10:00'),
(2, 1, 'backend', 'DATA_CAST', 'Java 稳居技能热度榜首', '在全部岗位中，要求 Java 的职位数量最多，其后为 Python、MySQL。后端方向需求持续旺盛。', NULL, 'blue', '平台数据播报', NULL, '/admin/dashboard', 96, 0, 1, '2026-07-09 08:12:00'),
(3, 1, NULL, 'DATA_CAST', '上海、北京、深圳岗位最集中', '按城市分布，上海岗位数领先，北京、深圳紧随其后；杭州、成都为新一线热门去向。', NULL, 'green', '平台数据播报', NULL, '/admin/dashboard', 74, 0, 1, '2026-07-09 08:14:00'),
(4, 1, 'bigdata', 'DATA_CAST', '大数据方向平均薪资领先', '按行业统计，大数据 / 人工智能方向平均薪资高于全站均值，Spark、Flink 等技能溢价明显。', NULL, 'purple', '平台数据播报', NULL, '/admin/employment', 63, 0, 1, '2026-07-09 08:16:00'),
(5, 1, NULL, 'DATA_CAST', '投递转化：73 份投递中已产生 3 个 OFFER', '站内投递共 73 份，其中面试阶段 19 份、录用 3 份；及时完善画像与简历有助于提升转化。', NULL, 'amber', '平台数据播报', NULL, '/admin/employment', 51, 0, 1, '2026-07-09 08:18:00'),
(6, 1, 'backend', 'ARTICLE', '2026 后端开发就业观察：微服务与云原生成标配', '从平台岗位要求看，Spring Boot、微服务、容器化几乎成为后端岗位的默认门槛。', '<p>综合本平台采集到的后端岗位数据，Spring Boot、Spring Cloud、Redis、消息队列（Kafka/RocketMQ）出现频率显著上升。</p><p>建议在校生优先夯实 Java 基础与数据库，再向微服务、容器化（Docker/K8s）延伸，配合一到两个完整项目经历，将明显提升竞争力。</p>', 'blue', '就业指导中心', NULL, NULL, 210, 1, 1, '2026-07-06 10:00:00'),
(7, 1, 'frontend', 'ARTICLE', '前端招聘趋势：工程化与框架深度并重', '企业更看重 Vue/React 的工程化实践与组件设计能力，而非仅会写页面。', '<p>平台前端岗位中，Vue、TypeScript、构建工具（Vite/Webpack）、组件化设计是高频关键词。</p><p>建议同学在掌握一个主流框架的基础上，理解其响应式原理与工程化配置，并积累可展示的项目。</p>', 'green', '就业指导中心', NULL, NULL, 143, 0, 1, '2026-07-05 14:30:00'),
(8, 1, NULL, 'EXTERNAL', '示例：外部就业资讯（接入开源中国 RSS 后在此展示）', '外部资讯仅展示标题与摘要，点击「阅读原文」跳转来源站点；封面用色块占位。', NULL, 'purple', '开源中国', 'https://www.oschina.net/news', NULL, 88, 0, 1, '2026-07-08 09:00:00');
