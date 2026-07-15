package com.occupation.recommend.service;

import com.occupation.common.ai.AiMessage;
import com.occupation.recommend.vo.ResumeReviewVO;

import java.util.List;

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

    /**
     * 多轮润色聊天 — 支持用户持续提要求（如"再精简一点""突出技术栈"）。
     * <p>
     * 前端维护完整对话历史（含 system prompt 外的所有轮次），
     * 后端负责拼接 system prompt 后发给大模型。
     *
     * @param section      所属板块
     * @param originalText 原文（首次润色时传入，后续轮次可为空）
     * @param history      对话历史（不含 system prompt）
     * @param userMessage  本轮用户指令
     * @return AI 回复（润色后的文字）
     */
    String polishChat(String section, String originalText, List<AiMessage> history, String userMessage);
}
