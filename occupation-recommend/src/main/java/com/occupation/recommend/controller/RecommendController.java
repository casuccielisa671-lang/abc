package com.occupation.recommend.controller;

import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.common.result.Result;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.JobMatchService;
import com.occupation.recommend.service.PushService;
import com.occupation.recommend.vo.MatchJobVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 推荐与求职行为接口（学生端）
 * <p>
 * 推荐流 / 职位详情（附带记录 VIEW）/ 收藏 / 投递。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class RecommendController {

    private final JobMatchService jobMatchService;
    private final JobDetailService jobDetailService;
    private final BehaviorService behaviorService;
    private final PushService pushService;

    /** 个性化推荐列表（按匹配分降序，含匹配理由和缺失技能提示） */
    @GetMapping("/recommend")
    public Result<List<MatchJobVO>> recommend(@RequestParam(defaultValue = "20") int topN) {
        return Result.ok(jobMatchService.match(UserContextHolder.getUserId(), topN));
    }

    /** 职位详情（自动记录 VIEW 行为，用于活跃度统计与反馈闭环） */
    @GetMapping("/job/{jobId}")
    public Result<JobDetailVO> getJobDetail(@PathVariable Long jobId) {
        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            throw new BizException("职位不存在或已下架");
        }
        behaviorService.record(UserContextHolder.getUserId(), jobId, "VIEW");
        return Result.ok(job);
    }

    /** 收藏职位 */
    @PostMapping("/job/{jobId}/favorite")
    public Result<Void> favorite(@PathVariable Long jobId) {
        behaviorService.record(UserContextHolder.getUserId(), jobId, "FAVORITE");
        return Result.ok();
    }

    /** 取消收藏 */
    @DeleteMapping("/job/{jobId}/favorite")
    public Result<Void> unfavorite(@PathVariable Long jobId) {
        behaviorService.removeFavorite(UserContextHolder.getUserId(), jobId);
        return Result.ok();
    }

    /** 收藏列表 */
    @GetMapping("/favorites")
    public Result<List<JobDetailVO>> favorites() {
        List<Long> jobIds = behaviorService.listJobIdsByAction(UserContextHolder.getUserId(), "FAVORITE");
        List<JobDetailVO> jobs = jobIds.stream()
                .map(jobDetailService::getJobById)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return Result.ok(jobs);
    }

    /** 投递职位（记录 APPLY 行为 + 生成投递成功推送） */
    @PostMapping("/job/{jobId}/apply")
    public Result<Void> apply(@PathVariable Long jobId) {
        Long userId = UserContextHolder.getUserId();
        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            throw new BizException("职位不存在或已下架");
        }
        behaviorService.record(userId, jobId, "APPLY");
        pushService.createPush(userId, "SYSTEM", "投递成功",
                String.format("你已成功投递「%s · %s」，请留意后续通知。", job.getCompany(), job.getTitle()));
        return Result.ok();
    }
}
