package com.occupation.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报告记录实体 — 映射 report_record 表
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_record")
public class ReportRecord extends BaseEntity {

    /** 归属人：null=管理员生成的租户级报告；有值=该学生的个人 AI 报告 */
    private Long userId;

    /** 报告名称（生成时按 大类 + 范围 自动生成，如「学生就业数据报告（软件工程-2022-1班）」） */
    private String name;

    /** 报告大类：MARKET=市场行业 / EMPLOYMENT=学生就业 */
    private String category;

    /** 生成参数（JSON）；EMPLOYMENT 类在此存 scope：{major,enrollYear,classId} */
    private String params;

    /** 生成文件 URL */
    private String fileUrl;

    /** 文件类型：PDF / WORD / HTML */
    private String fileType;

    /** 生成时产出的智能摘要（开放 API 直接读取，不必重新调用大模型） */
    private String aiSummary;

    /** 状态：PENDING / GENERATING / SUCCESS / FAILED */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    /**
     * 可见性：PUBLIC=全体可见 / SELF=仅自己可见。
     * 主要用于市场报告的广播开关；就业报告的可见范围由 report_delivery 决定，此字段保持 PUBLIC。
     */
    private String visibility;
}
