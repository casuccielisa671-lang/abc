package com.occupation.analysis.controller;

import com.occupation.analysis.service.AnalysisJobService;
import com.occupation.analysis.service.MapService;
import com.occupation.analysis.vo.CityStatVO;
import com.occupation.analysis.vo.JobCityHeatVO;
import com.occupation.analysis.vo.RecommendJobVO;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页 3D 地图数据接口
 */
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final AnalysisJobService analysisJobService;

    /**
     * 推荐职业列表（首页左侧卡片）
     */
    @GetMapping("/recommendJobs")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT','TEACHER','HR')")
    public Result<List<RecommendJobVO>> recommendJobs() {
        return Result.ok(mapService.recommendJobs());
    }

    /**
     * 某职业在各城市的聚集度（3D 热力图 + 右侧图表）
     */
    @GetMapping("/getJobCityHeat")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT','TEACHER','HR')")
    public Result<List<JobCityHeatVO>> getJobCityHeat(@RequestParam String jobName) {
        return Result.ok(mapService.getJobCityHeat(jobName));
    }

    /**
     * 全量城市岗位分布（岗位数 + 平均薪资 + 坐标），3D 柱状地图默认展示
     */
    @GetMapping("/cityDistribution")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT','TEACHER','HR')")
    public Result<List<CityStatVO>> cityDistribution() {
        return Result.ok(mapService.cityDistribution());
    }

    /**
     * 管理员 — 触发 Spark/分析任务重算（刷新聚集度数据源）
     */
    @PostMapping("/rebuildJobGather")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> rebuildJobGather() {
        int rows = analysisJobService.runAll();
        return Result.ok(rows);
    }
}
