package com.occupation.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.dto.JobSaveDTO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.ApplicationVO;
import com.occupation.recommend.vo.TalentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企业 HR 端接口 — 职位发布 + 人才浏览（脱敏）+ 收到的投递
 * <p>
 * 职位归属：job_detail.publisher_id 记录发布者，HR 只能看到/改动自己发布的职位。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrController {

    /** 单次投递查询涉及的职位上限，防止 IN 子句无限增长 */
    private static final int MAX_OWNED_JOBS = 500;

    private final JobDetailService jobDetailService;
    private final StudentProfileService profileService;
    private final BehaviorService behaviorService;

    /** 发布职位（平台内发布，source=HR_PUBLISH，publisher_id=当前 HR） */
    @PostMapping("/jobs")
    public Result<Long> publishJob(@RequestBody @Validated JobSaveDTO dto) {
        dto.setId(null);
        return Result.ok(jobDetailService.saveJob(dto, UserContextHolder.getUserId()));
    }

    /** 编辑职位（仅限本人发布的 HR_PUBLISH 职位） */
    @PutMapping("/jobs/{id}")
    public Result<Long> updateJob(@PathVariable Long id, @RequestBody @Validated JobSaveDTO dto) {
        dto.setId(id);
        return Result.ok(jobDetailService.saveJob(dto, UserContextHolder.getUserId()));
    }

    /** 下架职位（仅限本人发布的职位） */
    @DeleteMapping("/jobs/{id}")
    public Result<Void> removeJob(@PathVariable Long id) {
        jobDetailService.removeJob(id, UserContextHolder.getUserId());
        return Result.ok();
    }

    /** 我发布的职位列表 —— publisherId 由服务端强制覆盖，前端传入无效 */
    @GetMapping("/jobs")
    public Result<PageResult<JobDetailVO>> myJobs(JobQueryDTO query) {
        query.setPublisherId(UserContextHolder.getUserId());
        Page<JobDetailVO> page = jobDetailService.queryJobs(query);
        return Result.ok(PageResult.of(page));
    }

    /**
     * 人才浏览 — 脱敏：只暴露专业/技能/学历/意向/活跃度，不含姓名、联系方式、userId
     */
    @GetMapping("/talents")
    public Result<PageResult<TalentVO>> talents(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String education,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        Page<SysStudentProfile> profilePage = profileService.pageProfiles(keyword, education, page, size);
        List<SysStudentProfile> profiles = profilePage.getRecords();

        Map<Long, Map<String, Long>> behaviorCounts = behaviorService.countByActionGroupedByUser(
                profiles.stream().map(SysStudentProfile::getUserId).collect(Collectors.toSet()));

        List<TalentVO> list = profiles.stream()
                .map(p -> TalentVO.from(p, behaviorCounts.getOrDefault(p.getUserId(), Collections.emptyMap())))
                .collect(Collectors.toList());
        return Result.ok(PageResult.of(profilePage.getTotal(), profilePage.getCurrent(),
                profilePage.getSize(), list));
    }

    /**
     * 收到的投递 —— 本 HR 发布的职位上，学生的 APPLY 行为
     * <p>
     * 学生信息保持脱敏（与人才浏览一致，不返回姓名/联系方式），
     * 但附带职位标题与投递时间，便于 HR 定位是哪条职位收到了投递。
     * 租户隔离由 student_behavior 上的多租户插件保证。
     */
    @GetMapping("/applications")
    public Result<List<ApplicationVO>> applications() {
        // 1. 我发布的职位
        JobQueryDTO query = new JobQueryDTO();
        query.setPublisherId(UserContextHolder.getUserId());
        query.setPageNum(1);
        query.setPageSize(MAX_OWNED_JOBS);
        List<JobDetailVO> myJobs = jobDetailService.queryJobs(query).getRecords();
        if (myJobs.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        Map<Long, JobDetailVO> jobById = myJobs.stream()
                .collect(Collectors.toMap(JobDetailVO::getId, Function.identity()));

        // 2. 这些职位上的 APPLY 行为
        List<StudentBehavior> applies = behaviorService.listByJobIdsAndAction(jobById.keySet(), "APPLY");
        if (applies.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 3. 一次性取回投递人画像，避免逐条查库
        Set<Long> applicantIds = applies.stream()
                .map(StudentBehavior::getUserId).collect(Collectors.toSet());
        Map<Long, SysStudentProfile> profileByUser = profileService.listAll().stream()
                .filter(p -> applicantIds.contains(p.getUserId()))
                .collect(Collectors.toMap(SysStudentProfile::getUserId, Function.identity(), (a, b) -> a));

        List<ApplicationVO> list = applies.stream()
                .map(b -> ApplicationVO.of(b, jobById.get(b.getJobId()), profileByUser.get(b.getUserId())))
                .collect(Collectors.toList());
        return Result.ok(list);
    }
}
