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
            "你是高校计算机类专业的教学负责人。下面给出本校学生的技能掌握情况与市场需求的对比数据。",
            "请写一段面向教研室的教学调整建议，要求：",
            "1. 先给整体判断，再点名 2-3 个最该优先补的技能，必须引用给出的岗位数与掌握人数。",
            "2. 落到具体动作：新开/调整哪门课、加什么实验、如何评估效果。",
            "3. marketDemand 是相对热度（该技能岗位数 ÷ 最热技能岗位数 × 100），排第一的恒为 100，"
                    + "不要把它解读成「100% 的岗位都要求它」。",
            "4. 只用给出的数据，不要编造。350 字以内，连贯段落，不要 markdown。");

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
            return AdvisorReplyVO.of(aiChatClient.ask(SYSTEM, buildPrompt(suggestion)), true);
        } catch (Exception e) {
            log.warn("教学建议 AI 解读失败，降级为规则文案: {}", e.getMessage());
            return AdvisorReplyVO.of(ruleAnalysis(suggestion), false);
        }
    }

    private String buildPrompt(TeachingSuggestionVO s) {
        StringBuilder sb = new StringBuilder();
        sb.append("已填写画像的学生人数：").append(s.getStudentsWithProfile()).append("\n\n");
        sb.append("技能缺口（按缺口大小排序）：\n");
        for (TeachingSuggestionVO.SkillGap g : topGaps(s)) {
            sb.append("- ").append(g.getSkill())
              .append("：市场相对热度 ").append(g.getMarketDemand())
              .append("，相关岗位 ").append(g.getJobCount()).append(" 个")
              .append("；学生掌握率 ").append(g.getStudentRate()).append("%")
              .append("（").append(g.getMasteredCount()).append(" 人掌握）")
              .append("，缺口 ").append(g.getGap()).append('\n');
        }
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
