package com.occupation.recommend.service;

import com.occupation.common.ai.AiMessage;
import com.occupation.recommend.vo.JdOptimizeVO;

import java.util.List;

/**
 * HR 端 AI JD 服务 — JD 生成、分析、多轮优化
 *
 * @author occupation-team
 */
public interface HrJdAiService {

    /**
     * AI 生成 JD
     *
     * @param title           职位名称
     * @param company         公司名称
     * @param city            工作城市
     * @param salaryMin       最低薪资
     * @param salaryMax       最高薪资
     * @param education       学历要求
     * @param experienceYears 经验年限
     * @param skills          核心技能列表
     * @param style           风格（professional/startup/foreign）
     * @return 生成的 JD 文本
     */
    String generate(String title, String company, String city,
                    Integer salaryMin, Integer salaryMax,
                    String education, Integer experienceYears,
                    List<String> skills, String style);

    /**
     * AI 分析 JD 质量
     *
     * @param jdContent JD 文本
     * @return 分析结果（评分 + 维度 + 建议）
     */
    JdOptimizeVO analyze(String jdContent);

    /**
     * AI 多轮 JD 优化
     *
     * @param jdContent JD 原文
     * @param history   对话历史（不含 system）
     * @return 优化后的 JD 文本
     */
    String optimize(String jdContent, List<AiMessage> history);
}
