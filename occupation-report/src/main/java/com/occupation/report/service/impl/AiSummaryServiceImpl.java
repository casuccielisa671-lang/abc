package com.occupation.report.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.report.service.AiSummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * AI 摘要实现 — OpenAI 兼容 chat/completions 接口 + 规则降级
 * <p>
 * 设计要点：
 * <ul>
 *   <li>API Key 通过环境变量注入（AI_API_KEY），严禁硬编码（实训规范）</li>
 *   <li>LLM 不可用时降级为模板文字，报告生成链路不因外部依赖失败而中断</li>
 *   <li>Prompt 中只注入聚合统计值，不含任何个人敏感数据</li>
 * </ul>
 *
 * @author occupation-team
 */
@Slf4j
@Service
public class AiSummaryServiceImpl implements AiSummaryService {

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.base-url:}")
    private String baseUrl;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String summarize(DashboardVO dashboard) {
        if (!aiEnabled || apiKey == null || apiKey.isEmpty()) {
            return fallbackSummary(dashboard);
        }
        try {
            return callLlm(buildPrompt(dashboard));
        } catch (Exception e) {
            log.warn("LLM 调用失败，降级为模板摘要: {}", e.getMessage());
            return fallbackSummary(dashboard);
        }
    }

    /** 构造 Prompt：注入 Top 数据，要求输出面向学生/教师的就业形势解读 */
    private String buildPrompt(DashboardVO dashboard) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是高校就业指导中心的数据分析师。请根据以下就业市场统计数据，")
          .append("写一段 300 字以内的就业形势解读，包含：整体趋势判断、值得关注的行业与技能、")
          .append("对在校学生的 1~2 条学习建议。语气专业客观，直接输出正文。\n\n");
        appendTop(sb, "行业岗位数 Top", dashboard.getIndustryTop(), 5);
        appendTop(sb, "城市岗位分布 Top", dashboard.getCityDist(), 5);
        appendTop(sb, "热门技能 Top", dashboard.getSkillHot(), 10);
        appendTop(sb, "学历需求分布", dashboard.getEducationDist(), 5);
        return sb.toString();
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

    /** 调用 OpenAI 兼容 /chat/completions 接口 */
    private String callLlm(String prompt) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        JSONArray messages = new JSONArray();
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 800);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String response = restTemplate.postForObject(
                baseUrl + "/chat/completions",
                new HttpEntity<>(body.toJSONString(), headers),
                String.class);

        JSONObject json = JSON.parseObject(response);
        return json.getJSONArray("choices")
                   .getJSONObject(0)
                   .getJSONObject("message")
                   .getString("content");
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
