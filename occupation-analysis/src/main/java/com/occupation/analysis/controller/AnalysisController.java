package com.occupation.analysis.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.service.AnalysisJobService;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.service.DataCleanService;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.analysis.vo.EmploymentVO;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 分析数据控制器 — Dashboard + 职位查询
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final JobDetailService jobDetailService;
    private final AnalysisJobService analysisJobService;
    private final DataCleanService dataCleanService;

    /**
     * Dashboard 分析数据
     * <p>
     * 返回 5 个维度的可视化数据：行业 Top、城市分布、技能热度、学历分布、趋势
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(DashboardQueryDTO query) {
        log.info("请求 Dashboard 数据, tenantId={}", query.getTenantId());
        DashboardVO vo = analysisService.getDashboard(query);
        return Result.ok(vo);
    }

    /**
     * 就业分析：投递漏斗 / 供需错配 / 自主求职流向
     * <p>
     * 与 {@code /dashboard} 分开：看板讲「市场有什么岗位」，这里讲「本校学生怎么样」。
     * 数据同样来自 {@code analysis_result}，需先执行过「手动重算分析数据」。
     * <p>
     * 限管理员与教师：投递转化率反映的是 HR 的处理效率，学生和 HR 都不该看到全局口径。
     * （{@code /dashboard} 是纯市场数据，不限角色。）
     */
    @GetMapping("/employment")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<EmploymentVO> getEmployment() {
        return Result.ok(analysisService.getEmployment());
    }

    /**
     * 职位分页查询
     * <p>
     * 支持按城市、行业、薪资范围、学历、经验、关键词筛选
     */
    @GetMapping("/jobs")
    public Result<Page<JobDetailVO>> queryJobs(JobQueryDTO query) {
        log.info("请求职位列表, pageNum={}, pageSize={}, keyword={}",
                query.getPageNum(), query.getPageSize(), query.getKeyword());
        Page<JobDetailVO> page = jobDetailService.queryJobs(query);
        return Result.ok(page);
    }

    /**
     * 手动触发统计重算（管理员）
     * <p>
     * 演示/联调用：立即将 job_detail 聚合到 analysis_result，无需等凌晨定时任务。
     */
    @PostMapping("/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> rebuild() {
        int rows = analysisJobService.runAll();
        log.info("手动统计重算完成，写入 {} 条", rows);
        return Result.ok(rows);
    }

    /**
     * 手动触发数据清洗（管理员）
     * <p>
     * 将 raw_job_data 中 status=RAW 的存量数据清洗写入 job_detail。
     */
    @PostMapping("/clean")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> clean() {
        int count = dataCleanService.cleanPendingRawData();
        log.info("手动清洗完成，入库 {} 条", count);
        return Result.ok(count);
    }

    /**
     * 手动触发完整数据流水线：清洗 + 统计重算（管理员）
     */
    @PostMapping("/pipeline")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> pipeline() {
        int cleaned = dataCleanService.cleanPendingRawData();
        log.info("流水线-清洗完成: {} 条", cleaned);
        int analyzed = analysisJobService.runAll();
        log.info("流水线-分析完成: {} 条", analyzed);
        Map<String, Object> result = new HashMap<>();
        result.put("cleaned", cleaned);
        result.put("analyzed", analyzed);
        return Result.ok(result);
    }
}
