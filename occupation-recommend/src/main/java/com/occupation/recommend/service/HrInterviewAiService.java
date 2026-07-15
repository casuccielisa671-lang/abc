package com.occupation.recommend.service;

import com.occupation.recommend.vo.InterviewQuestionVO;

/**
 * HR 端 AI 面试问题生成服务
 *
 * @author occupation-team
 */
public interface HrInterviewAiService {

    /**
     * 根据 JD 和候选人简历生成针对性面试问题
     *
     * @param jobId      职位ID
     * @param applicantId 候选人ID（可选，传null则只基于JD生成通用问题）
     * @return 分类面试问题
     */
    InterviewQuestionVO generateQuestions(Long jobId, Long applicantId);
}
