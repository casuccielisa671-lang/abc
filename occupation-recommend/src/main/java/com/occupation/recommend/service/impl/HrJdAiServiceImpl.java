package com.occupation.recommend.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.occupation.common.ai.AiChatClient;
import com.occupation.common.ai.AiMessage;
import com.occupation.recommend.service.HrJdAiService;
import com.occupation.recommend.vo.JdOptimizeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HR 端 AI JD 服务实现
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrJdAiServiceImpl implements HrJdAiService {

    private static final int MAX_HISTORY = 10;
    private static final int MAX_MESSAGE_CHARS = 3000;
    private static final double JD_TEMPERATURE = 0.5;

    private static final String GENERATE_SYSTEM = String.join("\n",
            "你是一位资深 HR 招聘专家，擅长撰写高质量的职位描述（JD）。",
            "你的 JD 必须结构清晰、用词专业、有吸引力，包含以下模块：",
            "1. 职位概述（2-3句话，说明团队定位和岗位价值）",
            "2. 岗位职责（3-5条，每条以动词开头，量化可衡量）",
            "3. 任职要求（分必备和加分项，技能明确、年限具体）",
            "4. 福利待遇（具体可感知，不写\u201c面议\u201d）",
            "要求：",
            "- 避免空洞词汇（如\u201c负责相关工作\u201d），每条职责要有具体产出",
            "- 技能要求使用行业通用名称，不缩写",
            "- 福利要具体（如\u201c双休\u201d\u201c年度体检\u201d\u201c技术书籍报销\u201d），不写\u201c薪资丰厚\u201d\u201c待遇从优\u201d",
            "- 整体控制在 800 字以内",
            "- 直接输出 JD 正文，不要加\u201c以下是生成的JD\u201d之类的引导语");

    private static final String ANALYZE_SYSTEM = String.join("\n",
            "你是一位 JD 质量评审专家。请分析以下职位描述，输出严格的 JSON 格式。",
            "评分维度：",
            "- completeness（完整性）：是否包含职责、要求、福利等关键模块",
            "- attractiveness（吸引力）：用词是否积极、是否有亮点和成长空间",
            "- compliance（合规性）：是否有歧视性用语、是否合规",
            "- specificity（具体性）：职责是否量化、技能是否明确",
            "",
            "输出格式（严格 JSON）：",
            "{",
            "  \"score\": 78,",
            "  \"dimensions\": [",
            "    {\"name\": \"完整性\", \"score\": 85},",
            "    {\"name\": \"吸引力\", \"score\": 72},",
            "    {\"name\": \"合规性\", \"score\": 95},",
            "    {\"name\": \"具体性\", \"score\": 60}",
            "  ],",
            "  \"suggestions\": [",
            "    \"建议增加福利待遇描述，具体列出五险一金、假期、培训等\",",
            "    \"职责描述可以更具体，增加量化指标如'负责日均XX万UV的系统'\"",
            "  ]",
            "}",
            "score 为 0-100 的综合评分，suggestions 为 3-5 条具体可执行的改进建议。");

    private static final String OPTIMIZE_SYSTEM = String.join("\n",
            "你是一位 JD 优化专家。根据 HR 的要求优化职位描述。",
            "规则：",
            "1. 保留原文的核心信息（职位、技能要求、薪资范围等），只优化表达方式",
            "2. 严格遵循 HR 的优化指令（如\u201c更简洁\u201d\u201c突出成长空间\u201d\u201c增加量化指标\u201d）",
            "3. 优化后的 JD 保持结构清晰：职位概述 → 岗位职责 → 任职要求 → 福利待遇",
            "4. 不要编造原文没有的信息（如原文没写薪资就不要加）",
            "5. 直接输出优化后的完整 JD，不要加解释或引导语");

    private final AiChatClient aiChatClient;

    @Override
    public String generate(String title, String company, String city,
                           Integer salaryMin, Integer salaryMax,
                           String education, Integer experienceYears,
                           List<String> skills, String style) {
        if (!aiChatClient.isEnabled()) {
            return buildFallbackJd(title, company, city, salaryMin, salaryMax,
                    education, experienceYears, skills, style);
        }

        String userPrompt = buildGeneratePrompt(title, company, city,
                salaryMin, salaryMax, education, experienceYears, skills, style);

        try {
            return aiChatClient.ask(GENERATE_SYSTEM, userPrompt);
        } catch (Exception e) {
            log.warn("AI JD 生成失败，降级为模板: {}", e.getMessage());
            return buildFallbackJd(title, company, city, salaryMin, salaryMax,
                    education, experienceYears, skills, style);
        }
    }

    @Override
    public JdOptimizeVO analyze(String jdContent) {
        if (!aiChatClient.isEnabled()) {
            return buildFallbackAnalysis(jdContent);
        }

        try {
            String result = aiChatClient.askJson(ANALYZE_SYSTEM, "请分析以下JD：\n\n" + jdContent);
            return parseAnalysisResult(result);
        } catch (Exception e) {
            log.warn("AI JD 分析失败，降级为规则评分: {}", e.getMessage());
            return buildFallbackAnalysis(jdContent);
        }
    }

    @Override
    public String optimize(String jdContent, List<AiMessage> history) {
        if (!aiChatClient.isEnabled()) {
            return jdContent + "\n\n（AI 未启用，无法优化。建议：检查职责是否量化、技能是否明确、是否包含福利描述。）";
        }

        List<AiMessage> messages = new ArrayList<>();
        messages.add(AiMessage.system(OPTIMIZE_SYSTEM));
        messages.add(AiMessage.user("以下是原始 JD：\n\n" + jdContent));
        messages.addAll(sanitize(history));

        try {
            return aiChatClient.chat(messages, JD_TEMPERATURE);
        } catch (Exception e) {
            log.warn("AI JD 优化失败: {}", e.getMessage());
            return jdContent + "\n\n（AI 优化暂时不可用：" + e.getMessage() + "）";
        }
    }

    // ================== Prompt 构造 ==================

    private String buildGeneratePrompt(String title, String company, String city,
                                       Integer salaryMin, Integer salaryMax,
                                       String education, Integer experienceYears,
                                       List<String> skills, String style) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下岗位生成一份专业 JD：\n\n");
        sb.append("职位名称：").append(title).append('\n');
        sb.append("公司：").append(company).append('\n');
        sb.append("工作城市：").append(city).append('\n');
        if (salaryMin != null && salaryMax != null) {
            sb.append("薪资范围：").append(salaryMin).append(" - ").append(salaryMax).append(" 元/月\n");
        }
        if (education != null && !education.isEmpty()) {
            sb.append("学历要求：").append(education).append('\n');
        }
        if (experienceYears != null) {
            sb.append("经验要求：").append(experienceYears).append(" 年\n");
        }
        if (skills != null && !skills.isEmpty()) {
            sb.append("核心技能：").append(String.join("、", skills)).append('\n');
        }
        if (style != null) {
            String styleDesc;
            switch (style) {
                case "startup":
                    styleDesc = "创业公司风格——扁平化管理、快速成长、强调 ownership 和影响力";
                    break;
                case "foreign":
                    styleDesc = "外企风格——强调 work-life balance、国际化团队、英文能力";
                    break;
                default:
                    styleDesc = "专业正式风格——适合中大型企业";
            }
            sb.append("风格要求：").append(styleDesc).append('\n');
        }
        return sb.toString();
    }

    // ================== 降级逻辑 ==================

    private String buildFallbackJd(String title, String company, String city,
                                   Integer salaryMin, Integer salaryMax,
                                   String education, Integer experienceYears,
                                   List<String> skills, String style) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(title).append("】\n\n");
        sb.append("公司：").append(company).append(" | 地点：").append(city);
        if (salaryMin != null && salaryMax != null) {
            sb.append(" | 薪资：").append(salaryMin).append("-").append(salaryMax).append("元/月");
        }
        sb.append("\n\n");
        sb.append("岗位职责：\n");
        sb.append("1. 负责").append(title).append("相关业务模块的设计与开发\n");
        sb.append("2. 参与需求分析、技术方案设计、代码评审\n");
        sb.append("3. 持续优化系统性能与稳定性\n\n");
        sb.append("任职要求：\n");
        if (education != null && !education.isEmpty()) {
            sb.append("1. ").append(education).append("及以上学历\n");
        }
        if (experienceYears != null && experienceYears > 0) {
            sb.append("2. ").append(experienceYears).append("年以上相关工作经验\n");
        }
        if (skills != null && !skills.isEmpty()) {
            sb.append("3. 熟练掌握").append(String.join("、", skills)).append("\n");
        }
        sb.append("4. 具备良好的沟通能力和团队协作精神\n\n");
        sb.append("福利待遇：\n");
        sb.append("- 五险一金、带薪年假\n");
        sb.append("- 弹性工作制、定期团建\n");
        sb.append("- 技术培训与成长空间\n\n");
        sb.append("（此为模板生成，AI 启用后可获得更精准的 JD）");
        return sb.toString();
    }

    private JdOptimizeVO buildFallbackAnalysis(String jdContent) {
        JdOptimizeVO vo = new JdOptimizeVO();
        List<String> suggestions = new ArrayList<>();
        List<JdOptimizeVO.Dimension> dims = new ArrayList<>();

        boolean hasResp = jdContent.contains("职责") || jdContent.contains("负责");
        boolean hasReq = jdContent.contains("要求") || jdContent.contains("任职");
        boolean hasBenefit = jdContent.contains("福利") || jdContent.contains("待遇");
        boolean hasSalary = jdContent.contains("薪") || jdContent.contains("k") || jdContent.contains("K");
        boolean hasDetail = jdContent.length() > 200;

        int score = 50;
        if (hasResp) score += 10; else suggestions.add("缺少岗位职责描述");
        if (hasReq) score += 10; else suggestions.add("缺少任职要求描述");
        if (hasBenefit) score += 10; else suggestions.add("建议增加福利待遇描述");
        if (hasSalary) score += 10; else suggestions.add("建议明确薪资范围");
        if (hasDetail) score += 10; else suggestions.add("JD 内容过短，建议补充更多细节");

        addDim(dims, "完整性", hasResp && hasReq && hasBenefit ? 85 : hasResp || hasReq ? 60 : 40);
        addDim(dims, "吸引力", hasBenefit && hasDetail ? 70 : 55);
        addDim(dims, "合规性", 90);
        addDim(dims, "具体性", hasDetail ? 65 : 45);

        if (suggestions.isEmpty()) {
            suggestions.add("JD 基本完整，建议使用 AI 分析获得更详细的优化建议");
        }

        vo.setScore(Math.min(score, 100));
        vo.setDimensions(dims);
        vo.setSuggestions(suggestions);
        return vo;
    }

    private JdOptimizeVO parseAnalysisResult(String json) {
        JSONObject obj = JSON.parseObject(json);
        JdOptimizeVO vo = new JdOptimizeVO();
        vo.setScore(obj.getInteger("score"));

        JSONArray dimArr = obj.getJSONArray("dimensions");
        if (dimArr != null) {
            List<JdOptimizeVO.Dimension> dims = new ArrayList<>();
            for (int i = 0; i < dimArr.size(); i++) {
                JSONObject d = dimArr.getJSONObject(i);
                JdOptimizeVO.Dimension dim = new JdOptimizeVO.Dimension();
                dim.setName(d.getString("name"));
                dim.setScore(d.getInteger("score"));
                dims.add(dim);
            }
            vo.setDimensions(dims);
        }

        JSONArray sugArr = obj.getJSONArray("suggestions");
        if (sugArr != null) {
            List<String> suggestions = new ArrayList<>();
            for (int i = 0; i < sugArr.size(); i++) {
                suggestions.add(sugArr.getString(i));
            }
            vo.setSuggestions(suggestions);
        }

        return vo;
    }

    private List<AiMessage> sanitize(List<AiMessage> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiMessage> valid = history.stream()
                .filter(m -> m != null && m.getContent() != null && !m.getContent().trim().isEmpty())
                .filter(m -> "user".equals(m.getRole()) || "assistant".equals(m.getRole()))
                .map(m -> new AiMessage(m.getRole(),
                        m.getContent().length() > MAX_MESSAGE_CHARS
                                ? m.getContent().substring(0, MAX_MESSAGE_CHARS) + "..."
                                : m.getContent()))
                .collect(Collectors.toList());
        return valid.size() <= MAX_HISTORY ? valid : valid.subList(valid.size() - MAX_HISTORY, valid.size());
    }

    private void addDim(List<JdOptimizeVO.Dimension> dims, String name, int score) {
        JdOptimizeVO.Dimension d = new JdOptimizeVO.Dimension();
        d.setName(name);
        d.setScore(score);
        dims.add(d);
    }
}
