package com.occupation.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.report.dto.GenerateReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.entity.ReportTemplate;
import com.occupation.report.service.ReportGeneratorService;
import com.occupation.report.service.ReportTemplateService;
import com.occupation.report.vo.ReportRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 报告生成与下载接口
 * <p>
 * 管理端：生成/删除；学生端与教师端：查看列表 + 下载（只读）。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportRecordController {

    private final ReportGeneratorService generatorService;
    private final ReportTemplateService templateService;

    /** 触发生成报告（ADMIN） */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReportRecord> generate(@RequestBody @Validated GenerateReportDTO dto) {
        return Result.ok(generatorService.generate(dto));
    }

    /** 报告记录列表（登录即可查看，租户内共享；附模板名称与报告类型） */
    @GetMapping("/records")
    public Result<PageResult<ReportRecordVO>> pageRecords(@RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        Page<ReportRecord> page = generatorService.pageRecords(pageNum, pageSize);

        // 一页最多十几条，逐条取模板即可；模板服务内部走 selectById，命中主键索引
        Map<Long, ReportTemplate> templates = page.getRecords().stream()
                .map(ReportRecord::getTemplateId)
                .distinct()
                .map(templateService::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ReportTemplate::getId, t -> t));

        List<ReportRecordVO> list = page.getRecords().stream()
                .map(r -> ReportRecordVO.of(r, templates.get(r.getTemplateId())))
                .collect(Collectors.toList());
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), list));
    }

    /** 下载报告文件 */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] bytes = generatorService.loadReportFile(id);
        ReportRecord record = generatorService.getRecord(id);
        // 带上扩展名，否则下载下来是个无扩展名文件，双击打不开
        String ext = extensionOf(record);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ext)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    private String extensionOf(ReportRecord record) {
        if (record == null || record.getFileType() == null) {
            return "";
        }
        switch (record.getFileType()) {
            case "PDF":
                return ".pdf";
            case "WORD":
                return ".docx";
            case "HTML":
                return ".html";
            default:
                return "";
        }
    }

    /** 删除报告（ADMIN） */
    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        generatorService.deleteRecord(id);
        return Result.ok();
    }
}
