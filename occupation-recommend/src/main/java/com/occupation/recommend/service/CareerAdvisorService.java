package com.occupation.recommend.service;

import com.occupation.common.ai.AiMessage;
import com.occupation.recommend.vo.AdvisorReplyVO;

import java.util.List;

/**
 * AI 职业顾问 — 结合学生画像与真实市场数据的对话式咨询
 *
 * @author occupation-team
 */
public interface CareerAdvisorService {

    /**
     * 一轮对话。
     *
     * @param userId  学生
     * @param history 历史消息（不含 system；服务端自行注入画像与市场上下文）
     * @return 顾问回复
     */
    AdvisorReplyVO chat(Long userId, List<AiMessage> history);

    /**
     * 单条职位的匹配理由自然语言化。
     * <p>
     * AI 不可用时回落到规则拼出的 matchReason，{@code aiGenerated=false}。
     */
    AdvisorReplyVO explainMatch(Long userId, Long jobId);
}
