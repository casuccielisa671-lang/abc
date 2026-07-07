package com.occupation.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.api.service.OpenDataService;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    // TODO(P5-B组): GET /api/open/reports/summary — 最新报告摘要
    //   实现思路：查 report_record 最新 SUCCESS 记录，返回报告元信息 + AI 摘要文本
    //   （需要 report 模块暴露 Service 接口，api/pom.xml 增加 occupation-report 依赖）
}
