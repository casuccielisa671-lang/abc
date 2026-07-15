package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 简历 AI 诊断结果
 * <p>
 * {@code aiGenerated=false} 表示大模型不可用、本次是规则化降级结果 ——
 * 前端据此给出「AI 未启用，以下为规则诊断」的提示，避免把模板文字当成 AI 输出。
 *
 * @author occupation-team
 */
@Data
public class ResumeReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 综合评分 0~100 */
    private Integer score;

    /** 一句话总评 */
    private String summary;

    /** 亮点 */
    private List<String> strengths;

    /** 待改进项 */
    private List<String> weaknesses;

    /** 逐条改进建议 */
    private List<Suggestion> suggestions;

    /** 相对目标岗位缺失的技能（无目标岗位时为空） */
    private List<String> missingSkills;

    /** 本次诊断对标的岗位标题，无则为 null */
    private String targetJobTitle;

    /** true=大模型生成，false=规则降级 */
    private boolean aiGenerated;

    /** 与同专业同龄人相比的竞争力评估（如 "前 30%"、"中等偏上"、"需大幅提升"） */
    private String marketCompetitiveness;

    /** 一条改进建议：命中哪个板块、问题是什么、怎么改 */
    @Data
    public static class Suggestion implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 自我评价 / 项目经历 / 实习经历 / 教育经历 / 技能 */
        private String section;

        private String issue;

        private String advice;

        /** 优先级：高/中/低 */
        private String priority;

        /** 预计提升效果描述 */
        private String expectedEffect;
    }
}
