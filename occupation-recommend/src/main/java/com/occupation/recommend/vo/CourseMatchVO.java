package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 课程-岗位匹配 VO
 *
 * @author occupation-team
 */
@Data
public class CourseMatchVO {

    /** 关联岗位 */
    private List<RelatedJob> relatedJobs;

    /** 趋势月份标签 */
    private List<String> months;

    /** 趋势数据（与 months 一一对应） */
    private List<Integer> trend;

    /** 技能变化 */
    private List<SkillChange> skillChanges;

    /** 教学建议 */
    private List<String> suggestions;

    @Data
    public static class RelatedJob {
        private String name;
        private Integer relevance;
    }

    @Data
    public static class SkillChange {
        private String name;
        /** 当前需求占比 (0-100) */
        private Integer current;
        /** 变化百分比 */
        private Integer change;
        /** up / down */
        private String trend;
    }
}
