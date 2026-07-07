package com.occupation.analysis.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Dashboard 查询入参
 *
 * @author occupation-team
 */
@Data
public class DashboardQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分析维度（可多选） */
    private String dimension;

    /** 租户 ID */
    private Long tenantId;

    /** 时间范围起始 */
    private String startDate;

    /** 时间范围结束 */
    private String endDate;
}
