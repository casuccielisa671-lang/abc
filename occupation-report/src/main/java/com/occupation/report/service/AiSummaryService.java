package com.occupation.report.service;

import com.occupation.analysis.vo.DashboardVO;

/**
 * AI 智能摘要服务 — 差异化亮点：让报告"像分析师一样写洞察"
 * <p>
 * 输入分析数据，输出一段自然语言的就业形势解读（趋势判断 + 建议）。
 * 通过 OpenAI 兼容接口调用 LLM（DeepSeek / 通义千问，配置见 app.ai.*）；
 * AI 关闭或调用失败时自动降级为规则模板文字，保证报告链路永不阻塞。
 * <p>
 * 对应课程知识点：SpringAI / LLM 应用（第 6 天教学内容的落地场景）。
 *
 * @author occupation-team
 */
public interface AiSummaryService {

    /**
     * 基于 Dashboard 分析数据生成就业形势摘要
     *
     * @param dashboard 5 维度分析数据
     * @return 一段 200~400 字的自然语言解读（LLM 生成或模板降级）
     */
    String summarize(DashboardVO dashboard);
}
