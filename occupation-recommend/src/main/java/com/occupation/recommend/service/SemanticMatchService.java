package com.occupation.recommend.service;

import com.occupation.recommend.vo.SemanticMatchVO;

/**
 * JD-简历语义匹配服务。
 * <p>
 * 利用大模型做 JD 文本与简历文本的语义相似度计算，弥补规则匹配中
 * "Java" 和 "Spring Boot" 被视为完全无关技能的问题。
 * <p>
 * 缓存策略：同一学生对同一职位的语义匹配结果缓存 24 小时（Redis）。
 * AI 不可用时返回空结果，不影响主流程。
 *
 * @author occupation-team
 */
public interface SemanticMatchService {

    /**
     * 计算 JD 与简历的语义匹配度。
     *
     * @param userId 学生 ID
     * @param jobId  职位 ID
     * @param jdText    JD 描述文本
     * @param resumeText 简历文本
     * @return 语义匹配结果，AI 不可用时 similarity=0, aiGenerated=false
     */
    SemanticMatchVO match(Long userId, Long jobId, String jdText, String resumeText);
}
