-- ============================================================
-- 增量升级脚本：news 表增加 cover_image 字段
-- 生成日期: 2026-07-13
-- ============================================================
-- 用途：给资讯表增加封面图片字段，支持上传真实图片作为卡片封面。
--       有 cover_image 时优先显示图片，否则 fallback 到 cover_style 色块。
-- 可重复执行：ALTER TABLE ... ADD COLUMN IF NOT EXISTS 兼容写法。
-- ============================================================

SET NAMES utf8mb4;

-- MySQL 8.0 不支持 IF NOT EXISTS for ADD COLUMN，用存储过程兜底
DROP PROCEDURE IF EXISTS add_cover_image_if_not_exists;

DELIMITER //
CREATE PROCEDURE add_cover_image_if_not_exists()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'news'
          AND COLUMN_NAME = 'cover_image'
    ) THEN
        ALTER TABLE news
            ADD COLUMN cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL（有值时优先显示图片，否则使用 cover_style 色块）'
            AFTER cover_style;
    END IF;
END //
DELIMITER ;

CALL add_cover_image_if_not_exists();
DROP PROCEDURE IF EXISTS add_cover_image_if_not_exists;
