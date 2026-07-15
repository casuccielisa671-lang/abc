package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 简历筛选结果 VO
 *
 * @author occupation-team
 */
@Data
public class ResumeScreenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 候选人 userId */
    private Long userId;

    /** 候选人姓名 */
    private String realName;

    /** 匹配度评分 (0-100)，纯摘要模式为 null */
    private Integer matchScore;

    /** AI 生成的候选人摘要 */
    private String summary;

    /** 候选人亮点 */
    private List<String> highlights;

    /** 风险点/注意事项 */
    private List<String> risks;

    /** 与 JD 的匹配分析（仅 screen 模式） */
    private MatchAnalysis matchAnalysis;

    /** 是否 AI 生成 */
    private boolean aiGenerated;

    @Data
    public static class MatchAnalysis implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 匹配的优势点 */
        private List<String> strengths;
        /** 技能差距 */
        private List<String> gaps;
        /** 综合建议 */
        private String suggestion;
    }
}
