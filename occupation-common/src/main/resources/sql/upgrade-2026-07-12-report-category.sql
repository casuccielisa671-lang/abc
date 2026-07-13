-- ============================================================
-- 增量升级脚本：报告市场/就业两类统一（report_template / report_record 加 category）
-- 生成日期: 2026-07-12
-- ============================================================
--
-- 用途：给「已有数据、不想 down -v 重建」的库补上报告大类字段：
--   1) report_template.category（MARKET=市场行业 / EMPLOYMENT=学生就业）
--   2) report_record.category（生成时从模板固化，便于列表按类筛选）
--   并新增一个 EMPLOYMENT 类模板样例。
-- 全新的库不需要执行 —— init.sql 已含同样 DDL 与种子。
--
-- 可重复执行：列存在性判断 + INSERT IGNORE。ADD COLUMN 带 DEFAULT 'MARKET'，
--             已有行自动取 MARKET，无需额外 UPDATE。
--
-- ⚠️ 不写 `USE occupation;`（init.sql 的 USE 会覆盖命令行库名，已知脚枪）。命令行显式指定库：
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < upgrade-2026-07-12-report-category.sql
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 1. report_template.category ----------
SET @c1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_template' AND COLUMN_NAME = 'category');
SET @s1 := IF(@c1 = 0,
    'ALTER TABLE report_template ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT ''MARKET'' COMMENT ''报告大类：MARKET=市场行业 EMPLOYMENT=学生就业'' AFTER industry',
    'DO 0');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

-- ---------- 2. report_record.category ----------
SET @c2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_record' AND COLUMN_NAME = 'category');
SET @s2 := IF(@c2 = 0,
    'ALTER TABLE report_record ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT ''MARKET'' COMMENT ''报告大类：MARKET/EMPLOYMENT（生成时从模板固化）'' AFTER template_id',
    'DO 0');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;

-- ---------- 3. 新增 EMPLOYMENT 类模板样例（与 init.sql 同源，id=7） ----------
INSERT IGNORE INTO report_template (id, tenant_id, name, industry, category, type, template_content, status, deleted, create_time) VALUES
(7, 1, '学生就业数据报告（按专业/年级/班级）', NULL, 'EMPLOYMENT', 'YEARLY', NULL, 1, 0, '2026-07-12 10:00:00');

-- ---------- 自检 ----------
SELECT category, COUNT(*) AS 模板数 FROM report_template GROUP BY category;
