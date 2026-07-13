-- ============================================================
-- 增量升级脚本：学院内组织结构（班级 / 学生归属 / 教师可见范围）
-- 生成日期: 2026-07-12
-- ============================================================
--
-- 用途：给「已经有数据、不想 docker-compose down -v 重建」的库补上：
--   1) 新表 sys_class（班级：专业-入学年级-班级）
--   2) sys_user 新增 class_id 列（学生班级归属）
--   3) 新表 teacher_scope（教师可见范围：班主任/专业老师/届老师）
--   并写入与 init.sql 同源的种子数据。
-- 全新的库不需要执行 —— init.sql 里已包含同样的 DDL 与种子。
--
-- 可重复执行：CREATE TABLE IF NOT EXISTS + 列存在性判断 + INSERT IGNORE +
--             UPDATE ... WHERE class_id IS NULL（不覆盖已有归属）。
--
-- ⚠️ 刻意不写 `USE occupation;`：init.sql 里那句 USE 会覆盖命令行指定的库名，是个已知脚枪。
--    请在命令行显式指定库名：
--
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < upgrade-2026-07-12-class-org.sql
--
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 1. 班级表 ----------
CREATE TABLE IF NOT EXISTS sys_class (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户（学院）ID',
    major       VARCHAR(100) NOT NULL COMMENT '专业',
    enroll_year INT          NOT NULL COMMENT '入学年级（如 2022）',
    class_name  VARCHAR(50)  NOT NULL COMMENT '班级名（如 "1班"）',
    code        VARCHAR(200) NOT NULL COMMENT '统一命名：专业-入学年级-班级',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code),
    KEY idx_tenant_major (tenant_id, major),
    KEY idx_tenant_year (tenant_id, enroll_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ---------- 2. 教师可见范围表 ----------
CREATE TABLE IF NOT EXISTS teacher_scope (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    teacher_id  BIGINT       NOT NULL COMMENT '教师用户ID（sys_user.id，role=TEACHER）',
    scope_type  VARCHAR(10)  NOT NULL COMMENT '范围类型：CLASS=班主任 MAJOR=专业老师 GRADE=届老师',
    scope_value VARCHAR(200) NOT NULL COMMENT '范围值：CLASS→班级id / MAJOR→专业名 / GRADE→入学年级',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tenant_teacher (tenant_id, teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师可见范围表';

-- ---------- 3. sys_user 增加 class_id 列（幂等：先查 information_schema 再决定是否 ALTER）----------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'class_id'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN class_id BIGINT DEFAULT NULL COMMENT ''所属班级ID（仅学生，关联 sys_class.id）'' AFTER deleted, ADD KEY idx_class_id (class_id)',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 4. 种子：班级（与 init.sql / gen-seed-data.js 同源）----------
INSERT IGNORE INTO sys_class (id, tenant_id, major, enroll_year, class_name, code, status) VALUES
(1, 1, '软件工程', 2022, '1班', '软件工程-2022-1班', 1),
(2, 1, '计算机科学与技术', 2022, '1班', '计算机科学与技术-2022-1班', 1),
(3, 1, '数据科学与大数据技术', 2022, '1班', '数据科学与大数据技术-2022-1班', 1),
(4, 1, '人工智能', 2022, '1班', '人工智能-2022-1班', 1),
(5, 1, '信息安全', 2022, '1班', '信息安全-2022-1班', 1),
(6, 1, '统计学', 2022, '1班', '统计学-2022-1班', 1),
(7, 1, '物联网工程', 2022, '1班', '物联网工程-2022-1班', 1),
(8, 1, '电子商务', 2023, '1班', '电子商务-2023-1班', 1),
(9, 1, '计算机应用技术', 2023, '1班', '计算机应用技术-2023-1班', 1),
(10, 1, '教育技术学', 2023, '1班', '教育技术学-2023-1班', 1),
(11, 2, '软件工程', 2022, '1班', '软件工程-2022-1班', 1);

-- ---------- 5. 种子：学生班级归属（只填 class_id 仍为空的，不覆盖已有归属）----------
UPDATE sys_user SET class_id = 2  WHERE id IN (2, 8)     AND class_id IS NULL;
UPDATE sys_user SET class_id = 1  WHERE id IN (5, 11, 16) AND class_id IS NULL;
UPDATE sys_user SET class_id = 3  WHERE id = 6  AND class_id IS NULL;
UPDATE sys_user SET class_id = 4  WHERE id = 7  AND class_id IS NULL;
UPDATE sys_user SET class_id = 5  WHERE id = 9  AND class_id IS NULL;
UPDATE sys_user SET class_id = 6  WHERE id = 12 AND class_id IS NULL;
UPDATE sys_user SET class_id = 7  WHERE id = 15 AND class_id IS NULL;
UPDATE sys_user SET class_id = 8  WHERE id = 10 AND class_id IS NULL;
UPDATE sys_user SET class_id = 9  WHERE id = 13 AND class_id IS NULL;
UPDATE sys_user SET class_id = 10 WHERE id = 14 AND class_id IS NULL;
UPDATE sys_user SET class_id = 11 WHERE id = 24 AND class_id IS NULL;

-- ---------- 6. 种子：教师可见范围 ----------
INSERT IGNORE INTO teacher_scope (id, tenant_id, teacher_id, scope_type, scope_value) VALUES
(1, 1, 17, 'CLASS', '1'),
(2, 1, 18, 'MAJOR', '计算机科学与技术'),
(3, 1, 3,  'GRADE', '2022'),
(4, 2, 25, 'CLASS', '11');

-- ---------- 自检：三种范围各自能看到几名学生 ----------
SELECT '班主任 teacher01→软件工程-2022-1班' AS 范围,
       (SELECT COUNT(*) FROM sys_user WHERE role='STUDENT' AND deleted=0 AND class_id=1) AS 学生数
UNION ALL
SELECT '专业老师 teacher02→计算机科学与技术',
       (SELECT COUNT(*) FROM sys_user u JOIN sys_class c ON c.id=u.class_id
        WHERE u.role='STUDENT' AND u.deleted=0 AND c.major='计算机科学与技术')
UNION ALL
SELECT '届老师 teacher→2022 级',
       (SELECT COUNT(*) FROM sys_user u JOIN sys_class c ON c.id=u.class_id
        WHERE u.role='STUDENT' AND u.deleted=0 AND c.enroll_year=2022);
