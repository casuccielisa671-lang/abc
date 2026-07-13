package com.occupation.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 报告下发记录 — 映射 report_delivery 表。
 * <p>
 * 管理员把一份就业报告「发送」给某范围内的学生时，每个接收学生落一行。
 * 市场行业报告走「发布即全体可见」的广播口径，不落 delivery 行（学生端查询时按
 * {@code category=MARKET} 直接可见）。同一报告不重复发给同一人（DB 唯一键兜底）。
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_delivery")
public class ReportDelivery extends BaseEntity {

    /** 报告 id（指向 report_record） */
    private Long reportId;

    /** 接收学生 userId */
    private Long userId;

    /** 阅读时间；null=未读 */
    private LocalDateTime readTime;
}
