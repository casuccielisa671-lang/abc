package com.occupation.crawler.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.crawler.entity.CrawlerLog;
import com.occupation.crawler.entity.CrawlerTask;
import com.occupation.crawler.mapper.CrawlerLogMapper;
import com.occupation.crawler.mapper.CrawlerTaskMapper;
import com.occupation.crawler.service.CrawlerService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * XXL-Job 采集任务处理器
 * <p>
 * 定时扫描 crawler_task 表中 status=ON 且到达执行时间的任务，
 * 启动对应爬虫并记录日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerJobHandler {

    private final CrawlerTaskMapper crawlerTaskMapper;
    private final CrawlerLogMapper crawlerLogMapper;
    private final CrawlerService crawlerService;

    /**
     * 定时扫描并执行采集任务
     * <p>
     * XXL-Job Cron: 每分钟执行一次
     * 检查 crawler_task 表中 status=1 且当前不在运行中的任务
     */
    @XxlJob("crawlerScheduleJob")
    public void scheduleJob() {
        log.info("===== 采集任务定时扫描开始 =====");

        List<CrawlerTask> enabledTasks = crawlerTaskMapper.selectList(
                new LambdaQueryWrapper<CrawlerTask>().eq(CrawlerTask::getStatus, 1)
        );

        if (enabledTasks.isEmpty()) {
            log.info("无启用的采集任务，跳过");
            XxlJobHelper.handleSuccess("无启用的采集任务");
            return;
        }

        int startedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (CrawlerTask task : enabledTasks) {
            // 跳过已在运行中的任务
            if (crawlerService.isRunning(task.getId())) {
                skippedCount++;
                continue;
            }

            try {
                crawlerService.startCrawl(task);
                startedCount++;
                log.info("任务启动成功 — taskId={}, source={}", task.getId(), task.getSourceType());
            } catch (Exception e) {
                errorCount++;
                log.error("任务启动失败 — taskId={}, error={}", task.getId(), e.getMessage());

                // 记录失败日志
                CrawlerLog errorLog = new CrawlerLog();
                errorLog.setTaskId(task.getId());
                errorLog.setTenantId(task.getTenantId());
                errorLog.setStartTime(LocalDateTime.now());
                errorLog.setEndTime(LocalDateTime.now());
                errorLog.setRecordCount(0);
                errorLog.setStatus("FAILED");
                errorLog.setErrorMsg(e.getMessage());
                crawlerLogMapper.insert(errorLog);
            }
        }

        String result = String.format("扫描完成 — 启动=%d, 跳过=%d, 失败=%d", startedCount, skippedCount, errorCount);
        log.info(result);
        XxlJobHelper.handleSuccess(result);
    }

    /**
     * 手动触发单个采集任务（通过 XXL-Job 参数传入 taskId）
     */
    @XxlJob("crawlerManualJob")
    public void manualJob() {
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.trim().isEmpty()) {
            XxlJobHelper.handleFail("缺少任务参数：请传入 taskId");
            return;
        }

        try {
            Long taskId = Long.parseLong(param.trim());
            CrawlerTask task = crawlerTaskMapper.selectById(taskId);
            if (task == null) {
                XxlJobHelper.handleFail("任务不存在: " + taskId);
                return;
            }

            if (crawlerService.isRunning(taskId)) {
                XxlJobHelper.handleFail("任务已在运行中: " + taskId);
                return;
            }

            crawlerService.startCrawl(task);
            XxlJobHelper.handleSuccess("任务启动成功: " + taskId);
        } catch (NumberFormatException e) {
            XxlJobHelper.handleFail("无效的任务ID: " + param);
        } catch (Exception e) {
            log.error("手动任务执行失败", e);
            XxlJobHelper.handleFail("执行失败: " + e.getMessage());
        }
    }

    /**
     * 停止所有运行中的采集任务
     */
    @XxlJob("crawlerStopAllJob")
    public void stopAllJob() {
        List<CrawlerTask> runningTasks = crawlerService.listRunningTasks();
        int stopped = 0;

        for (CrawlerTask task : runningTasks) {
            try {
                crawlerService.stopCrawl(task.getId());
                stopped++;
            } catch (Exception e) {
                log.error("停止任务失败 — taskId={}", task.getId(), e);
            }
        }

        XxlJobHelper.handleSuccess("已停止 " + stopped + " 个任务");
    }

}
