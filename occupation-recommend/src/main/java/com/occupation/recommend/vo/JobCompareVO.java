package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 多岗位对比视图 — 学生端工具箱
 *
 * @author occupation-team
 */
@Data
public class JobCompareVO {

    /** 参与对比的岗位列表 */
    private List<JobItem> jobs;

    /** 对比总结 */
    private CompareSummary summary;

    @Data
    public static class JobItem {
        private Long id;
        private String title;
        private String company;
        private String city;
        private String industry;
        private String salaryRange;
        private Integer salaryMin;
        private Integer salaryMax;
        private String education;
        private String experience;
        private List<String> skills;
        private String publishDate;
    }

    @Data
    public static class CompareSummary {
        /** 薪资最高的岗位名 */
        private String highestSalary;
        /** 薪资最低的岗位名 */
        private String lowestSalary;
        /** 共同技能 */
        private List<String> commonSkills;
        /** 各岗位独有技能 */
        private Map<String, List<String>> uniqueSkills;
    }
}
