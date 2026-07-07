package com.occupation.recommend.scheduler;

import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.entity.SysTenant;
import com.occupation.common.mapper.SysTenantMapper;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.JobMatchService;
import com.occupation.recommend.service.PushService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.MatchJobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 推荐定时调度器 — 每日 8:00 为每个学生推送 Top5 匹配职位
 * <p>
 * 逐租户执行（推送/画像表均按租户隔离）；单个学生失败不影响整体。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class RecommendScheduler {

    private final SysTenantMapper sysTenantMapper;
    private final StudentProfileService profileService;
    private final JobMatchService jobMatchService;
    private final PushService pushService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void dailyRecommendPush() {
        List<SysTenant> tenants = sysTenantMapper.selectList(null);
        log.info("[定时任务] 每日推荐推送开始，租户数={}", tenants.size());
        for (SysTenant tenant : tenants) {
            try {
                TenantContextHolder.setTenantId(tenant.getId());
                pushForTenant();
            } catch (Exception e) {
                log.error("[定时任务] 租户 {} 推送失败", tenant.getId(), e);
            } finally {
                TenantContextHolder.clear();
            }
        }
    }

    private void pushForTenant() {
        List<SysStudentProfile> profiles = profileService.listAll();
        for (SysStudentProfile profile : profiles) {
            try {
                List<MatchJobVO> top5 = jobMatchService.match(profile.getUserId(), 5);
                if (top5.isEmpty()) {
                    continue;
                }
                StringBuilder content = new StringBuilder("今日为你精选 " + top5.size() + " 个高匹配职位：\n");
                for (MatchJobVO m : top5) {
                    content.append(String.format("· %s | %s | 匹配度 %d%%\n",
                            m.getJob().getTitle(), m.getJob().getCompany(), m.getScore()));
                }
                pushService.createPush(profile.getUserId(), "RECOMMEND",
                        "今日职位推荐", content.toString());
            } catch (Exception e) {
                // 画像不完整等业务异常跳过该学生
                log.warn("学生 {} 推荐推送跳过: {}", profile.getUserId(), e.getMessage());
            }
        }
        log.info("[定时任务] 本租户推送完成，学生数={}", profiles.size());
    }
}
