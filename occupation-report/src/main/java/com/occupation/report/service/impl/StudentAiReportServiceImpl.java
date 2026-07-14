package com.occupation.report.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.DashboardQueryDTO;
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
import com.occupation.report.export.MarkdownRenderer;
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
    private static final String ROLE = String.join("\n",
            "你是一名资深高校职业规划顾问。请基于提供的这名学生的真实数据，为 TA 撰写一份个性化的求职分析报告。",
            "语气专业、真诚、可执行。**只依据给出的数据，不要编造数据里没有的信息。**",
            "",
            "报告必须包含以下六大板块，缺一不可：",
            "① 个人画像总览：一句话概括学生的基本情况（专业、学历、技能、求职方向）",
            "② 市场定位分析（你在哪里）：基于市场数据，分析学生在同龄人中的位置，指出优势领域和薄弱环节",
            "③ 技能竞争力评估（你缺什么）：对比市场热门技能，列出已掌握和缺失的关键技能，给出竞争力评级",
            "④ 薪资预期合理性：对比市场薪资数据，评估学生的薪资期望是否合理",
            "⑤ 3 条具体求职路线：每条路线包含目标岗位类型、推荐公司类型（如大厂/中厂/创业公司）、所需准备",
            "⑥ 90 天行动计划：按时间线（30天/60天/90天）列出具体可执行的学习和求职动作",
            "",
            "【输出格式硬性要求】",
            "- 板块用 Markdown 二级标题（## ① ...）开头，二级以下用 ### / ####；",
            "- 强调用 **加粗**；列表项用 * 开头；",
            "- 严禁使用 ``` 代码块；不要输出 <think> 等思考过程；",
            "- 整篇控制在 2000-3000 字，必须写完六大板块，一个都不能少；",
            "- 如果输出即将达到上限，优先压缩每个板块的细节描述，而不是砍掉整个板块；",
            "- 绝对不要把半句话留在末尾，每个板块的最后一句话必须是完整的句号结尾。");

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
            // 学生报告要写六大板块 + 多轮修改历史，4000 token 仍可能被截尾；
            // 提升到 6000，确保六大板块完整输出。
            String content = aiChatClient.chat(messages, 0.5, 6000);
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

        // Top3 匹配职位 + 缺失技能
        List<MatchJobVO> matches = jobMatchService.match(userId, 3);
        if (!matches.isEmpty()) {
            sb.append("\n【匹配职位 Top3】\n");
            for (int i = 0; i < matches.size(); i++) {
                MatchJobVO m = matches.get(i);
                sb.append(i + 1).append(". ").append(m.getJob() == null ? "-" : m.getJob().getTitle())
                  .append(" @ ").append(m.getJob() == null ? "-" : m.getJob().getCompany())
                  .append("（匹配度 ").append(m.getScore()).append(" 分）\n");
                if (m.getMissingSkills() != null && !m.getMissingSkills().isEmpty()) {
                    sb.append("   缺失技能：").append(String.join("、", m.getMissingSkills())).append("\n");
                }
            }
        }

        // 市场热门技能
        List<String> marketSkills = analysisService.topSkills(10).stream()
                .map(DashboardVO.DimensionItem::getName).collect(Collectors.toList());
        if (!marketSkills.isEmpty()) {
            sb.append("\n市场当前热门技能：").append(String.join("、", marketSkills)).append("\n");
        }

        // 同专业学生平均数据（行业薪资分位等）
        try {
            DashboardVO dashboard = analysisService.getDashboard(new DashboardQueryDTO());
            if (dashboard.getIndustryTop() != null && !dashboard.getIndustryTop().isEmpty()) {
                sb.append("\n行业薪资参考：");
                dashboard.getIndustryTop().stream().limit(3).forEach(item ->
                        sb.append(item.getName()).append("(").append(item.getValue()).append("个岗位) "));
                sb.append("\n");
            }
        } catch (Exception e) {
            log.debug("获取市场数据失败，跳过: {}", e.getMessage());
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
        // AI 报告正文是 Markdown（**强调** / #### 标题 / * 列表 / 表格），
        // 先用 flexmark 解析为 XHTML，再用内联 CSS 美化，最后包到 XHTML 文档里给 Flying Saucer 渲染 PDF。
        String body = MarkdownRenderer.render(content);
        return "<html><head><meta charset=\"UTF-8\"/><style>"
                + "body{font-family:SimSun,\"Microsoft YaHei\",\"WenQuanYi Zen Hei\",\"Noto Sans CJK SC\",\"PingFang SC\",sans-serif;"
                + "padding:32px 36px;line-height:1.85;color:#1f2937;font-size:13px;}"
                + "h1{border-bottom:2px solid #1f2937;padding-bottom:10px;font-size:22px;margin-top:0;}"
                + "h2{font-size:18px;margin-top:24px;padding-left:10px;border-left:4px solid #2563eb;color:#1e3a8a;}"
                + "h3{font-size:15px;margin-top:18px;color:#1e40af;}"
                + "h4{font-size:14px;margin-top:14px;color:#374151;}"
                + "p{margin:8px 0;text-indent:2em;}"
                + "ul,ol{margin:8px 0 8px 24px;padding-left:8px;}"
                + "li{margin:4px 0;}"
                + "strong{color:#0f172a;font-weight:700;}"
                + "code{background:#f3f4f6;padding:1px 6px;border-radius:3px;font-family:Consolas,monospace;color:#be185d;}"
                + "pre{background:#1e293b;color:#e2e8f0;padding:12px;border-radius:6px;overflow:auto;}"
                + "pre code{background:transparent;color:inherit;padding:0;}"
                + "table{border-collapse:collapse;width:100%;margin:12px 0;font-size:12px;}"
                + "th,td{border:1px solid #d1d5db;padding:6px 10px;text-align:left;}"
                + "th{background:#f3f4f6;font-weight:600;}"
                + "blockquote{border-left:4px solid #cbd5e1;color:#475569;margin:10px 0;padding:6px 14px;background:#f8fafc;}"
                + "</style></head><body>"
                + "<h1>" + escapeHtml(title) + "</h1><div>" + body + "</div></body></html>";
    }

    /** HTML 实体转义（仅用于我们自己注入到模板的字符串，避免 XSS / 解析错误） */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
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
