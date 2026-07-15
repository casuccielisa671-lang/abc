package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 面试问题 VO
 *
 * @author occupation-team
 */
@Data
public class InterviewQuestionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 技术能力问题 */
    private List<QuestionItem> technical;

    /** 项目经验问题 */
    private List<QuestionItem> project;

    /** 行为/软技能问题 */
    private List<QuestionItem> behavioral;

    /** 是否 AI 生成 */
    private boolean aiGenerated;

    @Data
    public static class QuestionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 问题内容 */
        private String question;
        /** 考察要点 */
        private String purpose;
        /** 参考答案要点 */
        private String expectedAnswer;
    }
}
