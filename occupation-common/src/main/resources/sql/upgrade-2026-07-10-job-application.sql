-- ============================================================
-- 增量升级脚本：新增 job_application（投递记录表）
-- 生成日期: 2026-07-10
-- ============================================================
--
-- 用途：给「已经有数据、不想 docker-compose down -v 重建」的库补上这张表，
-- 并把历史的 APPLY 行为回填成投递实体。
-- 全新的库不需要执行 —— init.sql 里已包含同样的 DDL 与种子数据。
--
-- 可重复执行：CREATE TABLE IF NOT EXISTS + INSERT IGNORE。
--
-- ⚠️ 刻意不写 `USE occupation;`：init.sql 里那句 USE 会覆盖命令行指定的库名，是个已知脚枪。
--    请在命令行显式指定库名：
--
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < upgrade-2026-07-10-job-application.sql
--
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS job_application (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '投递ID',
    tenant_id         BIGINT       NOT NULL COMMENT '所属租户ID',
    user_id           BIGINT       NOT NULL COMMENT '投递学生的用户ID',
    job_id            BIGINT       NOT NULL COMMENT '被投递的职位ID',
    publisher_id      BIGINT       NOT NULL COMMENT '职位发布者（HR）的用户ID，投递时固化',
    status            VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED'
                      COMMENT '状态：SUBMITTED=已投递 VIEWED=已查看 INTERVIEW=邀请面试 OFFER=已录用 REJECTED=不合适',
    hr_note           VARCHAR(500) DEFAULT NULL COMMENT 'HR 备注（仅 HR 可见）',
    applied_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    status_changed_at DATETIME     DEFAULT NULL COMMENT '最近一次状态变更时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_job (user_id, job_id),
    KEY idx_publisher_status (publisher_id, status),
    KEY idx_user_id (user_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投递记录表';

-- ---------- 回填历史投递 ----------
-- 只回填落在「有主职位」（publisher_id 非空）上的 APPLY 行为。
-- 落在采集职位上的那些是幽灵投递：没有任何 HR 能处理它们，回填进来只会在
-- 学生的「我的投递」里制造永远停在「已投递」的僵尸记录。它们仍留在 student_behavior
-- 里当行为埋点，继续为推荐算法提供信号。
INSERT IGNORE INTO job_application
    (tenant_id, user_id, job_id, publisher_id, status, applied_at, create_time)
SELECT b.tenant_id, b.user_id, b.job_id, j.publisher_id, 'SUBMITTED', b.create_time, b.create_time
FROM student_behavior b
JOIN job_detail j ON j.id = b.job_id
WHERE b.action = 'APPLY'
  AND j.publisher_id IS NOT NULL;

-- 回填结果自检：下面两个数应该相等
SELECT
    (SELECT COUNT(*) FROM student_behavior b JOIN job_detail j ON j.id = b.job_id
     WHERE b.action = 'APPLY' AND j.publisher_id IS NOT NULL) AS `应回填数`,
    (SELECT COUNT(*) FROM job_application) AS `实际行数`,
    (SELECT COUNT(*) FROM student_behavior b JOIN job_detail j ON j.id = b.job_id
     WHERE b.action = 'APPLY' AND j.publisher_id IS NULL) AS `未回填的幽灵投递`;
