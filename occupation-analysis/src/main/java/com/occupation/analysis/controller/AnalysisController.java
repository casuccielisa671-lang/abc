package com.occupation.analysis.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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

    /**
     * Dashboard 分析数据
     * <p>
     * 返回 5 个维度的可视化数据：行业 Top、城市分布、技能热度、学历分布、趋势
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(@Valid DashboardQueryDTO query) {
        log.info("请求 Dashboard 数据, tenantId={}", query.getTenantId());
        DashboardVO vo = analysisService.getDashboard(query);
        return Result.ok(vo);
    }

    /**
     * 职位分页查询
     * <p>
     * 支持按城市、行业、薪资范围、学历、经验、关键词筛选
     */
    @GetMapping("/jobs")
    public Result<Page<JobDetailVO>> queryJobs(@Valid JobQueryDTO query) {
        log.info("请求职位列表, pageNum={}, pageSize={}, keyword={}",
                query.getPageNum(), query.getPageSize(), query.getKeyword());
        Page<JobDetailVO> page = jobDetailService.queryJobs(query);
        return Result.ok(page);
    }
}
