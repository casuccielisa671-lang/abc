package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 技能 ROI 分析视图 — 学生端工具箱
 *
 * @author occupation-team
 */
@Data
public class SkillRoiVO {

    /** 技能名称 */
    private String skillName;

    /** 要求该技能的职位数 */
    private int jobCount;

    /** 市场占比（要求该技能的职位 / 总职位） */
    private double marketShare;

    /** 平均薪资（元） */
    private int avgSalary;

    /** 薪资中位数 */
    private int medianSalary;

    /** 薪资增长潜力（有该技能 vs 无该技能的平均薪资差） */
    private int salaryPremium;

    /** 相关岗位 Top 5 */
    private List<RelatedJob> relatedJobs;

    /** 学习建议 */
    private List<String> suggestions;

    @Data
    public static class RelatedJob {
        private String title;
        private int jobCount;
        private int avgSalary;
    }
}
