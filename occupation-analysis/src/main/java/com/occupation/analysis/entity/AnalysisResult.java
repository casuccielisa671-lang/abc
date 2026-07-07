package com.occupation.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分析结果实体 — 映射 analysis_result 表
 * <p>
 * 注意：此表使用 calc_time 而非 create_time，不继承 BaseEntity。
 */
@Data
@TableName("analysis_result")
public class AnalysisResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 分析维度：industry / city / skill / education / trend */
    private String dimension;

    /** 维度值（如 "Java"、"北京"） */
    private String dimensionValue;

    /** 指标名称：job_count / avg_salary_min / avg_salary_max */
    private String metricName;

    /** 指标值 */
    private BigDecimal metricValue;

    /** 周期类型：DAY / WEEK / MONTH / YEAR */
    private String periodType;

    /** 周期值（如 "20260706"、"2026W27"） */
    private String periodValue;

    /** 计算时间 */
    private LocalDateTime calcTime;
}
