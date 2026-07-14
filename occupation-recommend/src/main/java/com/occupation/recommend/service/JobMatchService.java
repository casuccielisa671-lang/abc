package com.occupation.recommend.service;

import com.occupation.recommend.vo.MatchJobVO;

import java.util.List;

/**
 * 职位匹配服务 — 推荐模块核心算法
 * <p>
 * 规则打分模型（总分 100）：
 * <pre>
 * 技能匹配 35 分 — 学生技能与职位技能标签的交集覆盖率 × 35
 * 行业匹配 20 分 — 意向行业与职位行业一致 20 分，否则 0
 * 城市匹配 20 分 — 意向城市一致 20 分，否则 0
 * 薪资匹配 15 分 — 期望区间与职位区间有交集 15 分，否则 0
 * 学历匹配 10 分 — 学历 ≥ 职位要求 10 分，低一档 3 分，其余 0
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
     * 分栏推荐：可投递（HR 站内）与市场参考（采集）两栏各自独立取前 perCategory 名。
     * <p>
     * 与 {@link #match} 的区别：match 是单一混合榜单取 Top N，数量众多的市场参考职位
     * 会把可投递职位挤出榜单；本方法让两栏互不抢名额，各自展示最匹配的若干条。
     *
     * @param userId      学生用户 ID
     * @param perCategory 每栏返回条数（如 25）
     * @return 可投递在前、市场参考在后的合并列表（前端按 applicable 分栏渲染）
     */
    List<MatchJobVO> matchGrouped(Long userId, int perCategory);

    /**
     * 对单个职位打分（与 {@link #match} 同一套规则，不受「已投递则排除」的过滤影响）。
     * <p>
     * 供「AI 解读这条推荐」按需调用 —— 推荐列表一次 20 条，逐条调大模型既慢又贵，
     * 所以列表只给规则分，用户点开某一条时才生成自然语言解读。
     */
    MatchJobVO scoreOne(Long userId, Long jobId);
}
