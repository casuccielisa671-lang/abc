package com.occupation.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.dto.JobSaveDTO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.service.UserService;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.recommend.dto.ApplicationStatusDTO;
import com.occupation.recommend.entity.ApplicationStatus;
import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.JobApplicationService;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.ApplicantDetailVO;
import com.occupation.recommend.vo.ApplicationVO;
import com.occupation.recommend.vo.ResumeVO;
import com.occupation.recommend.vo.TalentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrController {

    private final JobDetailService jobDetailService;
    private final StudentProfileService profileService;
    private final BehaviorService behaviorService;
    private final JobApplicationService applicationService;
    private final ResumeService resumeService;
    private final UserService userService;

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
     * 学生主动投递即视为授权本 HR 查看其身份，故返回 userId 与姓名；
     * 联系方式与简历正文不在列表里，需再调 {@link #applicantDetail(Long)} 单独拉取。
     * 租户隔离由 student_behavior 上的多租户插件保证。
     */
    @GetMapping("/applications")
    public Result<List<ApplicationVO>> applications() {
        Long hrId = UserContextHolder.getUserId();
        // 读业务实体而不是行为埋点：只有它带状态与备注。
        // publisher_id 在投递时已固化，所以不必先查「我发布了哪些职位」再 IN 一大串 jobId。
        List<JobApplication> apps = applicationService.listByPublisher(hrId);
        if (apps.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        Map<Long, JobDetailVO> jobById = jobDetailService.listByIds(
                        apps.stream().map(JobApplication::getJobId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(JobDetailVO::getId, Function.identity()));

        // 一次性取回投递人的画像 / 姓名 / 简历有无，避免逐条查库
        Set<Long> applicantIds = apps.stream()
                .map(JobApplication::getUserId).collect(Collectors.toSet());
        Map<Long, SysStudentProfile> profileByUser = profileService.listAll().stream()
                .filter(p -> applicantIds.contains(p.getUserId()))
                .collect(Collectors.toMap(SysStudentProfile::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> userById = userService.mapByIds(applicantIds);
        Set<Long> withResume = resumeService.filterUsersWithResume(applicantIds);

        List<ApplicationVO> list = apps.stream()
                .map(a -> {
                    SysUser u = userById.get(a.getUserId());
                    return ApplicationVO.of(a, jobById.get(a.getJobId()), profileByUser.get(a.getUserId()),
                            u == null ? null : u.getRealName(),
                            withResume.contains(a.getUserId()));
                })
                .collect(Collectors.toList());
        return Result.ok(list);
    }

    /**
     * 变更投递状态（已查看 / 邀请面试 / 已录用 / 不合适）
     * <p>
     * 归属校验与状态流转合法性都在 Service 里做：不能改别人职位上的投递，
     * 不能从终态回退。
     */
    @PutMapping("/applications/{id}/status")
    public Result<Void> changeApplicationStatus(@PathVariable Long id,
                                                @RequestBody @Validated ApplicationStatusDTO dto) {
        applicationService.changeStatus(id, UserContextHolder.getUserId(), dto);
        return Result.ok();
    }

    /**
     * 投递人详情 —— 姓名、联系方式、画像、简历全文
     * <p>
     * <b>归属校验是这个接口的全部安全性所在</b>：只有当该学生投递过<b>本 HR 发布的</b>职位时才放行。
     * 少了这一步，任何 HR 只要把 userId 从 1 枚举到 N，就能拖走全校学生的手机号和邮箱。
     */
    @GetMapping("/applicants/{userId}")
    public Result<ApplicantDetailVO> applicantDetail(@PathVariable Long userId) {
        Long hrId = UserContextHolder.getUserId();

        // 该学生投到「我的职位」上的投递记录；一条都没有 → 无权查看
        List<JobApplication> hisApplies = applicationService
                .groupByApplicant(hrId, Collections.singletonList(userId))
                .getOrDefault(userId, Collections.emptyList());
        if (hisApplies.isEmpty()) {
            log.warn("拒绝越权查看投递人: hrId={}, targetUserId={}", hrId, userId);
            throw new BizException(403, "无权查看该学生：他并未投递你发布的职位");
        }

        // HR 打开了简历 —— 把还停在「已投递」的记录推进到「已查看」，学生端能看到进度动了。
        // 已经进入面试/终态的不会被拉回。
        applicationService.markViewed(hrId, userId);

        Map<Long, JobDetailVO> jobById = jobDetailService.listByIds(
                        hisApplies.stream().map(JobApplication::getJobId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(JobDetailVO::getId, Function.identity()));

        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BizException("该学生账号不存在");
        }

        ApplicantDetailVO vo = new ApplicantDetailVO();
        vo.setUserId(userId);
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmployedElsewhere(applicationService.isEmployed(userId));

        // 简历里填了求职专用联系方式就用它，否则回落到账号信息
        ResumeVO resume = resumeService.getByUserId(userId);
        vo.setResume(resume);
        vo.setPhone(firstNonBlank(resume.getContactPhone(), user.getPhone()));
        vo.setEmail(firstNonBlank(resume.getContactEmail(), user.getEmail()));

        SysStudentProfile profile = profileService.getByUserId(userId);
        vo.setProfileCompleted(profile != null);
        if (profile != null) {
            vo.setMajor(profile.getMajor());
            vo.setSkills(profile.getSkills());
            vo.setEducationLevel(profile.getEducationLevel());
            vo.setExpectedCity(profile.getExpectedCity());
            vo.setExpectedIndustry(profile.getExpectedIndustry());
            vo.setExpectedSalaryMin(profile.getExpectedSalaryMin());
            vo.setExpectedSalaryMax(profile.getExpectedSalaryMax());
        }

        Map<String, Long> counts = behaviorService.countByAction(userId);
        vo.setViewCount(counts.getOrDefault(BehaviorAction.VIEW, 0L));
        vo.setFavoriteCount(counts.getOrDefault(BehaviorAction.FAVORITE, 0L));
        vo.setApplyCount(counts.getOrDefault(BehaviorAction.APPLY, 0L));

        // 只列出投到「我的职位」上的记录，不暴露他投过别家公司
        vo.setAppliedJobs(hisApplies.stream().map(a -> {
            ApplicantDetailVO.AppliedJob aj = new ApplicantDetailVO.AppliedJob();
            JobDetailVO job = jobById.get(a.getJobId());
            aj.setApplicationId(a.getId());
            aj.setJobId(a.getJobId());
            aj.setApplyTime(a.getAppliedAt());
            // markViewed 已经把 SUBMITTED 推进过了，这里重新读一次状态才不会显示旧值
            ApplicationStatus st = ApplicationStatus.valueOf(a.getStatus());
            if (st == ApplicationStatus.SUBMITTED) {
                st = ApplicationStatus.VIEWED;
            }
            aj.setStatus(st.name());
            aj.setStatusLabel(st.getLabel());
            aj.setTerminal(st.isTerminal());
            aj.setHrNote(a.getHrNote());
            aj.setInterviewTime(a.getInterviewTime());
            aj.setInterviewPlace(a.getInterviewPlace());
            aj.setInterviewContact(a.getInterviewContact());
            aj.setInterviewContent(a.getInterviewContent());
            if (job != null) {
                aj.setJobTitle(job.getTitle());
                aj.setJobCity(job.getCity());
            }
            return aj;
        }).collect(Collectors.toList()));

        return Result.ok(vo);
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.trim().isEmpty() ? a : b;
    }
}
