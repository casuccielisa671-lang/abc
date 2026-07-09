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

    /** 关联模板 ID */
    private Long templateId;

    /** 生成参数（JSON） */
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
}
