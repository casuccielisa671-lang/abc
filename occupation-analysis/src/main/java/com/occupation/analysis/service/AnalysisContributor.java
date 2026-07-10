package com.occupation.analysis.service;

/**
 * 分析扩展点（SPI）
 * <p>
 * <b>为什么需要它</b>：`analysis` 模块只看得到 {@code job_detail}（市场供给侧）。
 * 学生画像、行为、投递这三张表在 `recommend` 模块里，而 `recommend` 依赖 `analysis` ——
 * 反过来依赖会成环。于是由 `analysis` 定义扩展点，`recommend` 实现，Spring 注入。
 * <p>
 * 实现类只需把结果写进 {@code analysis_result}（用 {@link AnalysisResultWriter}），
 * 读侧就能像读内置维度一样读它，看板与报告口径天然一致。
 * <p>
 * 调用时机：{@code AnalysisJobService.runAll()} 跑完内置维度之后。此时租户上下文已就绪。
 *
 * @author occupation-team
 */
public interface AnalysisContributor {

    /** 日志里用的名字 */
    String name();

    /**
     * 计算并写入 analysis_result。
     *
     * @return 写入的记录条数
     */
    int contribute();
}
