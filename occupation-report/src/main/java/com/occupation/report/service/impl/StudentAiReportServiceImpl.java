package com.occupation.report.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.common.ai.AiChatClient;
import com.occupation.common.ai.AiMessage;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.service.JobMatchService;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.vo.MatchJobVO;
import com.occupation.recommend.vo.ResumeVO;
import com.occupation.report.dto.StudentReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.export.PdfExporter;
import com.occupation.report.mapper.ReportRecordMapper;
import com.occupation.report.service.StudentAiReportService;
import com.occupation.report.vo.StudentAiReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生个人 AI 分析报告实现。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentAiReportServiceImpl implements StudentAiReportService {

    private static final String CATEGORY = "STUDENT_AI";
    private static final String DEFAULT_TITLE = "我的求职分析报告";
    private static final String ROLE = "你是一名资深高校职业规划顾问。请基于提供的这名学生的真实数据，"
            + "为 TA 撰写一份个性化的求职分析报告，分「个人概况」「与市场的匹配」「能力差距与提升建议」"
            + "「求职方向建议」几部分，语气专业、真诚、可执行。**只依据给出的数据，不要编造数据里没有的信息。**";

    private final StudentProfileService profileService;
    private final ResumeService resumeService;
    private final JobMatchService jobMatchService;
    private final AnalysisService analysisService;
    private final AiChatClient aiChatClient;
    private final ReportRecordMapper recordMapper;
    private final PdfExporter pdfExporter;

    @Value("${app.report.storage-path:./data/reports}")
    private String storagePath;

    @Override
    public StudentAiReportVO preview(Long userId, StudentReportDTO dto) {
        String context = buildContext(userId);
        List<AiMessage> messages = new ArrayList<>();
        messages.add(AiMessage.system(ROLE + "\n\n【学生数据】\n" + context));
        // 前端持有的多轮历史（多轮改）
        if (dto.getHistory() != null) {
            for (StudentReportDTO.Msg m : dto.getHistory()) {
                if ("assistant".equals(m.getRole())) {
                    messages.add(AiMessage.assistant(m.getContent()));
                } else if ("user".equals(m.getRole())) {
                    messages.add(AiMessage.user(m.getContent()));
                }
            }
        }
        String instruction = (dto.getInstruction() == null || dto.getInstruction().trim().isEmpty())
                ? "请基于以上数据生成我的求职分析报告。"
                : dto.getInstruction().trim();
        messages.add(AiMessage.user(instruction));

        try {
            String content = aiChatClient.chat(messages, 0.7);
            return StudentAiReportVO.of(DEFAULT_TITLE, content, true);
        } catch (Exception e) {
            log.warn("学生 AI 报告生成失败，降级为规则化: {}", e.getMessage());
            return StudentAiReportVO.of(DEFAULT_TITLE, ruleBased(userId), false);
        }
    }

    @Override
    public ReportRecord save(Long userId, StudentReportDTO dto) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new BizException("报告正文不能为空");
        }
        String name = (dto.getName() == null || dto.getName().trim().isEmpty()) ? DEFAULT_TITLE : dto.getName().trim();
        String fileType = "HTML".equalsIgnoreCase(dto.getFileType()) ? "HTML" : "PDF";

        ReportRecord record = new ReportRecord();
        record.setUserId(userId);
        record.setCategory(CATEGORY);
        record.setName(name);
        record.setParams("{}");
        record.setFileType(fileType);
        record.setAiSummary(dto.getContent());
        record.setStatus("GENERATING");
        recordMapper.insert(record);

        try {
            String html = renderHtml(name, dto.getContent());
            byte[] bytes;
            String ext;
            if ("HTML".equals(fileType)) {
                bytes = html.getBytes(StandardCharsets.UTF_8);
                ext = "html";
            } else {
                bytes = pdfExporter.export(html);
                ext = "pdf";
            }
            File dir = new File(storagePath, String.valueOf(TenantContextHolder.getTenantId()));
            File file = new File(dir, IdUtil.fastSimpleUUID() + "." + ext);
            FileUtil.writeBytes(bytes, file);

            record.setFileUrl(file.getPath());
            record.setStatus("SUCCESS");
            recordMapper.updateById(record);
            return record;
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setErrorMsg(e.getMessage());
            recordMapper.updateById(record);
            throw new BizException("报告保存失败: " + e.getMessage());
        }
    }

    @Override
    public Page<ReportRecord> pageMy(Long userId, int pageNum, int pageSize) {
        return recordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ReportRecord>()
                        .eq(ReportRecord::getUserId, userId)
                        .orderByDesc(ReportRecord::getCreateTime));
    }

    // ==================== 内部 ====================

    /** 把学生的画像/简历/匹配/市场数据拼成给 AI 的上下文文本 */
    private String buildContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        SysStudentProfile p = profileService.getByUserId(userId);
        if (p != null) {
            sb.append("专业：").append(nn(p.getMajor())).append("；学历：").append(nn(p.getEducationLevel())).append("\n");
            List<String> skills = SkillUtils.parse(p.getSkills());
            sb.append("掌握技能：").append(skills.isEmpty() ? "（未填写）" : String.join("、", skills)).append("\n");
            sb.append("期望：城市 ").append(nn(p.getExpectedCity()))
              .append("；行业 ").append(nn(p.getExpectedIndustry()))
              .append("；薪资 ").append(salary(p)).append("\n");
        } else {
            sb.append("（该学生尚未填写个人画像）\n");
        }
        ResumeVO resume = resumeService.getByUserId(userId);
        sb.append("简历：").append(resume != null && resume.isExists()
                ? "已填写，求职意向：" + nn(resume.getJobIntention()) : "未填写").append("\n");

        List<MatchJobVO> matches = jobMatchService.match(userId, 3);
        if (!matches.isEmpty()) {
            MatchJobVO top = matches.get(0);
            sb.append("最匹配职位：").append(top.getJob() == null ? "-" : top.getJob().getTitle())
              .append(" @ ").append(top.getJob() == null ? "-" : top.getJob().getCompany())
              .append("（匹配度 ").append(top.getScore()).append(" 分）\n");
            if (top.getMissingSkills() != null && !top.getMissingSkills().isEmpty()) {
                sb.append("相比该岗位尚缺技能：").append(String.join("、", top.getMissingSkills())).append("\n");
            }
        }
        List<String> marketSkills = analysisService.topSkills(10).stream()
                .map(DashboardVO.DimensionItem::getName).collect(Collectors.toList());
        if (!marketSkills.isEmpty()) {
            sb.append("市场当前热门技能：").append(String.join("、", marketSkills)).append("\n");
        }
        return sb.toString();
    }

    /** AI 不可用时的规则化兜底正文 */
    private String ruleBased(Long userId) {
        return "（AI 未启用，以下为根据你的数据规则化生成）\n\n" + buildContext(userId)
                + "\n提升建议：优先补强与目标岗位相关的欠缺技能，并结合市场热门技能查漏补缺；"
                + "完善画像与简历后，推荐与匹配会更精准。";
    }

    private String renderHtml(String title, String content) {
        String safe = content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br/>");
        return "<html><head><meta charset=\"UTF-8\"/><style>"
                + "body{font-family:SimSun,\"Microsoft YaHei\",\"WenQuanYi Zen Hei\",\"Noto Sans CJK SC\",\"PingFang SC\",sans-serif;"
                + "padding:28px;line-height:1.75;color:#222;}"
                + "h1{border-bottom:2px solid #333;padding-bottom:8px;}</style></head><body>"
                + "<h1>" + title + "</h1><div>" + safe + "</div></body></html>";
    }

    private String nn(String s) {
        return (s == null || s.trim().isEmpty()) ? "（未填写）" : s;
    }

    private String salary(SysStudentProfile p) {
        if (p.getExpectedSalaryMin() == null && p.getExpectedSalaryMax() == null) {
            return "（未填写）";
        }
        return (p.getExpectedSalaryMin() == null ? "" : p.getExpectedSalaryMin())
                + "-" + (p.getExpectedSalaryMax() == null ? "" : p.getExpectedSalaryMax());
    }
}
