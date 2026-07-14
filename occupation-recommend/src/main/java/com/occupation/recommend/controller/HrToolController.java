package com.occupation.recommend.controller;

import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.dto.JdOptimizeRequest;
import com.occupation.recommend.service.HrToolService;
import com.occupation.recommend.vo.JdOptimizeVO;
import com.occupation.recommend.vo.SalaryBenchmarkVO;
import com.occupation.recommend.vo.TalentCompareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * HR端工具箱接口 — JD优化 / 人才对比 / 薪资竞争力
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/hr/tools")
@RequiredArgsConstructor
public class HrToolController {

    private final HrToolService hrToolService;

    /** JD 优化助手：分析职位描述文本 */
    @PostMapping("/optimize-jd")
    @PreAuthorize("hasRole('HR')")
    public Result<JdOptimizeVO> optimizeJd(@Valid @RequestBody JdOptimizeRequest request) {
        log.info("JD优化: hrId={}, textLen={}", UserContextHolder.getUserId(),
                request.getJdText() != null ? request.getJdText().length() : 0);
        return Result.ok(hrToolService.optimizeJd(request.getJdText()));
    }

    /** 人才对比：选择 2~4 个候选人 ID */
    @PostMapping("/compare-talents")
    @PreAuthorize("hasRole('HR')")
    public Result<TalentCompareVO> compareTalents(@RequestBody List<Long> userIds) {
        log.info("人才对比: hrId={}, userIds={}", UserContextHolder.getUserId(), userIds);
        return Result.ok(hrToolService.compareTalents(userIds));
    }

    /** 薪资竞争力分析：输入岗位名称 */
    @GetMapping("/salary-benchmark")
    @PreAuthorize("hasRole('HR')")
    public Result<SalaryBenchmarkVO> salaryBenchmark(@RequestParam String jobTitle) {
        log.info("薪资竞争力分析: hrId={}, jobTitle={}", UserContextHolder.getUserId(), jobTitle);
        return Result.ok(hrToolService.benchmarkSalary(jobTitle));
    }
}
