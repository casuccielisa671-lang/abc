package com.occupation.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报告模板实体 — 映射 report_template 表
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_template")
public class ReportTemplate extends BaseEntity {

    /** 模板名称 */
    private String name;

    /** 适用行业（NULL = 通用） */
    private String industry;

    /** 报告类型：MONTHLY / QUARTERLY / YEARLY */
    private String type;

    /** 模板内容（JSON 结构） */
    private String templateContent;

    /** 状态：1=启用 0=禁用 */
    private Integer status;
}
