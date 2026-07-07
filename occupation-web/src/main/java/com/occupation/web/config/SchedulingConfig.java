package com.occupation.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务总开关
 * <p>
 * 启用后，各业务模块的 @Scheduled 任务生效：
 * <ul>
 *   <li>analysis — AnalysisScheduler：每日凌晨统计汇总 job_detail → analysis_result</li>
 *   <li>recommend — RecommendScheduler：每日 8:00 为学生推送 Top5 匹配职位</li>
 * </ul>
 * 本地调试如不想跑定时任务，设置 {@code app.scheduler.enabled: false}。
 *
 * @author occupation-team
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
