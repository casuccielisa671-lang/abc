package com.occupation.analysis.service;

/**
 * 统计分析 Job 服务 — 数据管道第三环（清洗 → 统计 → 结果表）
 * <p>
 * 从 job_detail 全量聚合出 5 个维度的统计指标，写入 analysis_result 表，
 * 供 Dashboard / 报告引擎 / 开放 API 读取（读写分离：在线接口只查结果表，不做实时聚合）。
 * <p>
 * 维度与指标约定（与 AnalysisResult 实体一致）：
 * <pre>
 * dimension   dimension_value   metric_name                     说明
 * industry    行业名             job_count / avg_salary          行业岗位数、平均薪资
 * city        城市名             job_count / avg_salary          城市岗位数、平均薪资
 * education   学历档             job_count                       学历需求分布
 * skill       技能名             job_count                       技能热度（出现频次）
 * trend       -                 job_count / avg_salary          按月趋势（period_value=yyyy-MM）
 * </pre>
 * 实现方式说明：实训环境数据量（万级）用 SQL 聚合 + Java 内存计算即可秒级完成；
 * 如需展示大数据技术点，可将本接口的实现替换为 Spark local 模式跑批
 * （SparkSession 读 MySQL → groupBy 聚合 → 写回 analysis_result，接口不变）。
 *
 * @author occupation-team
 */
public interface AnalysisJobService {

    /**
     * 全量重算所有维度（先删当期旧数据再写入，保证可重复执行）
     *
     * @return 本次写入 analysis_result 的记录数
     */
    int runAll();

    /** 行业维度统计 */
    int analyzeIndustry();

    /** 城市维度统计 */
    int analyzeCity();

    /** 学历维度统计 */
    int analyzeEducation();

    /** 技能热度统计（解析 job_detail.skills JSON 数组，词频 Top 100） */
    int analyzeSkill();

    /** 时间趋势统计（按发布月份聚合岗位数与均薪） */
    int analyzeTrend();
}
