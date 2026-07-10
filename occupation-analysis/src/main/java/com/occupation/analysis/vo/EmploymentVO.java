package com.occupation.analysis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 就业分析出参 — 投递漏斗 / 供需错配 / 自主求职流向
 * <p>
 * 数据全部来自 {@code analysis_result}，由 recommend 模块的扩展点写入。
 * 与看板读的是同一张表，所以数字必然一致。
 * <p>
 * 之所以不塞进 {@link DashboardVO}：看板是「市场有什么岗位」，本 VO 是
 * 「本校学生怎么样」。两个视角，两个接口，互不影响。
 *
 * @author occupation-team
 */
@Data
public class EmploymentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 投递状态分布：SUBMITTED / VIEWED / INTERVIEW / OFFER / REJECTED / TOTAL */
    private Funnel funnel;

    /** 学生意向城市分布 */
    private List<DashboardVO.DimensionItem> studentCity;
    /** 学生意向行业分布 */
    private List<DashboardVO.DimensionItem> studentIndustry;
    /** 学生期望薪资分桶 */
    private List<DashboardVO.DimensionItem> studentSalary;

    /** 城市供需错配（学生扎堆的前 10 个城市） */
    private List<CityGap> cityGap;

    /** 薪资期望偏差 */
    private SalaryGap salaryGap;

    /** 自主求职流向：城市 */
    private List<DashboardVO.DimensionItem> contactCity;
    /** 自主求职流向：行业 */
    private List<DashboardVO.DimensionItem> contactIndustry;

    /**
     * 投递转化。
     * <p>
     * <b>只统计 job_application</b>，不含历史上投在无主职位的「幽灵投递」。
     * <p>
     * 注意 {@code interview}/{@code offer} 是<b>当前处于</b>该状态的条数，不是「到达过」——
     * 状态机允许 VIEWED 直接跳 OFFER，从当前状态推不出中间经过了什么。
     */
    @Data
    public static class Funnel implements Serializable {
        private static final long serialVersionUID = 1L;

        private long total;
        private long submitted;
        private long viewed;
        private long interview;
        private long offer;
        private long rejected;

        /** 已被 HR 处理过的投递数（= total - submitted） */
        private long responded;
        /** 投出去还没人看的 */
        private long unresponded;
        /** HR 响应时长中位数（小时） */
        private BigDecimal medianResponseHours;

        /** 查看率 = responded / total */
        private BigDecimal viewRate;
        /** 面试率 = (interview + offer) / total */
        private BigDecimal interviewRate;
        /** 录用率 = offer / total */
        private BigDecimal offerRate;
    }

    /** 一个城市的供需错配 */
    @Data
    public static class CityGap implements Serializable {
        private static final long serialVersionUID = 1L;

        private String city;
        /** 想去这个城市的学生占比（%） */
        private BigDecimal studentRatio;
        /** 这个城市的岗位占比（%） */
        private BigDecimal jobRatio;
        /** 错配比 = studentRatio / jobRatio。>1 学生扎堆，<1 岗位过剩，999 表示该城市几乎没有岗位 */
        private BigDecimal gapRatio;
    }

    /** 薪资期望偏差 */
    @Data
    public static class SalaryGap implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 学生期望薪资中位数（元/月） */
        private BigDecimal studentMedian;
        /** 市场薪资中位数（元/月） */
        private BigDecimal marketMedian;
        /** 偏差百分比。正数表示学生期望高于市场 */
        private BigDecimal deviationPercent;
    }
}
