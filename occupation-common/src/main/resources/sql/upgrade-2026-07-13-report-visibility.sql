-- ============================================================
-- 增量升级：报告可见性（市场报告支持「全体可见 / 仅自己可见」）
-- 幂等；不 down -v 的库直接跑：
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < occupation-common/src/main/resources/sql/upgrade-2026-07-13-report-visibility.sql
-- 与 init.sql 同源：report_record 加 visibility 列（默认 PUBLIC，保持现有「市场报告全体可见」行为不变）。
-- ============================================================

DROP PROCEDURE IF EXISTS _add_visibility_col;
DELIMITER $$
CREATE PROCEDURE _add_visibility_col()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_record' AND COLUMN_NAME = 'visibility') THEN
        ALTER TABLE report_record
            ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'
            COMMENT '可见性：PUBLIC=全体可见 SELF=仅自己可见' AFTER error_msg;
    END IF;
END$$
DELIMITER ;

CALL _add_visibility_col();
DROP PROCEDURE IF EXISTS _add_visibility_col;
