package com.occupation.report.service.impl;

import com.occupation.analysis.vo.DashboardVO;
import com.occupation.common.ai.AiChatClient;
import com.occupation.report.service.AiSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 摘要实现 — 调用 {@link AiChatClient} + 规则降级
 * <p>
 * 设计要点：
 * <ul>
 *   <li>API Key 由 {@code app.ai.api-key} 注入（环境变量或 gitignored 的 application-local.yml），严禁硬编码</li>
 *   <li>LLM 不可用时降级为模板文字，报告生成链路不因外部依赖失败而中断</li>
 *   <li>Prompt 中只注入聚合统计值，不含任何个人敏感数据</li>
 * </ul>
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    /** 角色设定与输出约束分离到 system，用户消息只放数据 —— 模型更不容易跑题 */
    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是高校就业指导中心的资深数据分析师，面向学生和教师解读就业市场数据。",
            "要求：",
            "① 结论先行，先给判断再给依据；",
            "② 必须引用给定数据中的具体数字，不得编造未提供的数据；",
            "③ 不使用 Markdown 标题与列表符号，输出连贯段落；",
            "④ 全文 600-800 字；",
            "⑤ 按以下四大板块组织：",
            "  a. 整体趋势判断：引用同比/环比变化，指出市场是扩张还是收缩",
            "  b. 行业与城市热点分析：哪些行业/城市需求旺盛，哪些在降温",
            "  c. 技能需求变化：哪些技能需求上升、哪些下降，指出「值得关注的信号」",
            "  d. 对学生/教师的建议：基于数据给出 2-3 条具体可执行的建议");

    private final AiChatClient aiChatClient;

    @Override
    public String summarize(DashboardVO dashboard) {
        if (!aiChatClient.isEnabled()) {
            return fallbackSummary(dashboard);
        }
        try {
            return aiChatClient.ask(SYSTEM_PROMPT, buildPrompt(dashboard));
        } catch (Exception e) {
            log.warn("LLM 调用失败，降级为模板摘要: {}", e.getMessage());
            return fallbackSummary(dashboard);
        }
    }

    /** 构造 Prompt：注入 Top 数据 + 薪资分布 + 月度趋势，要求输出面向学生/教师的就业形势解读 */
    private String buildPrompt(DashboardVO dashboard) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是本校就业服务平台采集到的岗位市场统计数据，请据此写一段就业形势解读。\n\n");
        appendTop(sb, "行业岗位数 Top", dashboard.getIndustryTop(), 5);
        appendTop(sb, "城市岗位分布 Top", dashboard.getCityDist(), 5);
        appendTop(sb, "热门技能 Top", dashboard.getSkillHot(), 10);
        appendTop(sb, "学历需求分布", dashboard.getEducationDist(), 5);
        appendSalary(sb, dashboard);
        appendTrend(sb, dashboard);
        sb.append("\n请按四大板块（整体趋势判断 → 行业与城市热点分析 → 技能需求变化 → 对学生/教师的建议）组织内容，")
          .append("引用同比/环比变化，指出「值得关注的信号」。全文 600-800 字。");
        return sb.toString();
    }

    private void appendSalary(StringBuilder sb, DashboardVO dashboard) {
        if (dashboard.getTrend() == null || dashboard.getTrend().isEmpty()) {
            return;
        }
        // 取最近两期趋势数据，计算薪资变化
        List<DashboardVO.TrendItem> trends = dashboard.getTrend();
        DashboardVO.TrendItem latest = trends.get(trends.size() - 1);
        sb.append("\n薪资水平：最近一期（").append(latest.getPeriod()).append("）")
          .append("平均薪资 ").append(latest.getAvgSalary()).append(" 元/月，")
          .append("岗位数 ").append(latest.getJobCount()).append(" 个。");
        if (trends.size() >= 2) {
            DashboardVO.TrendItem prev = trends.get(trends.size() - 2);
            sb.append("上期（").append(prev.getPeriod()).append("）")
              .append("平均薪资 ").append(prev.getAvgSalary()).append(" 元/月，")
              .append("岗位数 ").append(prev.getJobCount()).append(" 个。");
        }
        sb.append('\n');
    }

    private void appendTrend(StringBuilder sb, DashboardVO dashboard) {
        if (dashboard.getTrend() == null || dashboard.getTrend().size() < 2) {
            return;
        }
        sb.append("\n月度趋势对比（最近 6 期）：\n");
        List<DashboardVO.TrendItem> trends = dashboard.getTrend();
        int start = Math.max(0, trends.size() - 6);
        for (int i = start; i < trends.size(); i++) {
            DashboardVO.TrendItem t = trends.get(i);
            sb.append(t.getPeriod()).append("：岗位 ").append(t.getJobCount())
              .append("，均薪 ").append(t.getAvgSalary()).append('\n');
        }
    }

    private void appendTop(StringBuilder sb, String title, java.util.List<DashboardVO.DimensionItem> items, int limit) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append(title).append("：");
        items.stream().limit(limit).forEach(i ->
                sb.append(i.getName()).append("(").append(i.getValue()).append(") "));
        sb.append("\n");
    }

    /** 规则降级：用统计数据拼一段结构化文字，保证报告永远有"摘要"章节 */
    private String fallbackSummary(DashboardVO dashboard) {
        StringBuilder sb = new StringBuilder("本期就业市场概况：");
        if (dashboard.getIndustryTop() != null && !dashboard.getIndustryTop().isEmpty()) {
            DashboardVO.DimensionItem top = dashboard.getIndustryTop().get(0);
            sb.append("岗位需求最大的行业为「").append(top.getName()).append("」；");
        }
        if (dashboard.getSkillHot() != null && !dashboard.getSkillHot().isEmpty()) {
            sb.append("当前热度最高的技能包括 ");
            dashboard.getSkillHot().stream().limit(5)
                     .forEach(s -> sb.append(s.getName()).append("、"));
            sb.setLength(sb.length() - 1);
            sb.append(" 等；");
        }
        if (dashboard.getCityDist() != null && !dashboard.getCityDist().isEmpty()) {
            sb.append("岗位集中度最高的城市为「")
              .append(dashboard.getCityDist().get(0).getName()).append("」。");
        }
        sb.append("建议在校学生结合上述热门技能规划学习路径，关注目标城市与行业的用人趋势。");
        return sb.toString();
    }
}
