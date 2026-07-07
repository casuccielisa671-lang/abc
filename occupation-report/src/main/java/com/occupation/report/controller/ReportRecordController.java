package com.occupation.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.report.dto.GenerateReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.service.ReportGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    /** 触发生成报告（ADMIN） */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReportRecord> generate(@RequestBody @Validated GenerateReportDTO dto) {
        return Result.ok(generatorService.generate(dto));
    }

    /** 报告记录列表（登录即可查看，租户内共享） */
    @GetMapping("/records")
    public Result<PageResult<ReportRecord>> pageRecords(@RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        Page<ReportRecord> page = generatorService.pageRecords(pageNum, pageSize);
        return Result.ok(PageResult.of(page));
    }

    /** 下载报告文件 */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        byte[] bytes = generatorService.loadReportFile(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    /** 删除报告（ADMIN） */
    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        generatorService.deleteRecord(id);
        return Result.ok();
    }
}
