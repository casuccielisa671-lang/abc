package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 教学建议（技能缺口诊断）出参
 * <p>
 * 全部字段由真实数据算出：市场热度来自 analysis_result（dimension=skill），
 * 掌握率来自本租户学生画像的 skills 字段。不含任何预置文案或随机数。
 *
 * @author occupation-team
 */
@Data
public class TeachingSuggestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参与统计的学生数（已填写画像的人数）——掌握率的分母 */
    private int studentsWithProfile;

    /** 技能缺口明细，按缺口程度降序 */
    private List<SkillGap> skillGaps;

    /** 课程改革建议，由缺口最大的技能生成 */
    private List<CourseSuggestion> courseSuggestions;

    /**
     * 单个技能的缺口诊断。
     * <p>
     * marketDemand 是相对热度：该技能岗位数 ÷ 最热技能岗位数 × 100，
     * 因此排名第一的技能恒为 100。它衡量的是「相对于最热门技能的需求强度」，
     * 不是「百分之多少的岗位要求它」。
     */
    @Data
    public static class SkillGap implements Serializable {
        private static final long serialVersionUID = 1L;

        private String skill;
        /** 市场相对热度 0~100 */
        private int marketDemand;
        /** 学生掌握率 0~100 */
        private int studentRate;
        /** 缺口 = max(0, marketDemand - studentRate) */
        private int gap;
        /** 证据：市场上要求该技能的岗位数 */
        private long jobCount;
        /** 证据：掌握该技能的学生数 */
        private long masteredCount;
        /** 依据上述数字生成的建议文案 */
        private String suggestion;
    }

    /** 课程改革建议条目 */
    @Data
    public static class CourseSuggestion implements Serializable {
        private static final long serialVersionUID = 1L;

        private long id;
        private String title;
        private String description;
        /** HIGH / MEDIUM / LOW，由 gap 分档 */
        private String priority;
    }
}
