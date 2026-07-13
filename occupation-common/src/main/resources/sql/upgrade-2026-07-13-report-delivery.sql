-- ============================================================
-- 增量升级：报告下发（管理员把就业报告发给某范围学生）
-- 幂等；不 down -v 的库直接跑：
--   docker exec -i occupation-mysql mysql -uroot -proot occupation < occupation-common/src/main/resources/sql/upgrade-2026-07-13-report-delivery.sql
-- 与 init.sql 第 19 张表同源。
-- ============================================================

CREATE TABLE IF NOT EXISTS report_delivery (
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
