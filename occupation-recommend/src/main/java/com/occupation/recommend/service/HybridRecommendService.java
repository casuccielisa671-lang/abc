package com.occupation.recommend.service;

import com.occupation.recommend.vo.MatchJobVO;

import java.util.List;

/**
 * 混合推荐引擎 — 融合多种推荐策略的加权混合推荐
 * <p>
 * 融合策略：
 * <ol>
 *   <li><b>规则打分 + 语义匹配</b>（JobMatchService）：技能/城市/薪资/学历 + AI 语义</li>
 *   <li><b>协同过滤</b>（CollaborativeFilterService）：相似用户群体的行为推荐</li>
 *   <li><b>基于内容的推荐</b>（ContentBasedRecommendService）：技能标签 TF-IDF 相似度</li>
 * </ol>
 * <p>
 * 混合策略：以规则打分为主（占 70%），协同过滤和内容推荐作为多样性补充（各占 15%），
 * 最终去重合并，保证推荐结果既有精准度又有多样性。
 *
 * @author occupation-team
 */
public interface HybridRecommendService {

    /**
     * 混合推荐：融合多种策略的 Top N 职位推荐。
     *
     * @param userId 学生 ID
     * @param topN   返回条数
     * @return 混合推荐结果（含得分、理由、缺失技能）
     */
    List<MatchJobVO> recommend(Long userId, int topN);
}
