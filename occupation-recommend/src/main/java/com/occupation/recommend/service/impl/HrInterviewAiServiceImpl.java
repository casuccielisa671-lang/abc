package com.occupation.recommend.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.ai.AiChatClient;
import com.occupation.recommend.entity.StudentResume;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.HrInterviewAiService;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.InterviewQuestionVO;
import com.occupation.common.utils.SkillUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HR 端 AI 面试问题生成服务实现
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrInterviewAiServiceImpl implements HrInterviewAiService {

    private static final String INTERVIEW_SYSTEM = String.join("\n",
            "你是一位资深面试官，擅长设计高质量的面试问题。",
            "请根据职位要求和候选人简历，生成针对性的面试问题，输出严格的 JSON 格式。",
            "",
            "问题分为三类：",
            "1. technical（技术能力）：考察岗位所需的技术栈掌握程度",
            "2. project（项目经验）：针对候选人简历中的项目/实习经历深挖",
            "3. behavioral（行为/软技能）：考察沟通、协作、解决问题等软能力",
            "",
            "输出格式：",
            "{",
            "  \"technical\": [",
            "    {\"question\": \"请解释Spring Boot的自动配置原理\", \"purpose\": \"考察框架理解深度\", \"expectedAnswer\": \"应提到@EnableAutoConfiguration、spring.factories、条件注解等\"}",
            "  ],",
            "  \"project\": [",
            "    {\"question\": \"你在XX项目中遇到的最大技术挑战是什么？如何解决的？\", \"purpose\": \"考察问题解决能力\", \"expectedAnswer\": \"应具体描述问题、分析过程、解决方案和结果\"}",
            "  ],",
            "  \"behavioral\": [",
            "    {\"question\": \"请描述一次你与团队成员意见不一致的经历\", \"purpose\": \"考察沟通协作能力\", \"expectedAnswer\": \"应体现倾听、理性分析、达成共识的过程\"}",
            "  ]",
            "}",
            "",
            "规则：",
            "- 每类生成 3-5 个问题",
            "- question 要具体、开放、有区分度",
            "- purpose 说明考察什么能力",
            "- expectedAnswer 给出评分要点，帮助面试官判断回答质量",
            "- 如果候选人没有简历，project 类问题改为通用项目经验问题",
            "- 不要问歧视性问题（婚育、年龄、籍贯等）");

    private final AiChatClient aiChatClient;
    private final JobDetailService jobDetailService;
    private final ResumeService resumeService;
    private final StudentProfileService profileService;

    @Override
    public InterviewQuestionVO generateQuestions(Long jobId, Long applicantId) {
        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            InterviewQuestionVO vo = new InterviewQuestionVO();
            vo.setAiGenerated(false);
            vo.setTechnical(Collections.emptyList());
            vo.setProject(Collections.emptyList());
            vo.setBehavioral(Collections.emptyList());
            return vo;
        }

        if (!aiChatClient.isEnabled()) {
            return buildFallbackQuestions(job);
        }

        try {
            String userPrompt = buildInterviewPrompt(job, applicantId);
            String result = aiChatClient.askJson(INTERVIEW_SYSTEM, userPrompt);
            return parseInterviewResult(result);
        } catch (Exception e) {
            log.warn("AI 面试问题生成失败，降级为模板: {}", e.getMessage());
            return buildFallbackQuestions(job);
        }
    }

    private String buildInterviewPrompt(JobDetailVO job, Long applicantId) {
        StringBuilder sb = new StringBuilder();
        sb.append("【职位信息】\n");
        sb.append("职位：").append(job.getTitle()).append('\n');
        sb.append("技能要求：").append(joinSkills(job.getSkills())).append('\n');
        sb.append("学历要求：").append(nvl(job.getEducation())).append('\n');
        sb.append("经验要求：").append(nvl(job.getExperience())).append('\n');
        if (job.getDescription() != null) {
            sb.append("职位描述：").append(truncate(job.getDescription(), 500)).append('\n');
        }

        if (applicantId != null) {
            StudentResume resume = resumeService.findByUserId(applicantId);
            SysStudentProfile profile = profileService.getByUserId(applicantId);

            sb.append("\n【候选人信息】\n");
            if (profile != null) {
                sb.append("专业：").append(nvl(profile.getMajor())).append('\n');
                sb.append("技能：").append(joinSkills(profile.getSkills())).append('\n');
            }
            if (resume != null) {
                sb.append("自我评价：").append(truncate(nvl(resume.getSelfIntro()), 200)).append('\n');
                if (resume.getProjects() != null) {
                    sb.append("项目经历：").append(truncate(resume.getProjects(), 400)).append('\n');
                }
                if (resume.getInternships() != null) {
                    sb.append("实习经历：").append(truncate(resume.getInternships(), 400)).append('\n');
                }
            }
            sb.append("\n请结合候选人简历中的项目/实习经历，生成针对性的 project 类问题。");
        } else {
            sb.append("\n没有候选人简历，project 类问题请改为通用项目经验考察问题。");
        }

        return sb.toString();
    }

    private InterviewQuestionVO buildFallbackQuestions(JobDetailVO job) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setAiGenerated(false);

        String title = job.getTitle() != null ? job.getTitle() : "该岗位";
        List<String> skills = SkillUtils.parse(job.getSkills());

        // 技术问题
        List<InterviewQuestionVO.QuestionItem> technical = new ArrayList<>();
        if (!skills.isEmpty()) {
            technical.add(q("请介绍一下你对" + skills.get(0) + "的掌握程度和使用经验",
                    "考察核心技术栈熟练度", "应能说出使用场景、常见问题和最佳实践"));
        }
        technical.add(q("请描述一次你解决过的技术难题，以及你是如何分析和解决的",
                "考察问题分析与解决能力", "应体现问题定位、方案对比、最终效果"));
        technical.add(q("你如何保证代码质量？请谈谈你的具体实践",
                "考察工程素养", "应提到代码评审、单元测试、CI/CD等"));
        vo.setTechnical(technical);

        // 项目问题
        List<InterviewQuestionVO.QuestionItem> project = new ArrayList<>();
        project.add(q("请介绍一个你最有成就感的项目，你在其中扮演什么角色？",
                "考察项目经验和角色定位", "应说明项目背景、个人职责、技术选型和成果"));
        project.add(q("在项目中你如何做技术选型？请举例说明",
                "考察技术决策能力", "应体现需求分析、方案对比、trade-off思考"));
        project.add(q("如果让你重新做这个项目，你会做哪些不同的选择？",
                "考察复盘与成长意识", "应体现对过去决策的反思和改进思路"));
        vo.setProject(project);

        // 行为问题
        List<InterviewQuestionVO.QuestionItem> behavioral = new ArrayList<>();
        behavioral.add(q("请描述一次你与团队成员意见不一致的经历，最终如何解决？",
                "考察沟通协作能力", "应体现倾听、理性表达、寻求共识"));
        behavioral.add(q("你是如何保持技术学习的？最近在学什么？",
                "考察学习能力和自驱力", "应体现持续学习的习惯和具体行动"));
        behavioral.add(q("你为什么想加入我们公司？对这个岗位有什么期待？",
                "考察求职动机和文化匹配", "应对公司和岗位有基本了解"));
        vo.setBehavioral(behavioral);

        return vo;
    }

    private InterviewQuestionVO parseInterviewResult(String json) {
        JSONObject obj = JSON.parseObject(json);
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setAiGenerated(true);
        vo.setTechnical(parseQuestions(obj.getJSONArray("technical")));
        vo.setProject(parseQuestions(obj.getJSONArray("project")));
        vo.setBehavioral(parseQuestions(obj.getJSONArray("behavioral")));
        return vo;
    }

    private List<InterviewQuestionVO.QuestionItem> parseQuestions(JSONArray arr) {
        if (arr == null) return Collections.emptyList();
        List<InterviewQuestionVO.QuestionItem> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            InterviewQuestionVO.QuestionItem item = new InterviewQuestionVO.QuestionItem();
            item.setQuestion(obj.getString("question"));
            item.setPurpose(obj.getString("purpose"));
            item.setExpectedAnswer(obj.getString("expectedAnswer"));
            list.add(item);
        }
        return list;
    }

    private InterviewQuestionVO.QuestionItem q(String question, String purpose, String expectedAnswer) {
        InterviewQuestionVO.QuestionItem item = new InterviewQuestionVO.QuestionItem();
        item.setQuestion(question);
        item.setPurpose(purpose);
        item.setExpectedAnswer(expectedAnswer);
        return item;
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
