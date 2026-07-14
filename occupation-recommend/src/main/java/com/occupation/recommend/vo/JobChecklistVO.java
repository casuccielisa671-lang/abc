package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 求职清单生成器视图 — 学生端工具箱
 *
 * @author occupation-team
 */
@Data
public class JobChecklistVO {

    /** 目标岗位名称 */
    private String jobTitle;

    /** 综合匹配度（0-100） */
    private int matchScore;

    /** 技能差距分析 */
    private List<SkillGap> skillGaps;

    /** 学习路径建议 */
    private List<LearningStep> learningPath;

    /** 简历优化建议 */
    private List<String> resumeTips;

    /** 推荐学习资源 */
    private List<String> resources;

    @Data
    public static class SkillGap {
        /** 技能名称 */
        private String skill;
        /** 岗位要求程度：required / preferred */
        private String requirement;
        /** 学生是否具备 */
        private boolean possessed;
        /** 差距描述 */
        private String description;
    }

    @Data
    public static class LearningStep {
        /** 步骤序号 */
        private int order;
        /** 步骤标题 */
        private String title;
        /** 步骤描述 */
        private String description;
        /** 预计耗时 */
        private String estimatedTime;
    }
}
