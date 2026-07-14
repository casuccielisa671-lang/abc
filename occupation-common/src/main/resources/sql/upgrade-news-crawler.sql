-- ============================================================
-- 升级脚本：资讯爬虫种子数据
-- 用途：在 crawler_task 表中插入 InfoQ / OSCHINA 资讯采集任务
-- 执行时机：资讯爬虫功能上线前执行
-- 依赖：crawler_task 表已存在（init.sql 中创建）
-- ============================================================

-- InfoQ 中文站资讯采集任务
INSERT INTO crawler_task (source_type, source_name, url_pattern, cron_expr, status, tenant_id, create_time, update_time, deleted)
VALUES ('NEWS_INFOQ', 'InfoQ 中文站资讯采集', 'https://www.infoq.cn/feed', '0 0 8,12,18 * * ?', 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE source_name = VALUES(source_name), url_pattern = VALUES(url_pattern);

-- OSCHINA 开源中国资讯采集任务
INSERT INTO crawler_task (source_type, source_name, url_pattern, cron_expr, status, tenant_id, create_time, update_time, deleted)
VALUES ('NEWS_OSCHINA', '开源中国资讯采集', 'https://www.oschina.net/news/rss', '0 0 8,12,18 * * ?', 1, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE source_name = VALUES(source_name), url_pattern = VALUES(url_pattern);
