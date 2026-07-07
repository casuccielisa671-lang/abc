package com.occupation.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.dto.JobSaveDTO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 企业 HR 端接口 — 职位发布 + 人才浏览（脱敏）
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrController {

    private final JobDetailService jobDetailService;
    private final StudentProfileService profileService;

    /** 发布职位（平台内发布，source=HR_PUBLISH） */
    @PostMapping("/jobs")
    public Result<Long> publishJob(@RequestBody @Validated JobSaveDTO dto) {
        dto.setId(null);
        return Result.ok(jobDetailService.saveJob(dto));
    }

    /** 编辑职位（仅限 HR_PUBLISH 来源） */
    @PutMapping("/jobs/{id}")
    public Result<Long> updateJob(@PathVariable Long id, @RequestBody @Validated JobSaveDTO dto) {
        dto.setId(id);
        return Result.ok(jobDetailService.saveJob(dto));
    }

    /** 下架职位 */
    @DeleteMapping("/jobs/{id}")
    public Result<Void> removeJob(@PathVariable Long id) {
        jobDetailService.removeJob(id);
        return Result.ok();
    }

    /** 职位列表（复用职位查询，keyword 传公司名可筛选自己发布的职位）
     *  TODO(P5-C组): job_detail 增加 publisher_id 字段后改为"只看我发布的" */
    @GetMapping("/jobs")
    public Result<PageResult<JobDetailVO>> myJobs(JobQueryDTO query) {
        Page<JobDetailVO> page = jobDetailService.queryJobs(query);
        return Result.ok(PageResult.of(page));
    }

    /** 人才浏览 — 脱敏：只暴露专业/技能/学历/意向，不含姓名、联系方式、userId */
    @GetMapping("/talents")
    public Result<List<TalentVO>> talents() {
        List<TalentVO> list = profileService.listAll().stream()
                .map(TalentVO::from)
                .collect(Collectors.toList());
        return Result.ok(list);
    }

    // TODO(P5-C组): GET /api/hr/applications — 收到的投递列表
    //   实现思路：查 student_behavior 中 action=APPLY 且 job_id 属于本 HR 发布的职位，
    //   关联学生画像（脱敏）返回；依赖 job_detail.publisher_id 字段（见上方 TODO）。

    /** 人才卡片（脱敏视图）——内部静态 VO，防止画像实体直接外泄 */
    @lombok.Data
    public static class TalentVO {
        private String major;
        private String skills;
        private String expectedCity;
        private String expectedIndustry;
        private String educationLevel;

        static TalentVO from(SysStudentProfile p) {
            TalentVO vo = new TalentVO();
            vo.major = p.getMajor();
            vo.skills = p.getSkills();
            vo.expectedCity = p.getExpectedCity();
            vo.expectedIndustry = p.getExpectedIndustry();
            vo.educationLevel = p.getEducationLevel();
            return vo;
        }
    }
}
