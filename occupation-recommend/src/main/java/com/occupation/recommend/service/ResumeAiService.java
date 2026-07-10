package com.occupation.recommend.service;

import com.occupation.recommend.vo.ResumeReviewVO;

/**
 * 简历 AI 能力 — 诊断与润色
 *
 * @author occupation-team
 */
public interface ResumeAiService {

    /**
     * 诊断简历。
     * <p>
     * AI 不可用时返回规则化诊断（{@code aiGenerated=false}），不抛异常。
     *
     * @param userId      学生
     * @param targetJobId 对标岗位，可为 null（此时以市场热门技能为基准）
     * @param refresh     true=强制重新调用大模型；false=命中缓存则直接返回上次结果
     */
    ResumeReviewVO review(Long userId, Long targetJobId, boolean refresh);

    /**
     * 润色一段简历文字。
     * <p>
     * 润色没有合理的规则化降级（拿什么改写？），AI 不可用时直接抛业务异常，
     * 由前端提示「AI 未启用」。
     *
     * @param section 所属板块，如「自我评价」「项目经历」，作为改写风格的上下文
     * @param text    原文
     * @return 润色后的文字
     */
    String polish(String section, String text);
}
