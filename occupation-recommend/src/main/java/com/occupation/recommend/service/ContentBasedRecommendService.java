package com.occupation.recommend.service;

import com.occupation.analysis.vo.JobDetailVO;

import java.util.List;

/**
 * 基于内容的推荐服务 — TF-IDF 技能向量化 + 余弦相似度
 * <p>
 * 将职位技能标签视为文档词袋，计算 TF-IDF 权重向量，通过余弦相似度
 * 找到与目标职位最相似的 Top N 职位。用于「看了这个职位的人还看了...」
 * 和推荐列表的多样性补充。
 * <p>
 * 计算时机：每次查询时实时计算（技能标签维度有限，无需离线预计算）。
 *
 * @author occupation-team
 */
public interface ContentBasedRecommendService {

    /**
     * 基于职位内容相似度推荐。
     *
     * @param jobId 种子职位 ID
     * @param topN  返回条数
     * @return 相似职位列表（按相似度降序，不含种子职位自身）
     */
    List<JobDetailVO> similarJobs(Long jobId, int topN);

    /**
     * 基于学生已投递/收藏的职位，推荐内容相似的其他职位。
     *
     * @param userId 学生 ID
     * @param topN   返回条数
     * @return 内容相似职位列表
     */
    List<JobDetailVO> recommendByUserHistory(Long userId, int topN);
}
