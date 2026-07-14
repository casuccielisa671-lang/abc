package com.occupation.recommend.controller;

import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.service.StudentToolService;
import com.occupation.recommend.vo.JobChecklistVO;
import com.occupation.recommend.vo.JobCompareVO;
import com.occupation.recommend.vo.SalaryCalcVO;
import com.occupation.recommend.vo.SkillRoiVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生端工具箱接口 — 多岗位对比 / 技能ROI / 期望薪资 / 求职清单
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/student/tools")
@RequiredArgsConstructor
public class StudentToolController {

    private final StudentToolService studentToolService;

    /** 多岗位对比：选择 2~4 个岗位 ID */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/compare-jobs")
    public Result<JobCompareVO> compareJobs(@RequestBody List<Long> jobIds) {
        log.info("多岗位对比: userId={}, jobIds={}", UserContextHolder.getUserId(), jobIds);
        return Result.ok(studentToolService.compareJobs(jobIds));
    }

    /** 技能 ROI 分析 */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/skill-roi")
    public Result<SkillRoiVO> skillRoi(@RequestParam String skill) {
        log.info("技能ROI分析: userId={}, skill={}", UserContextHolder.getUserId(), skill);
        return Result.ok(studentToolService.analyzeSkillRoi(skill));
    }

    /** 期望薪资计算器 */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/salary-calc")
    public Result<SalaryCalcVO> salaryCalc(@RequestParam(required = false) String city,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String education,
                                           @RequestParam(required = false) String experience) {
        log.info("期望薪资计算: userId={}, city={}, keyword={}, edu={}, exp={}",
                UserContextHolder.getUserId(), city, keyword, education, experience);
        return Result.ok(studentToolService.calcSalary(city, keyword, education, experience));
    }

    /** 求职清单生成器 */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/job-checklist")
    public Result<JobChecklistVO> jobChecklist(@RequestParam Long jobId) {
        Long userId = UserContextHolder.getUserId();
        log.info("求职清单: userId={}, jobId={}", userId, jobId);
        return Result.ok(studentToolService.generateChecklist(jobId, userId));
    }
}
