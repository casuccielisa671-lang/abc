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

-- 强制客户端连接字符集，防止 docker-entrypoint 执行本脚本时中文种子数据变乱码
SET NAMES utf8mb4;

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
    class_id      BIGINT       DEFAULT NULL COMMENT '所属班级ID（仅学生，关联 sys_class.id；教师/HR/管理员为 NULL）',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_username (tenant_id, username),
    KEY idx_tenant_id (tenant_id),
    KEY idx_role (role),
    KEY idx_class_id (class_id)
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
    avatar_url         VARCHAR(500) DEFAULT NULL COMMENT '证件照URL（上传后存储的相对路径或完整URL）',
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
    source_type VARCHAR(30)  NOT NULL COMMENT '采集源类型：MOCK/OFFICIAL_PUBLIC/ZHAOPIN；历史资讯源仅兼容旧任务',
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
    publisher_id BIGINT       DEFAULT NULL COMMENT '发布者用户ID（仅 source=HR_PUBLISH 有值；采集数据为 NULL）',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_city (city),
    KEY idx_industry (industry),
    KEY idx_salary (salary_min, salary_max),
    KEY idx_publish_date (publish_date),
    KEY idx_publisher_id (publisher_id)
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
-- 9. 报告记录表（无模板：报告由 大类+范围 直接生成）
-- ============================================================
DROP TABLE IF EXISTS report_record;
CREATE TABLE report_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    user_id     BIGINT       DEFAULT NULL COMMENT '归属人：NULL=管理员生成的租户级报告；有值=该学生的个人 AI 报告',
    name        VARCHAR(200) NOT NULL COMMENT '报告名称（生成时按 大类+范围 自动生成）',
    category    VARCHAR(20)  NOT NULL DEFAULT 'MARKET' COMMENT '报告大类：MARKET=市场行业 EMPLOYMENT=学生就业 STUDENT_AI=学生个人AI分析',
    params      TEXT         DEFAULT NULL COMMENT '生成参数（JSON）；EMPLOYMENT 类在此存 scope：{major,enrollYear,classId}',
    file_url    VARCHAR(500) DEFAULT NULL COMMENT '生成文件URL',
    file_type   VARCHAR(10)  DEFAULT NULL COMMENT '文件类型：PDF/WORD/HTML',
    ai_summary  LONGTEXT     DEFAULT NULL COMMENT '生成时产出的智能摘要（开放API直接读取，避免重复调用大模型）',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/GENERATING/SUCCESS/FAILED',
    error_msg   TEXT         DEFAULT NULL COMMENT '错误信息',
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC' COMMENT '可见性：PUBLIC=全体可见 SELF=仅自己可见（主要用于市场报告；就业报告的可见范围由 report_delivery 决定）',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_user_id (user_id),
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
    type        VARCHAR(20)  NOT NULL COMMENT '推送类型：RECOMMEND/SYSTEM/INTERVIEW/OFFER/REJECT/REPORT',
    title       VARCHAR(200) NOT NULL COMMENT '推送标题',
    content     TEXT         DEFAULT NULL COMMENT '推送内容',
    ref_type    VARCHAR(20)  DEFAULT NULL COMMENT '关联对象类型：APPLICATION（投递）/REPORT（报告），空=纯通知不跳转',
    ref_id      BIGINT       DEFAULT NULL COMMENT '关联对象ID，前端据此跳转（投递详情/报告页）',
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
    action      VARCHAR(20) NOT NULL COMMENT '行为类型：VIEW/FAVORITE/APPLY/IGNORE/CONTACT（自主联系外部岗位）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_job (user_id, job_id),
    KEY idx_user_action (user_id, action),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生行为记录表';

-- ============================================================
-- 13. API 客户端表（对外 API 鉴权）
-- ============================================================
DROP TABLE IF EXISTS api_client;
CREATE TABLE api_client (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '客户端ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    client_name VARCHAR(100) NOT NULL COMMENT '客户端名称',
    api_key     VARCHAR(64)  NOT NULL COMMENT 'API Key（客户端标识）',
    api_secret  VARCHAR(255) NOT NULL COMMENT 'API Secret（加密存储）',
    scopes      VARCHAR(200) DEFAULT NULL COMMENT '授权范围（逗号分隔）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=禁用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_key (api_key),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API 客户端表';

-- ============================================================
-- 14. 系统告警表
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
-- 15. 学生简历表
-- ============================================================
-- 与 sys_student_profile 的分工：
--   画像(profile) = 结构化的「匹配依据」，字段扁平、可索引，喂给推荐算法；
--   简历(resume)  = 面向人阅读的「自我陈述」，多条目、变长，喂给 HR 和大模型。
-- 三段经历用 JSON 数组存：条目数不定、字段只读不查，拆子表徒增 join 而无收益。
-- 联系方式冗余在这里而不是读 sys_user：学生可以填一个只用于求职的手机/邮箱，
-- 不必污染登录账号信息；为空时后端回落到 sys_user。
DROP TABLE IF EXISTS student_resume;
CREATE TABLE student_resume (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    tenant_id      BIGINT       NOT NULL COMMENT '所属租户ID',
    user_id        BIGINT       NOT NULL COMMENT '关联用户ID',
    contact_phone  VARCHAR(20)  DEFAULT NULL COMMENT '求职手机号（为空则用 sys_user.phone）',
    contact_email  VARCHAR(100) DEFAULT NULL COMMENT '求职邮箱（为空则用 sys_user.email）',
    job_intention  VARCHAR(100) DEFAULT NULL COMMENT '求职意向岗位，如「Java后端开发」',
    self_intro     TEXT         DEFAULT NULL COMMENT '自我评价',
    educations     TEXT         DEFAULT NULL COMMENT '教育经历 JSON 数组：[{school,major,degree,startDate,endDate,gpa}]',
    projects       TEXT         DEFAULT NULL COMMENT '项目经历 JSON 数组：[{name,role,startDate,endDate,description,skills}]',
    internships    TEXT         DEFAULT NULL COMMENT '实习经历 JSON 数组：[{company,position,startDate,endDate,description}]',
    honors         TEXT         DEFAULT NULL COMMENT '获奖与证书 JSON 数组：["ACM省赛二等奖","CET-6"]',
    ai_review      LONGTEXT     DEFAULT NULL COMMENT '最近一次 AI 诊断结果（JSON），避免重复调用大模型',
    ai_review_time DATETIME     DEFAULT NULL COMMENT 'AI 诊断时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生简历表';

-- ============================================================
-- 16. 投递记录表
-- ============================================================
-- 与 student_behavior 的分工（两张表都记录「投递」，但语义完全不同）：
--   student_behavior = 行为埋点。VIEW/FAVORITE/APPLY/IGNORE 一视同仁，只记「谁在什么时候
--     对哪个职位做了什么」。它喂给推荐算法做行为加权，是只增不改的日志。
--   job_application  = 业务实体。有状态机、有 HR 备注、有状态变更时间。HR 要能标记
--     「已查看 / 邀请面试 / 不合适」，学生要能看到进度 —— 这些在埋点表里无处安放。
--
-- 投递时两张表都写：behavior 保证推荐算法与各类统计零改动，application 承载业务流程。
--
-- publisher_id 冗余存一份：投递发生时职位归属就固化下来。不这么做的话 HR 查「我收到的
-- 投递」要先查自己发布的职位再 IN 一大串 job_id；而且职位一旦被下架（job_detail 是物理
-- 删除，没有 deleted 列），投递记录就再也找不到主人。
DROP TABLE IF EXISTS job_application;
CREATE TABLE job_application (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '投递ID',
    tenant_id         BIGINT       NOT NULL COMMENT '所属租户ID',
    user_id           BIGINT       NOT NULL COMMENT '投递学生的用户ID',
    job_id            BIGINT       NOT NULL COMMENT '被投递的职位ID',
    publisher_id      BIGINT       NOT NULL COMMENT '职位发布者（HR）的用户ID，投递时固化',
    status            VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED'
                      COMMENT '状态：SUBMITTED=已投递 VIEWED=已查看 INTERVIEW=邀请面试 OFFER=已录用 ACCEPTED=已入职(学生接收) REJECTED=不合适',
    hr_note           VARCHAR(500) DEFAULT NULL COMMENT 'HR 备注（仅 HR 可见）',
    interview_time    DATETIME     DEFAULT NULL COMMENT '面试时间（HR 邀请面试时填）',
    interview_place   VARCHAR(300) DEFAULT NULL COMMENT '面试地点/方式（线下地址或线上会议链接）',
    interview_contact VARCHAR(100) DEFAULT NULL COMMENT '面试官/联系人',
    interview_content VARCHAR(500) DEFAULT NULL COMMENT '面试内容/环节说明',
    applied_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    status_changed_at DATETIME     DEFAULT NULL COMMENT '最近一次状态变更时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    -- 同一个人不能重复投同一个职位。behavior 表靠 Service 做幂等，这里用唯一索引兜死
    UNIQUE KEY uk_user_job (user_id, job_id),
    KEY idx_publisher_status (publisher_id, status),
    KEY idx_user_id (user_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投递记录表';

-- ============================================================
-- 17. 班级表（学院内组织结构：专业-入学年级-班级）
-- ============================================================
DROP TABLE IF EXISTS sys_class;
CREATE TABLE sys_class (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户（学院）ID',
    major       VARCHAR(100) NOT NULL COMMENT '专业',
    enroll_year INT          NOT NULL COMMENT '入学年级（如 2022）',
    class_name  VARCHAR(50)  NOT NULL COMMENT '班级名（如 "1班"）',
    code        VARCHAR(200) NOT NULL COMMENT '统一命名：专业-入学年级-班级（如 软件工程-2022-1班）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code),
    KEY idx_tenant_major (tenant_id, major),
    KEY idx_tenant_year (tenant_id, enroll_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ============================================================
-- 18. 教师可见范围表（一个教师可多行：班主任=CLASS / 专业老师=MAJOR / 届老师=GRADE）
-- ============================================================
DROP TABLE IF EXISTS teacher_scope;
CREATE TABLE teacher_scope (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    tenant_id   BIGINT       NOT NULL COMMENT '所属租户ID',
    teacher_id  BIGINT       NOT NULL COMMENT '教师用户ID（sys_user.id，role=TEACHER）',
    scope_type  VARCHAR(10)  NOT NULL COMMENT '范围类型：CLASS=班主任 MAJOR=专业老师 GRADE=届老师',
    scope_value VARCHAR(200) NOT NULL COMMENT '范围值：CLASS→班级id / MAJOR→专业名 / GRADE→入学年级',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tenant_teacher (tenant_id, teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师可见范围表';

-- ============================================================
-- 19. 资讯表（首页资讯板块：数据播报 / 精选文章 / 外部资讯）
-- ============================================================
DROP TABLE IF EXISTS news;
CREATE TABLE news (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
    tenant_id    BIGINT       NOT NULL COMMENT '所属租户ID',
    category     VARCHAR(20)  DEFAULT NULL COMMENT '技术方向：backend/frontend/test/devops/bigdata；NULL=通用',
    type         VARCHAR(20)  NOT NULL COMMENT '类型：DATA_CAST=数据播报 ARTICLE=精选文章 EXTERNAL=外部资讯',
    title        VARCHAR(300) NOT NULL COMMENT '标题',
    summary      VARCHAR(600) DEFAULT NULL COMMENT '摘要',
    content      LONGTEXT     DEFAULT NULL COMMENT '正文（仅精选文章，外部/播报为空）',
    cover_style  VARCHAR(20)  DEFAULT 'blue' COMMENT '封面色块样式：blue/green/purple/amber',
    cover_image  VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL（有值时优先显示图片，否则使用 cover_style 色块）',
    source       VARCHAR(100) DEFAULT NULL COMMENT '来源：平台数据播报 / RSS源名 / 作者',
    source_url   VARCHAR(600) DEFAULT NULL COMMENT '外部原文链接（EXTERNAL 点击跳出）',
    link_target  VARCHAR(200) DEFAULT NULL COMMENT '站内跳转（DATA_CAST 点击去对应图表，如 /admin/dashboard）',
    view_count   INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
    featured     TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶/精选：1=是 0=否',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=上架 0=下架',
    publish_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_type (tenant_id, type),
    KEY idx_tenant_cat (tenant_id, category),
    KEY idx_publish (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯表';

-- ============================================================
-- 19. 报告下发记录表
--   管理员把「学生就业报告」定向发给某范围学生时，每个接收学生落一行。
--   市场行业报告走「发布即全体可见」的广播口径，不落此表（学生端按 category=MARKET 直接可见）。
-- ============================================================
DROP TABLE IF EXISTS report_delivery;
CREATE TABLE report_delivery (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '下发记录ID',
    tenant_id   BIGINT   NOT NULL COMMENT '所属租户ID',
    report_id   BIGINT   NOT NULL COMMENT '报告ID（指向 report_record）',
    user_id     BIGINT   NOT NULL COMMENT '接收学生 userId',
    read_time   DATETIME DEFAULT NULL COMMENT '阅读时间；NULL=未读',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_user (report_id, user_id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告下发记录表';

-- ============================================================
-- 初始化种子数据（由 scripts/gen-seed-data.js 确定性生成，勿手改）
--
-- 【职位数据分两类，看 source + publisher_id 两列就能区分】
--   source=HR_PUBLISH,   publisher_id=NOT NULL → HR 在平台发布的（可投递）
--   source=MOCK/ZHAOPIN, publisher_id=NULL     → 采集来的（无主，只能自主联系）
--
-- 【2026-07-14 数据模型调整：开箱全是可投递数据】
--   init.sql 只预置 90 条可投递(HR_PUBLISH)职位，覆盖 12 家公司 / 8 行业 / 10 城市，
--   看板 / 推荐 / 就业分析开箱即基于这 90 条。种子里不再有 MOCK 职位。
--   「自主联系(采集)」数据改为运行时产生：点采集任务 → 读 mock/mock-jobs.json（60 条，
--   mock.local 域名）→ 走 Kafka + 清洗进库，publisher_id 恒 NULL。采完平台数据即"更新"，
--   看板与推荐随之纳入这批市场参考数据。
--   ⚠️ 撤走 MOCK 的连带影响：CONTACT(自主联系)行为开箱为 0，「自主求职流向」图开箱空；
--   点采集 + 学生自主联系后才有数据。这是有意为之，符合"开箱只有可投递"的模型。
--
-- 【生成器维护的不变量，改动时务必保持】
--   90 条可投递职位轮流分摊到 12 个 HR，每个 HR 至少收到若干投递；
--   APPLY 只落可投递职位（种子里已无采集职位，故无 CONTACT）；
--   analysis_result 由 job_detail 按后端同口径聚合得出；
--   student_behavior / push_record 引用真实存在的用户与职位
--
-- 所有账号密码均为 admin123
-- ============================================================

-- ---------- 租户 ----------
INSERT INTO sys_tenant (id, name, status) VALUES
(1, '测试学院', 1),
(2, '示范大学', 1),
(3, '停用学院', 0);  -- 已禁用租户：测试“租户停用后无法登录”

-- ---------- 班级（学院内组织：专业-入学年级-班级）----------
INSERT INTO sys_class (id, tenant_id, major, enroll_year, class_name, code, status) VALUES
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

-- ---------- 用户（密码均 admin123）----------
INSERT INTO sys_user (id, tenant_id, username, password_hash, role, real_name, phone, email, status, deleted, class_id) VALUES
(1, 1, 'admin', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'ADMIN', '系统管理员', '13800000001', 'admin@test.edu.cn', 1, 0, NULL),
(2, 1, 'student', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '演示学生', '13800000002', 'student@stu.test.edu.cn', 1, 0, 2),
(3, 1, 'teacher', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'TEACHER', '演示教师', '13800000003', 'teacher@test.edu.cn', 1, 0, NULL),
(4, 1, 'hr', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '演示HR', '13800000004', 'hr@yunpin.example.com', 1, 0, NULL),
(5, 1, 'student01', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '陈嘉怡', '13811000001', 'chenjiayi@stu.test.edu.cn', 1, 0, 1),
(6, 1, 'student02', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '李昊然', '13811000002', 'lihaoran@stu.test.edu.cn', 1, 0, 3),
(7, 1, 'student03', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '王雨桐', '13811000003', 'wangyutong@stu.test.edu.cn', 1, 0, 4),
(8, 1, 'student04', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '张子墨', '13811000004', 'zhangzimo@stu.test.edu.cn', 1, 0, 2),
(9, 1, 'student05', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '刘思远', '13811000005', 'liusiyuan@stu.test.edu.cn', 1, 0, 5),
(10, 1, 'student06', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '杨欣然', '13811000006', 'yangxinran@stu.test.edu.cn', 1, 0, 8),
(11, 1, 'student07', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '赵一鸣', '13811000007', 'zhaoyiming@stu.test.edu.cn', 1, 0, 1),
(12, 1, 'student08', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '黄诗涵', '13811000008', 'huangshihan@stu.test.edu.cn', 1, 0, 6),
(13, 1, 'student09', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '周俊杰', '13811000009', 'zhoujunjie@stu.test.edu.cn', 1, 0, 9),
(14, 1, 'student10', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '吴雅静', '13811000010', 'wuyajing@stu.test.edu.cn', 1, 0, 10),
(15, 1, 'student11', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '徐浩宇', '13811000011', 'xuhaoyu@stu.test.edu.cn', 1, 0, 7),
(16, 1, 'student12', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '孙梦琪', '13811000012', 'sunmengqi@stu.test.edu.cn', 1, 0, 1),
(17, 1, 'teacher01', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'TEACHER', '王建国', '13822000001', 'wangjianguo@test.edu.cn', 1, 0, NULL),
(18, 1, 'teacher02', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'TEACHER', '林晓芳', '13822000002', 'linxiaofang@test.edu.cn', 1, 0, NULL),
(19, 1, 'hr01', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '郑倩', '13833000001', 'zhengqian@yunpin.example.com', 1, 0, NULL),
(20, 1, 'hr02', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '高翔', '13833000002', 'gaoxiang@zhihui.example.com', 1, 0, NULL),
(21, 1, 'student98', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '钱多多（已禁用）', '13844000001', 'qianduoduo@stu.test.edu.cn', 0, 0, NULL),
(22, 1, 'student99', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '孔乙己（已删除）', '13844000002', 'kongyiji@stu.test.edu.cn', 1, 1, NULL),
(23, 2, 'admin', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'ADMIN', '示范大学管理员', '13900000001', 'admin@demo.edu.cn', 1, 0, NULL),
(24, 2, 'student', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'STUDENT', '示范学生', '13900000002', 'student@demo.edu.cn', 1, 0, 11),
(25, 2, 'teacher', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'TEACHER', '示范教师', '13900000003', 'teacher@demo.edu.cn', 1, 0, NULL),
(26, 1, 'hr03', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '孙浩', '13833000003', 'sunhao@borui.example.com', 1, 0, NULL),
(27, 1, 'hr04', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '许静怡', '13833000004', 'xujingyi@jinqiao.example.com', 1, 0, NULL),
(28, 1, 'hr05', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '罗晨', '13833000005', 'luochen@youxuan.example.com', 1, 0, NULL),
(29, 1, 'hr06', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '范文博', '13833000006', 'fanwenbo@leyou.example.com', 1, 0, NULL),
(30, 1, 'hr07', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '秦悦', '13833000007', 'qinyue@jinggong.example.com', 1, 0, NULL),
(31, 1, 'hr08', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '邵磊', '13833000008', 'shaolei@huaxin.example.com', 1, 0, NULL),
(32, 1, 'hr09', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '常雪', '13833000009', 'changxue@shulian.example.com', 1, 0, NULL),
(33, 1, 'hr10', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '钟毅', '13833000010', 'zhongyi@qiming.example.com', 1, 0, NULL),
(34, 1, 'hr11', '$2a$10$cWguMXjjJh1vYVAE34YdaODzl0uCf/XQD2FOmXGfHxvBhr7VlG80q', 'HR', '汤敏', '13833000011', 'tangmin@xinlian.example.com', 1, 0, NULL);

-- ---------- 教师可见范围（班主任/专业老师/届老师）----------
INSERT INTO teacher_scope (id, tenant_id, teacher_id, scope_type, scope_value) VALUES
(1, 1, 17, 'CLASS', '1'),
(2, 1, 18, 'MAJOR', '计算机科学与技术'),
(3, 1, 3, 'GRADE', '2022'),
(4, 2, 25, 'CLASS', '11');

-- ---------- 学生画像（student12/孙梦琪 故意无画像，测试“请先完善画像”提示）----------
INSERT INTO sys_student_profile (id, tenant_id, user_id, major, skills, expected_city, expected_industry, expected_salary_min, expected_salary_max, education_level) VALUES
(1, 1, 2, '计算机科学与技术', '["Java","Spring Boot","MySQL","Redis","Linux","Git"]', '杭州', '互联网/IT', 8000, 15000, '本科'),
(2, 1, 5, '软件工程', '["Java","MySQL","Vue","JavaScript","Git"]', '上海', '互联网/IT', 8000, 14000, '本科'),
(3, 1, 6, '数据科学与大数据技术', '["Python","SQL","Spark","Hadoop","数据分析"]', '北京', '大数据', 10000, 18000, '本科'),
(4, 1, 7, '人工智能', '["Python","机器学习","深度学习","PyTorch","SQL"]', '北京', '人工智能', 12000, 20000, '硕士'),
(5, 1, 8, '计算机科学与技术', '["C++","数据结构","Linux","Python"]', '深圳', '游戏', 10000, 16000, '本科'),
(6, 1, 9, '信息安全', '["Linux","Python","网络安全","Docker"]', '深圳', '互联网/IT', 9000, 15000, '本科'),
(7, 1, 10, '电子商务', '["数据分析","Excel","SQL","用户运营","文案策划"]', '杭州', '电子商务', 6000, 10000, '本科'),
(8, 1, 11, '软件工程', '["Java","Spring Boot","微服务","Redis","Kafka","Docker"]', '南京', '互联网/IT', 9000, 16000, '本科'),
(9, 1, 12, '统计学', '["SQL","Python","数理统计","数据分析","Tableau"]', '上海', '金融', 9000, 15000, '硕士'),
(10, 1, 13, '计算机应用技术', '["JavaScript","Vue","CSS","Git"]', '成都', '互联网/IT', 6000, 10000, '专科'),
(11, 1, 14, '教育技术学', '["产品设计","数据分析","项目管理","Axure"]', '北京', '教育', 7000, 12000, '本科'),
(12, 1, 15, '物联网工程', '["C++","单片机","Linux","Python"]', '苏州', '智能制造', 7000, 12000, '本科'),
(13, 2, 24, '软件工程', '["Java","MySQL","Vue"]', '武汉', '互联网/IT', 7000, 12000, '本科');

-- ---------- 学生简历（只覆盖部分学生：未建简历的用于测 HR 端空态）----------
INSERT INTO student_resume (id, tenant_id, user_id, contact_phone, contact_email, job_intention, self_intro, educations, projects, internships, honors) VALUES
(1, 1, 2, '13800000002', 'student@stu.test.edu.cn', 'Java后端开发工程师', '计算机科学与技术专业本科在读，熟悉 Java、Spring Boot、MySQL 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在杭州从事Java后端开发工程师相关工作。', '[{"school":"测试学院","major":"计算机科学与技术","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.35/4.0"}]', '[{"name":"计算机科学与技术综合实训 · Redis实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 Redis、Spring Boot、Git 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Redis","Spring Boot","Git"]},{"name":"校园数据分析平台","role":"核心开发","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 Java、MySQL 相关模块开发，负责其中约 40% 的编码量。","skills":["Java","MySQL"]}]', '[]', '["校级一等奖学金","全国大学生数学建模竞赛省二等奖"]'),
(2, 1, 5, '13811000001', 'chenjiayi@stu.test.edu.cn', '后端开发工程师', '软件工程专业本科在读，熟悉 Java、MySQL、Vue 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在上海从事后端开发工程师相关工作。', '[{"school":"测试学院","major":"软件工程","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.20/4.0"}]', '[{"name":"软件工程综合实训 · JavaScript实践项目","role":"核心开发","startDate":"2025-03","endDate":"2025-07","description":"使用 JavaScript、Git、MySQL 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["JavaScript","Git","MySQL"]}]', '[{"company":"金桥融信金融服务有限公司","position":"后端开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["国家励志奖学金","优秀学生干部","校级一等奖学金"]'),
(3, 1, 6, '13811000002', 'lihaoran@stu.test.edu.cn', '大数据开发工程师', '数据科学与大数据技术专业本科在读，熟悉 Python、SQL、Spark 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在北京从事大数据开发工程师相关工作。', '[{"school":"测试学院","major":"数据科学与大数据技术","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.44/4.0"}]', '[{"name":"数据科学与大数据技术综合实训 · Hadoop实践项目","role":"负责人","startDate":"2025-03","endDate":"2025-07","description":"使用 Hadoop、数据分析、Python 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Hadoop","数据分析","Python"]},{"name":"校园数据分析平台","role":"核心开发","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 Spark、Hadoop 相关模块开发，负责其中约 40% 的编码量。","skills":["Spark","Hadoop"]}]', '[{"company":"康桥医疗信息科技有限公司","position":"大数据开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["ACM-ICPC 省赛二等奖","国家励志奖学金"]'),
(4, 1, 7, '13811000003', 'wangyutong@stu.test.edu.cn', '算法工程师', '人工智能专业硕士在读，熟悉 Python、机器学习、深度学习 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在北京从事算法工程师相关工作。', '[{"school":"测试学院","major":"人工智能","degree":"硕士","startDate":"2023-09","endDate":"2026-06","gpa":"3.80/4.0"}]', '[{"name":"人工智能综合实训 · PyTorch实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 PyTorch、深度学习、SQL 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["PyTorch","深度学习","SQL"]},{"name":"校园数据分析平台","role":"组员","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 SQL、机器学习 相关模块开发，负责其中约 40% 的编码量。","skills":["SQL","机器学习"]}]', '[{"company":"联达自动化技术有限公司","position":"算法工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["ACM-ICPC 省赛二等奖","优秀学生干部","“互联网+”大学生创新创业大赛校赛金奖"]'),
(5, 1, 8, '13811000004', 'zhangzimo@stu.test.edu.cn', 'Java后端开发工程师', '计算机科学与技术专业本科在读，熟悉 C++、数据结构、Linux 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在深圳从事Java后端开发工程师相关工作。', '[{"school":"测试学院","major":"计算机科学与技术","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.71/4.0"}]', '[{"name":"计算机科学与技术综合实训 · 数据结构实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 数据结构、Linux、C++ 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["数据结构","Linux","C++"]}]', '[{"company":"优选电商集团有限公司","position":"Java后端开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["CET-6（546 分）","软件设计师（中级）","校级一等奖学金"]'),
(6, 1, 9, '13811000005', 'liusiyuan@stu.test.edu.cn', '安全工程师', '信息安全专业本科在读，熟悉 Linux、Python、网络安全 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在深圳从事安全工程师相关工作。', '[{"school":"测试学院","major":"信息安全","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.44/4.0"}]', '[{"name":"信息安全综合实训 · Linux实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 Linux、网络安全、Python 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Linux","网络安全","Python"]}]', '[{"company":"数联云图大数据有限公司","position":"安全工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["国家励志奖学金","软件设计师（中级）"]'),
(7, 1, 10, '13811000006', 'yangxinran@stu.test.edu.cn', '数据分析师', '电子商务专业本科在读，熟悉 数据分析、Excel、SQL 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在杭州从事数据分析师相关工作。', '[{"school":"测试学院","major":"电子商务","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.54/4.0"}]', '[{"name":"电子商务综合实训 · 文案策划实践项目","role":"负责人","startDate":"2025-03","endDate":"2025-07","description":"使用 文案策划、用户运营、Excel 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["文案策划","用户运营","Excel"]}]', '[]', '["蓝桥杯省赛一等奖"]'),
(8, 1, 11, '13811000007', 'zhaoyiming@stu.test.edu.cn', '后端开发工程师', '软件工程专业本科在读，熟悉 Java、Spring Boot、微服务 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在南京从事后端开发工程师相关工作。', '[{"school":"测试学院","major":"软件工程","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.28/4.0"}]', '[{"name":"软件工程综合实训 · Kafka实践项目","role":"负责人","startDate":"2025-03","endDate":"2025-07","description":"使用 Kafka、Java、微服务 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["Kafka","Java","微服务"]},{"name":"校园信息服务平台","role":"组员","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 Redis、Docker 相关模块开发，负责其中约 40% 的编码量。","skills":["Redis","Docker"]}]', '[]', '["全国大学生数学建模竞赛省二等奖","CET-6（546 分）"]'),
(9, 1, 12, '13811000008', 'huangshihan@stu.test.edu.cn', '数据分析师', '统计学专业硕士在读，熟悉 SQL、Python、数理统计 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在上海从事数据分析师相关工作。', '[{"school":"测试学院","major":"统计学","degree":"硕士","startDate":"2023-09","endDate":"2026-06","gpa":"3.60/4.0"}]', '[{"name":"统计学综合实训 · 数据分析实践项目","role":"后端开发","startDate":"2025-03","endDate":"2025-07","description":"使用 数据分析、数理统计、Tableau 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["数据分析","数理统计","Tableau"]},{"name":"校园信息服务平台","role":"组员","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 Tableau、Python 相关模块开发，负责其中约 40% 的编码量。","skills":["Tableau","Python"]}]', '[{"company":"康桥医疗信息科技有限公司","position":"数据分析师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["国家励志奖学金","蓝桥杯省赛一等奖"]'),
(10, 1, 13, '13811000009', 'zhoujunjie@stu.test.edu.cn', '前端开发工程师', '计算机应用技术专业专科在读，熟悉 JavaScript、Vue、CSS 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在成都从事前端开发工程师相关工作。', '[{"school":"测试学院","major":"计算机应用技术","degree":"专科","startDate":"2022-09","endDate":"2026-06","gpa":"3.35/4.0"}]', '[{"name":"计算机应用技术综合实训 · CSS实践项目","role":"数据处理","startDate":"2025-03","endDate":"2025-07","description":"使用 CSS、Vue、Git 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["CSS","Vue","Git"]},{"name":"校园数据分析平台","role":"组员","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 CSS、Git 相关模块开发，负责其中约 40% 的编码量。","skills":["CSS","Git"]}]', '[{"company":"优选电商集团有限公司","position":"前端开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["ACM-ICPC 省赛二等奖","“互联网+”大学生创新创业大赛校赛金奖"]'),
(11, 1, 14, '13811000010', 'wuyajing@stu.test.edu.cn', '产品经理', '教育技术学专业本科在读，熟悉 产品设计、数据分析、项目管理 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在北京从事产品经理相关工作。', '[{"school":"测试学院","major":"教育技术学","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.03/4.0"}]', '[{"name":"教育技术学综合实训 · 产品设计实践项目","role":"数据处理","startDate":"2025-03","endDate":"2025-07","description":"使用 产品设计、数据分析、Axure 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["产品设计","数据分析","Axure"]},{"name":"校园竞赛训练平台","role":"核心开发","startDate":"2024-09","endDate":"2025-01","description":"参与需求梳理与 Axure、项目管理 相关模块开发，负责其中约 40% 的编码量。","skills":["Axure","项目管理"]}]', '[{"company":"汇通证券数据服务有限公司","position":"产品经理实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["全国大学生数学建模竞赛省二等奖"]'),
(12, 1, 15, '13811000011', 'xuhaoyu@stu.test.edu.cn', '嵌入式开发工程师', '物联网工程专业本科在读，熟悉 C++、单片机、Linux 等技术，有完整项目开发经验，具备良好的工程习惯与团队协作能力。期望在苏州从事嵌入式开发工程师相关工作。', '[{"school":"测试学院","major":"物联网工程","degree":"本科","startDate":"2022-09","endDate":"2026-06","gpa":"3.07/4.0"}]', '[{"name":"物联网工程综合实训 · 单片机实践项目","role":"数据处理","startDate":"2025-03","endDate":"2025-07","description":"使用 单片机、C++、Linux 搭建完整链路，独立完成核心模块设计与实现，项目在课程答辩中获评优秀，代码已开源。","skills":["单片机","C++","Linux"]}]', '[{"company":"金桥融信金融服务有限公司","position":"嵌入式开发工程师实习生","startDate":"2025-07","endDate":"2025-09","description":"参与线上业务模块迭代，独立承担 2 个需求的开发与自测，代码通过 Code Review 合入主干。"}]', '["软件设计师（中级）","ACM-ICPC 省赛二等奖","国家励志奖学金"]');

-- ---------- 采集任务 ----------
INSERT INTO crawler_task (id, tenant_id, source_type, source_name, url_pattern, cron_expr, status, create_time) VALUES
(1, 1, 'MOCK', '模拟采集-mock-jobs.json', 'mock-jobs.json', '0 0 2 * * ?', 0, '2026-06-18 10:00:00'),
(2, 1, 'OFFICIAL_PUBLIC', '官方公开招聘公告示例', 'url=https://example.gov.cn/jobs/&maxItems=20', NULL, 0, '2026-06-25 14:20:00'),
(3, 2, 'MOCK', '模拟采集-示范大学', 'mock-jobs.json', NULL, 0, '2026-07-03 11:00:00');

-- ---------- 采集日志（开箱只有少量采集历史：1 次成功采到 5 条待清洗 + 1 次失败示例 + 1 条运行中）----------
INSERT INTO crawler_log (id, task_id, tenant_id, start_time, end_time, record_count, status, error_msg) VALUES
(1, 1, 1, '2026-07-08 02:00:00', '2026-07-08 02:03:20', 5, 'SUCCESS', NULL),
(2, 2, 1, '2026-06-21 03:00:00', '2026-06-21 03:02:14', 0, 'FAILED', '连接目标站点超时（已重试 3 次）：目标站点触发反爬验证'),
(3, 4, 1, '2026-07-09 04:00:00', NULL, 0, 'RUNNING', NULL);

-- ---------- 原始职位数据（仅 3 条待清洗 + 2 条脏数据，供「存量清洗」与容错演示；种子不再预置 MOCK 归档）----------
INSERT INTO raw_job_data (id, source, source_url, raw_content, fetch_time, status) VALUES
(1, 'MOCK', 'https://mock.occupation.dev/pending/1', '{"title":"Python爬虫开发工程师","company":"数联云图大数据有限公司","city":"广州市","industry":"大数据","salaryMin":9000,"salaryMax":14000,"education":"本科","experience":"1-3年","skills":["Python","Linux","MySQL"],"description":"负责数据采集系统开发与维护。","publishDate":"2026-07-06"}', '2026-07-08 02:20:00', 'RAW'),
(2, 'MOCK', 'https://mock.occupation.dev/pending/2', '{"title":"云计算运维工程师","company":"华信云科技有限公司","city":"杭州市","industry":"互联网/IT","salaryMin":10000,"salaryMax":16000,"education":"本科","experience":"3-5年","skills":["Linux","Docker","Kubernetes"],"description":"负责云平台的部署、监控与故障处理。","publishDate":"2026-07-07"}', '2026-07-08 02:20:00', 'RAW'),
(3, 'MOCK', 'https://mock.occupation.dev/pending/3', '{"title":"数据标注专员","company":"博睿人工智能研究院有限公司","city":"西安市","industry":"人工智能","salaryMin":5000,"salaryMax":7000,"education":"专科","experience":"经验不限","skills":["Excel","数据标注"],"description":"负责 AI 训练数据的标注与质检。","publishDate":"2026-07-08"}', '2026-07-08 02:20:00', 'RAW'),
(4, 'MOCK', 'https://mock.occupation.dev/dirty/4', '{title:非法JSON数据,,,', '2026-07-08 02:21:00', 'RAW'),
(5, 'MOCK', 'https://mock.occupation.dev/dirty/5', '{"title":"缺少公司名的职位","city":"北京","salaryMin":8000,"salaryMax":12000}', '2026-07-08 02:22:00', 'RAW');

-- ---------- 清洗后职位 ----------
INSERT INTO job_detail (id, title, company, city, industry, salary_min, salary_max, education, experience, skills, description, publish_date, source, source_url, publisher_id, create_time) VALUES
(1, '测试开发工程师', '云聘互联科技有限公司', '杭州', '互联网/IT', 12000, 17500, '本科', '3-5年', '["Python","Selenium","Linux","MySQL","团队协作"]', '【企业直招】云聘互联科技有限公司招聘测试开发工程师，工作地点杭州。本科及以上学历，工作经验3-5年；要求熟悉 Python、Selenium、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-23', 'HR_PUBLISH', NULL, 4, '2026-02-23 14:30:00'),
(2, '大数据开发工程师', '恒信数据技术有限公司', '上海', '大数据', 9000, 13000, '专科', '经验不限', '["Java","Spark","Hadoop","Hive"]', '【企业直招】恒信数据技术有限公司招聘大数据开发工程师，工作地点上海。专科及以上学历，经验不限；要求熟悉 Java、Spark、Hadoop 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-09', 'HR_PUBLISH', NULL, 19, '2026-02-09 18:30:00'),
(3, '教学数据分析师', '智汇未来教育科技有限公司', '广州', '教育', 5000, 7000, '本科', '应届生', '["SQL","Excel","数据分析"]', '【企业直招】智汇未来教育科技有限公司招聘教学数据分析师，工作地点广州。本科及以上学历，工作经验应届生；要求熟悉 SQL、Excel、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-05', 'HR_PUBLISH', NULL, 20, '2026-02-05 14:30:00'),
(4, '计算机视觉工程师', '博睿人工智能研究院有限公司', '上海', '人工智能', 12500, 18000, '本科', '应届生', '["Python","计算机视觉","深度学习","TensorFlow"]', '【企业直招】博睿人工智能研究院有限公司招聘计算机视觉工程师，工作地点上海。本科及以上学历，工作经验应届生；要求熟悉 Python、计算机视觉、深度学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-15', 'HR_PUBLISH', NULL, 26, '2026-02-15 15:30:00'),
(5, '量化研究员', '金桥融信金融服务有限公司', '上海', '金融', 16000, 23000, '本科', '经验不限', '["Python","C++","机器学习","数理统计"]', '【企业直招】金桥融信金融服务有限公司招聘量化研究员，工作地点上海。本科及以上学历，经验不限；要求熟悉 Python、C++、机器学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-22', 'HR_PUBLISH', NULL, 27, '2026-02-22 14:30:00'),
(6, '用户增长运营', '优选电商集团有限公司', '西安', '电子商务', 4500, 6500, '本科', '应届生', '["用户运营","数据分析","文案策划"]', '【企业直招】优选电商集团有限公司招聘用户增长运营，工作地点西安。本科及以上学历，工作经验应届生；要求熟悉 用户运营、数据分析、文案策划 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-13', 'HR_PUBLISH', NULL, 28, '2026-02-13 14:30:00'),
(7, '游戏客户端开发', '乐游互动娱乐有限公司', '广州', '游戏', 15000, 22500, '本科', '3-5年', '["C++","Unity","数据结构"]', '【企业直招】乐游互动娱乐有限公司招聘游戏客户端开发，工作地点广州。本科及以上学历，工作经验3-5年；要求熟悉 C++、Unity、数据结构 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-12', 'HR_PUBLISH', NULL, 29, '2026-02-12 19:30:00'),
(8, '自动化测试工程师', '精工智造装备股份有限公司', '武汉', '智能制造', 7000, 10000, '本科', '1-3年', '["Python","Linux","Selenium"]', '【企业直招】精工智造装备股份有限公司招聘自动化测试工程师，工作地点武汉。本科及以上学历，工作经验1-3年；要求熟悉 Python、Linux、Selenium 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-28', 'HR_PUBLISH', NULL, 30, '2026-02-28 17:30:00'),
(9, '后端开发工程师', '华信云科技有限公司', '上海', '互联网/IT', 9000, 13000, '本科', '经验不限', '["Java","MySQL","Redis","分布式","Linux"]', '【企业直招】华信云科技有限公司招聘后端开发工程师，工作地点上海。本科及以上学历，经验不限；要求熟悉 Java、MySQL、Redis 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-21', 'HR_PUBLISH', NULL, 31, '2026-02-21 18:30:00'),
(10, '大数据开发工程师', '数联云图大数据有限公司', '北京', '大数据', 21000, 31000, '硕士', '3-5年', '["Java","Spark","Hadoop","Hive"]', '【企业直招】数联云图大数据有限公司招聘大数据开发工程师，工作地点北京。硕士及以上学历，工作经验3-5年；要求熟悉 Java、Spark、Hadoop 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-02-21', 'HR_PUBLISH', NULL, 32, '2026-02-21 19:30:00'),
(11, '测试开发工程师', '启明星辰网络技术有限公司', '杭州', '互联网/IT', 10000, 15000, '硕士', '1-3年', '["Python","Selenium","Linux","MySQL"]', '【企业直招】启明星辰网络技术有限公司招聘测试开发工程师，工作地点杭州。硕士及以上学历，工作经验1-3年；要求熟悉 Python、Selenium、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-20', 'HR_PUBLISH', NULL, 33, '2026-03-20 13:30:00'),
(12, '嵌入式软件工程师', '芯联半导体科技有限公司', '北京', '智能制造', 16000, 23500, '硕士', '3-5年', '["C++","Linux","单片机"]', '【企业直招】芯联半导体科技有限公司招聘嵌入式软件工程师，工作地点北京。硕士及以上学历，工作经验3-5年；要求熟悉 C++、Linux、单片机 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-24', 'HR_PUBLISH', NULL, 34, '2026-03-24 19:30:00'),
(13, '前端开发工程师', '云聘互联科技有限公司', '武汉', '互联网/IT', 6500, 9000, '专科', '1-3年', '["JavaScript","Vue","CSS","TypeScript"]', '【企业直招】云聘互联科技有限公司招聘前端开发工程师，工作地点武汉。专科及以上学历，工作经验1-3年；要求熟悉 JavaScript、Vue、CSS 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-24', 'HR_PUBLISH', NULL, 4, '2026-03-24 17:30:00'),
(14, '大数据开发工程师', '恒信数据技术有限公司', '杭州', '大数据', 16500, 24000, '本科', '3-5年', '["Java","Spark","Hadoop","Hive"]', '【企业直招】恒信数据技术有限公司招聘大数据开发工程师，工作地点杭州。本科及以上学历，工作经验3-5年；要求熟悉 Java、Spark、Hadoop 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-24', 'HR_PUBLISH', NULL, 19, '2026-03-24 19:30:00'),
(15, '课程研发工程师', '智汇未来教育科技有限公司', '苏州', '教育', 5500, 8000, '本科', '应届生', '["Java","Vue","MySQL","Git"]', '【企业直招】智汇未来教育科技有限公司招聘课程研发工程师，工作地点苏州。本科及以上学历，工作经验应届生；要求熟悉 Java、Vue、MySQL 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-16', 'HR_PUBLISH', NULL, 20, '2026-03-16 12:30:00'),
(16, '计算机视觉工程师', '博睿人工智能研究院有限公司', '广州', '人工智能', 21500, 32000, '硕士', '3-5年', '["Python","计算机视觉","深度学习","TensorFlow"]', '【企业直招】博睿人工智能研究院有限公司招聘计算机视觉工程师，工作地点广州。硕士及以上学历，工作经验3-5年；要求熟悉 Python、计算机视觉、深度学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-04', 'HR_PUBLISH', NULL, 26, '2026-03-04 18:30:00'),
(17, '量化研究员', '金桥融信金融服务有限公司', '北京', '金融', 32500, 48000, '硕士', '3-5年', '["Python","C++","机器学习","数理统计"]', '【企业直招】金桥融信金融服务有限公司招聘量化研究员，工作地点北京。硕士及以上学历，工作经验3-5年；要求熟悉 Python、C++、机器学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-03', 'HR_PUBLISH', NULL, 27, '2026-03-03 17:30:00'),
(18, '产品经理', '优选电商集团有限公司', '北京', '电子商务', 8500, 12500, '本科', '应届生', '["产品设计","Axure","数据分析","项目管理"]', '【企业直招】优选电商集团有限公司招聘产品经理，工作地点北京。本科及以上学历，工作经验应届生；要求熟悉 产品设计、Axure、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-27', 'HR_PUBLISH', NULL, 28, '2026-03-27 19:30:00'),
(19, '游戏服务端开发', '乐游互动娱乐有限公司', '北京', '游戏', 12500, 18500, '硕士', '应届生', '["Go","Redis","MySQL","分布式"]', '【企业直招】乐游互动娱乐有限公司招聘游戏服务端开发，工作地点北京。硕士及以上学历，工作经验应届生；要求熟悉 Go、Redis、MySQL 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-25', 'HR_PUBLISH', NULL, 29, '2026-03-25 12:30:00'),
(20, '工业软件开发工程师', '精工智造装备股份有限公司', '北京', '智能制造', 17500, 26000, '硕士', '3-5年', '["Java","MySQL","Linux"]', '【企业直招】精工智造装备股份有限公司招聘工业软件开发工程师，工作地点北京。硕士及以上学历，工作经验3-5年；要求熟悉 Java、MySQL、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-19', 'HR_PUBLISH', NULL, 30, '2026-03-19 17:30:00'),
(21, '测试开发工程师', '华信云科技有限公司', '北京', '互联网/IT', 6000, 9000, '不限', '应届生', '["Python","Selenium","Linux","MySQL"]', '【企业直招】华信云科技有限公司招聘测试开发工程师，工作地点北京。学历不限，工作经验应届生；要求熟悉 Python、Selenium、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-13', 'HR_PUBLISH', NULL, 31, '2026-03-13 17:30:00'),
(22, '数据分析师', '数联云图大数据有限公司', '深圳', '大数据', 16500, 24000, '本科', '5-10年', '["SQL","Python","数据分析","Tableau","Linux"]', '【企业直招】数联云图大数据有限公司招聘数据分析师，工作地点深圳。本科及以上学历，工作经验5-10年；要求熟悉 SQL、Python、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-03-02', 'HR_PUBLISH', NULL, 32, '2026-03-02 12:30:00'),
(23, '前端开发工程师', '启明星辰网络技术有限公司', '北京', '互联网/IT', 8500, 12000, '本科', '经验不限', '["JavaScript","Vue","CSS","TypeScript","Git"]', '【企业直招】启明星辰网络技术有限公司招聘前端开发工程师，工作地点北京。本科及以上学历，经验不限；要求熟悉 JavaScript、Vue、CSS 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-09', 'HR_PUBLISH', NULL, 33, '2026-04-09 18:30:00'),
(24, '工业软件开发工程师', '芯联半导体科技有限公司', '北京', '智能制造', 7000, 10500, '专科', '应届生', '["Java","MySQL","Linux"]', '【企业直招】芯联半导体科技有限公司招聘工业软件开发工程师，工作地点北京。专科及以上学历，工作经验应届生；要求熟悉 Java、MySQL、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-28', 'HR_PUBLISH', NULL, 34, '2026-04-28 12:30:00'),
(25, '全栈开发工程师', '云聘互联科技有限公司', '广州', '互联网/IT', 14000, 20500, '本科', '3-5年', '["JavaScript","Vue","Java","MySQL"]', '【企业直招】云聘互联科技有限公司招聘全栈开发工程师，工作地点广州。本科及以上学历，工作经验3-5年；要求熟悉 JavaScript、Vue、Java 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-07', 'HR_PUBLISH', NULL, 4, '2026-04-07 19:30:00'),
(26, '数据仓库工程师', '恒信数据技术有限公司', '上海', '大数据', 10500, 15000, '专科', '1-3年', '["SQL","Hive","Spark","数据建模"]', '【企业直招】恒信数据技术有限公司招聘数据仓库工程师，工作地点上海。专科及以上学历，工作经验1-3年；要求熟悉 SQL、Hive、Spark 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-08', 'HR_PUBLISH', NULL, 19, '2026-04-08 14:30:00'),
(27, '教学数据分析师', '智汇未来教育科技有限公司', '上海', '教育', 10500, 15500, '硕士', '1-3年', '["SQL","Excel","数据分析"]', '【企业直招】智汇未来教育科技有限公司招聘教学数据分析师，工作地点上海。硕士及以上学历，工作经验1-3年；要求熟悉 SQL、Excel、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-01', 'HR_PUBLISH', NULL, 20, '2026-04-01 17:30:00'),
(28, 'NLP算法工程师', '博睿人工智能研究院有限公司', '北京', '人工智能', 27000, 39500, '专科', '5-10年', '["Python","深度学习","NLP","PyTorch"]', '【企业直招】博睿人工智能研究院有限公司招聘NLP算法工程师，工作地点北京。专科及以上学历，工作经验5-10年；要求熟悉 Python、深度学习、NLP 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-06', 'HR_PUBLISH', NULL, 26, '2026-04-06 18:30:00'),
(29, '金融数据分析师', '金桥融信金融服务有限公司', '南京', '金融', 7000, 10000, '本科', '经验不限', '["SQL","Python","数据分析","Excel"]', '【企业直招】金桥融信金融服务有限公司招聘金融数据分析师，工作地点南京。本科及以上学历，经验不限；要求熟悉 SQL、Python、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-13', 'HR_PUBLISH', NULL, 27, '2026-04-13 12:30:00'),
(30, '产品经理', '优选电商集团有限公司', '杭州', '电子商务', 14500, 21000, '本科', '3-5年', '["产品设计","Axure","数据分析","项目管理"]', '【企业直招】优选电商集团有限公司招聘产品经理，工作地点杭州。本科及以上学历，工作经验3-5年；要求熟悉 产品设计、Axure、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-16', 'HR_PUBLISH', NULL, 28, '2026-04-16 13:30:00'),
(31, '游戏客户端开发', '乐游互动娱乐有限公司', '深圳', '游戏', 21500, 32000, '硕士', '3-5年', '["C++","Unity","数据结构"]', '【企业直招】乐游互动娱乐有限公司招聘游戏客户端开发，工作地点深圳。硕士及以上学历，工作经验3-5年；要求熟悉 C++、Unity、数据结构 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-01', 'HR_PUBLISH', NULL, 29, '2026-04-01 19:30:00'),
(32, '嵌入式软件工程师', '精工智造装备股份有限公司', '成都', '智能制造', 6000, 9000, '专科', '经验不限', '["C++","Linux","单片机"]', '【企业直招】精工智造装备股份有限公司招聘嵌入式软件工程师，工作地点成都。专科及以上学历，经验不限；要求熟悉 C++、Linux、单片机 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-02', 'HR_PUBLISH', NULL, 30, '2026-04-02 15:30:00'),
(33, '测试开发工程师', '华信云科技有限公司', '成都', '互联网/IT', 9000, 13000, '本科', '3-5年', '["Python","Selenium","Linux","MySQL"]', '【企业直招】华信云科技有限公司招聘测试开发工程师，工作地点成都。本科及以上学历，工作经验3-5年；要求熟悉 Python、Selenium、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-04', 'HR_PUBLISH', NULL, 31, '2026-04-04 16:30:00'),
(34, '数据仓库工程师', '数联云图大数据有限公司', '西安', '大数据', 9500, 13500, '本科', '1-3年', '["SQL","Hive","Spark","数据建模","Linux"]', '【企业直招】数联云图大数据有限公司招聘数据仓库工程师，工作地点西安。本科及以上学历，工作经验1-3年；要求熟悉 SQL、Hive、Spark 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-08', 'HR_PUBLISH', NULL, 32, '2026-04-08 13:30:00'),
(35, '运维开发工程师', '启明星辰网络技术有限公司', '广州', '互联网/IT', 6500, 9500, '本科', '应届生', '["Linux","Docker","Kubernetes","Python","Git"]', '【企业直招】启明星辰网络技术有限公司招聘运维开发工程师，工作地点广州。本科及以上学历，工作经验应届生；要求熟悉 Linux、Docker、Kubernetes 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-15', 'HR_PUBLISH', NULL, 33, '2026-04-15 11:30:00'),
(36, '工业软件开发工程师', '恒信数据技术有限公司', '北京', '智能制造', 15000, 22000, '不限', '5-10年', '["Java","MySQL","Linux"]', '【企业直招】恒信数据技术有限公司招聘工业软件开发工程师，工作地点北京。学历不限，工作经验5-10年；要求熟悉 Java、MySQL、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-04-17', 'HR_PUBLISH', NULL, 34, '2026-04-17 13:30:00'),
(37, 'Java开发工程师', '云聘互联科技有限公司', '深圳', '互联网/IT', 19000, 28000, '本科', '5-10年', '["Java","Spring Boot","MySQL","Redis","微服务"]', '【企业直招】云聘互联科技有限公司招聘Java开发工程师，工作地点深圳。本科及以上学历，工作经验5-10年；要求熟悉 Java、Spring Boot、MySQL 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-10', 'HR_PUBLISH', NULL, 4, '2026-05-10 16:30:00'),
(38, '数据仓库工程师', '恒信数据技术有限公司', '杭州', '大数据', 19500, 28500, '硕士', '3-5年', '["SQL","Hive","Spark","数据建模"]', '【企业直招】恒信数据技术有限公司招聘数据仓库工程师，工作地点杭州。硕士及以上学历，工作经验3-5年；要求熟悉 SQL、Hive、Spark 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-06', 'HR_PUBLISH', NULL, 19, '2026-05-06 12:30:00'),
(39, '课程研发工程师', '智汇未来教育科技有限公司', '深圳', '教育', 12500, 18000, '本科', '3-5年', '["Java","Vue","MySQL"]', '【企业直招】智汇未来教育科技有限公司招聘课程研发工程师，工作地点深圳。本科及以上学历，工作经验3-5年；要求熟悉 Java、Vue、MySQL 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-12', 'HR_PUBLISH', NULL, 20, '2026-05-12 16:30:00'),
(40, '算法工程师', '博睿人工智能研究院有限公司', '武汉', '人工智能', 10500, 15500, '本科', '经验不限', '["Python","机器学习","数据结构","C++"]', '【企业直招】博睿人工智能研究院有限公司招聘算法工程师，工作地点武汉。本科及以上学历，经验不限；要求熟悉 Python、机器学习、数据结构 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-01', 'HR_PUBLISH', NULL, 26, '2026-05-01 15:30:00'),
(41, '量化研究员', '华信云科技有限公司', '上海', '金融', 20500, 30000, '不限', '3-5年', '["Python","C++","机器学习","数理统计"]', '【企业直招】华信云科技有限公司招聘量化研究员，工作地点上海。学历不限，工作经验3-5年；要求熟悉 Python、C++、机器学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-14', 'HR_PUBLISH', NULL, 27, '2026-05-14 15:30:00'),
(42, '产品经理', '云聘互联科技有限公司', '北京', '电子商务', 9000, 13500, '专科', '1-3年', '["产品设计","Axure","数据分析","项目管理","Linux"]', '【企业直招】云聘互联科技有限公司招聘产品经理，工作地点北京。专科及以上学历，工作经验1-3年；要求熟悉 产品设计、Axure、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-21', 'HR_PUBLISH', NULL, 28, '2026-05-21 17:30:00'),
(43, '游戏数值策划', '乐游互动娱乐有限公司', '南京', '游戏', 7000, 10000, '本科', '经验不限', '["Excel","数据分析","数理统计","团队协作"]', '【企业直招】乐游互动娱乐有限公司招聘游戏数值策划，工作地点南京。本科及以上学历，经验不限；要求熟悉 Excel、数据分析、数理统计 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-25', 'HR_PUBLISH', NULL, 29, '2026-05-25 17:30:00'),
(44, '自动化测试工程师', '精工智造装备股份有限公司', '广州', '智能制造', 6500, 9500, '硕士', '应届生', '["Python","Linux","Selenium"]', '【企业直招】精工智造装备股份有限公司招聘自动化测试工程师，工作地点广州。硕士及以上学历，工作经验应届生；要求熟悉 Python、Linux、Selenium 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-24', 'HR_PUBLISH', NULL, 30, '2026-05-24 13:30:00'),
(45, '运维开发工程师', '华信云科技有限公司', '西安', '互联网/IT', 6000, 8500, '本科', '应届生', '["Linux","Docker","Kubernetes","Python"]', '【企业直招】华信云科技有限公司招聘运维开发工程师，工作地点西安。本科及以上学历，工作经验应届生；要求熟悉 Linux、Docker、Kubernetes 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-18', 'HR_PUBLISH', NULL, 31, '2026-05-18 11:30:00'),
(46, '实时计算工程师', '数联云图大数据有限公司', '北京', '大数据', 11000, 16500, '本科', '应届生', '["Java","Flink","Kafka","Redis"]', '【企业直招】数联云图大数据有限公司招聘实时计算工程师，工作地点北京。本科及以上学历，工作经验应届生；要求熟悉 Java、Flink、Kafka 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-09', 'HR_PUBLISH', NULL, 32, '2026-05-09 12:30:00'),
(47, 'Go开发工程师', '启明星辰网络技术有限公司', '北京', '互联网/IT', 15500, 22500, '专科', '3-5年', '["Go","MySQL","Redis","微服务"]', '【企业直招】启明星辰网络技术有限公司招聘Go开发工程师，工作地点北京。专科及以上学历，工作经验3-5年；要求熟悉 Go、MySQL、Redis 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-09', 'HR_PUBLISH', NULL, 33, '2026-05-09 15:30:00'),
(48, '自动化测试工程师', '芯联半导体科技有限公司', '武汉', '智能制造', 10000, 14500, '本科', '3-5年', '["Python","Linux","Selenium","团队协作"]', '【企业直招】芯联半导体科技有限公司招聘自动化测试工程师，工作地点武汉。本科及以上学历，工作经验3-5年；要求熟悉 Python、Linux、Selenium 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-20', 'HR_PUBLISH', NULL, 34, '2026-05-20 19:30:00'),
(49, '全栈开发工程师', '云聘互联科技有限公司', '上海', '互联网/IT', 19000, 28000, '专科', '5-10年', '["JavaScript","Vue","Java","MySQL"]', '【企业直招】云聘互联科技有限公司招聘全栈开发工程师，工作地点上海。专科及以上学历，工作经验5-10年；要求熟悉 JavaScript、Vue、Java 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-12', 'HR_PUBLISH', NULL, 4, '2026-05-12 16:30:00'),
(50, '数据分析师', '恒信数据技术有限公司', '武汉', '大数据', 11000, 16000, '本科', '3-5年', '["SQL","Python","数据分析","Tableau"]', '【企业直招】恒信数据技术有限公司招聘数据分析师，工作地点武汉。本科及以上学历，工作经验3-5年；要求熟悉 SQL、Python、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-19', 'HR_PUBLISH', NULL, 19, '2026-05-19 13:30:00'),
(51, '在线教育产品经理', '智汇未来教育科技有限公司', '上海', '教育', 7000, 10000, '本科', '应届生', '["产品设计","数据分析","项目管理"]', '【企业直招】智汇未来教育科技有限公司招聘在线教育产品经理，工作地点上海。本科及以上学历，工作经验应届生；要求熟悉 产品设计、数据分析、项目管理 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-08', 'HR_PUBLISH', NULL, 20, '2026-05-08 18:30:00'),
(52, '算法工程师', '博睿人工智能研究院有限公司', '深圳', '人工智能', 18500, 27000, '本科', '3-5年', '["Python","机器学习","数据结构","C++"]', '【企业直招】博睿人工智能研究院有限公司招聘算法工程师，工作地点深圳。本科及以上学历，工作经验3-5年；要求熟悉 Python、机器学习、数据结构 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-05-04', 'HR_PUBLISH', NULL, 26, '2026-05-04 12:30:00'),
(53, '量化研究员', '数联云图大数据有限公司', '北京', '金融', 36000, 52500, '博士', '3-5年', '["Python","C++","机器学习","数理统计"]', '【企业直招】数联云图大数据有限公司招聘量化研究员，工作地点北京。博士及以上学历，工作经验3-5年；要求熟悉 Python、C++、机器学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-02', 'HR_PUBLISH', NULL, 27, '2026-06-02 18:30:00'),
(54, '电商数据运营', '优选电商集团有限公司', '上海', '电子商务', 11000, 16000, '本科', '3-5年', '["数据分析","Excel","SQL","用户运营"]', '【企业直招】优选电商集团有限公司招聘电商数据运营，工作地点上海。本科及以上学历，工作经验3-5年；要求熟悉 数据分析、Excel、SQL 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-19', 'HR_PUBLISH', NULL, 28, '2026-06-19 12:30:00'),
(55, '游戏客户端开发', '云聘互联科技有限公司', '深圳', '游戏', 31500, 46500, '博士', '5-10年', '["C++","Unity","数据结构","团队协作"]', '【企业直招】云聘互联科技有限公司招聘游戏客户端开发，工作地点深圳。博士及以上学历，工作经验5-10年；要求熟悉 C++、Unity、数据结构 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-24', 'HR_PUBLISH', NULL, 29, '2026-06-24 15:30:00'),
(56, '自动化测试工程师', '精工智造装备股份有限公司', '上海', '智能制造', 6000, 9000, '不限', '应届生', '["Python","Linux","Selenium"]', '【企业直招】精工智造装备股份有限公司招聘自动化测试工程师，工作地点上海。学历不限，工作经验应届生；要求熟悉 Python、Linux、Selenium 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-13', 'HR_PUBLISH', NULL, 30, '2026-06-13 14:30:00'),
(57, '前端开发工程师', '华信云科技有限公司', '杭州', '互联网/IT', 8500, 12500, '本科', '1-3年', '["JavaScript","Vue","CSS","TypeScript"]', '【企业直招】华信云科技有限公司招聘前端开发工程师，工作地点杭州。本科及以上学历，工作经验1-3年；要求熟悉 JavaScript、Vue、CSS 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-24', 'HR_PUBLISH', NULL, 31, '2026-06-24 19:30:00'),
(58, '数据分析师', '数联云图大数据有限公司', '杭州', '大数据', 8000, 12000, '本科', '1-3年', '["SQL","Python","数据分析","Tableau"]', '【企业直招】数联云图大数据有限公司招聘数据分析师，工作地点杭州。本科及以上学历，工作经验1-3年；要求熟悉 SQL、Python、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-09', 'HR_PUBLISH', NULL, 32, '2026-06-09 16:30:00'),
(59, 'Go开发工程师', '启明星辰网络技术有限公司', '广州', '互联网/IT', 8000, 12000, '不限', '经验不限', '["Go","MySQL","Redis","微服务"]', '【企业直招】启明星辰网络技术有限公司招聘Go开发工程师，工作地点广州。学历不限，经验不限；要求熟悉 Go、MySQL、Redis 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-09', 'HR_PUBLISH', NULL, 33, '2026-06-09 19:30:00'),
(60, '嵌入式软件工程师', '芯联半导体科技有限公司', '广州', '智能制造', 6500, 9500, '不限', '经验不限', '["C++","Linux","单片机"]', '【企业直招】芯联半导体科技有限公司招聘嵌入式软件工程师，工作地点广州。学历不限，经验不限；要求熟悉 C++、Linux、单片机 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-04', 'HR_PUBLISH', NULL, 34, '2026-06-04 15:30:00'),
(61, 'Go开发工程师', '云聘互联科技有限公司', '上海', '互联网/IT', 11500, 17000, '本科', '经验不限', '["Go","MySQL","Redis","微服务"]', '【企业直招】云聘互联科技有限公司招聘Go开发工程师，工作地点上海。本科及以上学历，经验不限；要求熟悉 Go、MySQL、Redis 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-19', 'HR_PUBLISH', NULL, 4, '2026-06-19 18:30:00'),
(62, '数据仓库工程师', '恒信数据技术有限公司', '武汉', '大数据', 7500, 11000, '本科', '应届生', '["SQL","Hive","Spark","数据建模"]', '【企业直招】恒信数据技术有限公司招聘数据仓库工程师，工作地点武汉。本科及以上学历，工作经验应届生；要求熟悉 SQL、Hive、Spark 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-16', 'HR_PUBLISH', NULL, 19, '2026-06-16 11:30:00'),
(63, '在线教育产品经理', '智汇未来教育科技有限公司', '武汉', '教育', 6500, 9500, '本科', '经验不限', '["产品设计","数据分析","项目管理"]', '【企业直招】智汇未来教育科技有限公司招聘在线教育产品经理，工作地点武汉。本科及以上学历，经验不限；要求熟悉 产品设计、数据分析、项目管理 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-25', 'HR_PUBLISH', NULL, 20, '2026-06-25 13:30:00'),
(64, '计算机视觉工程师', '博睿人工智能研究院有限公司', '深圳', '人工智能', 10500, 15500, '本科', '应届生', '["Python","计算机视觉","深度学习","TensorFlow","团队协作"]', '【企业直招】博睿人工智能研究院有限公司招聘计算机视觉工程师，工作地点深圳。本科及以上学历，工作经验应届生；要求熟悉 Python、计算机视觉、深度学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-16', 'HR_PUBLISH', NULL, 26, '2026-06-16 15:30:00'),
(65, '风控算法专员', '金桥融信金融服务有限公司', '北京', '金融', 19500, 29000, '专科', '5-10年', '["SQL","Python","机器学习","数据分析","Git"]', '【企业直招】金桥融信金融服务有限公司招聘风控算法专员，工作地点北京。专科及以上学历，工作经验5-10年；要求熟悉 SQL、Python、机器学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-26', 'HR_PUBLISH', NULL, 27, '2026-06-26 15:30:00'),
(66, '产品经理', '优选电商集团有限公司', '广州', '电子商务', 8500, 12500, '专科', '1-3年', '["产品设计","Axure","数据分析","项目管理","Git"]', '【企业直招】优选电商集团有限公司招聘产品经理，工作地点广州。专科及以上学历，工作经验1-3年；要求熟悉 产品设计、Axure、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-18', 'HR_PUBLISH', NULL, 28, '2026-06-18 17:30:00'),
(67, '游戏数值策划', '乐游互动娱乐有限公司', '西安', '游戏', 5500, 8500, '本科', '应届生', '["Excel","数据分析","数理统计","Git"]', '【企业直招】乐游互动娱乐有限公司招聘游戏数值策划，工作地点西安。本科及以上学历，工作经验应届生；要求熟悉 Excel、数据分析、数理统计 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-06', 'HR_PUBLISH', NULL, 29, '2026-06-06 16:30:00'),
(68, '嵌入式软件工程师', '精工智造装备股份有限公司', '北京', '智能制造', 7500, 11000, '专科', '应届生', '["C++","Linux","单片机"]', '【企业直招】精工智造装备股份有限公司招聘嵌入式软件工程师，工作地点北京。专科及以上学历，工作经验应届生；要求熟悉 C++、Linux、单片机 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-26', 'HR_PUBLISH', NULL, 30, '2026-06-26 14:30:00'),
(69, '测试开发工程师', '华信云科技有限公司', '武汉', '互联网/IT', 4500, 6500, '不限', '应届生', '["Python","Selenium","Linux","MySQL"]', '【企业直招】华信云科技有限公司招聘测试开发工程师，工作地点武汉。学历不限，工作经验应届生；要求熟悉 Python、Selenium、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-27', 'HR_PUBLISH', NULL, 31, '2026-06-27 12:30:00'),
(70, '数据仓库工程师', '数联云图大数据有限公司', '上海', '大数据', 20000, 29000, '专科', '5-10年', '["SQL","Hive","Spark","数据建模","Linux"]', '【企业直招】数联云图大数据有限公司招聘数据仓库工程师，工作地点上海。专科及以上学历，工作经验5-10年；要求熟悉 SQL、Hive、Spark 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-25', 'HR_PUBLISH', NULL, 32, '2026-06-25 16:30:00'),
(71, 'Go开发工程师', '启明星辰网络技术有限公司', '成都', '互联网/IT', 10500, 15000, '本科', '1-3年', '["Go","MySQL","Redis","微服务"]', '【企业直招】启明星辰网络技术有限公司招聘Go开发工程师，工作地点成都。本科及以上学历，工作经验1-3年；要求熟悉 Go、MySQL、Redis 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-06-20', 'HR_PUBLISH', NULL, 33, '2026-06-20 15:30:00'),
(72, '自动化测试工程师', '芯联半导体科技有限公司', '杭州', '智能制造', 7000, 10500, '专科', '1-3年', '["Python","Linux","Selenium"]', '【企业直招】芯联半导体科技有限公司招聘自动化测试工程师，工作地点杭州。专科及以上学历，工作经验1-3年；要求熟悉 Python、Linux、Selenium 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-02', 'HR_PUBLISH', NULL, 34, '2026-07-02 14:30:00'),
(73, '运维开发工程师', '云聘互联科技有限公司', '广州', '互联网/IT', 14000, 21000, '硕士', '3-5年', '["Linux","Docker","Kubernetes","Python","团队协作"]', '【企业直招】云聘互联科技有限公司招聘运维开发工程师，工作地点广州。硕士及以上学历，工作经验3-5年；要求熟悉 Linux、Docker、Kubernetes 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-02', 'HR_PUBLISH', NULL, 4, '2026-07-02 17:30:00'),
(74, '实时计算工程师', '恒信数据技术有限公司', '上海', '大数据', 22500, 33000, '本科', '5-10年', '["Java","Flink","Kafka","Redis"]', '【企业直招】恒信数据技术有限公司招聘实时计算工程师，工作地点上海。本科及以上学历，工作经验5-10年；要求熟悉 Java、Flink、Kafka 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-06', 'HR_PUBLISH', NULL, 19, '2026-07-06 17:30:00'),
(75, '教学数据分析师', '恒信数据技术有限公司', '上海', '教育', 6000, 9000, '本科', '应届生', '["SQL","Excel","数据分析"]', '【企业直招】恒信数据技术有限公司招聘教学数据分析师，工作地点上海。本科及以上学历，工作经验应届生；要求熟悉 SQL、Excel、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-04', 'HR_PUBLISH', NULL, 20, '2026-07-04 15:30:00'),
(76, 'NLP算法工程师', '启明星辰网络技术有限公司', '北京', '人工智能', 12000, 18000, '专科', '经验不限', '["Python","深度学习","NLP","PyTorch"]', '【企业直招】启明星辰网络技术有限公司招聘NLP算法工程师，工作地点北京。专科及以上学历，经验不限；要求熟悉 Python、深度学习、NLP 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-07', 'HR_PUBLISH', NULL, 26, '2026-07-07 17:30:00'),
(77, '风控算法专员', '金桥融信金融服务有限公司', '杭州', '金融', 10500, 15500, '本科', '1-3年', '["SQL","Python","机器学习","数据分析"]', '【企业直招】金桥融信金融服务有限公司招聘风控算法专员，工作地点杭州。本科及以上学历，工作经验1-3年；要求熟悉 SQL、Python、机器学习 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-04', 'HR_PUBLISH', NULL, 27, '2026-07-04 14:30:00'),
(78, '电商数据运营', '优选电商集团有限公司', '深圳', '电子商务', 6500, 10000, '本科', '经验不限', '["数据分析","Excel","SQL","用户运营","Git"]', '【企业直招】优选电商集团有限公司招聘电商数据运营，工作地点深圳。本科及以上学历，经验不限；要求熟悉 数据分析、Excel、SQL 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-04', 'HR_PUBLISH', NULL, 28, '2026-07-04 17:30:00'),
(79, '游戏客户端开发', '乐游互动娱乐有限公司', '西安', '游戏', 17000, 24500, '本科', '5-10年', '["C++","Unity","数据结构"]', '【企业直招】乐游互动娱乐有限公司招聘游戏客户端开发，工作地点西安。本科及以上学历，工作经验5-10年；要求熟悉 C++、Unity、数据结构 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-05', 'HR_PUBLISH', NULL, 29, '2026-07-05 13:30:00'),
(80, '工业软件开发工程师', '精工智造装备股份有限公司', '上海', '智能制造', 17500, 26000, '本科', '5-10年', '["Java","MySQL","Linux"]', '【企业直招】精工智造装备股份有限公司招聘工业软件开发工程师，工作地点上海。本科及以上学历，工作经验5-10年；要求熟悉 Java、MySQL、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-01', 'HR_PUBLISH', NULL, 30, '2026-07-01 19:30:00'),
(81, '测试开发工程师', '华信云科技有限公司', '深圳', '互联网/IT', 8000, 12000, '本科', '经验不限', '["Python","Selenium","Linux","MySQL"]', '【企业直招】华信云科技有限公司招聘测试开发工程师，工作地点深圳。本科及以上学历，经验不限；要求熟悉 Python、Selenium、Linux 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-04', 'HR_PUBLISH', NULL, 31, '2026-07-04 17:30:00'),
(82, '数据分析师', '数联云图大数据有限公司', '西安', '大数据', 6000, 9000, '本科', '经验不限', '["SQL","Python","数据分析","Tableau"]', '【企业直招】数联云图大数据有限公司招聘数据分析师，工作地点西安。本科及以上学历，经验不限；要求熟悉 SQL、Python、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-03', 'HR_PUBLISH', NULL, 32, '2026-07-03 14:30:00'),
(83, '前端开发工程师', '启明星辰网络技术有限公司', '成都', '互联网/IT', 8000, 12000, '本科', '1-3年', '["JavaScript","Vue","CSS","TypeScript","Git"]', '【企业直招】启明星辰网络技术有限公司招聘前端开发工程师，工作地点成都。本科及以上学历，工作经验1-3年；要求熟悉 JavaScript、Vue、CSS 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-03', 'HR_PUBLISH', NULL, 33, '2026-07-03 14:30:00'),
(84, '嵌入式软件工程师', '芯联半导体科技有限公司', '上海', '智能制造', 10500, 16000, '本科', '1-3年', '["C++","Linux","单片机","团队协作"]', '【企业直招】芯联半导体科技有限公司招聘嵌入式软件工程师，工作地点上海。本科及以上学历，工作经验1-3年；要求熟悉 C++、Linux、单片机 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-08', 'HR_PUBLISH', NULL, 34, '2026-07-08 18:30:00'),
(85, '全栈开发工程师', '云聘互联科技有限公司', '北京', '互联网/IT', 12000, 17500, '本科', '1-3年', '["JavaScript","Vue","Java","MySQL"]', '【企业直招】云聘互联科技有限公司招聘全栈开发工程师，工作地点北京。本科及以上学历，工作经验1-3年；要求熟悉 JavaScript、Vue、Java 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-08', 'HR_PUBLISH', NULL, 4, '2026-07-08 16:30:00'),
(86, '数据仓库工程师', '恒信数据技术有限公司', '北京', '大数据', 11500, 16500, '本科', '经验不限', '["SQL","Hive","Spark","数据建模"]', '【企业直招】恒信数据技术有限公司招聘数据仓库工程师，工作地点北京。本科及以上学历，经验不限；要求熟悉 SQL、Hive、Spark 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-08', 'HR_PUBLISH', NULL, 19, '2026-07-08 16:30:00'),
(87, '课程研发工程师', '智汇未来教育科技有限公司', '北京', '教育', 12000, 18000, '硕士', '1-3年', '["Java","Vue","MySQL","Linux"]', '【企业直招】智汇未来教育科技有限公司招聘课程研发工程师，工作地点北京。硕士及以上学历，工作经验1-3年；要求熟悉 Java、Vue、MySQL 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-08', 'HR_PUBLISH', NULL, 20, '2026-07-08 18:30:00'),
(88, 'NLP算法工程师', '博睿人工智能研究院有限公司', '深圳', '人工智能', 18500, 27000, '专科', '3-5年', '["Python","深度学习","NLP","PyTorch"]', '【企业直招】博睿人工智能研究院有限公司招聘NLP算法工程师，工作地点深圳。专科及以上学历，工作经验3-5年；要求熟悉 Python、深度学习、NLP 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-02', 'HR_PUBLISH', NULL, 26, '2026-07-02 14:30:00'),
(89, '金融数据分析师', '金桥融信金融服务有限公司', '深圳', '金融', 11500, 17000, '硕士', '1-3年', '["SQL","Python","数据分析","Excel"]', '【企业直招】金桥融信金融服务有限公司招聘金融数据分析师，工作地点深圳。硕士及以上学历，工作经验1-3年；要求熟悉 SQL、Python、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-07', 'HR_PUBLISH', NULL, 27, '2026-07-07 16:30:00'),
(90, '产品经理', '优选电商集团有限公司', '深圳', '电子商务', 17500, 25500, '专科', '5-10年', '["产品设计","Axure","数据分析","项目管理"]', '【企业直招】优选电商集团有限公司招聘产品经理，工作地点深圳。专科及以上学历，工作经验5-10年；要求熟悉 产品设计、Axure、数据分析 等技术/工具，具备良好的团队协作意识。通过本平台发布，欢迎在校学生投递。', '2026-07-02', 'HR_PUBLISH', NULL, 28, '2026-07-02 16:30:00');

-- ---------- 分析结果 ----------
-- 职位维度（industry/city/skill/education/trend）：口径与 AnalysisJobServiceImpl 一致
--   avg_salary = AVG((min+max)/2)。job_detail 是全平台共享表，所以两租户各一份相同的数据。
-- 学生维度（apply_funnel/gap_*/student_*/contact_*）：口径与 EmploymentAnalysisContributor 一致
--   这些是按租户隔离的，两租户的数字不同。预置它们是为了让「就业分析」页开箱有数据，
--   否则新库打开是一堆空图表。管理员点「重算分析数据」会用后端口径覆盖，数值应当一致。
INSERT INTO analysis_result (id, tenant_id, dimension, dimension_value, metric_name, metric_value, period_type, period_value, calc_time) VALUES
(1, 1, 'industry', '互联网/IT', 'job_count', 22, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(2, 1, 'industry', '互联网/IT', 'avg_salary', 12659.09, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(3, 1, 'industry', '大数据', 'job_count', 15, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(4, 1, 'industry', '大数据', 'avg_salary', 16400, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(5, 1, 'industry', '教育', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(6, 1, 'industry', '教育', 'avg_salary', 10000, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(7, 1, 'industry', '人工智能', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(8, 1, 'industry', '人工智能', 'avg_salary', 20218.75, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(9, 1, 'industry', '金融', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(10, 1, 'industry', '金融', 'avg_salary', 23656.25, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(11, 1, 'industry', '电子商务', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(12, 1, 'industry', '电子商务', 'avg_salary', 12343.75, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(13, 1, 'industry', '游戏', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(14, 1, 'industry', '游戏', 'avg_salary', 19464.29, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(15, 1, 'industry', '智能制造', 'job_count', 14, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(16, 1, 'industry', '智能制造', 'avg_salary', 12392.86, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(17, 1, 'city', '杭州', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(18, 1, 'city', '杭州', 'avg_salary', 14611.11, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(19, 1, 'city', '上海', 'job_count', 17, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(20, 1, 'city', '上海', 'avg_salary', 15867.65, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(21, 1, 'city', '广州', 'job_count', 10, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(22, 1, 'city', '广州', 'avg_salary', 13075, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(23, 1, 'city', '西安', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(24, 1, 'city', '西安', 'avg_salary', 9916.67, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(25, 1, 'city', '武汉', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(26, 1, 'city', '武汉', 'avg_salary', 9718.75, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(27, 1, 'city', '北京', 'job_count', 21, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(28, 1, 'city', '北京', 'avg_salary', 18690.48, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(29, 1, 'city', '苏州', 'job_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(30, 1, 'city', '苏州', 'avg_salary', 6750, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(31, 1, 'city', '深圳', 'job_count', 12, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(32, 1, 'city', '深圳', 'avg_salary', 19770.83, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(33, 1, 'city', '南京', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(34, 1, 'city', '南京', 'avg_salary', 8500, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(35, 1, 'city', '成都', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(36, 1, 'city', '成都', 'avg_salary', 10312.5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(37, 1, 'education', '本科', 'job_count', 50, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(38, 1, 'education', '本科', 'avg_salary', 12925, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(39, 1, 'education', '专科', 'job_count', 17, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(40, 1, 'education', '专科', 'avg_salary', 15955.88, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(41, 1, 'education', '硕士', 'job_count', 14, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(42, 1, 'education', '硕士', 'avg_salary', 20071.43, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(43, 1, 'education', '不限', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(44, 1, 'education', '不限', 'avg_salary', 11750, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(45, 1, 'education', '博士', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(46, 1, 'education', '博士', 'avg_salary', 41625, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(47, 1, 'skill', 'Python', 'job_count', 34, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(48, 1, 'skill', 'Linux', 'job_count', 29, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(49, 1, 'skill', 'MySQL', 'job_count', 23, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(50, 1, 'skill', '数据分析', 'job_count', 23, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(51, 1, 'skill', 'SQL', 'job_count', 19, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(52, 1, 'skill', 'Java', 'job_count', 17, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(53, 1, 'skill', 'C++', 'job_count', 15, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(54, 1, 'skill', 'Selenium', 'job_count', 11, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(55, 1, 'skill', 'Vue', 'job_count', 10, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(56, 1, 'skill', 'Spark', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(57, 1, 'skill', 'Hive', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(58, 1, 'skill', 'Excel', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(59, 1, 'skill', 'Redis', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(60, 1, 'skill', '机器学习', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(61, 1, 'skill', 'Git', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(62, 1, 'skill', '团队协作', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(63, 1, 'skill', 'JavaScript', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(64, 1, 'skill', '产品设计', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(65, 1, 'skill', '项目管理', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(66, 1, 'skill', '深度学习', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(67, 1, 'skill', '数理统计', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(68, 1, 'skill', '数据结构', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(69, 1, 'skill', '数据建模', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(70, 1, 'skill', '单片机', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(71, 1, 'skill', 'Axure', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(72, 1, 'skill', 'Go', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(73, 1, 'skill', '微服务', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(74, 1, 'skill', 'Unity', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(75, 1, 'skill', 'CSS', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(76, 1, 'skill', 'TypeScript', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(77, 1, 'skill', 'Tableau', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(78, 1, 'skill', 'Hadoop', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(79, 1, 'skill', '计算机视觉', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(80, 1, 'skill', 'TensorFlow', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(81, 1, 'skill', '用户运营', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(82, 1, 'skill', 'NLP', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(83, 1, 'skill', 'PyTorch', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(84, 1, 'skill', 'Docker', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(85, 1, 'skill', 'Kubernetes', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(86, 1, 'skill', '分布式', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(87, 1, 'skill', 'Flink', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(88, 1, 'skill', 'Kafka', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(89, 1, 'skill', '文案策划', 'job_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(90, 1, 'skill', 'Spring Boot', 'job_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(91, 1, 'trend', '2026-02', 'job_count', 10, 'MONTH', '2026-02', '2026-07-09 08:00:00'),
(92, 1, 'trend', '2026-02', 'avg_salary', 13625, 'MONTH', '2026-02', '2026-07-09 08:00:00'),
(93, 1, 'trend', '2026-03', 'job_count', 12, 'MONTH', '2026-03', '2026-07-09 08:00:00'),
(94, 1, 'trend', '2026-03', 'avg_salary', 17458.33, 'MONTH', '2026-03', '2026-07-09 08:00:00'),
(95, 1, 'trend', '2026-04', 'job_count', 14, 'MONTH', '2026-04', '2026-07-09 08:00:00'),
(96, 1, 'trend', '2026-04', 'avg_salary', 14625, 'MONTH', '2026-04', '2026-07-09 08:00:00'),
(97, 1, 'trend', '2026-05', 'job_count', 16, 'MONTH', '2026-05', '2026-07-09 08:00:00'),
(98, 1, 'trend', '2026-05', 'avg_salary', 15578.13, 'MONTH', '2026-05', '2026-07-09 08:00:00'),
(99, 1, 'trend', '2026-06', 'job_count', 19, 'MONTH', '2026-06', '2026-07-09 08:00:00'),
(100, 1, 'trend', '2026-06', 'avg_salary', 14789.47, 'MONTH', '2026-06', '2026-07-09 08:00:00'),
(101, 1, 'trend', '2026-07', 'job_count', 19, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(102, 1, 'trend', '2026-07', 'avg_salary', 14907.89, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(103, 1, 'apply_funnel', 'SUBMITTED', 'application_count', 22, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(104, 1, 'apply_funnel', 'VIEWED', 'application_count', 16, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(105, 1, 'apply_funnel', 'INTERVIEW', 'application_count', 12, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(106, 1, 'apply_funnel', 'OFFER', 'application_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(107, 1, 'apply_funnel', 'REJECTED', 'application_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(108, 1, 'apply_funnel', 'TOTAL', 'application_count', 60, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(109, 1, 'apply_response', 'responded', 'application_count', 38, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(110, 1, 'apply_response', 'unresponded', 'application_count', 22, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(111, 1, 'apply_response', 'median_hours', 'hours', 115, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(112, 1, 'student_city', '杭州', 'student_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(113, 1, 'student_city', '上海', 'student_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(114, 1, 'student_city', '北京', 'student_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(115, 1, 'student_city', '深圳', 'student_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(116, 1, 'student_city', '南京', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(117, 1, 'student_city', '成都', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(118, 1, 'student_city', '苏州', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(119, 1, 'student_industry', '互联网/IT', 'student_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(120, 1, 'student_industry', '大数据', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(121, 1, 'student_industry', '人工智能', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(122, 1, 'student_industry', '游戏', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(123, 1, 'student_industry', '电子商务', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(124, 1, 'student_industry', '金融', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(125, 1, 'student_industry', '教育', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(126, 1, 'student_industry', '智能制造', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(127, 1, 'student_salary', '6000以下', 'student_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(128, 1, 'student_salary', '6000-8000', 'student_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(129, 1, 'student_salary', '8000-10000', 'student_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(130, 1, 'student_salary', '10000-15000', 'student_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(131, 1, 'student_salary', '15000以上', 'student_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(132, 1, 'gap_city', '北京', 'student_ratio', 25, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(133, 1, 'gap_city', '北京', 'job_ratio', 23.33, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(134, 1, 'gap_city', '北京', 'gap_ratio', 1.07, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(135, 1, 'gap_city', '杭州', 'student_ratio', 16.67, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(136, 1, 'gap_city', '杭州', 'job_ratio', 10, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(137, 1, 'gap_city', '杭州', 'gap_ratio', 1.67, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(138, 1, 'gap_city', '上海', 'student_ratio', 16.67, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(139, 1, 'gap_city', '上海', 'job_ratio', 18.89, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(140, 1, 'gap_city', '上海', 'gap_ratio', 0.88, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(141, 1, 'gap_city', '深圳', 'student_ratio', 16.67, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(142, 1, 'gap_city', '深圳', 'job_ratio', 13.33, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(143, 1, 'gap_city', '深圳', 'gap_ratio', 1.25, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(144, 1, 'gap_city', '南京', 'student_ratio', 8.33, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(145, 1, 'gap_city', '南京', 'job_ratio', 2.22, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(146, 1, 'gap_city', '南京', 'gap_ratio', 3.75, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(147, 1, 'gap_city', '成都', 'student_ratio', 8.33, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(148, 1, 'gap_city', '成都', 'job_ratio', 4.44, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(149, 1, 'gap_city', '成都', 'gap_ratio', 1.88, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(150, 1, 'gap_city', '苏州', 'student_ratio', 8.33, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(151, 1, 'gap_city', '苏州', 'job_ratio', 1.11, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(152, 1, 'gap_city', '苏州', 'gap_ratio', 7.5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(153, 1, 'gap_salary', 'overall', 'student_median', 11750, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(154, 1, 'gap_salary', 'overall', 'market_median', 13000, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(155, 1, 'gap_salary', 'overall', 'deviation_percent', -9.62, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(156, 2, 'industry', '互联网/IT', 'job_count', 22, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(157, 2, 'industry', '互联网/IT', 'avg_salary', 12659.09, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(158, 2, 'industry', '大数据', 'job_count', 15, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(159, 2, 'industry', '大数据', 'avg_salary', 16400, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(160, 2, 'industry', '教育', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(161, 2, 'industry', '教育', 'avg_salary', 10000, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(162, 2, 'industry', '人工智能', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(163, 2, 'industry', '人工智能', 'avg_salary', 20218.75, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(164, 2, 'industry', '金融', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(165, 2, 'industry', '金融', 'avg_salary', 23656.25, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(166, 2, 'industry', '电子商务', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(167, 2, 'industry', '电子商务', 'avg_salary', 12343.75, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(168, 2, 'industry', '游戏', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(169, 2, 'industry', '游戏', 'avg_salary', 19464.29, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(170, 2, 'industry', '智能制造', 'job_count', 14, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(171, 2, 'industry', '智能制造', 'avg_salary', 12392.86, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(172, 2, 'city', '杭州', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(173, 2, 'city', '杭州', 'avg_salary', 14611.11, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(174, 2, 'city', '上海', 'job_count', 17, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(175, 2, 'city', '上海', 'avg_salary', 15867.65, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(176, 2, 'city', '广州', 'job_count', 10, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(177, 2, 'city', '广州', 'avg_salary', 13075, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(178, 2, 'city', '西安', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(179, 2, 'city', '西安', 'avg_salary', 9916.67, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(180, 2, 'city', '武汉', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(181, 2, 'city', '武汉', 'avg_salary', 9718.75, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(182, 2, 'city', '北京', 'job_count', 21, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(183, 2, 'city', '北京', 'avg_salary', 18690.48, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(184, 2, 'city', '苏州', 'job_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(185, 2, 'city', '苏州', 'avg_salary', 6750, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(186, 2, 'city', '深圳', 'job_count', 12, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(187, 2, 'city', '深圳', 'avg_salary', 19770.83, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(188, 2, 'city', '南京', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(189, 2, 'city', '南京', 'avg_salary', 8500, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(190, 2, 'city', '成都', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(191, 2, 'city', '成都', 'avg_salary', 10312.5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(192, 2, 'education', '本科', 'job_count', 50, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(193, 2, 'education', '本科', 'avg_salary', 12925, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(194, 2, 'education', '专科', 'job_count', 17, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(195, 2, 'education', '专科', 'avg_salary', 15955.88, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(196, 2, 'education', '硕士', 'job_count', 14, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(197, 2, 'education', '硕士', 'avg_salary', 20071.43, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(198, 2, 'education', '不限', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(199, 2, 'education', '不限', 'avg_salary', 11750, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(200, 2, 'education', '博士', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(201, 2, 'education', '博士', 'avg_salary', 41625, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(202, 2, 'skill', 'Python', 'job_count', 34, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(203, 2, 'skill', 'Linux', 'job_count', 29, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(204, 2, 'skill', 'MySQL', 'job_count', 23, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(205, 2, 'skill', '数据分析', 'job_count', 23, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(206, 2, 'skill', 'SQL', 'job_count', 19, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(207, 2, 'skill', 'Java', 'job_count', 17, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(208, 2, 'skill', 'C++', 'job_count', 15, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(209, 2, 'skill', 'Selenium', 'job_count', 11, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(210, 2, 'skill', 'Vue', 'job_count', 10, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(211, 2, 'skill', 'Spark', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(212, 2, 'skill', 'Hive', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(213, 2, 'skill', 'Excel', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(214, 2, 'skill', 'Redis', 'job_count', 9, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(215, 2, 'skill', '机器学习', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(216, 2, 'skill', 'Git', 'job_count', 8, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(217, 2, 'skill', '团队协作', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(218, 2, 'skill', 'JavaScript', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(219, 2, 'skill', '产品设计', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(220, 2, 'skill', '项目管理', 'job_count', 7, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(221, 2, 'skill', '深度学习', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(222, 2, 'skill', '数理统计', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(223, 2, 'skill', '数据结构', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(224, 2, 'skill', '数据建模', 'job_count', 6, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(225, 2, 'skill', '单片机', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(226, 2, 'skill', 'Axure', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(227, 2, 'skill', 'Go', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(228, 2, 'skill', '微服务', 'job_count', 5, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(229, 2, 'skill', 'Unity', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(230, 2, 'skill', 'CSS', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(231, 2, 'skill', 'TypeScript', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(232, 2, 'skill', 'Tableau', 'job_count', 4, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(233, 2, 'skill', 'Hadoop', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(234, 2, 'skill', '计算机视觉', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(235, 2, 'skill', 'TensorFlow', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(236, 2, 'skill', '用户运营', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(237, 2, 'skill', 'NLP', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(238, 2, 'skill', 'PyTorch', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(239, 2, 'skill', 'Docker', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(240, 2, 'skill', 'Kubernetes', 'job_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(241, 2, 'skill', '分布式', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(242, 2, 'skill', 'Flink', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(243, 2, 'skill', 'Kafka', 'job_count', 2, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(244, 2, 'skill', '文案策划', 'job_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(245, 2, 'skill', 'Spring Boot', 'job_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(246, 2, 'trend', '2026-02', 'job_count', 10, 'MONTH', '2026-02', '2026-07-09 08:00:00'),
(247, 2, 'trend', '2026-02', 'avg_salary', 13625, 'MONTH', '2026-02', '2026-07-09 08:00:00'),
(248, 2, 'trend', '2026-03', 'job_count', 12, 'MONTH', '2026-03', '2026-07-09 08:00:00'),
(249, 2, 'trend', '2026-03', 'avg_salary', 17458.33, 'MONTH', '2026-03', '2026-07-09 08:00:00'),
(250, 2, 'trend', '2026-04', 'job_count', 14, 'MONTH', '2026-04', '2026-07-09 08:00:00'),
(251, 2, 'trend', '2026-04', 'avg_salary', 14625, 'MONTH', '2026-04', '2026-07-09 08:00:00'),
(252, 2, 'trend', '2026-05', 'job_count', 16, 'MONTH', '2026-05', '2026-07-09 08:00:00'),
(253, 2, 'trend', '2026-05', 'avg_salary', 15578.13, 'MONTH', '2026-05', '2026-07-09 08:00:00'),
(254, 2, 'trend', '2026-06', 'job_count', 19, 'MONTH', '2026-06', '2026-07-09 08:00:00'),
(255, 2, 'trend', '2026-06', 'avg_salary', 14789.47, 'MONTH', '2026-06', '2026-07-09 08:00:00'),
(256, 2, 'trend', '2026-07', 'job_count', 19, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(257, 2, 'trend', '2026-07', 'avg_salary', 14907.89, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(258, 2, 'apply_funnel', 'SUBMITTED', 'application_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(259, 2, 'apply_funnel', 'VIEWED', 'application_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(260, 2, 'apply_funnel', 'INTERVIEW', 'application_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(261, 2, 'apply_funnel', 'OFFER', 'application_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(262, 2, 'apply_funnel', 'REJECTED', 'application_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(263, 2, 'apply_funnel', 'TOTAL', 'application_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(264, 2, 'apply_response', 'responded', 'application_count', 3, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(265, 2, 'apply_response', 'unresponded', 'application_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(266, 2, 'apply_response', 'median_hours', 'hours', 73, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(267, 2, 'student_city', '武汉', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(268, 2, 'student_industry', '互联网/IT', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(269, 2, 'student_salary', '6000以下', 'student_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(270, 2, 'student_salary', '6000-8000', 'student_count', 1, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(271, 2, 'student_salary', '8000-10000', 'student_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(272, 2, 'student_salary', '10000-15000', 'student_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(273, 2, 'student_salary', '15000以上', 'student_count', 0, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(274, 2, 'gap_city', '武汉', 'student_ratio', 100, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(275, 2, 'gap_city', '武汉', 'job_ratio', 8.89, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(276, 2, 'gap_city', '武汉', 'gap_ratio', 11.25, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(277, 2, 'gap_salary', 'overall', 'student_median', 9500, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(278, 2, 'gap_salary', 'overall', 'market_median', 13000, 'MONTH', '2026-07', '2026-07-09 08:00:00'),
(279, 2, 'gap_salary', 'overall', 'deviation_percent', -26.92, 'MONTH', '2026-07', '2026-07-09 08:00:00');

-- ---------- 报告记录（一条历史失败样例；成功记录请在页面上现场生成。报告已无模板概念，由 大类+范围 直接生成）----------
INSERT INTO report_record (id, tenant_id, name, category, params, file_url, file_type, status, error_msg, create_time) VALUES
(1, 1, '就业市场分析报告', 'MARKET', '{}', NULL, 'PDF', 'FAILED', '示例：一条历史失败记录（PDF 渲染失败）', '2026-06-28 14:12:00');

-- ---------- 资讯（DATA_CAST 数据播报 / ARTICLE 精选文章 / EXTERNAL 外部资讯占位）----------
INSERT INTO news (id, tenant_id, category, type, title, summary, content, cover_style, source, source_url, link_target, view_count, featured, status, publish_time) VALUES
(1, 1, NULL, 'DATA_CAST', '本平台在库岗位达 90 个，数据分析已就绪', '均为企业在平台发布的可投递岗位，覆盖 10 座城市、8 大技术方向，看板与推荐均基于此；点采集任务可再引入外部市场参考数据。', NULL, 'blue', '平台数据播报', NULL, '/admin/dashboard', 282, 1, 1, '2026-07-09 08:10:00'),
(2, 1, 'backend', 'DATA_CAST', 'Java 稳居技能热度榜首', '在全部岗位中，要求 Java 的职位数量最多，其后为 Python、MySQL。后端方向需求持续旺盛。', NULL, 'blue', '平台数据播报', NULL, '/admin/dashboard', 40, 0, 1, '2026-07-09 08:12:00'),
(3, 1, NULL, 'DATA_CAST', '北京、上海、深圳岗位最集中', '按城市分布，北京岗位数领先，上海、深圳紧随其后；一线与新一线城市为主要去向。', NULL, 'green', '平台数据播报', NULL, '/admin/dashboard', 179, 0, 1, '2026-07-09 08:14:00'),
(4, 1, 'bigdata', 'DATA_CAST', '大数据方向平均薪资领先', '按行业统计，大数据 / 人工智能方向平均薪资高于全站均值，Spark、Flink 等技能溢价明显。', NULL, 'purple', '平台数据播报', NULL, '/admin/employment', 155, 0, 1, '2026-07-09 08:16:00'),
(5, 1, NULL, 'DATA_CAST', '投递转化：63 份投递中已产生 4 个 OFFER', '站内投递共 63 份，其中面试阶段 13 份、录用 4 份；及时完善画像与简历有助于提升转化。', NULL, 'amber', '平台数据播报', NULL, '/admin/employment', 188, 0, 1, '2026-07-09 08:18:00'),
(6, 1, 'backend', 'ARTICLE', '2026 后端开发就业观察：微服务与云原生成标配', '从平台岗位要求看，Spring Boot、微服务、容器化几乎成为后端岗位的默认门槛。', '<p>综合本平台采集到的后端岗位数据，Spring Boot、Spring Cloud、Redis、消息队列（Kafka/RocketMQ）出现频率显著上升。</p><p>建议在校生优先夯实 Java 基础与数据库，再向微服务、容器化（Docker/K8s）延伸，配合一到两个完整项目经历，将明显提升竞争力。</p>', 'blue', '就业指导中心', NULL, NULL, 283, 1, 1, '2026-07-06 10:00:00'),
(7, 1, 'frontend', 'ARTICLE', '前端招聘趋势：工程化与框架深度并重', '企业更看重 Vue/React 的工程化实践与组件设计能力，而非仅会写页面。', '<p>平台前端岗位中，Vue、TypeScript、构建工具（Vite/Webpack）、组件化设计是高频关键词。</p><p>建议同学在掌握一个主流框架的基础上，理解其响应式原理与工程化配置，并积累可展示的项目。</p>', 'green', '就业指导中心', NULL, NULL, 140, 0, 1, '2026-07-05 14:30:00'),
(8, 1, NULL, 'EXTERNAL', '示例：外部就业资讯（接入 Google News RSS 后在此展示）', '外部资讯仅展示标题与摘要，点击「阅读原文」跳转来源站点；封面用色块占位。', NULL, 'purple', '外部来源示例', 'https://news.google.com/', NULL, 276, 0, 1, '2026-07-08 09:00:00');

-- ---------- 推送记录 ----------
INSERT INTO push_record (id, tenant_id, user_id, type, title, content, is_read, create_time) VALUES
(1, 1, 2, 'RECOMMEND', '为你推荐：测试开发工程师', '根据你的画像为你匹配到职位【测试开发工程师】- 启明星辰网络技术有限公司（杭州，10000-15000元/月），快去看看吧！', 1, '2026-07-01 13:22:03'),
(2, 1, 2, 'RECOMMEND', '为你推荐：测试开发工程师', '根据你的画像为你匹配到职位【测试开发工程师】- 云聘互联科技有限公司（杭州，12000-17500元/月），快去看看吧！', 1, '2026-07-01 18:48:42'),
(3, 1, 2, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:02'),
(4, 1, 5, 'RECOMMEND', '为你推荐：大数据开发工程师', '根据你的画像为你匹配到职位【大数据开发工程师】- 恒信数据技术有限公司（上海，9000-13000元/月），快去看看吧！', 0, '2026-07-01 23:32:22'),
(5, 1, 5, 'RECOMMEND', '为你推荐：后端开发工程师', '根据你的画像为你匹配到职位【后端开发工程师】- 华信云科技有限公司（上海，9000-13000元/月），快去看看吧！', 1, '2026-07-02 04:28:50'),
(6, 1, 5, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:05'),
(7, 1, 6, 'RECOMMEND', '为你推荐：数据仓库工程师', '根据你的画像为你匹配到职位【数据仓库工程师】- 恒信数据技术有限公司（北京，11500-16500元/月），快去看看吧！', 0, '2026-07-02 08:40:28'),
(8, 1, 6, 'RECOMMEND', '为你推荐：测试开发工程师', '根据你的画像为你匹配到职位【测试开发工程师】- 华信云科技有限公司（北京，6000-9000元/月），快去看看吧！', 1, '2026-07-02 11:45:59'),
(9, 1, 6, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:06'),
(10, 1, 7, 'RECOMMEND', '为你推荐：量化研究员', '根据你的画像为你匹配到职位【量化研究员】- 金桥融信金融服务有限公司（北京，32500-48000元/月），快去看看吧！', 0, '2026-07-02 15:15:36'),
(11, 1, 7, 'RECOMMEND', '为你推荐：NLP算法工程师', '根据你的画像为你匹配到职位【NLP算法工程师】- 博睿人工智能研究院有限公司（北京，12000-18000元/月），快去看看吧！', 1, '2026-07-02 17:49:18'),
(12, 1, 7, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:07'),
(13, 1, 8, 'RECOMMEND', '为你推荐：测试开发工程师', '根据你的画像为你匹配到职位【测试开发工程师】- 华信云科技有限公司（深圳，8000-12000元/月），快去看看吧！', 1, '2026-07-02 21:15:40'),
(14, 1, 8, 'RECOMMEND', '为你推荐：计算机视觉工程师', '根据你的画像为你匹配到职位【计算机视觉工程师】- 博睿人工智能研究院有限公司（深圳，10500-15500元/月），快去看看吧！', 1, '2026-07-03 00:36:40'),
(15, 1, 8, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:08'),
(16, 1, 9, 'RECOMMEND', '为你推荐：数据分析师', '根据你的画像为你匹配到职位【数据分析师】- 数联云图大数据有限公司（深圳，16500-24000元/月），快去看看吧！', 0, '2026-07-03 04:05:56'),
(17, 1, 9, 'RECOMMEND', '为你推荐：测试开发工程师', '根据你的画像为你匹配到职位【测试开发工程师】- 华信云科技有限公司（深圳，8000-12000元/月），快去看看吧！', 0, '2026-07-03 06:07:53'),
(18, 1, 9, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:09'),
(19, 1, 10, 'RECOMMEND', '为你推荐：风控算法专员', '根据你的画像为你匹配到职位【风控算法专员】- 金桥融信金融服务有限公司（杭州，10500-15500元/月），快去看看吧！', 1, '2026-07-03 09:36:02'),
(20, 1, 10, 'RECOMMEND', '为你推荐：数据仓库工程师', '根据你的画像为你匹配到职位【数据仓库工程师】- 恒信数据技术有限公司（杭州，19500-28500元/月），快去看看吧！', 0, '2026-07-03 14:58:05'),
(21, 1, 10, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:10'),
(22, 1, 11, 'RECOMMEND', '为你推荐：测试开发工程师', '根据你的画像为你匹配到职位【测试开发工程师】- 华信云科技有限公司（北京，6000-9000元/月），快去看看吧！', 0, '2026-07-03 20:06:30'),
(23, 1, 11, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:11'),
(24, 1, 12, 'RECOMMEND', '为你推荐：教学数据分析师', '根据你的画像为你匹配到职位【教学数据分析师】- 智汇未来教育科技有限公司（上海，6000-9000元/月），快去看看吧！', 1, '2026-07-03 22:15:04'),
(25, 1, 12, 'RECOMMEND', '为你推荐：数据仓库工程师', '根据你的画像为你匹配到职位【数据仓库工程师】- 数联云图大数据有限公司（上海，20000-29000元/月），快去看看吧！', 1, '2026-07-04 00:14:03'),
(26, 1, 12, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:12'),
(27, 1, 13, 'RECOMMEND', '为你推荐：前端开发工程师', '根据你的画像为你匹配到职位【前端开发工程师】- 启明星辰网络技术有限公司（成都，8000-12000元/月），快去看看吧！', 1, '2026-07-04 04:18:29'),
(28, 1, 13, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:13'),
(29, 1, 14, 'RECOMMEND', '为你推荐：风控算法专员', '根据你的画像为你匹配到职位【风控算法专员】- 金桥融信金融服务有限公司（北京，19500-29000元/月），快去看看吧！', 1, '2026-07-04 06:37:33'),
(30, 1, 14, 'RECOMMEND', '为你推荐：产品经理', '根据你的画像为你匹配到职位【产品经理】- 优选电商集团有限公司（北京，9000-13500元/月），快去看看吧！', 1, '2026-07-04 10:06:08'),
(31, 1, 14, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:14'),
(32, 1, 15, 'RECOMMEND', '为你推荐：课程研发工程师', '根据你的画像为你匹配到职位【课程研发工程师】- 智汇未来教育科技有限公司（苏州，5500-8000元/月），快去看看吧！', 1, '2026-07-04 12:08:20'),
(33, 1, 15, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像（专业、技能、意向城市）后，系统将为你提供更精准的职位推荐。', 1, '2026-06-20 10:00:15'),
(34, 1, 16, 'SYSTEM', '请完善个人画像', '检测到你尚未填写专业、技能与求职意向，完善后才能使用职位推荐功能。', 0, '2026-07-02 09:00:00'),
(35, 2, 24, 'SYSTEM', '欢迎使用职业能力大数据服务平台', '完善个人画像后，系统将为你提供更精准的职位推荐。', 0, '2026-07-03 09:00:00');

-- ---------- 学生行为（VIEW→FAVORITE→APPLY 时间有序，职位均真实存在）----------
INSERT INTO student_behavior (id, tenant_id, user_id, job_id, action, create_time) VALUES
(1, 1, 15, 60, 'VIEW', '2026-06-21 13:59:39'),
(2, 1, 12, 54, 'VIEW', '2026-06-21 14:36:32'),
(3, 1, 9, 37, 'VIEW', '2026-06-21 15:34:30'),
(4, 1, 9, 37, 'IGNORE', '2026-06-21 15:38:28'),
(5, 1, 5, 39, 'VIEW', '2026-06-21 17:15:59'),
(6, 1, 6, 53, 'VIEW', '2026-06-21 17:47:56'),
(7, 1, 11, 25, 'VIEW', '2026-06-21 20:04:30'),
(8, 1, 11, 25, 'IGNORE', '2026-06-21 20:13:57'),
(9, 1, 14, 21, 'VIEW', '2026-06-21 22:02:32'),
(10, 1, 15, 12, 'VIEW', '2026-06-22 00:26:34'),
(11, 1, 5, 39, 'APPLY', '2026-06-22 01:19:28'),
(12, 1, 12, 54, 'FAVORITE', '2026-06-22 02:08:04'),
(13, 1, 15, 60, 'FAVORITE', '2026-06-22 06:24:08'),
(14, 1, 13, 32, 'VIEW', '2026-06-22 07:12:42'),
(15, 1, 14, 18, 'VIEW', '2026-06-22 07:53:11'),
(16, 1, 6, 50, 'VIEW', '2026-06-22 08:38:15'),
(17, 2, 24, 15, 'VIEW', '2026-06-22 09:15:13'),
(18, 1, 13, 32, 'FAVORITE', '2026-06-22 12:47:45'),
(19, 1, 6, 58, 'VIEW', '2026-06-22 14:20:00'),
(20, 1, 8, 52, 'VIEW', '2026-06-22 14:43:43'),
(21, 1, 9, 35, 'VIEW', '2026-06-22 15:23:11'),
(22, 1, 12, 58, 'VIEW', '2026-06-22 16:33:28'),
(23, 1, 12, 58, 'IGNORE', '2026-06-22 16:40:43'),
(24, 1, 9, 31, 'VIEW', '2026-06-22 19:09:35'),
(25, 1, 8, 31, 'APPLY', '2026-06-22 20:58:50'),
(26, 1, 14, 20, 'VIEW', '2026-06-22 21:00:29'),
(27, 1, 7, 21, 'VIEW', '2026-06-23 00:30:54'),
(28, 1, 12, 56, 'VIEW', '2026-06-23 00:35:59'),
(29, 1, 2, 36, 'VIEW', '2026-06-23 02:34:57'),
(30, 1, 2, 1, 'VIEW', '2026-06-23 03:19:58'),
(31, 1, 14, 20, 'FAVORITE', '2026-06-23 03:58:46'),
(32, 1, 7, 12, 'VIEW', '2026-06-23 04:08:26'),
(33, 1, 2, 37, 'VIEW', '2026-06-23 04:25:26'),
(34, 1, 15, 60, 'APPLY', '2026-06-23 05:19:50'),
(35, 1, 12, 54, 'APPLY', '2026-06-23 05:46:57'),
(36, 1, 10, 27, 'VIEW', '2026-06-23 07:25:54'),
(37, 1, 10, 27, 'IGNORE', '2026-06-23 07:27:15'),
(38, 1, 5, 15, 'APPLY', '2026-06-23 07:40:24'),
(39, 2, 24, 40, 'VIEW', '2026-06-23 09:49:42'),
(40, 2, 24, 40, 'IGNORE', '2026-06-23 09:55:22'),
(41, 2, 24, 50, 'VIEW', '2026-06-23 10:02:12'),
(42, 1, 12, 4, 'VIEW', '2026-06-23 10:43:10'),
(43, 1, 12, 4, 'IGNORE', '2026-06-23 10:47:57'),
(44, 1, 9, 35, 'FAVORITE', '2026-06-23 13:05:50'),
(45, 1, 9, 52, 'VIEW', '2026-06-23 15:52:08'),
(46, 1, 2, 1, 'APPLY', '2026-06-23 16:12:35'),
(47, 1, 5, 9, 'VIEW', '2026-06-23 17:37:56'),
(48, 1, 2, 36, 'FAVORITE', '2026-06-23 18:18:24'),
(49, 1, 7, 12, 'FAVORITE', '2026-06-23 21:24:16'),
(50, 1, 15, 15, 'VIEW', '2026-06-23 21:58:38'),
(51, 1, 8, 31, 'VIEW', '2026-06-23 22:13:59'),
(52, 1, 11, 46, 'VIEW', '2026-06-23 23:03:38'),
(53, 2, 24, 50, 'FAVORITE', '2026-06-23 23:15:18'),
(54, 1, 2, 21, 'VIEW', '2026-06-23 23:35:00'),
(55, 1, 2, 19, 'VIEW', '2026-06-24 00:14:32'),
(56, 1, 12, 41, 'VIEW', '2026-06-24 02:02:53'),
(57, 1, 5, 61, 'VIEW', '2026-06-24 03:21:08'),
(58, 1, 2, 9, 'APPLY', '2026-06-24 03:32:36'),
(59, 1, 2, 38, 'VIEW', '2026-06-24 04:12:51'),
(60, 1, 5, 56, 'VIEW', '2026-06-24 04:50:11'),
(61, 1, 14, 24, 'VIEW', '2026-06-24 08:32:46'),
(62, 1, 5, 25, 'VIEW', '2026-06-24 11:11:31'),
(63, 1, 11, 46, 'FAVORITE', '2026-06-24 11:33:34'),
(64, 1, 10, 38, 'VIEW', '2026-06-24 12:30:59'),
(65, 1, 2, 30, 'VIEW', '2026-06-24 12:59:48'),
(66, 1, 5, 56, 'FAVORITE', '2026-06-24 13:52:03'),
(67, 1, 6, 10, 'VIEW', '2026-06-24 13:54:15'),
(68, 1, 2, 19, 'APPLY', '2026-06-24 13:55:15'),
(69, 1, 8, 12, 'VIEW', '2026-06-24 16:28:52'),
(70, 1, 8, 12, 'IGNORE', '2026-06-24 16:31:11'),
(71, 1, 14, 42, 'VIEW', '2026-06-24 17:24:49'),
(72, 1, 11, 43, 'VIEW', '2026-06-24 17:56:07'),
(73, 1, 2, 30, 'FAVORITE', '2026-06-24 19:04:43'),
(74, 1, 6, 50, 'APPLY', '2026-06-24 20:55:07'),
(75, 1, 13, 71, 'VIEW', '2026-06-24 23:43:01'),
(76, 1, 14, 20, 'APPLY', '2026-06-25 00:35:26'),
(77, 1, 14, 24, 'APPLY', '2026-06-25 00:44:21'),
(78, 1, 7, 17, 'VIEW', '2026-06-25 01:00:44'),
(79, 1, 11, 43, 'APPLY', '2026-06-25 01:28:05'),
(80, 1, 10, 38, 'FAVORITE', '2026-06-25 02:56:16'),
(81, 1, 5, 25, 'FAVORITE', '2026-06-25 04:24:41'),
(82, 1, 15, 48, 'VIEW', '2026-06-25 04:46:56'),
(83, 2, 24, 8, 'VIEW', '2026-06-25 05:05:46'),
(84, 1, 5, 25, 'APPLY', '2026-06-25 05:24:56'),
(85, 1, 8, 40, 'VIEW', '2026-06-25 05:25:04'),
(86, 1, 6, 10, 'FAVORITE', '2026-06-25 05:47:36'),
(87, 1, 7, 46, 'VIEW', '2026-06-25 08:05:28'),
(88, 1, 7, 46, 'IGNORE', '2026-06-25 08:14:33'),
(89, 1, 15, 56, 'VIEW', '2026-06-25 09:20:16'),
(90, 1, 10, 54, 'VIEW', '2026-06-25 09:57:26'),
(91, 1, 10, 38, 'APPLY', '2026-06-25 10:25:33'),
(92, 1, 2, 14, 'VIEW', '2026-06-25 11:01:48'),
(93, 1, 2, 14, 'IGNORE', '2026-06-25 11:05:34'),
(94, 1, 7, 19, 'VIEW', '2026-06-25 11:24:46'),
(95, 1, 2, 36, 'APPLY', '2026-06-25 12:30:34'),
(96, 1, 14, 42, 'APPLY', '2026-06-25 13:43:34'),
(97, 2, 24, 62, 'VIEW', '2026-06-25 15:01:06'),
(98, 1, 5, 27, 'VIEW', '2026-06-25 17:08:42'),
(99, 1, 13, 23, 'APPLY', '2026-06-25 17:36:28'),
(100, 1, 13, 23, 'VIEW', '2026-06-25 17:46:17'),
(101, 1, 7, 19, 'FAVORITE', '2026-06-25 18:53:55'),
(102, 1, 13, 13, 'VIEW', '2026-06-25 20:18:23'),
(103, 1, 11, 29, 'VIEW', '2026-06-25 22:49:46'),
(104, 1, 11, 61, 'VIEW', '2026-06-25 23:11:41'),
(105, 1, 13, 23, 'FAVORITE', '2026-06-26 00:03:07'),
(106, 1, 12, 5, 'VIEW', '2026-06-26 01:58:13'),
(107, 1, 15, 56, 'FAVORITE', '2026-06-26 02:45:19'),
(108, 1, 8, 64, 'VIEW', '2026-06-26 04:26:54'),
(109, 1, 8, 5, 'VIEW', '2026-06-26 04:39:25'),
(110, 1, 7, 19, 'APPLY', '2026-06-26 05:14:15'),
(111, 1, 5, 56, 'APPLY', '2026-06-26 05:14:19'),
(112, 1, 10, 54, 'FAVORITE', '2026-06-26 06:36:49'),
(113, 1, 6, 10, 'APPLY', '2026-06-26 07:26:53'),
(114, 1, 12, 5, 'FAVORITE', '2026-06-26 12:13:19'),
(115, 2, 24, 25, 'VIEW', '2026-06-26 12:31:51'),
(116, 1, 2, 24, 'VIEW', '2026-06-26 12:41:54'),
(117, 1, 8, 5, 'FAVORITE', '2026-06-26 17:15:01'),
(118, 1, 2, 58, 'VIEW', '2026-06-26 18:06:41'),
(119, 1, 2, 58, 'IGNORE', '2026-06-26 18:08:32'),
(120, 1, 7, 18, 'VIEW', '2026-06-26 20:22:52'),
(121, 1, 15, 11, 'VIEW', '2026-06-26 22:11:06'),
(122, 1, 15, 11, 'IGNORE', '2026-06-26 22:12:30'),
(123, 1, 12, 70, 'VIEW', '2026-06-26 22:41:02'),
(124, 2, 24, 37, 'VIEW', '2026-06-26 22:45:47'),
(125, 1, 8, 45, 'VIEW', '2026-06-26 23:08:42'),
(126, 1, 2, 24, 'APPLY', '2026-06-27 00:06:59'),
(127, 1, 12, 70, 'FAVORITE', '2026-06-27 00:42:11'),
(128, 2, 24, 37, 'FAVORITE', '2026-06-27 00:49:49'),
(129, 1, 14, 30, 'VIEW', '2026-06-27 04:04:39'),
(130, 2, 24, 9, 'VIEW', '2026-06-27 04:09:18'),
(131, 1, 10, 50, 'VIEW', '2026-06-27 05:12:06'),
(132, 1, 12, 70, 'APPLY', '2026-06-27 05:30:23'),
(133, 1, 8, 5, 'APPLY', '2026-06-27 07:02:09'),
(134, 1, 10, 58, 'VIEW', '2026-06-27 07:18:58'),
(135, 2, 24, 37, 'APPLY', '2026-06-27 08:38:54'),
(136, 1, 8, 39, 'VIEW', '2026-06-27 10:18:01'),
(137, 1, 10, 58, 'FAVORITE', '2026-06-27 11:14:59'),
(138, 1, 10, 65, 'VIEW', '2026-06-27 11:42:42'),
(139, 1, 10, 65, 'FAVORITE', '2026-06-27 11:56:22'),
(140, 1, 10, 1, 'VIEW', '2026-06-27 15:47:24'),
(141, 1, 8, 45, 'FAVORITE', '2026-06-27 16:33:24'),
(142, 1, 10, 1, 'FAVORITE', '2026-06-27 19:13:26'),
(143, 1, 10, 54, 'APPLY', '2026-06-27 19:21:18'),
(144, 2, 24, 9, 'FAVORITE', '2026-06-27 20:49:06'),
(145, 1, 12, 50, 'APPLY', '2026-06-27 21:15:04'),
(146, 1, 11, 71, 'VIEW', '2026-06-28 00:28:37'),
(147, 2, 24, 48, 'VIEW', '2026-06-28 02:44:05'),
(148, 1, 7, 28, 'VIEW', '2026-06-28 04:20:58'),
(149, 1, 8, 39, 'FAVORITE', '2026-06-28 05:22:28'),
(150, 1, 8, 45, 'APPLY', '2026-06-28 06:25:26'),
(151, 2, 24, 48, 'FAVORITE', '2026-06-28 07:25:47'),
(152, 1, 9, 22, 'VIEW', '2026-06-28 07:48:39'),
(153, 1, 14, 51, 'VIEW', '2026-06-28 09:08:10'),
(154, 1, 12, 50, 'VIEW', '2026-06-28 09:34:29'),
(155, 1, 14, 10, 'VIEW', '2026-06-28 09:38:48'),
(156, 1, 8, 22, 'VIEW', '2026-06-28 14:45:06'),
(157, 1, 6, 29, 'VIEW', '2026-06-28 15:18:54'),
(158, 1, 12, 27, 'VIEW', '2026-06-28 15:37:15'),
(159, 1, 13, 66, 'VIEW', '2026-06-28 16:04:30'),
(160, 1, 8, 22, 'FAVORITE', '2026-06-28 16:55:14'),
(161, 1, 5, 15, 'VIEW', '2026-06-28 16:59:52'),
(162, 1, 9, 55, 'VIEW', '2026-06-28 20:01:33'),
(163, 1, 11, 37, 'VIEW', '2026-06-28 23:15:11'),
(164, 1, 13, 66, 'FAVORITE', '2026-06-28 23:30:42'),
(165, 1, 14, 51, 'FAVORITE', '2026-06-29 00:28:01'),
(166, 1, 10, 58, 'APPLY', '2026-06-29 00:59:54'),
(167, 1, 7, 28, 'APPLY', '2026-06-29 01:17:22'),
(168, 1, 14, 65, 'VIEW', '2026-06-29 02:47:18'),
(169, 1, 2, 15, 'APPLY', '2026-06-29 03:08:39'),
(170, 1, 11, 37, 'FAVORITE', '2026-06-29 04:21:59'),
(171, 1, 14, 10, 'FAVORITE', '2026-06-29 04:57:59'),
(172, 1, 6, 29, 'APPLY', '2026-06-29 05:05:24'),
(173, 1, 10, 6, 'VIEW', '2026-06-29 05:53:06'),
(174, 1, 5, 2, 'VIEW', '2026-06-29 06:36:47'),
(175, 1, 5, 23, 'APPLY', '2026-06-29 07:27:05'),
(176, 1, 10, 6, 'FAVORITE', '2026-06-29 09:01:50'),
(177, 2, 24, 9, 'APPLY', '2026-06-29 09:15:12'),
(178, 1, 10, 1, 'APPLY', '2026-06-29 09:47:08'),
(179, 1, 11, 37, 'APPLY', '2026-06-29 10:38:48'),
(180, 1, 6, 68, 'VIEW', '2026-06-29 11:44:44'),
(181, 1, 15, 44, 'VIEW', '2026-06-29 11:54:53'),
(182, 1, 12, 2, 'VIEW', '2026-06-29 15:10:49'),
(183, 1, 6, 21, 'VIEW', '2026-06-29 15:11:38'),
(184, 1, 12, 27, 'FAVORITE', '2026-06-29 15:16:37'),
(185, 1, 6, 21, 'IGNORE', '2026-06-29 15:18:03'),
(186, 2, 24, 63, 'VIEW', '2026-06-29 15:47:35'),
(187, 2, 24, 69, 'VIEW', '2026-06-29 16:38:31'),
(188, 1, 2, 20, 'VIEW', '2026-06-29 17:25:17'),
(189, 1, 2, 11, 'VIEW', '2026-06-29 18:56:10'),
(190, 1, 12, 51, 'VIEW', '2026-06-29 19:38:40'),
(191, 1, 15, 68, 'VIEW', '2026-06-29 21:40:38'),
(192, 1, 6, 68, 'FAVORITE', '2026-06-29 23:23:14'),
(193, 1, 15, 68, 'FAVORITE', '2026-06-30 00:09:25'),
(194, 1, 7, 10, 'VIEW', '2026-06-30 00:26:08'),
(195, 1, 7, 10, 'IGNORE', '2026-06-30 00:26:47'),
(196, 1, 10, 6, 'APPLY', '2026-06-30 00:30:09'),
(197, 1, 6, 18, 'VIEW', '2026-06-30 01:15:24'),
(198, 1, 6, 18, 'IGNORE', '2026-06-30 01:24:21'),
(199, 1, 15, 68, 'APPLY', '2026-06-30 03:29:43'),
(200, 1, 7, 47, 'VIEW', '2026-06-30 05:39:38'),
(201, 1, 12, 26, 'VIEW', '2026-06-30 06:12:58'),
(202, 1, 12, 26, 'FAVORITE', '2026-06-30 06:22:38'),
(203, 2, 24, 13, 'VIEW', '2026-06-30 06:24:01'),
(204, 1, 2, 20, 'APPLY', '2026-06-30 07:00:09'),
(205, 2, 24, 49, 'VIEW', '2026-06-30 07:45:36'),
(206, 1, 15, 12, 'APPLY', '2026-06-30 07:46:16'),
(207, 1, 14, 66, 'VIEW', '2026-06-30 07:52:20'),
(208, 1, 14, 47, 'VIEW', '2026-06-30 09:56:28'),
(209, 1, 15, 41, 'VIEW', '2026-06-30 09:59:07'),
(210, 1, 8, 22, 'APPLY', '2026-06-30 10:43:46'),
(211, 1, 11, 39, 'VIEW', '2026-06-30 10:53:52'),
(212, 1, 12, 2, 'FAVORITE', '2026-06-30 10:57:17'),
(213, 1, 5, 49, 'VIEW', '2026-06-30 11:01:26'),
(214, 1, 2, 11, 'FAVORITE', '2026-06-30 12:15:39'),
(215, 1, 14, 51, 'APPLY', '2026-06-30 13:03:51'),
(216, 1, 11, 9, 'VIEW', '2026-06-30 13:52:24'),
(217, 2, 24, 49, 'APPLY', '2026-06-30 13:52:24'),
(218, 1, 7, 65, 'VIEW', '2026-06-30 14:07:03'),
(219, 1, 12, 22, 'APPLY', '2026-06-30 14:16:15'),
(220, 1, 8, 52, 'APPLY', '2026-06-30 14:17:51'),
(221, 1, 2, 9, 'VIEW', '2026-06-30 14:49:00'),
(222, 2, 24, 69, 'FAVORITE', '2026-06-30 15:59:31'),
(223, 1, 13, 33, 'VIEW', '2026-06-30 18:45:28'),
(224, 1, 6, 65, 'VIEW', '2026-06-30 19:33:10'),
(225, 1, 12, 22, 'VIEW', '2026-06-30 20:24:09'),
(226, 1, 5, 23, 'VIEW', '2026-06-30 22:17:36'),
(227, 1, 2, 49, 'VIEW', '2026-06-30 22:22:18'),
(228, 1, 14, 66, 'FAVORITE', '2026-07-01 00:44:31'),
(229, 1, 15, 69, 'VIEW', '2026-07-01 02:27:49'),
(230, 1, 11, 24, 'VIEW', '2026-07-01 03:26:05'),
(231, 1, 10, 29, 'VIEW', '2026-07-01 04:27:43'),
(232, 1, 10, 29, 'IGNORE', '2026-07-01 04:30:09'),
(233, 1, 11, 9, 'FAVORITE', '2026-07-01 04:35:51'),
(234, 1, 9, 64, 'VIEW', '2026-07-01 05:04:36'),
(235, 1, 10, 3, 'VIEW', '2026-07-01 05:31:54'),
(236, 1, 12, 27, 'APPLY', '2026-07-01 06:52:22'),
(237, 1, 10, 3, 'FAVORITE', '2026-07-01 07:29:30'),
(238, 1, 7, 65, 'FAVORITE', '2026-07-01 08:36:50'),
(239, 1, 9, 64, 'APPLY', '2026-07-01 11:35:51'),
(240, 1, 8, 11, 'VIEW', '2026-07-01 11:45:51'),
(241, 1, 8, 11, 'FAVORITE', '2026-07-01 12:20:56'),
(242, 1, 13, 57, 'VIEW', '2026-07-01 14:49:15'),
(243, 1, 6, 65, 'APPLY', '2026-07-01 17:37:09'),
(244, 1, 11, 24, 'FAVORITE', '2026-07-01 18:47:20'),
(245, 1, 7, 53, 'VIEW', '2026-07-01 21:10:54'),
(246, 1, 8, 55, 'APPLY', '2026-07-01 22:03:23'),
(247, 1, 10, 30, 'VIEW', '2026-07-02 00:03:13'),
(248, 1, 2, 15, 'VIEW', '2026-07-02 03:46:03'),
(249, 1, 10, 30, 'FAVORITE', '2026-07-02 04:15:39'),
(250, 1, 2, 15, 'FAVORITE', '2026-07-02 04:46:03'),
(251, 1, 2, 57, 'VIEW', '2026-07-02 07:24:47'),
(252, 1, 2, 11, 'APPLY', '2026-07-02 08:23:07'),
(253, 1, 8, 55, 'VIEW', '2026-07-02 12:26:41'),
(254, 1, 2, 80, 'VIEW', '2026-07-02 12:52:11'),
(255, 1, 8, 55, 'FAVORITE', '2026-07-02 16:18:18'),
(256, 1, 7, 53, 'FAVORITE', '2026-07-02 17:54:24'),
(257, 1, 8, 88, 'VIEW', '2026-07-02 21:30:51'),
(258, 1, 7, 65, 'APPLY', '2026-07-03 02:29:03'),
(259, 1, 14, 90, 'VIEW', '2026-07-03 06:22:07'),
(260, 1, 9, 88, 'VIEW', '2026-07-03 11:29:10'),
(261, 1, 9, 88, 'IGNORE', '2026-07-03 11:29:56'),
(262, 1, 2, 80, 'APPLY', '2026-07-03 12:28:01'),
(263, 1, 8, 88, 'FAVORITE', '2026-07-03 13:46:22'),
(264, 1, 8, 90, 'VIEW', '2026-07-03 17:19:23'),
(265, 1, 8, 90, 'APPLY', '2026-07-03 18:49:53'),
(266, 1, 5, 80, 'VIEW', '2026-07-03 22:37:18'),
(267, 1, 5, 80, 'IGNORE', '2026-07-03 22:41:48'),
(268, 1, 5, 83, 'VIEW', '2026-07-04 10:45:48'),
(269, 1, 12, 80, 'VIEW', '2026-07-04 11:31:15'),
(270, 1, 13, 83, 'VIEW', '2026-07-04 22:32:24'),
(271, 1, 13, 83, 'APPLY', '2026-07-05 00:30:22'),
(272, 1, 8, 88, 'APPLY', '2026-07-05 00:55:37'),
(273, 1, 10, 75, 'VIEW', '2026-07-05 02:11:52'),
(274, 1, 5, 83, 'APPLY', '2026-07-05 03:56:47'),
(275, 1, 9, 81, 'VIEW', '2026-07-05 07:41:47'),
(276, 1, 9, 81, 'APPLY', '2026-07-05 13:45:12'),
(277, 1, 10, 72, 'VIEW', '2026-07-05 14:01:18'),
(278, 1, 15, 81, 'VIEW', '2026-07-05 14:47:23'),
(279, 1, 2, 77, 'VIEW', '2026-07-05 16:40:33'),
(280, 1, 2, 77, 'IGNORE', '2026-07-05 16:44:47'),
(281, 1, 8, 81, 'VIEW', '2026-07-05 20:12:24'),
(282, 1, 9, 73, 'VIEW', '2026-07-05 20:12:30'),
(283, 1, 15, 73, 'VIEW', '2026-07-05 21:29:29'),
(284, 1, 8, 81, 'FAVORITE', '2026-07-05 21:53:05'),
(285, 1, 12, 75, 'VIEW', '2026-07-06 08:04:40'),
(286, 1, 9, 78, 'VIEW', '2026-07-06 08:12:14'),
(287, 1, 10, 78, 'VIEW', '2026-07-06 08:25:34'),
(288, 1, 15, 81, 'FAVORITE', '2026-07-06 12:40:07'),
(289, 1, 7, 77, 'VIEW', '2026-07-06 14:49:59'),
(290, 1, 16, 1, 'VIEW', '2026-07-06 15:20:00'),
(291, 1, 2, 72, 'VIEW', '2026-07-06 16:47:30'),
(292, 1, 2, 72, 'IGNORE', '2026-07-06 16:53:49'),
(293, 1, 16, 1, 'APPLY', '2026-07-06 17:20:00'),
(294, 1, 9, 90, 'VIEW', '2026-07-06 18:02:21'),
(295, 1, 10, 78, 'FAVORITE', '2026-07-06 19:22:46'),
(296, 1, 9, 73, 'FAVORITE', '2026-07-06 20:02:33'),
(297, 1, 10, 77, 'VIEW', '2026-07-07 04:18:58'),
(298, 1, 5, 74, 'VIEW', '2026-07-07 08:00:23'),
(299, 1, 7, 77, 'FAVORITE', '2026-07-07 08:07:40'),
(300, 1, 10, 77, 'FAVORITE', '2026-07-07 09:13:22'),
(301, 1, 9, 73, 'APPLY', '2026-07-07 11:07:42'),
(302, 1, 10, 77, 'APPLY', '2026-07-07 12:19:45'),
(303, 1, 7, 76, 'APPLY', '2026-07-07 22:12:39'),
(304, 1, 5, 74, 'FAVORITE', '2026-07-07 23:11:09'),
(305, 1, 7, 76, 'VIEW', '2026-07-08 04:37:42'),
(306, 1, 11, 74, 'APPLY', '2026-07-08 04:41:23'),
(307, 1, 11, 74, 'VIEW', '2026-07-08 05:49:00'),
(308, 1, 10, 89, 'VIEW', '2026-07-08 06:02:44'),
(309, 1, 10, 89, 'IGNORE', '2026-07-08 06:08:40'),
(310, 1, 15, 81, 'APPLY', '2026-07-08 08:38:26'),
(311, 1, 8, 89, 'VIEW', '2026-07-08 13:30:22'),
(312, 1, 9, 89, 'VIEW', '2026-07-08 15:13:37'),
(313, 1, 13, 87, 'VIEW', '2026-07-08 18:32:54'),
(314, 1, 7, 86, 'VIEW', '2026-07-08 19:25:28'),
(315, 1, 5, 84, 'VIEW', '2026-07-08 19:36:18'),
(316, 1, 13, 85, 'VIEW', '2026-07-08 19:51:13'),
(317, 1, 14, 87, 'VIEW', '2026-07-08 20:28:30'),
(318, 1, 2, 87, 'VIEW', '2026-07-08 22:28:56'),
(319, 1, 6, 85, 'VIEW', '2026-07-08 22:57:33'),
(320, 1, 6, 85, 'IGNORE', '2026-07-08 22:58:20'),
(321, 1, 5, 85, 'VIEW', '2026-07-09 01:49:27'),
(322, 1, 7, 86, 'FAVORITE', '2026-07-09 01:50:38'),
(323, 1, 7, 85, 'VIEW', '2026-07-09 02:46:03'),
(324, 1, 6, 86, 'VIEW', '2026-07-09 03:19:28'),
(325, 1, 15, 84, 'VIEW', '2026-07-09 04:21:03'),
(326, 1, 5, 87, 'VIEW', '2026-07-09 04:54:13'),
(327, 1, 5, 84, 'FAVORITE', '2026-07-09 06:30:31'),
(328, 1, 2, 87, 'FAVORITE', '2026-07-09 07:26:54'),
(329, 1, 6, 86, 'APPLY', '2026-07-09 11:30:00');

-- ---------- 投递记录（与 student_behavior 的 APPLY 一一对应，状态铺开五种）----------
INSERT INTO job_application (id, tenant_id, user_id, job_id, publisher_id, status, hr_note, applied_at, status_changed_at, create_time) VALUES
(1, 1, 5, 39, 20, 'OFFER', '综合表现优秀，已发放录用意向', '2026-06-22 01:19:28', '2026-06-26 08:39:22', '2026-06-22 01:19:28'),
(2, 1, 8, 31, 29, 'SUBMITTED', NULL, '2026-06-22 20:58:50', NULL, '2026-06-22 20:58:50'),
(3, 1, 15, 60, 34, 'VIEWED', NULL, '2026-06-23 05:19:50', '2026-07-01 05:37:13', '2026-06-23 05:19:50'),
(4, 1, 12, 54, 28, 'SUBMITTED', NULL, '2026-06-23 05:46:57', NULL, '2026-06-23 05:46:57'),
(5, 1, 5, 15, 20, 'SUBMITTED', NULL, '2026-06-23 07:40:24', NULL, '2026-06-23 07:40:24'),
(6, 1, 2, 1, 4, 'SUBMITTED', NULL, '2026-06-23 16:12:35', NULL, '2026-06-23 16:12:35'),
(7, 1, 2, 9, 31, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-06-24 03:32:36', '2026-06-30 07:25:05', '2026-06-24 03:32:36'),
(8, 1, 2, 19, 29, 'SUBMITTED', NULL, '2026-06-24 13:55:15', NULL, '2026-06-24 13:55:15'),
(9, 1, 6, 50, 19, 'SUBMITTED', NULL, '2026-06-24 20:55:07', NULL, '2026-06-24 20:55:07'),
(10, 1, 14, 20, 30, 'VIEWED', NULL, '2026-06-25 00:35:26', '2026-06-26 08:24:56', '2026-06-25 00:35:26'),
(11, 1, 14, 24, 34, 'REJECTED', '岗位要求的项目经验暂不匹配', '2026-06-25 00:44:21', '2026-07-01 22:05:19', '2026-06-25 00:44:21'),
(12, 1, 11, 43, 29, 'SUBMITTED', NULL, '2026-06-25 01:28:05', NULL, '2026-06-25 01:28:05'),
(13, 1, 5, 25, 4, 'VIEWED', NULL, '2026-06-25 05:24:56', '2026-06-27 07:24:36', '2026-06-25 05:24:56'),
(14, 1, 10, 38, 19, 'REJECTED', '岗位要求的项目经验暂不匹配', '2026-06-25 10:25:33', '2026-07-02 22:02:04', '2026-06-25 10:25:33'),
(15, 1, 2, 36, 34, 'SUBMITTED', NULL, '2026-06-25 12:30:34', NULL, '2026-06-25 12:30:34'),
(16, 1, 14, 42, 28, 'VIEWED', NULL, '2026-06-25 13:43:34', '2026-06-27 09:49:40', '2026-06-25 13:43:34'),
(17, 1, 13, 23, 33, 'SUBMITTED', NULL, '2026-06-25 17:36:28', NULL, '2026-06-25 17:36:28'),
(18, 1, 7, 19, 29, 'VIEWED', NULL, '2026-06-26 05:14:15', '2026-07-05 05:41:23', '2026-06-26 05:14:15'),
(19, 1, 5, 56, 30, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-06-26 05:14:19', '2026-06-28 12:58:16', '2026-06-26 05:14:19'),
(20, 1, 6, 10, 32, 'VIEWED', NULL, '2026-06-26 07:26:53', '2026-06-30 11:04:25', '2026-06-26 07:26:53'),
(21, 1, 2, 24, 34, 'SUBMITTED', NULL, '2026-06-27 00:06:59', NULL, '2026-06-27 00:06:59'),
(22, 1, 12, 70, 32, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-06-27 05:30:23', '2026-07-01 03:22:36', '2026-06-27 05:30:23'),
(23, 1, 8, 5, 27, 'VIEWED', NULL, '2026-06-27 07:02:09', '2026-06-27 16:19:29', '2026-06-27 07:02:09'),
(24, 2, 24, 37, 4, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-06-27 08:38:54', '2026-06-30 10:31:20', '2026-06-27 08:38:54'),
(25, 1, 10, 54, 28, 'VIEWED', NULL, '2026-06-27 19:21:18', '2026-07-06 06:37:47', '2026-06-27 19:21:18'),
(26, 1, 12, 50, 19, 'SUBMITTED', NULL, '2026-06-27 21:15:04', NULL, '2026-06-27 21:15:04'),
(27, 1, 8, 45, 31, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-06-28 06:25:26', '2026-07-03 11:23:20', '2026-06-28 06:25:26'),
(28, 1, 10, 58, 32, 'REJECTED', '岗位要求的项目经验暂不匹配', '2026-06-29 00:59:54', '2026-07-05 06:30:13', '2026-06-29 00:59:54'),
(29, 1, 7, 28, 26, 'VIEWED', NULL, '2026-06-29 01:17:22', '2026-07-02 00:13:51', '2026-06-29 01:17:22'),
(30, 1, 2, 15, 20, 'REJECTED', '岗位要求的项目经验暂不匹配', '2026-06-29 03:08:39', '2026-07-04 11:04:57', '2026-06-29 03:08:39'),
(31, 1, 6, 29, 27, 'VIEWED', NULL, '2026-06-29 05:05:24', '2026-07-01 01:40:10', '2026-06-29 05:05:24'),
(32, 1, 5, 23, 33, 'REJECTED', '岗位要求的项目经验暂不匹配', '2026-06-29 07:27:05', '2026-07-08 18:20:25', '2026-06-29 07:27:05'),
(33, 2, 24, 9, 31, 'REJECTED', '岗位要求的项目经验暂不匹配', '2026-06-29 09:15:12', '2026-07-03 12:01:49', '2026-06-29 09:15:12'),
(34, 1, 10, 1, 4, 'VIEWED', NULL, '2026-06-29 09:47:08', '2026-06-29 21:12:34', '2026-06-29 09:47:08'),
(35, 1, 11, 37, 4, 'REJECTED', '岗位要求的项目经验暂不匹配', '2026-06-29 10:38:48', '2026-07-08 06:57:51', '2026-06-29 10:38:48'),
(36, 1, 10, 6, 28, 'SUBMITTED', NULL, '2026-06-30 00:30:09', NULL, '2026-06-30 00:30:09'),
(37, 1, 15, 68, 30, 'VIEWED', NULL, '2026-06-30 03:29:43', '2026-07-08 05:14:37', '2026-06-30 03:29:43'),
(38, 1, 2, 20, 30, 'SUBMITTED', NULL, '2026-06-30 07:00:09', NULL, '2026-06-30 07:00:09'),
(39, 1, 15, 12, 34, 'SUBMITTED', NULL, '2026-06-30 07:46:16', NULL, '2026-06-30 07:46:16'),
(40, 1, 8, 22, 32, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-06-30 10:43:46', '2026-07-02 21:53:19', '2026-06-30 10:43:46'),
(41, 1, 14, 51, 20, 'VIEWED', NULL, '2026-06-30 13:03:51', '2026-07-03 09:47:20', '2026-06-30 13:03:51'),
(42, 2, 24, 49, 4, 'VIEWED', NULL, '2026-06-30 13:52:24', '2026-07-03 04:44:57', '2026-06-30 13:52:24'),
(43, 1, 12, 22, 32, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-06-30 14:16:15', '2026-07-09 11:30:00', '2026-06-30 14:16:15'),
(44, 1, 8, 52, 26, 'VIEWED', NULL, '2026-06-30 14:17:51', '2026-07-06 19:54:24', '2026-06-30 14:17:51'),
(45, 1, 12, 27, 20, 'VIEWED', NULL, '2026-07-01 06:52:22', '2026-07-09 08:00:10', '2026-07-01 06:52:22'),
(46, 1, 9, 64, 26, 'SUBMITTED', NULL, '2026-07-01 11:35:51', NULL, '2026-07-01 11:35:51'),
(47, 1, 6, 65, 27, 'SUBMITTED', NULL, '2026-07-01 17:37:09', NULL, '2026-07-01 17:37:09'),
(48, 1, 8, 55, 29, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-07-01 22:03:23', '2026-07-09 11:30:00', '2026-07-01 22:03:23'),
(49, 1, 2, 11, 33, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-07-02 08:23:07', '2026-07-09 11:30:00', '2026-07-02 08:23:07'),
(50, 1, 7, 65, 27, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-07-03 02:29:03', '2026-07-09 11:30:00', '2026-07-03 02:29:03'),
(51, 1, 2, 80, 30, 'OFFER', '综合表现优秀，已发放录用意向', '2026-07-03 12:28:01', '2026-07-09 11:30:00', '2026-07-03 12:28:01'),
(52, 1, 8, 90, 28, 'SUBMITTED', NULL, '2026-07-03 18:49:53', NULL, '2026-07-03 18:49:53'),
(53, 1, 13, 83, 33, 'VIEWED', NULL, '2026-07-05 00:30:22', '2026-07-09 11:30:00', '2026-07-05 00:30:22'),
(54, 1, 8, 88, 26, 'OFFER', '综合表现优秀，已发放录用意向', '2026-07-05 00:55:37', '2026-07-09 11:30:00', '2026-07-05 00:55:37'),
(55, 1, 5, 83, 33, 'OFFER', '综合表现优秀，已发放录用意向', '2026-07-05 03:56:47', '2026-07-08 08:35:13', '2026-07-05 03:56:47'),
(56, 1, 9, 81, 31, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-07-05 13:45:12', '2026-07-09 11:30:00', '2026-07-05 13:45:12'),
(57, 1, 16, 1, 4, 'SUBMITTED', NULL, '2026-07-06 17:20:00', NULL, '2026-07-06 17:20:00'),
(58, 1, 9, 73, 4, 'SUBMITTED', NULL, '2026-07-07 11:07:42', NULL, '2026-07-07 11:07:42'),
(59, 1, 10, 77, 27, 'SUBMITTED', NULL, '2026-07-07 12:19:45', NULL, '2026-07-07 12:19:45'),
(60, 1, 7, 76, 26, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-07-07 22:12:39', '2026-07-09 11:30:00', '2026-07-07 22:12:39'),
(61, 1, 11, 74, 19, 'INTERVIEW', '简历匹配度高，约技术一面', '2026-07-08 04:41:23', '2026-07-09 11:30:00', '2026-07-08 04:41:23'),
(62, 1, 15, 81, 31, 'SUBMITTED', NULL, '2026-07-08 08:38:26', NULL, '2026-07-08 08:38:26'),
(63, 1, 6, 86, 19, 'SUBMITTED', NULL, '2026-07-09 11:30:00', NULL, '2026-07-09 11:30:00');

-- ---------- API 客户端 ----------
-- api_secret 以 BCrypt 存储（与 sys_user.password_hash 同一套编码器）。
-- 调用 /api/open/auth/token 时传明文 secret，服务端用 passwordEncoder.matches 比对：
--   occ_test_2026       → demo_secret_key_for_dev
--   occ_portal_2026     → portal_secret_key_for_dev
--   occ_screen_2025     → screen_secret_key_deprecated（客户端已停用）
--   occ_demo_univ_2026  → demo_univ_secret_key（租户 2）
INSERT INTO api_client (id, tenant_id, client_name, api_key, api_secret, scopes, status, create_time) VALUES
(1, 1, '测试客户端', 'occ_test_2026', '$2a$10$LotAmCrhQI8dQNSB3ouPB.NdT9mRVoQys8.5RYIaOVNnnFapmX7AC', 'jobs:read,reports:read,skills:read', 1, '2026-06-18 10:00:00'),
(2, 1, '就业信息门户', 'occ_portal_2026', '$2a$10$7b6HPt1Iub1QutbFFTYHyOw8wzqaZ8.CfC3YegqRuWmW7C4eyaTQ6', 'jobs:read,skills:read', 1, '2026-06-30 11:20:00'),
(3, 1, '旧数据大屏（已停用）', 'occ_screen_2025', '$2a$10$fCjw5VlVieCsk5HnhZlaMe7cM0WWSt5JMTu4CZZ4xMiRdtiH75faq', 'jobs:read', 0, '2026-06-18 10:10:00'),
(4, 2, '示范大学门户', 'occ_demo_univ_2026', '$2a$10$PompgOywvbOs8I3NNuRhd.DJrZYoblDjF9sSoPjxxPWd.veDxughq', 'jobs:read,reports:read', 1, '2026-07-03 11:30:00');

-- ---------- 系统告警（CRAWLER_FAILURE 与 crawler_log 失败记录时间对应）----------
INSERT INTO sys_alert (id, tenant_id, type, level, content, is_read, create_time) VALUES
(1, 1, 'SYSTEM', 'INFO', '系统初始化完成，欢迎使用职业能力大数据服务平台', 1, '2026-06-18 10:00:00'),
(2, 1, 'CRAWLER_FAILURE', 'ERROR', 'Boss直聘采集任务失败：连接目标站点超时（已重试 3 次），任务已自动停用', 1, '2026-06-21 03:02:15'),
(3, 1, 'KAFKA_LAG', 'WARN', 'Kafka 消费组 raw-job-data-group 消息积压 1240 条，请关注清洗服务消费速率', 1, '2026-07-05 02:40:00'),
(4, 1, 'DB_POOL', 'WARN', '数据库连接池使用率达 85%（17/20），高峰期请留意慢查询', 0, '2026-07-08 15:22:00'),
(5, 1, 'SYSTEM', 'INFO', '统计分析任务完成：本次重算写入 102 条结果（industry/city/education/skill/trend 五个维度）', 0, '2026-07-09 08:00:00'),
(6, 2, 'SYSTEM', 'INFO', '系统初始化完成，欢迎使用职业能力大数据服务平台', 0, '2026-07-03 11:00:00');

