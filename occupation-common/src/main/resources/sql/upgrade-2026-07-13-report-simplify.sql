-- ============================================================
-- 增量升级脚本：报告简化（去掉模板层）
-- 生成日期: 2026-07-13
-- ============================================================
--
-- 变更：报告不再有"模板"概念，直接按 大类 + 范围 生成。
--   1) report_record 新增 name（报告名称）
--   2) report_record 去掉 template_id（原 NOT NULL，会挡住新的"无模板"插入，必须去掉）
--   3) 删除 report_template 表
-- 全新的库不需要执行 —— init.sql 已是最终结构。
--
-- ⚠️ 不写 `USE occupation;`。命令行显式指定库：
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < upgrade-2026-07-13-report-simplify.sql
-- ============================================================

SET NAMES utf8mb4;

-- 1. report_record 加 name（幂等）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_record' AND COLUMN_NAME = 'name');
SET @s := IF(@c = 0,
    'ALTER TABLE report_record ADD COLUMN name VARCHAR(200) NOT NULL DEFAULT '''' COMMENT ''报告名称'' AFTER tenant_id',
    'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 回填历史记录的 name（按大类给默认名）
UPDATE report_record
   SET name = CASE category WHEN 'EMPLOYMENT' THEN '学生就业数据报告' ELSE '就业市场分析报告' END
 WHERE name IS NULL OR name = '';

-- 2. 去掉 template_id（关键：原列 NOT NULL 会让新的无模板插入失败；索引随列自动删除）
SET @c2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_record' AND COLUMN_NAME = 'template_id');
SET @s2 := IF(@c2 > 0, 'ALTER TABLE report_record DROP COLUMN template_id', 'DO 0');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;

-- 3. 删除报告模板表
DROP TABLE IF EXISTS report_template;

-- 自检
SELECT '报告记录数' AS chk, COUNT(*) AS v FROM report_record;
