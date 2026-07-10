package com.occupation.recommend.service;

import com.occupation.recommend.vo.AdvisorReplyVO;
import com.occupation.recommend.vo.TeachingSuggestionVO;

/**
 * 教学建议的 AI 解读
 *
 * @author occupation-team
 */
public interface TeachingAiService {

    /**
     * 把结构化的技能缺口诊断翻译成一段面向教研室的建议。
     * <p>
     * AI 不可用时返回规则化文字（{@code aiGenerated=false}），不抛异常。
     */
    AdvisorReplyVO analyze(TeachingSuggestionVO suggestion);
}
