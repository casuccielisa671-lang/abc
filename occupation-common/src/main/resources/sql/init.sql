-- ============================================================
-- 职业能力大数据服务平台 — 初始化数据库脚本
-- 版本: v1.0
-- 引擎: InnoDB | 字符集: utf8mb4
-- 执行方式: mysql -u root -p < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS occupation
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE occupation;

-- ============================================================
-- 1. 租户表（多租户核心表，不含 tenant_id）
-- ============================================================
DROP TABLE IF EXISTS sys_tenant;
CREATE TABLE sys_tenant (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    name        VARCHAR(100) NOT NULL COMMENT '租户名称（学校/企业名称）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- ============================================================
-- 2. 用户表
-- ============================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    tenant_id     BIGINT       NOT NULL COMMENT '所属租户ID',
    username      VARCHAR(50)  NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    role          VARCHAR(20)  NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN/HR',
    real_name     VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=禁用',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_username (tenant_id, username),
    KEY idx_tenant_id (tenant_id),
    KEY idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 3. 学生画像表
-- ============================================================
DROP TABLE IF EXISTS sys_student_profile;
CREATE TABLE sys_student_profile (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '画像ID',
    tenant_id          BIGINT       NOT NULL COMMENT '所属租户ID',
    user_id            BIGINT       NOT NULL COMMENT '关联用户ID',
    major              VARCHAR(100) DEFAULT NULL COMMENT '专业',
    skills             TEXT         DEFAULT NULL COMMENT '技能列表（JSON 数组或逗号分隔）',
    expected_city      VARCHAR(50)  DEFAULT NULL COMMENT '意向城市',
    expected_industry  VARCHAR(100) DEFAULT NULL COMMENT '意向行业',
    expected_salary_min INT         DEFAULT NULL COMMENT '期望薪资下限（元）',
    expected_salary_max INT         DEFAULT NULL COMMENT '期望薪资上限（元）',
    education_level    VARCHAR(20)  DEFAULT NULL COMMENT '学历：专科/本科/硕士/博士',
    deleted            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生画像表';

-- ============================================================
-- 4. 采集任务表
-- ============================================================
DROP TABLE IF EXISTS crawler_task;
CREATE TABLE crawler_task (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    source_type VARCHAR(30)  NOT NULL COMMENT '采集源类型：BOSS_ZHIPIN/ZHAOPIN/COMPANY_OFFICIAL',
    source_name VARCHAR(100) NOT NULL COMMENT '采集源名称',
    url_pattern VARCHAR(500) DEFAULT NULL COMMENT 'URL 匹配模式',
    cron_expr   VARCHAR(50)  DEFAULT NULL COMMENT 'Cron 定时表达式',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=停止 1=运行中',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务表';

-- ============================================================
-- 5. 采集日志表
-- ============================================================
DROP TABLE IF EXISTS crawler_log;
CREATE TABLE crawler_log (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    task_id     BIGINT   NOT NULL COMMENT '关联采集任务ID',
    tenant_id   BIGINT   NOT NULL COMMENT '所属租户ID',
    start_time  DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time    DATETIME DEFAULT NULL COMMENT '结束时间',
    record_count INT     DEFAULT 0 COMMENT '采集记录数',
    status      VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/SUCCESS/FAILED',
    error_msg   TEXT     DEFAULT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集日志表';

-- ============================================================
-- 6. 原始职位数据表（不含 tenant_id，全平台共享）
-- ============================================================
DROP TABLE IF EXISTS raw_job_data;
CREATE TABLE raw_job_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '数据ID',
    source      VARCHAR(50)  NOT NULL COMMENT '数据来源',
    source_url  VARCHAR(500) DEFAULT NULL COMMENT '来源URL',
    raw_content LONGTEXT     NOT NULL COMMENT '原始内容（JSON）',
    fetch_time  DATETIME     NOT NULL COMMENT '采集时间',
    status      VARCHAR(20)  NOT NULL DEFAULT 'RAW' COMMENT '状态：RAW=原始 CLEANED=已清洗',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_source (source),
    KEY idx_status (status),
    KEY idx_fetch_time (fetch_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始职位数据表';

-- ============================================================
-- 7. 清洗后职位表（全平台共享，不含 tenant_id）
-- ============================================================
DROP TABLE IF EXISTS job_detail;
CREATE TABLE job_detail (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '职位ID',
    title        VARCHAR(200) NOT NULL COMMENT '职位标题',
    company      VARCHAR(200) DEFAULT NULL COMMENT '公司名称',
    city         VARCHAR(50)  DEFAULT NULL COMMENT '城市',
    industry     VARCHAR(100) DEFAULT NULL COMMENT '行业',
    salary_min   INT          DEFAULT NULL COMMENT '薪资最低值（元）',
    salary_max   INT          DEFAULT NULL COMMENT '薪资最高值（元）',
    education    VARCHAR(20)  DEFAULT NULL COMMENT '学历要求',
    experience   VARCHAR(50)  DEFAULT NULL COMMENT '经验要求',
    skills       TEXT         DEFAULT NULL COMMENT '技能标签（JSON 数组）',
    description  LONGTEXT     DEFAULT NULL COMMENT '职位描述',
    publish_date DATE         DEFAULT NULL COMMENT '发布日期',
    source       VARCHAR(50)  DEFAULT NULL COMMENT '数据来源',
    source_url   VARCHAR(500) DEFAULT NULL COMMENT '来源URL',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_city (city),
    KEY idx_industry (industry),
    KEY idx_salary (salary_min, salary_max),
    KEY idx_publish_date (publish_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='清洗后职位详情表';

-- ============================================================
-- 8. 分析结果表
-- ============================================================
DROP TABLE IF EXISTS analysis_result;
CREATE TABLE analysis_result (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '结果ID',
    tenant_id       BIGINT        NOT NULL COMMENT '所属租户ID',
    dimension       VARCHAR(50)   NOT NULL COMMENT '分析维度：industry/city/skill/education/trend',
    dimension_value VARCHAR(200)  NOT NULL COMMENT '维度值（如"Java"、"北京"）',
    metric_name     VARCHAR(50)   NOT NULL COMMENT '指标名称：job_count/avg_salary_min/avg_salary_max',
    metric_value    DECIMAL(20,4) NOT NULL COMMENT '指标值',
    period_type     VARCHAR(10)   DEFAULT NULL COMMENT '周期类型：DAY/WEEK/MONTH/YEAR',
    period_value    VARCHAR(20)   DEFAULT NULL COMMENT '周期值（如"20260706"、"2026W27"）',
    calc_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    PRIMARY KEY (id),
    KEY idx_tenant_dimension (tenant_id, dimension),
    KEY idx_dimension_value (dimension, dimension_value),
    KEY idx_calc_time (calc_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析结果表';

-- ============================================================
-- 9. 报告模板表
-- ============================================================
DROP TABLE IF EXISTS report_template;
CREATE TABLE report_template (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    tenant_id        BIGINT       NOT NULL COMMENT '所属租户ID',
    name             VARCHAR(200) NOT NULL COMMENT '模板名称',
    industry         VARCHAR(100) DEFAULT NULL COMMENT '适用行业（NULL=通用）',
    type             VARCHAR(20)  NOT NULL COMMENT '报告类型：MONTHLY/QUARTERLY/YEARLY',
    template_content LONGTEXT     DEFAULT NULL COMMENT '模板内容（JSON 结构）',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=禁用',
    deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板表';

-- ============================================================
-- 10. 报告记录表
-- ============================================================
DROP TABLE IF EXISTS report_record;
CREATE TABLE report_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    template_id BIGINT       NOT NULL COMMENT '关联模板ID',
    params      TEXT         DEFAULT NULL COMMENT '生成参数（JSON）',
    file_url    VARCHAR(500) DEFAULT NULL COMMENT '生成文件URL',
    file_type   VARCHAR(10)  DEFAULT NULL COMMENT '文件类型：PDF/WORD/HTML',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/GENERATING/SUCCESS/FAILED',
    error_msg   TEXT         DEFAULT NULL COMMENT '错误信息',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_template_id (template_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告记录表';

-- ============================================================
-- 11. 推送记录表
-- ============================================================
DROP TABLE IF EXISTS push_record;
CREATE TABLE push_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '推送ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    user_id     BIGINT       NOT NULL COMMENT '目标用户ID',
    type        VARCHAR(20)  NOT NULL COMMENT '推送类型：RECOMMEND/SYSTEM',
    title       VARCHAR(200) NOT NULL COMMENT '推送标题',
    content     TEXT         DEFAULT NULL COMMENT '推送内容',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0=未读 1=已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, is_read),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送记录表';

-- ============================================================
-- 12. 学生行为记录表
-- ============================================================
DROP TABLE IF EXISTS student_behavior;
CREATE TABLE student_behavior (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '行为ID',
    tenant_id   BIGINT      NOT NULL COMMENT '所属租户ID',
    user_id     BIGINT      NOT NULL COMMENT '学生用户ID',
    job_id      BIGINT      NOT NULL COMMENT '职位ID',
    action      VARCHAR(20) NOT NULL COMMENT '行为类型：VIEW/FAVORITE/APPLY/IGNORE',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_job (user_id, job_id),
    KEY idx_user_action (user_id, action),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生行为记录表';

-- ============================================================
-- 13. 系统告警表
-- ============================================================
DROP TABLE IF EXISTS sys_alert;
CREATE TABLE sys_alert (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '告警ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    type        VARCHAR(50)  NOT NULL COMMENT '告警类型：KAFKA_LAG/CRAWLER_FAILURE/DB_POOL/SYSTEM',
    level       VARCHAR(10)  NOT NULL COMMENT '告警级别：INFO/WARN/ERROR',
    content     TEXT         NOT NULL COMMENT '告警内容',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0=未读 1=已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tenant_level (tenant_id, level),
    KEY idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统告警表';

-- ============================================================
-- 初始化种子数据
-- ============================================================

-- 默认租户：测试学院
INSERT INTO sys_tenant (id, name, status) VALUES (1, '测试学院', 1);

-- 默认管理员：admin / admin123（BCrypt 加密）
INSERT INTO sys_user (id, tenant_id, username, password_hash, role, real_name, status)
VALUES (1, 1, 'admin',
        '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q',
        'ADMIN', '系统管理员', 1);
