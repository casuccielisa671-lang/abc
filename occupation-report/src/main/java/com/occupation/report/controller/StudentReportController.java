package com.occupation.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.report.dto.StudentReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.service.ReportDeliveryService;
import com.occupation.report.service.StudentAiReportService;
import com.occupation.report.vo.ReceivedReportVO;
import com.occupation.report.vo.ReportRecordVO;
import com.occupation.report.vo.StudentAiReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生个人 AI 分析报告接口。
 * <p>
 * 预览可多轮改（前端持对话历史，不落库）；保存才落库为个人报告；「我的报告」只看自己的。
 * 下载复用 {@code /api/report/download/{id}}（含归属校验）。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentReportController {

    private final StudentAiReportService service;
    private final ReportDeliveryService deliveryService;

    /** 生成/多轮改：返回正文，不落库 */
    @PostMapping("/ai-report/preview")
    public Result<StudentAiReportVO> preview(@RequestBody StudentReportDTO dto) {
        return Result.ok(service.preview(UserContextHolder.getUserId(), dto));
    }

    /** 定稿保存：落库为个人报告 + 导出文件 */
    @PostMapping("/ai-report/save")
    public Result<ReportRecord> save(@RequestBody @Validated StudentReportDTO dto) {
        return Result.ok(service.save(UserContextHolder.getUserId(), dto));
    }

    /** 我的报告列表（仅本人） */
    @GetMapping("/reports")
    public Result<PageResult<ReportRecordVO>> myReports(@RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        Page<ReportRecord> page = service.pageMy(UserContextHolder.getUserId(), pageNum, pageSize);
        List<ReportRecordVO> list = page.getRecords().stream().map(ReportRecordVO::of).collect(Collectors.toList());
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), list));
    }

    /** 收到的报告：管理员广播的市场报告 + 定向下发给我的就业报告 */
    @GetMapping("/received-reports")
    public Result<PageResult<ReceivedReportVO>> received(@RequestParam(defaultValue = "1") int pageNum,
                                                         @RequestParam(defaultValue = "10") int pageSize) {
        Page<ReceivedReportVO> page = deliveryService.receivedFor(UserContextHolder.getUserId(), pageNum, pageSize);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    /** 标记某收到的报告为已读（下载时前端可顺手调） */
    @PostMapping("/received-reports/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        deliveryService.markRead(id, UserContextHolder.getUserId());
        return Result.ok();
    }
}
