package com.occupation.analysis.scheduler;

import com.occupation.analysis.service.AnalysisJobService;
import com.occupation.analysis.service.DataCleanService;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.entity.SysTenant;
import com.occupation.common.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分析定时调度器（Spring @Scheduled 方案，零部署成本）
 * <p>
 * 任务清单：
 * <ul>
 *   <li>每小时第 10 分：存量清洗补偿（raw_job_data 中 status=RAW 的重放清洗）</li>
 *   <li>每日 02:00：全量统计重算（遍历所有租户，逐租户写 analysis_result）</li>
 * </ul>
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AnalysisScheduler {

    private final AnalysisJobService analysisJobService;
    private final DataCleanService dataCleanService;
    private final SysTenantMapper sysTenantMapper;

    /**
     * 存量清洗补偿 — 每小时第 10 分执行
     */
    @Scheduled(cron = "0 10 * * * ?")
    public void cleanPendingData() {
        log.info("[定时任务] 存量清洗补偿开始");
        int count = dataCleanService.cleanPendingRawData();
        log.info("[定时任务] 存量清洗补偿结束，入库 {} 条", count);
    }

    /**
     * 全量统计重算 — 每日凌晨 2:00 执行
     * <p>
     * job_detail 为全平台共享数据，但 analysis_result 按租户隔离，
     * 因此遍历所有启用租户，逐一设置租户上下文后重算。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void rebuildAnalysis() {
        List<SysTenant> tenants = sysTenantMapper.selectList(null);
        log.info("[定时任务] 全量统计重算开始，租户数={}", tenants.size());
        for (SysTenant tenant : tenants) {
            try {
                TenantContextHolder.setTenantId(tenant.getId());
                int rows = analysisJobService.runAll();
                log.info("[定时任务] 租户 {}({}) 统计完成，写入 {} 条", tenant.getId(), tenant.getName(), rows);
            } catch (Exception e) {
                // 单租户失败不影响其他租户
                log.error("[定时任务] 租户 {} 统计失败", tenant.getId(), e);
            } finally {
                TenantContextHolder.clear();
            }
        }
    }
}
