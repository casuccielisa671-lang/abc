package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 期望薪资计算器视图 — 学生端工具箱
 *
 * @author occupation-team
 */
@Data
public class SalaryCalcVO {

    /** 建议期望薪资下限 */
    private int suggestedMin;

    /** 建议期望薪资上限 */
    private int suggestedMax;

    /** 市场中位数 */
    private int marketMedian;

    /** 市场 P25 */
    private int marketP25;

    /** 市场 P75 */
    private int marketP75;

    /** 样本数量 */
    private int sampleCount;

    /** 按城市分组的薪资参考 */
    private List<CitySalary> cityBreakdown;

    /** 按学历分组的薪资参考 */
    private List<EducationSalary> educationBreakdown;

    @Data
    public static class CitySalary {
        private String city;
        private int avgSalary;
        private int jobCount;
    }

    @Data
    public static class EducationSalary {
        private String education;
        private int avgSalary;
        private int jobCount;
    }
}
