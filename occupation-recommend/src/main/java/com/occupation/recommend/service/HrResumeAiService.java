package com.occupation.recommend.service;

import com.occupation.recommend.vo.ResumeScreenVO;

/**
 * HR 端 AI 简历筛选服务 — 简历摘要、JD匹配分析、候选人排序
 *
 * @author occupation-team
 */
public interface HrResumeAiService {

    /**
     * AI 简历摘要分析
     *
     * @param userId 候选人用户ID
     * @param jobId  目标职位ID（可选，传null则只做通用摘要）
     * @return 简历分析结果
     */
    ResumeScreenVO screen(Long userId, Long jobId);

    /**
     * 批量 AI 匹配排序
     *
     * @param jobId       目标职位ID
     * @param applicantIds 候选人ID列表
     * @return 按匹配度排序的候选人列表
     */
    java.util.List<ResumeScreenVO> rankByMatch(Long jobId, java.util.List<Long> applicantIds);
}
