package com.occupation.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.report.dto.DeliverReportDTO;
import com.occupation.report.dto.GenerateReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.service.ReportDeliveryService;
import com.occupation.report.service.ReportGeneratorService;
import com.occupation.report.vo.ReportRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final ReportDeliveryService deliveryService;

    /** 触发生成报告（ADMIN） */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ReportRecord> generate(@RequestBody @Validated GenerateReportDTO dto) {
        return Result.ok(generatorService.generate(dto));
    }

    /** 报告记录列表（登录即可查看，租户内共享） */
    @GetMapping("/records")
    public Result<PageResult<ReportRecordVO>> pageRecords(@RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        Page<ReportRecord> page = generatorService.pageRecords(pageNum, pageSize);
        List<ReportRecordVO> list = page.getRecords().stream()
                .map(ReportRecordVO::of)
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

    /**
     * 把就业报告发送给某范围内的学生（ADMIN）。
     * 市场行业报告已对全体可见、无需发送，服务端会拒绝。返回本次新增下发人数。
     */
    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> deliver(@PathVariable Long id, @RequestBody @Validated DeliverReportDTO dto) {
        return Result.ok(deliveryService.deliver(id, dto.getTargetType(), dto.getTargetValue()));
    }

    /** 该报告已下发的学生人数（ADMIN，用于列表展示「已发送 N 人」） */
    @GetMapping("/{id}/delivery-count")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> deliveryCount(@PathVariable Long id) {
        return Result.ok(deliveryService.deliveredCount(id));
    }
}
