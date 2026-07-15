package com.occupation.recommend.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.common.ai.AiChatClient;
import com.occupation.recommend.entity.StudentResume;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.HrResumeAiService;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.ResumeScreenVO;
import com.occupation.common.utils.SkillUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HR 端 AI 简历筛选服务实现
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrResumeAiServiceImpl implements HrResumeAiService {

    private static final String SCREEN_SYSTEM = String.join("\n",
            "你是一位资深招聘专家，擅长快速评估候选人简历。",
            "请分析以下候选人简历，输出严格的 JSON 格式。",
            "",
            "输出格式：",
            "{",
            "  \"summary\": \"候选人概述（2-3句话，包含核心技能、经验年限、教育背景）\",",
            "  \"highlights\": [\"亮点1\", \"亮点2\", \"亮点3\"],",
            "  \"risks\": [\"风险点1\", \"风险点2\"],",
            "  \"matchScore\": 75,",
            "  \"matchAnalysis\": {",
            "    \"strengths\": [\"匹配优势1\", \"匹配优势2\"],",
            "    \"gaps\": [\"技能差距1\", \"技能差距2\"],",
            "    \"suggestion\": \"综合建议（是否推荐面试、重点关注什么）\"",
            "  }",
            "}",
            "",
            "规则：",
            "- summary 要简洁，突出与岗位最相关的信息",
            "- highlights 列出候选人最突出的 3-5 个优势",
            "- risks 列出需要注意的风险点（如频繁跳槽、技能陈旧、经验不匹配等），没有则为空数组",
            "- matchScore 为 0-100 的匹配度评分",
            "- matchAnalysis 仅在提供了 JD 时输出，纯摘要模式不输出",
            "- 不要编造简历中没有的信息");

    private static final String RANK_SYSTEM = String.join("\n",
            "你是一位招聘专家，需要对多位候选人进行匹配度排序。",
            "请根据 JD 要求和候选人简历，给出每位候选人的匹配度评分（0-100）和简要理由。",
            "输出严格的 JSON 数组格式：",
            "[",
            "  {\"userId\": 1, \"matchScore\": 85, \"reason\": \"技能匹配度高，有相关项目经验\"},",
            "  {\"userId\": 2, \"matchScore\": 60, \"reason\": \"技能部分匹配，缺少核心框架经验\"}",
            "]",
            "按 matchScore 从高到低排序。");

    private final AiChatClient aiChatClient;
    private final ResumeService resumeService;
    private final StudentProfileService profileService;
    private final JobDetailService jobDetailService;

    @Override
    public ResumeScreenVO screen(Long userId, Long jobId) {
        ResumeScreenVO vo = new ResumeScreenVO();
        vo.setUserId(userId);

        StudentResume resume = resumeService.findByUserId(userId);
        SysStudentProfile profile = profileService.getByUserId(userId);

        if (resume == null && profile == null) {
            vo.setAiGenerated(false);
            vo.setSummary("该候选人尚未填写简历和个人画像，无法进行 AI 分析。");
            vo.setHighlights(Collections.emptyList());
            vo.setRisks(Collections.emptyList());
            return vo;
        }

        if (!aiChatClient.isEnabled()) {
            return buildFallbackScreen(userId, resume, profile, jobId);
        }

        try {
            String userPrompt = buildScreenPrompt(userId, resume, profile, jobId);
            String result = aiChatClient.askJson(SCREEN_SYSTEM, userPrompt);
            return parseScreenResult(userId, result);
        } catch (Exception e) {
            log.warn("AI 简历筛选失败，降级为规则分析: {}", e.getMessage());
            return buildFallbackScreen(userId, resume, profile, jobId);
        }
    }

    @Override
    public List<ResumeScreenVO> rankByMatch(Long jobId, List<Long> applicantIds) {
        if (applicantIds == null || applicantIds.isEmpty()) {
            return Collections.emptyList();
        }

        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            return applicantIds.stream().map(uid -> {
                ResumeScreenVO vo = new ResumeScreenVO();
                vo.setUserId(uid);
                vo.setAiGenerated(false);
                vo.setSummary("职位不存在");
                return vo;
            }).collect(Collectors.toList());
        }

        if (!aiChatClient.isEnabled()) {
            return fallbackRank(applicantIds, job);
        }

        try {
            String userPrompt = buildRankPrompt(job, applicantIds);
            String result = aiChatClient.askJson(RANK_SYSTEM, userPrompt);
            return parseRankResult(result);
        } catch (Exception e) {
            log.warn("AI 批量排序失败，降级为规则排序: {}", e.getMessage());
            return fallbackRank(applicantIds, job);
        }
    }

    // ================== Prompt 构造 ==================

    private String buildScreenPrompt(Long userId, StudentResume resume,
                                     SysStudentProfile profile, Long jobId) {
        StringBuilder sb = new StringBuilder();
        sb.append("【候选人信息】\n");
        sb.append("姓名：").append(userId).append('\n');

        if (profile != null) {
            sb.append("专业：").append(nvl(profile.getMajor())).append('\n');
            sb.append("学历：").append(nvl(profile.getEducationLevel())).append('\n');
            sb.append("技能：").append(joinSkills(profile.getSkills())).append('\n');
            sb.append("意向城市：").append(nvl(profile.getExpectedCity())).append('\n');
            if (profile.getExpectedSalaryMin() != null) {
                sb.append("期望薪资：").append(profile.getExpectedSalaryMin())
                        .append("-").append(nvl(String.valueOf(profile.getExpectedSalaryMax())))
                        .append(" 元/月\n");
            }
        }

        if (resume != null) {
            sb.append("求职意向：").append(nvl(resume.getJobIntention())).append('\n');
            sb.append("自我评价：").append(truncate(nvl(resume.getSelfIntro()), 300)).append('\n');
            if (resume.getEducations() != null) {
                sb.append("教育经历：").append(truncate(resume.getEducations(), 300)).append('\n');
            }
            if (resume.getProjects() != null) {
                sb.append("项目经历：").append(truncate(resume.getProjects(), 500)).append('\n');
            }
            if (resume.getInternships() != null) {
                sb.append("实习经历：").append(truncate(resume.getInternships(), 500)).append('\n');
            }
        }

        if (jobId != null) {
            JobDetailVO job = jobDetailService.getJobById(jobId);
            if (job != null) {
                sb.append("\n【目标职位】\n");
                sb.append("职位：").append(job.getTitle()).append('\n');
                sb.append("公司：").append(nvl(job.getCompany())).append('\n');
                sb.append("城市：").append(nvl(job.getCity())).append('\n');
                sb.append("学历要求：").append(nvl(job.getEducation())).append('\n');
                sb.append("经验要求：").append(nvl(job.getExperience())).append('\n');
                sb.append("薪资：").append(job.getSalaryMin()).append("-")
                        .append(job.getSalaryMax()).append(" 元/月\n");
                sb.append("技能要求：").append(joinSkills(job.getSkills())).append('\n');
                sb.append("职位描述：").append(truncate(nvl(job.getDescription()), 500)).append('\n');
            }
        }

        return sb.toString();
    }

    private String buildRankPrompt(JobDetailVO job, List<Long> applicantIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("【目标职位】\n");
        sb.append("职位：").append(job.getTitle()).append('\n');
        sb.append("技能要求：").append(joinSkills(job.getSkills())).append('\n');
        sb.append("学历要求：").append(nvl(job.getEducation())).append('\n');
        sb.append("经验要求：").append(nvl(job.getExperience())).append('\n');
        sb.append("描述：").append(truncate(nvl(job.getDescription()), 300)).append('\n');

        sb.append("\n【候选人列表】\n");
        for (Long uid : applicantIds) {
            StudentResume resume = resumeService.findByUserId(uid);
            SysStudentProfile profile = profileService.getByUserId(uid);
            sb.append("---\n");
            sb.append("候选人ID：").append(uid).append('\n');
            if (profile != null) {
                sb.append("专业：").append(nvl(profile.getMajor()))
                        .append(" | 学历：").append(nvl(profile.getEducationLevel())).append('\n');
                sb.append("技能：").append(joinSkills(profile.getSkills())).append('\n');
            }
            if (resume != null) {
                sb.append("自我评价：").append(truncate(nvl(resume.getSelfIntro()), 150)).append('\n');
                sb.append("项目经历：").append(truncate(nvl(resume.getProjects()), 200)).append('\n');
            }
        }

        sb.append("\n请对以上候选人按匹配度排序，输出 JSON 数组。");
        return sb.toString();
    }

    // ================== 降级逻辑 ==================

    private ResumeScreenVO buildFallbackScreen(Long userId, StudentResume resume,
                                               SysStudentProfile profile, Long jobId) {
        ResumeScreenVO vo = new ResumeScreenVO();
        vo.setUserId(userId);
        vo.setAiGenerated(false);

        List<String> highlights = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        if (profile != null) {
            if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
                highlights.add("已填写技能标签：" + joinSkills(profile.getSkills()));
            } else {
                risks.add("未填写技能信息");
            }
            if (profile.getEducationLevel() != null) {
                highlights.add("学历：" + profile.getEducationLevel());
            }
        } else {
            risks.add("未完善个人画像");
        }

        if (resume != null) {
            if (resume.getSelfIntro() != null && !resume.getSelfIntro().isEmpty()) {
                highlights.add("已填写自我评价");
            }
            if (resume.getProjects() != null && !resume.getProjects().equals("[]")) {
                highlights.add("有项目经历");
            }
            if (resume.getInternships() != null && !resume.getInternships().equals("[]")) {
                highlights.add("有实习经历");
            }
        } else {
            risks.add("未填写简历");
        }

        vo.setSummary("候选人" + userId + "（规则分析，AI 启用后可获得更详细分析）");
        vo.setHighlights(highlights);
        vo.setRisks(risks);
        return vo;
    }

    private List<ResumeScreenVO> fallbackRank(List<Long> applicantIds, JobDetailVO job) {
        return applicantIds.stream().map(uid -> {
            ResumeScreenVO vo = new ResumeScreenVO();
            vo.setUserId(uid);
            vo.setAiGenerated(false);
            vo.setMatchScore(50);
            vo.setSummary("规则排序（AI 启用后可获得精准排序）");
            return vo;
        }).collect(Collectors.toList());
    }

    // ================== 解析 ==================

    private ResumeScreenVO parseScreenResult(Long userId, String json) {
        JSONObject obj = JSON.parseObject(json);
        ResumeScreenVO vo = new ResumeScreenVO();
        vo.setUserId(userId);
        vo.setAiGenerated(true);
        vo.setSummary(obj.getString("summary"));

        JSONArray hl = obj.getJSONArray("highlights");
        vo.setHighlights(hl != null ? hl.toJavaList(String.class) : Collections.emptyList());

        JSONArray rk = obj.getJSONArray("risks");
        vo.setRisks(rk != null ? rk.toJavaList(String.class) : Collections.emptyList());

        Integer score = obj.getInteger("matchScore");
        vo.setMatchScore(score);

        JSONObject ma = obj.getJSONObject("matchAnalysis");
        if (ma != null) {
            ResumeScreenVO.MatchAnalysis analysis = new ResumeScreenVO.MatchAnalysis();
            JSONArray strengths = ma.getJSONArray("strengths");
            analysis.setStrengths(strengths != null ? strengths.toJavaList(String.class) : Collections.emptyList());
            JSONArray gaps = ma.getJSONArray("gaps");
            analysis.setGaps(gaps != null ? gaps.toJavaList(String.class) : Collections.emptyList());
            analysis.setSuggestion(ma.getString("suggestion"));
            vo.setMatchAnalysis(analysis);
        }

        return vo;
    }

    private List<ResumeScreenVO> parseRankResult(String json) {
        JSONArray arr = JSON.parseArray(json);
        List<ResumeScreenVO> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            ResumeScreenVO vo = new ResumeScreenVO();
            vo.setUserId(obj.getLong("userId"));
            vo.setMatchScore(obj.getInteger("matchScore"));
            vo.setSummary(obj.getString("reason"));
            vo.setAiGenerated(true);
            list.add(vo);
        }
        return list;
    }

    // ================== 工具方法 ==================

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
