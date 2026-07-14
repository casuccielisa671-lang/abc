package com.occupation.recommend.service;

import com.occupation.analysis.vo.JobDetailVO;

import java.util.List;

/**
 * 协同过滤推荐服务 — User-Based Collaborative Filtering
 * <p>
 * 基于学生行为（投递/收藏）构建用户-职位交互矩阵，通过余弦相似度
 * 找到与目标学生行为模式最相似的 Top K 个邻居，聚合邻居投递过但目标学生
 * 未接触过的职位作为推荐结果。
 * <p>
 * 计算时机：每次查询时实时计算（行为数据量可控，无需离线预计算）。
 *
 * @author occupation-team
 */
public interface CollaborativeFilterService {

    /**
     * 基于协同过滤为用户推荐职位。
     * <p>
     * 流程：找到与目标用户行为相似的 K 个邻居 → 聚合邻居投递/收藏的职位 →
     * 按被邻居交互次数加权排序 → 排除目标用户已交互的职位 → 返回 Top N。
     *
     * @param userId 目标学生 ID
     * @param topN   返回条数
     * @return 协同过滤推荐职位列表
     */
    List<JobDetailVO> recommend(Long userId, int topN);
}
