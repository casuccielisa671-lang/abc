-- 补建缺失的两张表：student_resume / job_application
-- 现象：打开「我的简历」「我的投递」提示系统内部错误(500)，因这两张表不在旧库里。
-- 本脚本只新增这两张表，不触碰现有 14 张表及其数据。

USE occupation;

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

DROP TABLE IF EXISTS job_application;
CREATE TABLE job_application (
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
