package com.occupation.crawler.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.dto.JobDataMessage;
import com.occupation.common.exception.BizException;
import com.occupation.crawler.entity.CrawlerLog;
import com.occupation.crawler.entity.CrawlerTask;
import com.occupation.crawler.mapper.CrawlerLogMapper;
import com.occupation.crawler.mapper.CrawlerTaskMapper;
import com.occupation.crawler.processor.InfoQNewsProcessor;
import com.occupation.crawler.processor.JobPipeline;
import com.occupation.crawler.processor.MockJobPageProcessor;
import com.occupation.crawler.processor.NewsPageProcessor;
import com.occupation.crawler.processor.OfficialPublicJobProcessor;
import com.occupation.crawler.processor.OfficialReportProcessor;
import com.occupation.crawler.processor.OsChinaNewsProcessor;
import com.occupation.crawler.processor.RobotsRules;
import com.occupation.crawler.processor.ZhaopinJobPageProcessor;
import com.occupation.crawler.service.CrawlerService;
import com.occupation.recommend.entity.News;
import com.occupation.recommend.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 采集任务调度服务。
 *
 * <p>岗位类来源写入 Kafka -> raw_job_data -> job_detail；报告/资讯类来源写入 news，
 * 避免把公开报告误当成岗位样本。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {

    private static final String DEFAULT_MOCK_FILE = "mock-jobs.json";

    private final CrawlerTaskMapper crawlerTaskMapper;
    private final CrawlerLogMapper crawlerLogMapper;
    private final JobPipeline jobPipeline;
    private final NewsMapper newsMapper;

    private final Map<Long, Spider> runningSpiders = new ConcurrentHashMap<>();
    private final Map<Long, Long> runningLogIds = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrawlerLog startCrawl(CrawlerTask task) {
        stopCrawl(task.getId());

        CrawlerLog crawlerLog = createLog(task);
        PageProcessor processor = createProcessor(task);

        if (processor instanceof MockJobPageProcessor) {
            MockJobPageProcessor mockProcessor = (MockJobPageProcessor) processor;
            mockProcessor.processAll(jobPipeline);
            crawlerLog.setEndTime(LocalDateTime.now());
            crawlerLog.setRecordCount(mockProcessor.getCollectedCount());
            crawlerLog.setStatus("SUCCESS");
            crawlerLogMapper.updateById(crawlerLog);

            task.setStatus(0);
            crawlerTaskMapper.updateById(task);
            log.info("Mock 采集完成 taskId={}, count={}", task.getId(), mockProcessor.getCollectedCount());
            return crawlerLog;
        }

        AtomicInteger collectedCount = new AtomicInteger(0);
        Spider spider = Spider.create(processor)
                .addPipeline(countingPipeline(collectedCount))
                .addPipeline(new ConsolePipeline())
                .thread(1);

        Request seed = resolveSeedRequest(task, processor, crawlerLog);
        if (seed == null) {
            task.setStatus(0);
            crawlerTaskMapper.updateById(task);
            return crawlerLog;
        }
        spider.addRequest(seed);

        runningSpiders.put(task.getId(), spider);
        runningLogIds.put(task.getId(), crawlerLog.getId());

        task.setStatus(1);
        crawlerTaskMapper.updateById(task);

        Long tenantId = task.getTenantId() != null ? task.getTenantId() : TenantContextHolder.getTenantId();
        startSpiderAfterCommit(task.getId(), crawlerLog.getId(), spider, collectedCount, processor, tenantId);
        log.info("采集任务已启动 taskId={}, source={}", task.getId(), task.getSourceType());
        return crawlerLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopCrawl(Long taskId) {
        Spider spider = runningSpiders.remove(taskId);
        Long logId = runningLogIds.remove(taskId);
        if (spider != null) {
            spider.stop();
            log.info("采集任务已停止 taskId={}", taskId);
        }

        if (logId != null) {
            CrawlerLog crawlerLog = new CrawlerLog();
            crawlerLog.setId(logId);
            crawlerLog.setEndTime(LocalDateTime.now());
            crawlerLog.setStatus("SUCCESS");
            crawlerLogMapper.updateById(crawlerLog);
        }

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
    public List<CrawlerTask> listRunningTasks() {
        return crawlerTaskMapper.selectList(
                new LambdaQueryWrapper<CrawlerTask>().eq(CrawlerTask::getStatus, 1));
    }

    @Override
    public List<CrawlerLog> getLogsByTaskId(Long taskId, int page, int size) {
        Page<CrawlerLog> pageParam = new Page<>(page, size);
        Page<CrawlerLog> result = crawlerLogMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CrawlerLog>()
                        .eq(CrawlerLog::getTaskId, taskId)
                        .orderByDesc(CrawlerLog::getCreateTime));
        return result.getRecords();
    }

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

    private Request resolveSeedRequest(CrawlerTask task, PageProcessor processor, CrawlerLog crawlerLog) {
        if (processor instanceof ZhaopinJobPageProcessor) {
            Map<String, String> params = parseUrlParams(task.getUrlPattern());
            String keyword = params.getOrDefault("kw", params.getOrDefault("query", "Java"));
            String city = params.getOrDefault("jl", params.getOrDefault("city", "530"));
            Request seed = ZhaopinJobPageProcessor.seedRequest(keyword, city);
            if (seed == null) {
                failLog(crawlerLog, "目标站点 robots.txt 不允许抓取该地址，已放弃本次采集");
            }
            return seed;
        }

        if (processor instanceof OfficialPublicJobProcessor) {
            Map<String, String> params = parseUrlParams(task.getUrlPattern());
            return checkedRequest(params.getOrDefault("url", task.getUrlPattern()), crawlerLog);
        }

        if (processor instanceof NewsPageProcessor) {
            return checkedRequest(((NewsPageProcessor) processor).getRssUrl(), crawlerLog);
        }

        return checkedRequest(task.getUrlPattern(), crawlerLog);
    }

    private Request checkedRequest(String url, CrawlerLog crawlerLog) {
        if (url == null || url.trim().isEmpty()) {
            failLog(crawlerLog, "采集任务没有配置 URL");
            return null;
        }
        if (!RobotsRules.isAllowed(url)) {
            failLog(crawlerLog, "目标站点 robots.txt 不允许抓取 " + url);
            return null;
        }
        return new Request(url);
    }

    private void failLog(CrawlerLog crawlerLog, String reason) {
        crawlerLog.setEndTime(LocalDateTime.now());
        crawlerLog.setStatus("FAILED");
        crawlerLog.setErrorMsg(reason);
        crawlerLogMapper.updateById(crawlerLog);
        log.warn("采集失败 taskId={}, reason={}", crawlerLog.getTaskId(), reason);
    }

    private Pipeline countingPipeline(AtomicInteger collectedCount) {
        return (ResultItems resultItems, us.codecraft.webmagic.Task task) -> {
            @SuppressWarnings("unchecked")
            List<JobDataMessage> jobs = resultItems.get("jobs");
            if (jobs != null) {
                collectedCount.addAndGet(jobs.size());
            }
            jobPipeline.process(resultItems, task);
        };
    }

    private void watchSpiderCompletion(Long taskId, Long logId, Spider spider, AtomicInteger collectedCount,
                                       PageProcessor processor, Long tenantId) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean started = false;
                while (true) {
                    Spider.Status status = spider.getStatus();
                    if (status == Spider.Status.Running) {
                        started = true;
                    } else if (started || status == Spider.Status.Stopped) {
                        break;
                    }
                    Thread.sleep(1000L);
                }
                CrawlerLog current = crawlerLogMapper.selectById(logId);
                if (current == null || !"RUNNING".equals(current.getStatus())) {
                    return;
                }

                collectedCount.addAndGet(persistCollectedNews(processor, tenantId));
                CrawlerLog finished = new CrawlerLog();
                finished.setId(logId);
                finished.setEndTime(LocalDateTime.now());
                finished.setRecordCount(collectedCount.get());
                finished.setStatus("SUCCESS");
                crawlerLogMapper.updateById(finished);

                CrawlerTask task = new CrawlerTask();
                task.setId(taskId);
                task.setStatus(0);
                crawlerTaskMapper.updateById(task);
                runningSpiders.remove(taskId);
                runningLogIds.remove(taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("采集完成状态回写失败 taskId={}, logId={}", taskId, logId, e);
            }
        });
    }

    private int persistCollectedNews(PageProcessor processor, Long tenantId) {
        if (!(processor instanceof NewsPageProcessor)) {
            return 0;
        }
        NewsPageProcessor newsProcessor = (NewsPageProcessor) processor;
        List<News> newsList = newsProcessor.drainCollectedNews();
        int inserted = 0;
        for (News news : newsList) {
            Long exists = newsMapper.selectCount(
                    new LambdaQueryWrapper<News>().eq(News::getTitle, news.getTitle()));
            if (exists != null && exists > 0) {
                continue;
            }
            if (tenantId != null) {
                news.setTenantId(tenantId);
            }
            try {
                newsMapper.insert(news);
                inserted++;
            } catch (Exception e) {
                log.warn("资讯入库失败，已跳过。title={}, source={}", news.getTitle(), news.getSource(), e);
            }
        }
        log.info("资讯采集入库完成 source={}, inserted={}", newsProcessor.getSourceName(), inserted);
        return inserted;
    }

    private void startSpiderAfterCommit(Long taskId, Long logId, Spider spider, AtomicInteger collectedCount,
                                        PageProcessor processor, Long tenantId) {
        Runnable starter = () -> {
            spider.runAsync();
            watchSpiderCompletion(taskId, logId, spider, collectedCount, processor, tenantId);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    starter.run();
                }
            });
        } else {
            starter.run();
        }
    }

    private PageProcessor createProcessor(CrawlerTask task) {
        switch (task.getSourceType()) {
            case "MOCK": {
                String dataFile = StrUtil.blankToDefault(task.getUrlPattern(), DEFAULT_MOCK_FILE);
                return new MockJobPageProcessor("mock/" + dataFile, true);
            }
            case "BOSS_ZHIPIN":
                throw new BizException("Boss 直聘 robots.txt 明确禁止抓取职位列表页，请改用 MOCK 或 OFFICIAL_PUBLIC。行业资讯请在资讯管理中拉取。");
            case "ZHAOPIN": {
                Map<String, String> params = parseUrlParams(task.getUrlPattern());
                String keyword = params.getOrDefault("kw", params.getOrDefault("query", "Java"));
                String cityCode = params.getOrDefault("jl", params.getOrDefault("city", "530"));
                int maxPages = Integer.parseInt(params.getOrDefault("maxPages", "3"));
                return new ZhaopinJobPageProcessor(keyword, cityCode, maxPages);
            }
            case "NEWS_INFOQ":
                return new InfoQNewsProcessor();
            case "OFFICIAL_PUBLIC": {
                Map<String, String> params = parseUrlParams(task.getUrlPattern());
                String url = params.getOrDefault("url", task.getUrlPattern());
                int maxItems = Integer.parseInt(params.getOrDefault("maxItems", "30"));
                return new OfficialPublicJobProcessor(url, task.getSourceName(), maxItems);
            }
            case "OFFICIAL_REPORT": {
                Map<String, String> params = parseUrlParams(task.getUrlPattern());
                String url = params.getOrDefault("url", task.getUrlPattern());
                int maxItems = Integer.parseInt(params.getOrDefault("maxItems", "20"));
                return new OfficialReportProcessor(url, task.getSourceName(), maxItems);
            }
            case "NEWS_OSCHINA":
                return new OsChinaNewsProcessor();
            default:
                throw new BizException("暂不支持的采集源类型：" + task.getSourceType()
                        + "。当前可用：MOCK / OFFICIAL_PUBLIC / ZHAOPIN");
        }
    }

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
