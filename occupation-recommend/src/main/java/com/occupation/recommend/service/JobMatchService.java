package com.occupation.recommend.service;

import com.occupation.recommend.vo.MatchJobVO;

import java.util.List;

/**
 * 职位匹配服务 — 推荐模块核心算法
 * <p>
 * 规则打分模型（总分 100）：
 * <pre>
 * 技能匹配 40 分 — 学生技能与职位技能标签的交集覆盖率 × 40
 * 城市匹配 25 分 — 意向城市一致 25 分，否则 0
 * 薪资匹配 20 分 — 期望区间与职位区间有交集 20 分，无交集按接近度衰减
 * 学历匹配 15 分 — 学历 ≥ 职位要求 15 分，低一档 5 分，其余 0
 * </pre>
 * 行为反馈加权（P4 进阶）：APPLY 过的职位相似技能 +10%，IGNORE -5%。
 *
 * @author occupation-team
 */
public interface JobMatchService {

    /**
     * 为指定学生计算 Top N 匹配职位
     *
     * @param userId 学生用户 ID
     * @param topN   返回条数（如 20）
     * @return 按匹配分降序的职位列表（含得分、理由、缺失技能）
     */
    List<MatchJobVO> match(Long userId, int topN);

    /**
     * 对单个职位打分（与 {@link #match} 同一套规则，不受「已投递则排除」的过滤影响）。
     * <p>
     * 供「AI 解读这条推荐」按需调用 —— 推荐列表一次 20 条，逐条调大模型既慢又贵，
     * 所以列表只给规则分，用户点开某一条时才生成自然语言解读。
     */
    MatchJobVO scoreOne(Long userId, Long jobId);
}
