package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 学生就业数据报告的数据载体（按 专业/年级/班级 scope 聚合）。
 * <p>
 * 供 occupation-report 生成 EMPLOYMENT 类报告使用。所有数字都限定在 scope 内的学生，
 * 与教师端可见范围口径一致；投递只统计 {@code job_application}（不含幽灵投递）。
 *
 * @author occupation-team
 */
@Data
public class EmploymentReportData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 范围标签：如「软件工程-2022-1班」/「计算机科学与技术 专业」/「2022 届」/「全校」 */
    private String scopeLabel;

    /** 范围内学生总数 */
    private int studentCount;
    /** 已填画像的学生数 */
    private int profiledCount;

    /** 总投递数（job_application） */
    private int applicationCount;
    /** 投递过的学生数 */
    private int appliedStudentCount;
    /** 拿到 OFFER 的投递数 */
    private int offerCount;
    /** OFFER 率 = offerCount / applicationCount（百分比，保留 1 位） */
    private double offerRate;

    /** 投递状态分布（SUBMITTED/VIEWED/INTERVIEW/OFFER/REJECTED） */
    private List<DimItem> funnel;
    /** 意向城市分布 */
    private List<DimItem> intentCity;
    /** 意向行业分布 */
    private List<DimItem> intentIndustry;
    /** 期望薪资分桶 */
    private List<DimItem> salaryBuckets;
    /** 学生掌握的热门技能 Top N */
    private List<DimItem> topSkills;

    @Data
    public static class DimItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private long value;

        public DimItem() {
        }

        public DimItem(String name, long value) {
            this.name = name;
            this.value = value;
        }
    }
}
