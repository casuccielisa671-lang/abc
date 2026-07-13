-- ============================================================
-- 增量升级脚本：report_record 加 user_id（报告归属人）
-- 生成日期: 2026-07-13
-- ============================================================
--
-- 用途：为"学生个人 AI 分析报告"做准备。
--   user_id NULL = 管理员生成的租户级报告（市场/就业）；有值 = 某学生的个人报告。
-- 全新的库不需要执行 —— init.sql 已含。可重复执行（列存在性判断）。
--
-- ⚠️ 不写 `USE occupation;`。命令行显式指定库：
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < upgrade-2026-07-13-report-user.sql
-- ============================================================

SET NAMES utf8mb4;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_record' AND COLUMN_NAME = 'user_id');
SET @s := IF(@c = 0,
    'ALTER TABLE report_record ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT ''归属人：NULL=租户级；有值=学生个人报告'' AFTER tenant_id, ADD KEY idx_user_id (user_id)',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
