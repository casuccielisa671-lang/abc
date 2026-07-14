package com.occupation.recommend.service.impl;

import com.occupation.common.ai.AiChatClient;
import com.occupation.recommend.service.TeachingAiService;
import com.occupation.recommend.vo.AdvisorReplyVO;
import com.occupation.recommend.vo.TeachingSuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 教学建议的自然语言解读
 * <p>
 * 结构化的技能缺口表（{@link TeachingSuggestionVO}）仍由 {@code TeachingSuggestionServiceImpl}
 * 真实计算，这里只负责把它翻译成教师能直接用的一段话 —— <b>数字全部来自结构化结果，模型不参与计算</b>。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingAiServiceImpl implements TeachingAiService {

    /** 喂给模型的技能缺口条数 */
    private static final int TOP_GAPS = 8;

    private static final String SYSTEM = String.join("\n",
            "你是高校计算机类专业的教学负责人。下面给出本校学生的技能掌握情况、市场需求对比数据、",
            "以及系统生成的课程改革建议。请基于这些数据，写一份面向教研室的详细教学调整报告。",
            "",
            "按以下五大板块组织，1000-1500 字，连贯段落，不要 markdown：",
            "",
            "① 整体诊断（200-300字）：概述学生技能与市场需求的匹配情况，引用具体的掌握率数字，",
            "   指出最突出的 2-3 个问题。要具体到「XX技能市场热度YY%，但学生掌握率仅ZZ%」这种程度。",
            "",
            "② 最紧迫的 3 个技能缺口（300-400字）：每个缺口必须引用给出的岗位数、掌握人数、",
            "   市场热度数据，说明为什么紧迫，以及如果不补上对学生就业的具体影响。",
            "   例如「如果不掌握 Docker，学生在云计算/DevOps 方向的岗位中将失去竞争力」。",
            "",
            "③ 课程调整建议（300-400字）：结合系统给出的课程改革建议，具体说明：",
            "   - 哪些现有课程需要调整（增加什么实验/项目）",
            "   - 是否需要新增课程（课程名、建议学时、核心内容）",
            "   - 建议在哪个学期开设",
            "   不要只说「增加实践环节」，要说「在《Web开发》课程中增加一个 4 学时的 Spring Boot 微服务项目」。",
            "",
            "④ 校企合作建议（200-300字）：基于缺口技能的类型，建议与什么类型的企业合作，",
            "   合作形式（实训基地/企业导师/联合项目/暑期实训营），并说明为什么这类企业适合。",
            "",
            "⑤ 预期效果与评估指标（150-200字）：给出可量化的预期效果和评估方式，",
            "   如「6 个月后该技能掌握率从 X% 提升到 Y%」「学生投递相关岗位的匹配度提升 Z 分」。",
            "",
            "注意事项：",
            "- marketDemand 是相对热度（该技能岗位数 ÷ 最热技能岗位数 × 100），排第一的恒为 100，",
            "  不要把它解读成「100% 的岗位都要求它」",
            "- 只用给出的数据，不要编造",
            "- 建议要具体可执行，不要「建议加强实践教学」这种空话",
            "- 每个板块必须写够字数，不能一两句话敷衍");

    private final AiChatClient aiChatClient;

    @Override
    public AdvisorReplyVO analyze(TeachingSuggestionVO suggestion) {
        if (suggestion.getSkillGaps() == null || suggestion.getSkillGaps().isEmpty()) {
            return AdvisorReplyVO.of("当前没有检测到明显的技能缺口：学生画像样本不足，或已填画像的学生"
                    + "对市场热门技能覆盖良好。建议先督促学生完善个人画像，样本足够后再看诊断结果。", false);
        }
        if (!aiChatClient.isEnabled()) {
            return AdvisorReplyVO.of(ruleAnalysis(suggestion), false);
        }
        try {
            // 教师解读需要 1000-1500 字 + 五大板块详细分析，用 chat 重载支持更大 max_tokens
            String reply = aiChatClient.chat(
                    java.util.Arrays.asList(
                            com.occupation.common.ai.AiMessage.system(SYSTEM),
                            com.occupation.common.ai.AiMessage.user(buildPrompt(suggestion))),
                    0.5, 4000);
            return AdvisorReplyVO.of(reply, true);
        } catch (Exception e) {
            log.warn("教学建议 AI 解读失败，降级为规则文案: {}", e.getMessage());
            return AdvisorReplyVO.of(ruleAnalysis(suggestion), false);
        }
    }

    private String buildPrompt(TeachingSuggestionVO s) {
        StringBuilder sb = new StringBuilder();
        sb.append("已填写画像的学生人数：").append(s.getStudentsWithProfile()).append("\n\n");

        sb.append("【技能缺口数据】（按缺口大小排序，共 ").append(s.getSkillGaps().size()).append(" 项）\n");
        for (TeachingSuggestionVO.SkillGap g : topGaps(s)) {
            sb.append("- ").append(g.getSkill())
              .append("：市场相对热度 ").append(g.getMarketDemand())
              .append("，相关岗位 ").append(g.getJobCount()).append(" 个")
              .append("；学生掌握率 ").append(g.getStudentRate()).append("%")
              .append("（").append(g.getMasteredCount()).append(" 人掌握）")
              .append("，缺口 ").append(g.getGap())
              .append("；建议：").append(g.getSuggestion() != null ? g.getSuggestion() : "无")
              .append("\n");
        }

        // 注入课程改革建议（之前完全没有用到）
        if (s.getCourseSuggestions() != null && !s.getCourseSuggestions().isEmpty()) {
            sb.append("\n【系统生成的课程改革建议】\n");
            for (TeachingSuggestionVO.CourseSuggestion cs : s.getCourseSuggestions()) {
                sb.append("- [").append(cs.getPriority()).append("] ").append(cs.getTitle())
                  .append("：").append(cs.getDescription()).append("\n");
            }
        }

        sb.append("\n请按五大板块（整体诊断 → 最紧迫的 3 个技能缺口 → 课程调整建议 → 校企合作建议 → 预期效果与评估指标）")
          .append("组织内容，1000-1500 字。每个板块必须写够要求的字数，不要敷衍。");
        return sb.toString();
    }

    /** AI 不可用时，把前三条缺口拼成一段可读的话 */
    private String ruleAnalysis(TeachingSuggestionVO s) {
        List<TeachingSuggestionVO.SkillGap> gaps = topGaps(s).stream().limit(3).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        sb.append("在 ").append(s.getStudentsWithProfile()).append(" 名已填写画像的学生中，")
          .append("检测到 ").append(s.getSkillGaps().size()).append(" 项技能缺口。最突出的是：");
        for (TeachingSuggestionVO.SkillGap g : gaps) {
            sb.append("「").append(g.getSkill()).append("」（相关岗位 ").append(g.getJobCount())
              .append(" 个，仅 ").append(g.getMasteredCount()).append(" 人掌握，掌握率 ")
              .append(g.getStudentRate()).append("%）；");
        }
        sb.setLength(sb.length() - 1);
        sb.append("。建议在下学期的实践课与课程设计中优先引入这几项技术，并以可运行的项目产出作为考核依据。");
        return sb.toString();
    }

    private List<TeachingSuggestionVO.SkillGap> topGaps(TeachingSuggestionVO s) {
        return s.getSkillGaps().stream().limit(TOP_GAPS).collect(Collectors.toList());
    }
}
