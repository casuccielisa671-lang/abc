package com.occupation.recommend.controller;

import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.common.result.Result;
import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.HybridRecommendService;
import com.occupation.recommend.service.JobApplicationService;
import com.occupation.recommend.service.JobMatchService;
import com.occupation.recommend.service.PushService;
import com.occupation.recommend.vo.MatchJobVO;
import com.occupation.recommend.vo.MyApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final HybridRecommendService hybridRecommendService;
    private final JobMatchService jobMatchService;
    private final JobDetailService jobDetailService;
    private final BehaviorService behaviorService;
    private final JobApplicationService applicationService;
    private final PushService pushService;

    /** 个性化推荐列表（混合推荐：规则打分 + 语义匹配 + 协同过滤 + 内容推荐） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/recommend")
    public Result<List<MatchJobVO>> recommend(@RequestParam(defaultValue = "20") int topN) {
        return Result.ok(hybridRecommendService.recommend(UserContextHolder.getUserId(), topN));
    }

    /** 职位详情（自动记录 VIEW 行为，用于活跃度统计与反馈闭环） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/job/{jobId}")
    public Result<JobDetailVO> getJobDetail(@PathVariable Long jobId) {
        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            throw new BizException("职位不存在或已下架");
        }
        behaviorService.record(UserContextHolder.getUserId(), jobId, BehaviorAction.VIEW);
        return Result.ok(job);
    }

    /** 收藏职位 */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/job/{jobId}/favorite")
    public Result<Void> favorite(@PathVariable Long jobId) {
        behaviorService.record(UserContextHolder.getUserId(), jobId, BehaviorAction.FAVORITE);
        return Result.ok();
    }

    /** 取消收藏 */
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/job/{jobId}/favorite")
    public Result<Void> unfavorite(@PathVariable Long jobId) {
        behaviorService.removeFavorite(UserContextHolder.getUserId(), jobId);
        return Result.ok();
    }

    /** 收藏列表（最近收藏在前） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/favorites")
    public Result<List<JobDetailVO>> favorites() {
        List<Long> jobIds = behaviorService.listJobIdsByAction(UserContextHolder.getUserId(), BehaviorAction.FAVORITE);
        return Result.ok(jobDetailService.listByIds(jobIds));
    }

    /**
     * 投递职位（记录 APPLY 行为 + 生成投递成功推送）
     * <p>
     * <b>只有 HR 在站内发布的职位可以投递。</b>采集来的职位（Mock / 智联）在平台上没有主人：
     * 真正的招聘方不知道这个平台存在，投递记录不会被任何人看到。放行只会制造「幽灵投递」——
     * 学生以为投出去了，HR 端一条也看不到。前端已按 {@code applicable} 隐藏了投递按钮，
     * 这里是服务端的兜底。
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/job/{jobId}/apply")
    public Result<Void> apply(@PathVariable Long jobId) {
        Long userId = UserContextHolder.getUserId();
        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            throw new BizException("职位不存在或已下架");
        }
        if (!job.isApplicable()) {
            throw new BizException("该职位来自外部招聘渠道，暂不支持站内投递；"
                    + "你可以收藏它，或前往原招聘平台投递");
        }
        // 双写：behavior 是行为埋点（推荐算法与各类统计读它），application 是业务实体（HR 处理它）。
        // 两者都幂等，重复投递不会产生第二条记录。
        behaviorService.record(userId, jobId, BehaviorAction.APPLY);
        applicationService.apply(userId, jobId, job.getPublisherId());

        pushService.createPush(userId, "SYSTEM", "投递成功",
                String.format("你已成功投递「%s · %s」，请留意后续通知。", job.getCompany(), job.getTitle()));
        return Result.ok();
    }

    /**
     * 自主联系 —— 对采集来的「市场参考」职位表达求职意向
     * <p>
     * 守卫与投递正好相反：<b>只有无主职位可以自主联系</b>，站内职位请走投递。
     * 平台只记录意向本身 —— 学生跳出去之后发生了什么，我们无从得知，
     * 所以这里没有状态机，只有一条行为埋点。
     * <p>
     * 返回原岗位链接（若有），前端据此决定要不要打开新窗口。
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/job/{jobId}/contact")
    public Result<Map<String, String>> contact(@PathVariable Long jobId) {
        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            throw new BizException("职位不存在或已下架");
        }
        if (job.isApplicable()) {
            throw new BizException("该职位由企业在本平台发布，请直接投递简历");
        }
        behaviorService.record(UserContextHolder.getUserId(), jobId, BehaviorAction.CONTACT);

        // Mock/种子数据的 sourceUrl 是不存在的域名，点了必然 404，不返回给前端
        String url = job.getSourceUrl();
        boolean real = url != null && url.startsWith("http") && !"MOCK".equals(job.getSource());
        Map<String, String> data = new HashMap<>(2);
        data.put("sourceUrl", real ? url : null);
        data.put("company", job.getCompany());
        return Result.ok(data);
    }

    /** 我的投递（含 HR 处理进度） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/applications")
    public Result<List<MyApplicationVO>> myApplications() {
        List<JobApplication> apps = applicationService.listByUser(UserContextHolder.getUserId());
        if (apps.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 一次批量取回涉及的职位，避免逐条查库
        Map<Long, JobDetailVO> jobs = jobDetailService.listByIds(
                        apps.stream().map(JobApplication::getJobId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(JobDetailVO::getId, j -> j));

        return Result.ok(apps.stream()
                .map(a -> MyApplicationVO.of(a, jobs.get(a.getJobId())))
                .collect(Collectors.toList()));
    }
}
