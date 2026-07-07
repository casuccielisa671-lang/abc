package com.occupation.analysis.service;

/**
 * 数据清洗服务 — 数据管道第二环（采集 → 清洗 → 分析）
 * <p>
 * 职责：把爬虫采集的原始 JSON（raw_job_data.raw_content）清洗为
 * 结构化的 job_detail 记录。清洗规则：
 * <ol>
 *   <li>去重：按 source_url 判断，已存在则跳过</li>
 *   <li>字段标准化：薪资统一为"元/月"整数、城市名去后缀（"北京市"→"北京"）、学历映射枚举</li>
 *   <li>技能标签提取：保留 skills 数组；缺失时从 description 关键词匹配（TODO）</li>
 *   <li>非法数据丢弃：无标题/无公司/薪资为负 的记录不入库</li>
 * </ol>
 *
 * @author occupation-team
 */
public interface DataCleanService {

    /**
     * 清洗一条原始职位数据并写入 job_detail
     *
     * @param rawContent 原始 JSON 字符串（爬虫解析出的职位字段）
     * @param source     数据来源标识（MOCK / BOSS_ZHIPIN / ...）
     * @param sourceUrl  来源 URL（用于去重）
     * @return true=入库成功；false=重复或非法数据被跳过
     */
    boolean cleanAndSave(String rawContent, String source, String sourceUrl);

    /**
     * 批量清洗 raw_job_data 表中 status=RAW 的存量数据（补偿任务，供定时调度调用）
     *
     * @return 本次成功清洗入库的记录数
     */
    int cleanPendingRawData();
}
