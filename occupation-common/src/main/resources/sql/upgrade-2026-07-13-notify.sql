-- ============================================================
-- 增量升级：站内通知增强（HR 面试通知 + 报告下发通知 + 首页消息中心）
-- 幂等；不 down -v 的库直接跑：
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < occupation-common/src/main/resources/sql/upgrade-2026-07-13-notify.sql
-- 与 init.sql 同源：push_record 加 ref_type/ref_id（消息可点击跳转），
--                   job_application 加 4 个面试字段（HR 邀请面试时填）。
-- MySQL 8.0 不支持 ALTER ... ADD COLUMN IF NOT EXISTS，用 information_schema 守卫做幂等。
-- ============================================================

DROP PROCEDURE IF EXISTS _add_col_if_absent;
DELIMITER $$
CREATE PROCEDURE _add_col_if_absent(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL _add_col_if_absent('push_record', 'ref_type',
    "ref_type VARCHAR(20) DEFAULT NULL COMMENT '关联对象类型：APPLICATION/REPORT' AFTER content");
CALL _add_col_if_absent('push_record', 'ref_id',
    "ref_id BIGINT DEFAULT NULL COMMENT '关联对象ID，前端据此跳转' AFTER ref_type");

CALL _add_col_if_absent('job_application', 'interview_time',
    "interview_time DATETIME DEFAULT NULL COMMENT '面试时间' AFTER hr_note");
CALL _add_col_if_absent('job_application', 'interview_place',
    "interview_place VARCHAR(300) DEFAULT NULL COMMENT '面试地点/方式' AFTER interview_time");
CALL _add_col_if_absent('job_application', 'interview_contact',
    "interview_contact VARCHAR(100) DEFAULT NULL COMMENT '面试官/联系人' AFTER interview_place");
CALL _add_col_if_absent('job_application', 'interview_content',
    "interview_content VARCHAR(500) DEFAULT NULL COMMENT '面试内容/环节' AFTER interview_contact");

DROP PROCEDURE IF EXISTS _add_col_if_absent;
