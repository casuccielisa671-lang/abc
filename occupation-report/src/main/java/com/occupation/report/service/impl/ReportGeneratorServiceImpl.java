package com.occupation.report.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.report.dto.GenerateReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.export.PdfExporter;
import com.occupation.report.export.WordExporter;
import com.occupation.report.mapper.ReportRecordMapper;
import com.occupation.recommend.service.EmploymentReportService;
import com.occupation.recommend.vo.EmploymentReportData;
import com.occupation.report.service.AiSummaryService;
import com.occupation.report.service.ReportDeliveryService;
import com.occupation.report.service.ReportGeneratorService;
import cn.hutool.json.JSONUtil;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报告生成引擎实现
 * <p>
 * 跨模块协作：analysis(数据) → ai(摘要) → freemarker(渲染) → exporter(导出)。
 * 生成的文件存放于 {@code app.report.storage-path}/{tenantId}/ 目录。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGeneratorServiceImpl implements ReportGeneratorService {

    private final ReportRecordMapper recordMapper;
    private final AnalysisService analysisService;
    private final AiSummaryService aiSummaryService;
    private final EmploymentReportService employmentReportService;
    private final ReportDeliveryService deliveryService;
    private final PdfExporter pdfExporter;
    private final WordExporter wordExporter;

    @Value("${app.report.storage-path:./data/reports}")
    private String storagePath;

    /** 市场行业报告内置模板（6 节，与 Word 版一致）。
     *  注意标签全部闭合（XHTML），保证可直接送 PDF 渲染。
     *  font-family 列出多个候选：PdfExporter 注册的是系统上实际存在的那一款中文字体，
     *  只写 SimSun 的话在没装宋体的机器（如 Linux 容器）上仍会退化成方块。 */
    private static final String DEFAULT_TEMPLATE =
            "<html><head><meta charset=\"UTF-8\"/><style>"
            + "body{font-family:SimSun,\"Microsoft YaHei\",\"WenQuanYi Zen Hei\","
            + "\"Noto Sans CJK SC\",\"PingFang SC\",sans-serif;padding:24px;}"
            + "h1{border-bottom:2px solid #333;} table{border-collapse:collapse;width:100%;}"
            + "td,th{border:1px solid #999;padding:6px;}</style></head><body>"
            + "<h1>${title}</h1>"
            + "<p>生成时间：${generateTime}</p>"
            + "<h2>一、智能摘要</h2><p>${aiSummary}</p>"
            + "<h2>二、行业岗位需求 Top10</h2><table><tr><th>行业</th><th>指标值</th></tr>"
            + "<#list industryTop as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>三、热门技能 Top20</h2><table><tr><th>技能</th><th>热度</th></tr>"
            + "<#list skillHot as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>四、城市分布</h2><table><tr><th>城市</th><th>岗位数</th></tr>"
            + "<#list cityDist as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>五、学历分布</h2><table><tr><th>学历</th><th>岗位数</th></tr>"
            + "<#list educationDist as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>六、按月趋势</h2><table><tr><th>周期</th><th>岗位数</th><th>平均薪资</th></tr>"
            + "<#list trend as item><tr><td>${item.period}</td><td>${item.jobCount}</td><td>${item.avgSalary!'-'}</td></tr></#list></table>"
            + "</body></html>";

    /** 内置就业报告模板（EMPLOYMENT 类，templateContent 为空时使用）。 */
    private static final String DEFAULT_EMPLOYMENT_TEMPLATE =
            "<html><head><meta charset=\"UTF-8\"/><style>"
            + "body{font-family:SimSun,\"Microsoft YaHei\",\"WenQuanYi Zen Hei\","
            + "\"Noto Sans CJK SC\",\"PingFang SC\",sans-serif;padding:24px;}"
            + "h1{border-bottom:2px solid #333;} table{border-collapse:collapse;width:100%;}"
            + "td,th{border:1px solid #999;padding:6px;} .meta{color:#808080;font-size:13px;}</style></head><body>"
            + "<h1>${title}</h1>"
            + "<p class=\"meta\">范围：${scopeLabel}　生成时间：${generateTime}</p>"
            + "<h2>一、智能摘要</h2><p>${aiSummary}</p>"
            + "<h2>二、总览</h2><table>"
            + "<tr><td>学生总数</td><td>${studentCount}</td><td>已填画像</td><td>${profiledCount}</td></tr>"
            + "<tr><td>累计投递</td><td>${applicationCount}</td><td>投递人数</td><td>${appliedStudentCount}</td></tr>"
            + "<tr><td>已录用</td><td>${offerCount}</td><td>OFFER 率</td><td>${offerRate}%</td></tr></table>"
            + "<h2>三、投递状态分布</h2><table><tr><th>状态</th><th>数量</th></tr>"
            + "<#list funnel as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>四、意向城市</h2><table><tr><th>城市</th><th>人数</th></tr>"
            + "<#list intentCity as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>五、意向行业</h2><table><tr><th>行业</th><th>人数</th></tr>"
            + "<#list intentIndustry as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>六、期望薪资分布</h2><table><tr><th>薪资区间</th><th>人数</th></tr>"
            + "<#list salaryBuckets as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "<h2>七、学生掌握技能 Top</h2><table><tr><th>技能</th><th>掌握人数</th></tr>"
            + "<#list topSkills as item><tr><td>${item.name}</td><td>${item.value}</td></tr></#list></table>"
            + "</body></html>";

    @Override
    public ReportRecord generate(GenerateReportDTO dto) {
        String category = (dto.getCategory() == null || dto.getCategory().isEmpty())
                ? "MARKET" : dto.getCategory().toUpperCase();
        boolean employment = "EMPLOYMENT".equals(category);

        // 记录先落库（GENERATING），失败时更新状态，保证过程可追溯
        ReportRecord record = new ReportRecord();
        record.setName(employment ? "学生就业数据报告" : "就业市场分析报告");
        record.setCategory(category);
        record.setParams(employment ? scopeJson(dto) : "{}");
        record.setFileType(dto.getFileType());
        record.setStatus("GENERATING");
        recordMapper.insert(record);

        try {
            String aiSummary;
            byte[] fileBytes;
            String ext = extOf(dto.getFileType());
            String name;

            if (employment) {
                // 学生就业数据报告：按 scope（专业/年级/班级，可空=全校）聚合
                EmploymentReportData emp = employmentReportService.build(
                        dto.getMajor(), dto.getEnrollYear(), dto.getClassId());
                name = "学生就业数据报告（" + emp.getScopeLabel() + "）";
                aiSummary = buildEmploymentSummary(emp);
                switch (dto.getFileType()) {
                    case "WORD":
                        fileBytes = wordExporter.exportEmployment(name, aiSummary, emp);
                        break;
                    case "PDF":
                        fileBytes = pdfExporter.export(renderEmploymentHtml(emp, aiSummary, name));
                        break;
                    default:
                        fileBytes = renderEmploymentHtml(emp, aiSummary, name)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
            } else {
                // 市场行业报告：全平台市场分析数据
                name = "就业市场分析报告";
                DashboardQueryDTO query = new DashboardQueryDTO();
                query.setTenantId(TenantContextHolder.getTenantId());
                DashboardVO dashboard = analysisService.getDashboard(query);
                aiSummary = aiSummaryService.summarize(dashboard);
                switch (dto.getFileType()) {
                    case "PDF":
                        fileBytes = pdfExporter.export(renderHtml(dashboard, aiSummary, name));
                        break;
                    case "WORD":
                        fileBytes = wordExporter.export(name, aiSummary, dashboard);
                        break;
                    default:
                        fileBytes = renderHtml(dashboard, aiSummary, name)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            // 落盘 + 更新记录
            String fileName = IdUtil.fastSimpleUUID() + "." + ext;
            File dir = new File(storagePath, String.valueOf(TenantContextHolder.getTenantId()));
            File file = new File(dir, fileName);
            FileUtil.writeBytes(fileBytes, file);

            record.setName(name);
            record.setFileUrl(file.getPath());
            record.setAiSummary(aiSummary);
            record.setStatus("SUCCESS");
            recordMapper.updateById(record);
            log.info("报告生成成功: {} ({}), file={}", name, category, file.getPath());
            return record;

        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setErrorMsg(e.getMessage());
            recordMapper.updateById(record);
            log.error("报告生成失败: category={}", category, e);
            throw new BizException("报告生成失败: " + e.getMessage());
        }
    }

    @Override
    public Page<ReportRecord> pageRecords(int pageNum, int pageSize) {
        // 租户级报告列表：只列管理员生成的（user_id IS NULL），不混入学生个人 AI 报告
        LambdaQueryWrapper<ReportRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(ReportRecord::getUserId)
               .orderByDesc(ReportRecord::getCreateTime);
        return recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ReportRecord latestSuccess() {
        LambdaQueryWrapper<ReportRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(ReportRecord::getUserId)
               .eq(ReportRecord::getStatus, "SUCCESS")
               .orderByDesc(ReportRecord::getCreateTime)
               .last("LIMIT 1");
        return recordMapper.selectOne(wrapper);
    }

    @Override
    public ReportRecord getRecord(Long recordId) {
        return recordMapper.selectById(recordId);
    }

    @Override
    public byte[] loadReportFile(Long recordId) {
        ReportRecord record = recordMapper.selectById(recordId);
        if (record == null || record.getFileUrl() == null) {
            throw new BizException("报告不存在或尚未生成");
        }
        // 归属校验，防枚举越权：
        String role = UserContextHolder.getRole();
        if (record.getUserId() != null) {
            // 个人 AI 报告：只有本人或管理员可下载
            if (!"ADMIN".equals(role) && !record.getUserId().equals(UserContextHolder.getUserId())) {
                throw new BizException(403, "无权下载他人的报告");
            }
        } else if ("STUDENT".equals(role)) {
            // 租户级报告：学生仅能下载「全体可见的市场报告」或「已下发给自己的就业报告」
            if (!deliveryService.canStudentAccess(recordId, UserContextHolder.getUserId())) {
                throw new BizException(403, "无权下载该报告");
            }
        }
        // ADMIN / TEACHER / HR 可下载本租户任意租户级报告（沿用原行为）
        File file = new File(record.getFileUrl());
        if (!file.exists()) {
            throw new BizException("报告文件已丢失，请重新生成");
        }
        return FileUtil.readBytes(file);
    }

    @Override
    public void deleteRecord(Long recordId) {
        ReportRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        if (record.getFileUrl() != null) {
            FileUtil.del(record.getFileUrl());
        }
        recordMapper.deleteById(recordId);
    }

    /** 市场行业报告 Freemarker 渲染（固定内置模板，6 节） */
    private String renderHtml(DashboardVO dashboard, String aiSummary, String title) throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("title", title);
        model.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        model.put("aiSummary", aiSummary);
        model.put("industryTop", safe(dashboard.getIndustryTop()));
        model.put("skillHot", safe(dashboard.getSkillHot()));
        model.put("cityDist", safe(dashboard.getCityDist()));
        model.put("educationDist", safe(dashboard.getEducationDist()));
        model.put("trend", safe(dashboard.getTrend()));

        freemarker.template.Configuration cfg =
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
        Template tmpl = new Template("report", new StringReader(DEFAULT_TEMPLATE), cfg);
        StringWriter writer = new StringWriter();
        tmpl.process(model, writer);
        return writer.toString();
    }

    /** 学生就业报告 Freemarker 渲染（固定内置模板） */
    private String renderEmploymentHtml(EmploymentReportData emp, String aiSummary, String title) throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("title", title);
        model.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        model.put("scopeLabel", emp.getScopeLabel());
        model.put("studentCount", emp.getStudentCount());
        model.put("profiledCount", emp.getProfiledCount());
        model.put("applicationCount", emp.getApplicationCount());
        model.put("appliedStudentCount", emp.getAppliedStudentCount());
        model.put("offerCount", emp.getOfferCount());
        model.put("offerRate", emp.getOfferRate());
        model.put("aiSummary", aiSummary);
        model.put("funnel", safe(emp.getFunnel()));
        model.put("intentCity", safe(emp.getIntentCity()));
        model.put("intentIndustry", safe(emp.getIntentIndustry()));
        model.put("salaryBuckets", safe(emp.getSalaryBuckets()));
        model.put("topSkills", safe(emp.getTopSkills()));

        freemarker.template.Configuration cfg =
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
        Template tmpl = new Template("employment-report", new StringReader(DEFAULT_EMPLOYMENT_TEMPLATE), cfg);
        StringWriter writer = new StringWriter();
        tmpl.process(model, writer);
        return writer.toString();
    }

    private String extOf(String fileType) {
        if ("PDF".equals(fileType)) {
            return "pdf";
        }
        if ("WORD".equals(fileType)) {
            return "docx";
        }
        return "html";
    }

    /** 把就业报告的 scope 入参序列化进 report_record.params */
    private String scopeJson(GenerateReportDTO dto) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (dto.getMajor() != null) {
            m.put("major", dto.getMajor());
        }
        if (dto.getEnrollYear() != null) {
            m.put("enrollYear", dto.getEnrollYear());
        }
        if (dto.getClassId() != null) {
            m.put("classId", dto.getClassId());
        }
        return JSONUtil.toJsonStr(m);
    }

    /** 就业报告的规则化摘要（无 AI 依赖，始终可用） */
    private String buildEmploymentSummary(EmploymentReportData e) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("本报告覆盖%s，共 %d 名学生，其中 %d 人已完善画像。",
                e.getScopeLabel(), e.getStudentCount(), e.getProfiledCount()));
        if (e.getApplicationCount() > 0) {
            sb.append(String.format("累计发起 %d 次站内投递（%d 名学生参与），获得 %d 个录用，OFFER 率 %.1f%%。",
                    e.getApplicationCount(), e.getAppliedStudentCount(), e.getOfferCount(), e.getOfferRate()));
        } else {
            sb.append("范围内暂无站内投递记录。");
        }
        appendTop(sb, "意向城市集中在", e.getIntentCity());
        appendTop(sb, "学生掌握较多的技能有", e.getTopSkills());
        return sb.toString();
    }

    private void appendTop(StringBuilder sb, String prefix, java.util.List<EmploymentReportData.DimItem> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        sb.append(prefix).append(list.get(0).getName());
        if (list.size() > 1) {
            sb.append("、").append(list.get(1).getName());
        }
        sb.append("等。");
    }

    private <T> java.util.List<T> safe(java.util.List<T> list) {
        return list == null ? java.util.Collections.emptyList() : list;
    }
}
