package com.occupation.report.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.report.dto.GenerateReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.entity.ReportTemplate;
import com.occupation.report.export.PdfExporter;
import com.occupation.report.export.WordExporter;
import com.occupation.report.mapper.ReportRecordMapper;
import com.occupation.report.service.AiSummaryService;
import com.occupation.report.service.ReportGeneratorService;
import com.occupation.report.service.ReportTemplateService;
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

    private final ReportTemplateService templateService;
    private final ReportRecordMapper recordMapper;
    private final AnalysisService analysisService;
    private final AiSummaryService aiSummaryService;
    private final PdfExporter pdfExporter;
    private final WordExporter wordExporter;

    @Value("${app.report.storage-path:./data/reports}")
    private String storagePath;

    /** 内置默认模板（模板 templateContent 为空时使用）。
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
            + "</body></html>";

    @Override
    public ReportRecord generate(GenerateReportDTO dto) {
        // ① 读模板
        ReportTemplate template = templateService.getById(dto.getTemplateId());

        // 记录先落库（GENERATING），失败时更新状态，保证过程可追溯
        ReportRecord record = new ReportRecord();
        record.setTemplateId(template.getId());
        record.setParams("{}");
        record.setFileType(dto.getFileType());
        record.setStatus("GENERATING");
        recordMapper.insert(record);

        try {
            // ② 取分析数据
            DashboardQueryDTO query = new DashboardQueryDTO();
            query.setTenantId(TenantContextHolder.getTenantId());
            DashboardVO dashboard = analysisService.getDashboard(query);

            // ③ AI 智能摘要（失败自动降级，不阻塞）
            String aiSummary = aiSummaryService.summarize(dashboard);

            // ④ Freemarker 渲染 HTML
            String html = renderHtml(template, dashboard, aiSummary);

            // ⑤ 按格式导出
            byte[] fileBytes;
            String ext;
            switch (dto.getFileType()) {
                case "PDF":
                    fileBytes = pdfExporter.export(html);
                    ext = "pdf";
                    break;
                case "WORD":
                    // 结构化导出：标题层级 + 各维度数据表格，与 PDF 走同一份分析数据
                    fileBytes = wordExporter.export(template.getName(), aiSummary, dashboard);
                    ext = "docx";
                    break;
                default:
                    fileBytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    ext = "html";
            }

            // ⑥ 落盘 + 更新记录
            String fileName = IdUtil.fastSimpleUUID() + "." + ext;
            File dir = new File(storagePath, String.valueOf(TenantContextHolder.getTenantId()));
            File file = new File(dir, fileName);
            FileUtil.writeBytes(fileBytes, file);

            record.setFileUrl(file.getPath());
            record.setAiSummary(aiSummary);
            record.setStatus("SUCCESS");
            recordMapper.updateById(record);
            log.info("报告生成成功: template={}, file={}", template.getName(), file.getPath());
            return record;

        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setErrorMsg(e.getMessage());
            recordMapper.updateById(record);
            log.error("报告生成失败: templateId={}", dto.getTemplateId(), e);
            throw new BizException("报告生成失败: " + e.getMessage());
        }
    }

    @Override
    public Page<ReportRecord> pageRecords(int pageNum, int pageSize) {
        LambdaQueryWrapper<ReportRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ReportRecord::getCreateTime);
        return recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ReportRecord latestSuccess() {
        LambdaQueryWrapper<ReportRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportRecord::getStatus, "SUCCESS")
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

    /** Freemarker 渲染：模板内容为空时使用内置默认模板 */
    private String renderHtml(ReportTemplate template, DashboardVO dashboard, String aiSummary) throws Exception {
        String templateContent = (template.getTemplateContent() == null || template.getTemplateContent().isEmpty())
                ? DEFAULT_TEMPLATE
                : template.getTemplateContent();

        Map<String, Object> model = new HashMap<>();
        model.put("title", template.getName());
        model.put("generateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        model.put("aiSummary", aiSummary);
        model.put("dashboard", dashboard);
        model.put("industryTop", safe(dashboard.getIndustryTop()));
        model.put("skillHot", safe(dashboard.getSkillHot()));
        model.put("cityDist", safe(dashboard.getCityDist()));
        model.put("educationDist", safe(dashboard.getEducationDist()));

        freemarker.template.Configuration cfg =
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
        Template tmpl = new Template("report", new StringReader(templateContent), cfg);
        StringWriter writer = new StringWriter();
        tmpl.process(model, writer);
        return writer.toString();
    }

    private <T> java.util.List<T> safe(java.util.List<T> list) {
        return list == null ? java.util.Collections.emptyList() : list;
    }
}
