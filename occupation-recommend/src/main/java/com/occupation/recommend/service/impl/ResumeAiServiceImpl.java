package com.occupation.recommend.service.impl;

import com.alibaba.fastjson.JSON;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.ai.AiChatClient;
import com.occupation.common.ai.AiMessage;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.StudentResume;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.ResumeAiService;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.ResumeReviewVO;
import com.occupation.recommend.vo.ResumeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简历 AI 服务实现
 * <p>
 * <b>诊断</b>走大模型的 JSON 模式，拿到结构化结果直接映射成 VO，不做正则抠字段。
 * 模型不可用 / 返回的 JSON 解析不了时，落到 {@link #ruleReview} 的规则诊断，
 * 并把 {@code aiGenerated} 置为 false 让前端如实告知用户。
 * <p>
 * <b>Prompt 里只放这名学生自己的简历</b>，不注入其他学生的数据。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiServiceImpl implements ResumeAiService {

    /** 喂给模型的市场热门技能条数 */
    private static final int MARKET_SKILL_TOP = 15;

    /** 项目/实习描述低于这个字数就认为「写得太空」 */
    private static final int THIN_DESCRIPTION = 40;

    private static final String REVIEW_SYSTEM = String.join("\n",
            "你是互联网大厂的资深校园招聘简历评审官，阅历丰富、点评一针见血。",
            "请评审这份应届生简历，只返回一个 JSON 对象，不要任何解释性文字、不要 markdown 代码块。",
            "JSON 结构必须严格如下：",
            "{",
            "  \"score\": 整数 0-100,",
            "  \"summary\": \"80-120 字总评，包含：整体评级 + 最突出的 1 个优点 + 最紧迫的 1 个问题\",",
            "  \"strengths\": [\"亮点，3-5 条，每条 50 字以内，必须引用简历原文\"],",
            "  \"weaknesses\": [\"待改进项，3-5 条，每条 50 字以内，区分「硬伤」和「可优化项」\"],",
            "  \"suggestions\": [",
            "    {\"section\": \"自我评价|教育经历|项目经历|实习经历|技能\",",
            "     \"issue\": \"具体问题\",",
            "     \"advice\": \"可直接照做的改法\",",
            "     \"priority\": \"高|中|低\",",
            "     \"expectedEffect\": \"预计提升效果描述\"}",
            "  ],",
            "  \"missingSkills\": [\"相对目标岗位缺失的技能\"],",
            "  \"marketCompetitiveness\": \"与同专业同龄人相比的竞争力评估，如「前30%」「中等偏上」「需大幅提升」\"",
            "}",
            "评审要求：",
            "1. 必须引用简历里的具体内容，禁止说空话套话；简历没写的东西不要编造。",
            "2. suggestions 给 4-6 条，每条都要能直接照着改，不要「建议丰富项目经历」这种废话。",
            "3. 项目描述若缺少量化结果（数据、性能、规模），必须指出并示范怎么加。",
            "4. score 要有区分度：内容空洞的 40-60，中规中矩的 60-75，有量化成果且与岗位匹配的 75-90。",
            "5. weaknesses 中要区分「硬伤」（如学历不达标、核心技能缺失）和「可优化项」（如描述不够量化）。",
            "6. marketCompetitiveness 要结合简历内容、技能覆盖度、项目质量综合判断。");

    private static final String POLISH_SYSTEM = String.join("\n",
            "你是简历写作教练。请改写用户给出的简历片段，使其更适合投递校招岗位。",
            "要求：",
            "1. 只输出改写后的正文，不要解释、不要引号、不要 markdown。",
            "2. 使用「动词 + 做了什么 + 用了什么技术 + 取得什么结果」的结构。",
            "3. 能量化的地方尽量量化；但绝不许编造原文没有的数字、公司名、奖项，",
            "   原文没有数据时用「显著提升」这类中性表述，或在括号里提示用户补充，如（可补充具体提升比例）。",
            "4. 字数与原文相当，不要膨胀成两倍。");

    private static final String POLISH_CHAT_SYSTEM = String.join("\n",
            "你是简历写作教练，正在帮一位学生逐轮打磨一段简历文字。",
            "用户会反复提要求（如「再精简一点」「把技术栈写得更突出」「加点数据量化」），",
            "你需要根据要求不断修改这段文字。",
            "规则：",
            "1. 只输出改写后的正文，不要解释、不要引号、不要 markdown。",
            "2. 使用「动词 + 做了什么 + 用了什么技术 + 取得什么结果」的结构。",
            "3. 能量化的地方尽量量化；但绝不许编造原文没有的数字、公司名、奖项。",
            "4. 如果用户没有特别要求字数，保持与上一轮输出相近的长度。",
            "5. 每次只输出当前版本，不要附带「这是修改后的版本」这类说明。");

    private final ResumeService resumeService;
    private final StudentProfileService profileService;
    private final JobDetailService jobDetailService;
    private final AnalysisService analysisService;
    private final AiChatClient aiChatClient;

    @Override
    public ResumeReviewVO review(Long userId, Long targetJobId, boolean refresh) {
        StudentResume entity = resumeService.findByUserId(userId);
        if (entity == null) {
            throw new BizException("请先填写简历再进行诊断");
        }
        ResumeVO resume = resumeService.getByUserId(userId);

        // 命中缓存：没指定目标岗位、不强制刷新、且上次诊断结果还在
        if (!refresh && targetJobId == null && resume.getAiReview() != null) {
            return resume.getAiReview();
        }

        SysStudentProfile profile = profileService.getByUserId(userId);
        JobDetailVO targetJob = targetJobId == null ? null : jobDetailService.getJobById(targetJobId);

        ResumeReviewVO result;
        if (aiChatClient.isEnabled()) {
            result = aiReview(resume, profile, targetJob);
            if (result == null) {
                result = ruleReview(resume, profile, targetJob);
            }
        } else {
            result = ruleReview(resume, profile, targetJob);
        }

        // 只缓存「无目标岗位」的通用诊断；对标某个岗位的结果因岗位而异，缓存了会串味
        if (targetJobId == null && result.isAiGenerated()) {
            resumeService.saveAiReview(userId, JSON.toJSONString(result));
        }
        return result;
    }

    @Override
    public String polish(String section, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BizException("请先填写内容再润色");
        }
        if (!aiChatClient.isEnabled()) {
            throw new BizException("AI 润色未启用，请联系管理员配置 app.ai.api-key");
        }
        String prompt = "简历板块：" + (section == null || section.isEmpty() ? "简历正文" : section)
                + "\n原文：\n" + text.trim();
        try {
            return aiChatClient.ask(POLISH_SYSTEM, prompt);
        } catch (Exception e) {
            log.warn("简历润色失败: {}", e.getMessage());
            throw new BizException("AI 润色暂时不可用：" + e.getMessage());
        }
    }

    @Override
    public String polishChat(String section, String originalText, List<AiMessage> history, String userMessage) {
        if (!aiChatClient.isEnabled()) {
            throw new BizException("AI 润色未启用，请联系管理员配置 app.ai.api-key");
        }
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new BizException("请输入润色要求");
        }

        // 构建完整消息列表：system + 历史 + 本轮用户消息
        List<AiMessage> messages = new ArrayList<>();

        // 首次润色时，在 system prompt 中注入板块和原文上下文
        String systemPrompt = POLISH_CHAT_SYSTEM;
        if (originalText != null && !originalText.trim().isEmpty()) {
            String sec = section == null || section.isEmpty() ? "简历正文" : section;
            systemPrompt = systemPrompt + "\n\n当前正在润色的板块：「" + sec + "」\n原文如下：\n" + originalText.trim();
        }
        messages.add(AiMessage.system(systemPrompt));

        // 追加已有的对话历史（不含 system prompt）
        if (history != null) {
            messages.addAll(history);
        }

        // 追加本轮用户消息
        messages.add(AiMessage.user(userMessage));

        try {
            return aiChatClient.chat(messages, 0.5);
        } catch (Exception e) {
            log.warn("多轮润色失败: {}", e.getMessage());
            throw new BizException("AI 润色暂时不可用：" + e.getMessage());
        }
    }

    // ================== 大模型诊断 ==================

    /** 返回 null 表示这次没成 —— 让调用方降级，而不是把异常抛给用户 */
    private ResumeReviewVO aiReview(ResumeVO resume, SysStudentProfile profile, JobDetailVO targetJob) {
        try {
            String raw = aiChatClient.askJson(REVIEW_SYSTEM, buildReviewPrompt(resume, profile, targetJob));
            ResumeReviewVO vo = JSON.parseObject(stripCodeFence(raw), ResumeReviewVO.class);
            if (vo == null || vo.getSummary() == null) {
                log.warn("AI 诊断返回的 JSON 缺少必要字段，降级为规则诊断");
                return null;
            }
            vo.setAiGenerated(true);
            vo.setTargetJobTitle(targetJob == null ? null : targetJob.getTitle());
            if (vo.getScore() == null) {
                vo.setScore(60);
            }
            vo.setScore(Math.max(0, Math.min(100, vo.getScore())));
            // 确保新增字段有默认值
            if (vo.getMarketCompetitiveness() == null || vo.getMarketCompetitiveness().isEmpty()) {
                vo.setMarketCompetitiveness(vo.getScore() >= 75 ? "中等偏上" :
                        vo.getScore() >= 60 ? "中等" : "需大幅提升");
            }
            return vo;
        } catch (Exception e) {
            log.warn("AI 简历诊断失败，降级为规则诊断: {}", e.getMessage());
            return null;
        }
    }

    /** 少数模型即便开了 json_object 仍会裹一层 ```json，这里剥掉 */
    private String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (start > 0 && end > start) {
                return t.substring(start + 1, end).trim();
            }
        }
        return t;
    }

    private String buildReviewPrompt(ResumeVO resume, SysStudentProfile profile, JobDetailVO targetJob) {
        StringBuilder sb = new StringBuilder();
        sb.append("【简历内容】\n");
        sb.append("求职意向：").append(nvl(resume.getJobIntention())).append('\n');
        if (profile != null) {
            sb.append("专业：").append(nvl(profile.getMajor()))
              .append("　学历：").append(nvl(profile.getEducationLevel()))
              .append("　意向城市：").append(nvl(profile.getExpectedCity())).append('\n');
            sb.append("已掌握技能：").append(String.join("、", SkillUtils.parse(profile.getSkills()))).append('\n');
        }
        sb.append("自我评价：").append(nvl(resume.getSelfIntro())).append('\n');

        sb.append("\n教育经历：\n");
        if (resume.getEducations().isEmpty()) {
            sb.append("（未填写）\n");
        } else {
            resume.getEducations().forEach(e -> sb.append("- ")
                    .append(nvl(e.getSchool())).append(' ').append(nvl(e.getMajor()))
                    .append(' ').append(nvl(e.getDegree()))
                    .append("　GPA:").append(nvl(e.getGpa())).append('\n'));
        }

        sb.append("\n项目经历：\n");
        if (resume.getProjects().isEmpty()) {
            sb.append("（未填写）\n");
        } else {
            resume.getProjects().forEach(p -> sb.append("- 【").append(nvl(p.getName())).append("】")
                    .append("担任").append(nvl(p.getRole())).append("，")
                    .append("技术栈：").append(p.getSkills() == null ? "" : String.join("、", p.getSkills()))
                    .append("\n  描述：").append(nvl(p.getDescription())).append('\n'));
        }

        sb.append("\n实习经历：\n");
        if (resume.getInternships().isEmpty()) {
            sb.append("（未填写）\n");
        } else {
            resume.getInternships().forEach(i -> sb.append("- ").append(nvl(i.getCompany()))
                    .append(' ').append(nvl(i.getPosition()))
                    .append("\n  描述：").append(nvl(i.getDescription())).append('\n'));
        }

        sb.append("\n获奖与证书：")
          .append(resume.getHonors().isEmpty() ? "（未填写）" : String.join("、", resume.getHonors()))
          .append('\n');

        if (targetJob != null) {
            sb.append("\n【目标岗位】\n")
              .append(targetJob.getTitle()).append(" - ").append(targetJob.getCompany())
              .append("（").append(nvl(targetJob.getCity())).append("，学历要求 ")
              .append(nvl(targetJob.getEducation())).append("）\n")
              .append("岗位技能要求：").append(String.join("、", SkillUtils.parse(targetJob.getSkills()))).append('\n')
              .append("岗位描述：").append(truncate(nvl(targetJob.getDescription()), 500)).append('\n')
              .append("\n请重点评估这份简历与该岗位的匹配度，missingSkills 填相对该岗位缺失的技能。\n");
        } else {
            List<String> hot = marketHotSkills();
            if (!hot.isEmpty()) {
                sb.append("\n【当前市场热门技能】").append(String.join("、", hot)).append('\n')
                  .append("没有指定目标岗位，请以上述市场热门技能为参照，missingSkills 填其中该学生尚未掌握、"
                          + "且与其求职意向相关的技能。\n");
            }
        }
        sb.append("\n请按要求返回 json。");
        return sb.toString();
    }

    private List<String> marketHotSkills() {
        try {
            DashboardQueryDTO query = new DashboardQueryDTO();
            query.setTenantId(TenantContextHolder.getTenantId());
            DashboardVO dashboard = analysisService.getDashboard(query);
            if (dashboard.getSkillHot() == null) {
                return Collections.emptyList();
            }
            return dashboard.getSkillHot().stream().limit(MARKET_SKILL_TOP)
                    .map(DashboardVO.DimensionItem::getName).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("取市场热门技能失败，跳过: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ================== 规则降级诊断 ==================

    /**
     * 不依赖大模型的诊断：按「简历完整度 + 内容厚度 + 技能覆盖」打分。
     * 结论粗糙，但永远可用，且不会给出编造的内容。
     */
    private ResumeReviewVO ruleReview(ResumeVO resume, SysStudentProfile profile, JobDetailVO targetJob) {
        int score = 0;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<ResumeReviewVO.Suggestion> suggestions = new ArrayList<>();

        // 自我评价
        String intro = nvl(resume.getSelfIntro());
        if (intro.isEmpty()) {
            weaknesses.add("未填写自我评价（硬伤）");
            suggestions.add(suggestion("自我评价", "自我评价为空",
                    "用 3 句话写清：专业与学历、最擅长的 2-3 项技术、求职方向。", "高", "补充后简历完整度显著提升，HR 能快速了解你的定位"));
        } else {
            score += 15;
            if (intro.length() >= 60) {
                score += 5;
                strengths.add("自我评价内容完整");
            } else {
                suggestions.add(suggestion("自我评价", "自我评价过短（" + intro.length() + " 字）",
                        "补充你最拿得出手的一个项目成果，让招聘方一眼看到亮点。", "中", "提升简历第一印象，增加被筛选通过的概率"));
            }
        }

        // 教育经历
        if (resume.getEducations().isEmpty()) {
            weaknesses.add("未填写教育经历（硬伤）");
            suggestions.add(suggestion("教育经历", "教育经历为空", "至少填写学校、专业、学历与起止时间。",
                    "高", "教育经历是简历的基本板块，缺失会直接导致筛选不通过"));
        } else {
            score += 15;
        }

        // 项目经历 —— 校招最看重的部分
        if (resume.getProjects().isEmpty()) {
            weaknesses.add("没有任何项目经历（硬伤）");
            suggestions.add(suggestion("项目经历", "项目经历为空",
                    "校招最看重项目。把课程设计、竞赛作品也写上，重点写你负责的模块和技术选型。",
                    "高", "项目经历是校招简历的核心，补充后面试邀约率可提升 50% 以上"));
        } else {
            score += Math.min(24, resume.getProjects().size() * 12);
            strengths.add("有 " + resume.getProjects().size() + " 段项目经历");
            long thin = resume.getProjects().stream()
                    .filter(p -> nvl(p.getDescription()).length() < THIN_DESCRIPTION).count();
            if (thin > 0) {
                weaknesses.add("有 " + thin + " 个项目描述过于简略（可优化项）");
                suggestions.add(suggestion("项目经历", thin + " 个项目的描述不足 " + THIN_DESCRIPTION + " 字",
                        "按「背景 → 我做了什么 → 用了什么技术 → 结果如何」展开，结果尽量量化。",
                        "中", "量化描述能让面试官快速评估你的实际贡献"));
            } else {
                score += 6;
            }
        }

        // 实习经历
        if (resume.getInternships().isEmpty()) {
            weaknesses.add("没有实习经历（可优化项）");
            suggestions.add(suggestion("实习经历", "实习经历为空",
                    "若确无实习，可用开源贡献、实验室课题或长期竞赛项目替代，说明投入时长与产出。",
                    "中", "实习经历是加分项，有总比没有好"));
        } else {
            score += 15;
            strengths.add("有实习经历");
        }

        // 获奖
        if (!resume.getHonors().isEmpty()) {
            score += 10;
            strengths.add("有 " + resume.getHonors().size() + " 项获奖或证书");
        } else {
            suggestions.add(suggestion("技能", "未填写获奖与证书",
                    "四六级、软考、竞赛奖项、奖学金都可以写，这是简历筛选阶段的加分项。",
                    "低", "证书和奖项是锦上添花，有则加分"));
        }

        if (!nvl(resume.getJobIntention()).isEmpty()) {
            score += 5;
        } else {
            weaknesses.add("未填写求职意向");
        }

        // 技能覆盖
        List<String> mySkills = profile == null ? Collections.emptyList() : SkillUtils.parse(profile.getSkills());
        List<String> missing = new ArrayList<>();
        List<String> benchmark = targetJob != null ? SkillUtils.parse(targetJob.getSkills()) : marketHotSkills();
        for (String s : benchmark) {
            if (!SkillUtils.containsIgnoreCase(mySkills, s)) {
                missing.add(s);
            }
        }
        if (!benchmark.isEmpty()) {
            int covered = benchmark.size() - missing.size();
            score += (int) Math.round(10.0 * covered / benchmark.size());
            if (!missing.isEmpty()) {
                suggestions.add(suggestion("技能",
                        "相对" + (targetJob != null ? "目标岗位" : "市场热门技能") + "缺少 " + missing.size() + " 项",
                        "优先补齐：" + String.join("、", missing.subList(0, Math.min(3, missing.size()))),
                        "高", "补齐核心技能可显著提升匹配度和面试机会"));
            }
        }

        ResumeReviewVO vo = new ResumeReviewVO();
        vo.setScore(Math.max(0, Math.min(100, score)));
        vo.setSummary(buildRuleSummary(vo.getScore(), weaknesses));
        vo.setStrengths(strengths);
        vo.setWeaknesses(weaknesses);
        vo.setSuggestions(suggestions);
        vo.setMissingSkills(missing.stream().limit(8).collect(Collectors.toList()));
        vo.setTargetJobTitle(targetJob == null ? null : targetJob.getTitle());
        vo.setAiGenerated(false);
        vo.setMarketCompetitiveness(score >= 75 ? "中等偏上" : score >= 60 ? "中等" : "需大幅提升");
        return vo;
    }

    private String buildRuleSummary(int score, List<String> weaknesses) {
        String level;
        if (score >= 80) {
            level = "简历完整度良好";
        } else if (score >= 60) {
            level = "简历基本完整，仍有提升空间";
        } else {
            level = "简历完整度不足，建议先补齐关键板块";
        }
        return weaknesses.isEmpty() ? level : level + "：" + weaknesses.get(0) + "。";
    }

    private ResumeReviewVO.Suggestion suggestion(String section, String issue, String advice) {
        return suggestion(section, issue, advice, "中", null);
    }

    private ResumeReviewVO.Suggestion suggestion(String section, String issue, String advice,
                                                  String priority, String expectedEffect) {
        ResumeReviewVO.Suggestion s = new ResumeReviewVO.Suggestion();
        s.setSection(section);
        s.setIssue(issue);
        s.setAdvice(advice);
        s.setPriority(priority);
        s.setExpectedEffect(expectedEffect);
        return s;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
