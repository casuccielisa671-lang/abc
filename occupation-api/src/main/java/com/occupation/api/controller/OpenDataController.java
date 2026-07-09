package com.occupation.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.api.service.OpenDataService;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.service.ReportGeneratorService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 开放数据接口 — 供第三方系统调用（需 Bearer accessToken，见 OpenAuthController）
 * <p>
 * 数据边界：只提供职位数据与聚合统计，不暴露任何用户/学生个人数据。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
public class OpenDataController {

    private final JobDetailService jobDetailService;
    private final OpenDataService openDataService;
    private final ReportGeneratorService reportGeneratorService;

    /** 职位查询（条件同站内查询：城市/行业/薪资/学历/关键词/分页） */
    @GetMapping("/jobs")
    public Result<PageResult<JobDetailVO>> queryJobs(JobQueryDTO query) {
        Page<JobDetailVO> page = jobDetailService.queryJobs(query);
        return Result.ok(PageResult.of(page));
    }

    /** 职位详情 */
    @GetMapping("/jobs/{id}")
    public Result<JobDetailVO> getJob(@PathVariable Long id) {
        return Result.ok(jobDetailService.getJobById(id));
    }

    /** 就业大盘统计 */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> statsOverview() {
        return Result.ok(openDataService.getStatsOverview());
    }

    /** 热门技能排行 */
    @GetMapping("/stats/skills/hot")
    public Result<List<Map<String, Object>>> skillHot(@RequestParam(defaultValue = "20") int topN) {
        return Result.ok(openDataService.getSkillHot(topN));
    }

    /** 行业岗位分布 */
    @GetMapping("/stats/industries")
    public Result<List<Map<String, Object>>> industryDist(@RequestParam(defaultValue = "10") int topN) {
        return Result.ok(openDataService.getIndustryDist(topN));
    }

    /**
     * 最新报告摘要 —— 本客户端所属租户下最近一份生成成功的报告
     * <p>
     * 只返回元信息与摘要文本，不返回文件下载地址（报告文件属租户内部资产）。
     * 租户范围由 ApiTokenInterceptor 建立的租户上下文限定。
     */
    @GetMapping("/reports/summary")
    public Result<ReportSummaryVO> reportSummary() {
        ReportRecord record = reportGeneratorService.latestSuccess();
        if (record == null) {
            return Result.ok(null);
        }
        ReportSummaryVO vo = new ReportSummaryVO();
        vo.setReportId(record.getId());
        vo.setFileType(record.getFileType());
        vo.setGeneratedAt(record.getCreateTime());
        vo.setSummary(record.getAiSummary());
        return Result.ok(vo);
    }

    /** 报告摘要出参（不含 fileUrl，避免暴露服务器路径） */
    @Data
    public static class ReportSummaryVO {
        private Long reportId;
        private String fileType;
        private LocalDateTime generatedAt;
        private String summary;
    }
}
