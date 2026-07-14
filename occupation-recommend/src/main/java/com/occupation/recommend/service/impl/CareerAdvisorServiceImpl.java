package com.occupation.recommend.service.impl;

import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.ai.AiChatClient;
import com.occupation.common.ai.AiMessage;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.StudentResume;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.CareerAdvisorService;
import com.occupation.recommend.service.JobMatchService;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.AdvisorReplyVO;
import com.occupation.recommend.vo.MatchJobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 职业顾问实现
 * <p>
 * <b>效果的来源不是模型有多强，而是 system prompt 里塞了多少这名学生的真实上下文。</b>
 * 这里注入三样东西：① 学生画像与简历概要；② 本平台真实的岗位市场统计；③ 行为约束。
 * 没有这些，模型只会输出「建议多学习多实践」这类正确的废话。
 * <p>
 * 大模型不可用时不抛异常，返回一段引导性的兜底文案（{@code aiGenerated=false}）。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CareerAdvisorServiceImpl implements CareerAdvisorService {

    /** 带进上下文的历史消息条数上限。太长既费 token 又会稀释 system 的约束 */
    private static final int MAX_HISTORY = 12;

    /** 单条消息字数上限，防止用户把一整本书贴进来撑爆上下文 */
    private static final int MAX_MESSAGE_CHARS = 2000;

    /** 对话要自然，比报告摘要的温度高一些 */
    private static final double CHAT_TEMPERATURE = 0.7;

    private static final int MARKET_TOP = 10;

    private static final String ADVISOR_ROLE = String.join("\n",
            "你是本校就业指导中心的 AI 职业顾问，服务对象是即将求职的在校大学生。",
            "行为准则：",
            "1. 回答必须结合下面给出的「学生档案」与「本平台岗位市场数据」，引用具体的技能名、城市、岗位数。",
            "2. 数据里没有的信息，直言不知道，不要猜测薪资、公司内情或录取概率。",
            "3. 不承诺内推、不评价具体公司好坏、不讨论与求职无关的话题（礼貌婉拒并拉回正题）。",
            "4. 给建议要具体到可执行：说清「学什么、按什么顺序、怎么验证学会了」。",
            "5. 口语化、简洁，控制在 500 字以内；不要用 markdown 标题，最多用短横线列点。",
            "6. 回答结构：先给结论，再分点展开，最后给出下一步行动。",
            "7. 当学生问「我该学什么」时，必须给出具体的学习路径：技术栈 → 项目 → 证明方式（如证书、作品集）。");

    private static final String EXPLAIN_SYSTEM = String.join("\n",
            "你是就业指导老师，向学生解释系统为什么把某个职位推荐给他。",
            "要求：用 2-3 句话说清匹配在哪、差在哪、下一步该补什么。",
            "必须基于给出的结构化打分数据，不要编造简历里没有的经历。",
            "直接输出正文，不要标题、不要 markdown。");

    private final StudentProfileService profileService;
    private final ResumeService resumeService;
    private final AnalysisService analysisService;
    private final JobMatchService jobMatchService;
    private final AiChatClient aiChatClient;

    @Override
    public AdvisorReplyVO chat(Long userId, List<AiMessage> history) {
        if (history == null || history.isEmpty()) {
            throw new BizException("请输入你想咨询的问题");
        }
        if (!aiChatClient.isEnabled()) {
            return AdvisorReplyVO.of(
                    "AI 职业顾问尚未启用。你仍然可以在「职位推荐」查看匹配分与建议学习的技能，"
                            + "或在「个人画像」补全技能后重新获取推荐。", false);
        }

        List<AiMessage> messages = new ArrayList<>();
        messages.add(AiMessage.system(ADVISOR_ROLE + "\n\n" + buildContext(userId)));
        messages.addAll(sanitize(history));

        try {
            return AdvisorReplyVO.of(aiChatClient.chat(messages, CHAT_TEMPERATURE), true);
        } catch (Exception e) {
            log.warn("职业顾问对话失败: {}", e.getMessage());
            return AdvisorReplyVO.of("AI 顾问暂时不可用（" + e.getMessage() + "），请稍后再试。", false);
        }
    }

    @Override
    public AdvisorReplyVO explainMatch(Long userId, Long jobId) {
        MatchJobVO match = jobMatchService.scoreOne(userId, jobId);
        if (!aiChatClient.isEnabled()) {
            return AdvisorReplyVO.of(ruleExplain(match), false);
        }
        try {
            return AdvisorReplyVO.of(aiChatClient.ask(EXPLAIN_SYSTEM, buildExplainPrompt(match)), true);
        } catch (Exception e) {
            log.warn("匹配理由生成失败，降级为规则文案: {}", e.getMessage());
            return AdvisorReplyVO.of(ruleExplain(match), false);
        }
    }

    // ================== 上下文构造 ==================

    /** 学生档案 + 市场数据 + 匹配职位。这段是回答质量的分水岭 */
    private String buildContext(Long userId) {
        StringBuilder sb = new StringBuilder();

        sb.append("【学生档案】\n");
        SysStudentProfile profile = profileService.getByUserId(userId);
        if (profile == null) {
            sb.append("该学生尚未填写个人画像。请在回答中提醒他先去「个人画像」补全专业、技能与求职意向，"
                    + "否则推荐与建议都会不准。\n");
        } else {
            sb.append("专业：").append(nvl(profile.getMajor()))
              .append("　学历：").append(nvl(profile.getEducationLevel())).append('\n')
              .append("已掌握技能：").append(joinSkills(profile.getSkills())).append('\n')
              .append("意向城市：").append(nvl(profile.getExpectedCity()))
              .append("　意向行业：").append(nvl(profile.getExpectedIndustry())).append('\n');
            if (profile.getExpectedSalaryMin() != null) {
                sb.append("期望薪资：").append(profile.getExpectedSalaryMin())
                  .append(" - ").append(nvl(String.valueOf(profile.getExpectedSalaryMax())))
                  .append(" 元/月\n");
            }
        }

        StudentResume resume = resumeService.findByUserId(userId);
        if (resume == null) {
            sb.append("简历：尚未填写。\n");
        } else {
            sb.append("简历求职意向：").append(nvl(resume.getJobIntention())).append('\n');
            sb.append("简历自我评价：").append(truncate(nvl(resume.getSelfIntro()), 200)).append('\n');
        }

        // 匹配分最高的 3 个职位及缺失技能
        try {
            List<MatchJobVO> matches = jobMatchService.match(userId, 3);
            if (!matches.isEmpty()) {
                sb.append("\n【匹配度最高的 3 个职位】\n");
                for (int i = 0; i < matches.size(); i++) {
                    MatchJobVO m = matches.get(i);
                    sb.append(i + 1).append(". ").append(m.getJob().getTitle())
                      .append(" @ ").append(m.getJob().getCompany())
                      .append("（匹配分 ").append(m.getScore()).append("）\n");
                    if (m.getMissingSkills() != null && !m.getMissingSkills().isEmpty()) {
                        sb.append("   缺失技能：").append(String.join("、", m.getMissingSkills())).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取匹配职位失败，跳过: {}", e.getMessage());
        }

        appendMarket(sb);

        sb.append("\n（以上数据来自本校就业服务平台，回答时可直接引用其中的数字。）");
        return sb.toString();
    }

    private void appendMarket(StringBuilder sb) {
        try {
            DashboardQueryDTO query = new DashboardQueryDTO();
            query.setTenantId(TenantContextHolder.getTenantId());
            DashboardVO d = analysisService.getDashboard(query);
            sb.append("\n【本平台岗位市场数据】\n");
            appendDim(sb, "热门技能（技能:岗位数）", d.getSkillHot(), MARKET_TOP);
            appendDim(sb, "行业岗位数", d.getIndustryTop(), 5);
            appendDim(sb, "城市岗位数", d.getCityDist(), 5);
            appendDim(sb, "学历需求分布", d.getEducationDist(), 4);
        } catch (Exception e) {
            log.warn("取市场数据失败，顾问上下文降级: {}", e.getMessage());
        }
    }

    private void appendDim(StringBuilder sb, String title, List<DashboardVO.DimensionItem> items, int limit) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append(title).append("：")
          .append(items.stream().limit(limit)
                  .map(i -> i.getName() + ":" + i.getValue().stripTrailingZeros().toPlainString())
                  .collect(Collectors.joining("、")))
          .append('\n');
    }

    /**
     * 只保留 user / assistant 两种角色，截断超长消息，取最近 N 条。
     * <p>
     * 前端传 role=system 就能覆盖我们的角色设定 —— 那是最基础的 prompt 注入，必须挡掉。
     */
    private List<AiMessage> sanitize(List<AiMessage> history) {
        List<AiMessage> valid = history.stream()
                .filter(m -> m != null && m.getContent() != null && !m.getContent().trim().isEmpty())
                .filter(m -> "user".equals(m.getRole()) || "assistant".equals(m.getRole()))
                .map(m -> new AiMessage(m.getRole(), truncate(m.getContent().trim(), MAX_MESSAGE_CHARS)))
                .collect(Collectors.toList());
        if (valid.isEmpty()) {
            throw new BizException("请输入你想咨询的问题");
        }
        return valid.size() <= MAX_HISTORY
                ? valid
                : valid.subList(valid.size() - MAX_HISTORY, valid.size());
    }

    private String buildExplainPrompt(MatchJobVO m) {
        JobDetailVO job = m.getJob();
        StringBuilder sb = new StringBuilder();
        sb.append("职位：").append(job.getTitle()).append(" - ").append(job.getCompany())
          .append("（").append(nvl(job.getCity())).append("，学历要求 ").append(nvl(job.getEducation()))
          .append("，薪资 ").append(job.getSalaryMin()).append("-").append(job.getSalaryMax()).append(" 元/月）\n")
          .append("岗位技能要求：").append(joinSkills(job.getSkills())).append('\n')
          .append("系统匹配分：").append(m.getScore()).append("/100\n")
          .append("规则命中项：").append(nvl(m.getMatchReason())).append('\n');
        if (m.getMissingSkills() != null && !m.getMissingSkills().isEmpty()) {
            sb.append("学生尚未掌握的岗位技能：").append(String.join("、", m.getMissingSkills())).append('\n');
        } else {
            sb.append("学生已覆盖该岗位全部技能要求。\n");
        }
        return sb.toString();
    }

    /** AI 不可用时，把结构化打分拼成一句人话 */
    private String ruleExplain(MatchJobVO m) {
        StringBuilder sb = new StringBuilder();
        sb.append("该职位与你的匹配分为 ").append(m.getScore()).append(" 分");
        if (m.getMatchReason() != null && !m.getMatchReason().isEmpty()) {
            sb.append("，命中：").append(m.getMatchReason());
        }
        sb.append("。");
        if (m.getMissingSkills() != null && !m.getMissingSkills().isEmpty()) {
            sb.append("建议优先补齐这些技能：")
              .append(String.join("、", m.getMissingSkills().subList(0, Math.min(3, m.getMissingSkills().size()))))
              .append("。");
        }
        return sb.toString();
    }

    private String joinSkills(String raw) {
        List<String> list = SkillUtils.parse(raw);
        return list.isEmpty() ? "（未填写）" : String.join("、", list);
    }

    private static String nvl(String s) {
        return s == null || "null".equals(s) ? "" : s;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
