package com.occupation.crawler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.crawler.entity.CrawlerLog;
import com.occupation.crawler.entity.CrawlerTask;
import com.occupation.crawler.mapper.CrawlerLogMapper;
import com.occupation.crawler.mapper.CrawlerTaskMapper;
import com.occupation.crawler.processor.BossJobPageProcessor;
import com.occupation.crawler.processor.MockJobPageProcessor;
import com.occupation.crawler.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 爬虫调度服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {

    private final CrawlerTaskMapper crawlerTaskMapper;
    private final CrawlerLogMapper crawlerLogMapper;
    private final com.occupation.crawler.processor.JobPipeline jobPipeline;

    /** 运行中的爬虫实例（taskId → Spider） */
    private final Map<Long, Spider> runningSpiders = new ConcurrentHashMap<>();

    /** 运行中采集任务的日志 ID */
    private final Map<Long, Long> runningLogIds = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrawlerLog startCrawl(CrawlerTask task) {
        // 1. 停止旧实例（如果存在）
        stopCrawl(task.getId());

        // 2. 创建采集日志（RUNNING）
        CrawlerLog crawlerLog = createLog(task);

        // 3. 根据 source_type 选择处理器
        PageProcessor processor = createProcessor(task);

        // 4. 启动爬虫
        if (processor instanceof MockJobPageProcessor) {
            // Mock 爬虫不通过 Spider 框架，直接调用 process 方法处理数据
            MockJobPageProcessor mockProcessor = (MockJobPageProcessor) processor;
            mockProcessor.processAll(jobPipeline);
            // 更新日志状态
            crawlerLog.setEndTime(LocalDateTime.now());
            crawlerLog.setRecordCount(mockProcessor.getCollectedCount());
            crawlerLog.setStatus("SUCCESS");
            crawlerLogMapper.updateById(crawlerLog);
            // 更新任务状态为停止
            task.setStatus(0);
            crawlerTaskMapper.updateById(task);
            log.info("Mock 采集完成 — taskId={}, 共 {} 条", task.getId(), mockProcessor.getCollectedCount());
        } else {
            Spider spider = Spider.create(processor)
                    .addPipeline(jobPipeline)
                    .addPipeline(new ConsolePipeline())
                    .thread(3);

            if (processor instanceof BossJobPageProcessor) {
                Map<String, String> params = parseUrlParams(task.getUrlPattern());
                String keyword = params.getOrDefault("query", "Java");
                String city = params.getOrDefault("city", "101010100");
                spider.addRequest(BossJobPageProcessor.seedRequest(keyword, city));
            } else {
                spider.addUrl(task.getUrlPattern());
            }

            spider.start();

            // 记录运行状态
            runningSpiders.put(task.getId(), spider);
            runningLogIds.put(task.getId(), crawlerLog.getId());

            // 更新任务状态为运行中
            task.setStatus(1);
            crawlerTaskMapper.updateById(task);

            log.info("爬虫启动成功 — taskId={}, source={}", task.getId(), task.getSourceType());
        }

        return crawlerLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopCrawl(Long taskId) {
        Spider spider = runningSpiders.remove(taskId);
        Long logId = runningLogIds.remove(taskId);

        if (spider != null) {
            spider.stop();
            log.info("爬虫已停止 — taskId={}", taskId);
        }

        // 更新日志状态
        if (logId != null) {
            CrawlerLog crawlerLog = new CrawlerLog();
            crawlerLog.setId(logId);
            crawlerLog.setEndTime(LocalDateTime.now());
            crawlerLog.setStatus("SUCCESS");
            crawlerLogMapper.updateById(crawlerLog);
        }

        // 更新任务状态为停止
        CrawlerTask task = new CrawlerTask();
        task.setId(taskId);
        task.setStatus(0);
        crawlerTaskMapper.updateById(task);

        return true;
    }

    @Override
    public boolean isRunning(Long taskId) {
        Spider spider = runningSpiders.get(taskId);
        return spider != null && spider.getStatus() == Spider.Status.Running;
    }

    @Override
    public CrawlerLog startMockCrawl(CrawlerTask task) {
        // 强制使用模拟处理器
        task.setSourceType("MOCK");
        task.setUrlPattern("mock-jobs.json");
        return startCrawl(task);
    }

    @Override
    public List<CrawlerTask> listRunningTasks() {
        return crawlerTaskMapper.selectList(
                new LambdaQueryWrapper<CrawlerTask>()
                        .eq(CrawlerTask::getStatus, 1)
        );
    }

    @Override
    public List<CrawlerLog> getLogsByTaskId(Long taskId, int page, int size) {
        Page<CrawlerLog> pageParam = new Page<>(page, size);
        Page<CrawlerLog> result = crawlerLogMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CrawlerLog>()
                        .eq(CrawlerLog::getTaskId, taskId)
                        .orderByDesc(CrawlerLog::getCreateTime)
        );
        return result.getRecords();
    }

    // ---- 内部方法 ----

    private CrawlerLog createLog(CrawlerTask task) {
        CrawlerLog logEntity = new CrawlerLog();
        logEntity.setTaskId(task.getId());
        logEntity.setTenantId(TenantContextHolder.getTenantId());
        logEntity.setStartTime(LocalDateTime.now());
        logEntity.setRecordCount(0);
        logEntity.setStatus("RUNNING");
        crawlerLogMapper.insert(logEntity);
        return logEntity;
    }

    private PageProcessor createProcessor(CrawlerTask task) {
        switch (task.getSourceType()) {
            case "MOCK":
                // 模拟爬虫：从 classpath 加载 mock 数据
                // 使用 InputStream 方式读取，兼容 JAR 包内运行
                String resourcePath = "mock/" + task.getUrlPattern();
                return new MockJobPageProcessor(resourcePath, true);
            case "BOSS_ZHIPIN":
                // 真实采集：从 urlPattern 解析 keyword 和 city
                Map<String, String> params = parseUrlParams(task.getUrlPattern());
                String keyword = params.getOrDefault("query", "Java");
                String cityCode = params.getOrDefault("city", "101010100");
                int maxPages = Integer.parseInt(params.getOrDefault("maxPages", "3"));
                return new BossJobPageProcessor(keyword, cityCode, maxPages);
            case "ZHAOPIN":
                throw new UnsupportedOperationException("智联招聘采集器将在后续版本实现");
            default:
                throw new IllegalArgumentException("不支持的采集源类型: " + task.getSourceType());
        }
    }

    /**
     * 解析 URL 参数字符串为 Map
     * 格式: "key1=value1&key2=value2"
     */
    private Map<String, String> parseUrlParams(String urlPattern) {
        Map<String, String> params = new LinkedHashMap<>();
        if (urlPattern == null || urlPattern.isEmpty()) {
            return params;
        }
        String[] pairs = urlPattern.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0].trim(), kv[1].trim());
            }
        }
        return params;
    }

}
