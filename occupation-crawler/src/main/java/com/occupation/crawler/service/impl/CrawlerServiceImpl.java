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
import com.occupation.crawler.processor.OfficialPublicDirectCollector;
import com.occupation.crawler.processor.OfficialPublicJobProcessor;
import com.occupation.crawler.processor.OfficialReportProcessor;
import com.occupation.crawler.processor.OsChinaNewsProcessor;
import com.occupation.crawler.processor.RobotsRules;
import com.occupation.crawler.processor.StackOverflowSurveyProcessor;
import com.occupation.crawler.processor.ZhaopinJobPageProcessor;
import com.occupation.analysis.service.DataCleanService;
import com.occupation.common.entity.RawJobData;
import com.occupation.common.mapper.RawJobDataMapper;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private static final int OFFICIAL_DIRECT_TIMEOUT_SECONDS = 120;

    private final CrawlerTaskMapper crawlerTaskMapper;
    private final CrawlerLogMapper crawlerLogMapper;
    private final JobPipeline jobPipeline;
    private final NewsMapper newsMapper;
    private final RawJobDataMapper rawJobDataMapper;
    private final DataCleanService dataCleanService;

    private final Map<Long, Spider> runningSpiders = new ConcurrentHashMap<>();
    private final Map<Long, Long> runningLogIds = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrawlerLog startCrawl(CrawlerTask task) {
        stopCrawl(task.getId());

        CrawlerLog crawlerLog = createLog(task);
        if ("OFFICIAL_PUBLIC".equals(task.getSourceType())) {
            runOfficialPublicDirect(task, crawlerLog);
            return crawlerLogMapper.selectById(crawlerLog.getId());
        }

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
            crawlerLog.setErrorMsg("任务已手动停止");
            crawlerLogMapper.updateById(crawlerLog);
        } else {
            CrawlerLog staleLog = new CrawlerLog();
            staleLog.setEndTime(LocalDateTime.now());
            staleLog.setStatus("FAILED");
            staleLog.setErrorMsg("页面显示任务运行中，但服务内存中没有对应采集实例，已自动修复为停止状态");
            crawlerLogMapper.update(staleLog, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrawlerLog>()
                    .eq(CrawlerLog::getTaskId, taskId)
                    .eq(CrawlerLog::getStatus, "RUNNING")
                    .isNull(CrawlerLog::getEndTime));
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

    private void runOfficialPublicDirect(CrawlerTask task, CrawlerLog crawlerLog) {
        CrawlerTask runningTask = new CrawlerTask();
        runningTask.setId(task.getId());
        runningTask.setStatus(1);
        crawlerTaskMapper.updateById(runningTask);

        try {
            Map<String, String> params = parseUrlParams(task.getUrlPattern());
            String url = params.getOrDefault("url", task.getUrlPattern());
            int maxItems = Integer.parseInt(params.getOrDefault("maxItems", "30"));
            int currentPage = extractPageNo(url);
            log.info("官方公开岗位直采开始 taskId={}, url={}, currentPage={}, maxItems={}",
                    task.getId(), url, currentPage, maxItems);

            // 每次只采集当前页，采集完后 pageNo 自增 1，下次采集自动从下一页开始
            OfficialPublicDirectCollector collector = new OfficialPublicDirectCollector(task.getSourceName(), maxItems);
            List<JobDataMessage> jobs = collectOfficialJobsWithTimeout(collector, url);

            int cleaned = 0;
            for (JobDataMessage job : jobs) {
                saveRawJob(job);
                if (dataCleanService.cleanAndSave(job.getRawContent(), job.getSource(), job.getSourceUrl())) {
                    cleaned++;
                }
            }

            // 采集完成后 pageNo 自增 1，下次触发采集自动从下一页开始
            String currentUrlPattern = task.getUrlPattern();
            log.info("翻页前 urlPattern={}", currentUrlPattern);
            String nextUrl = advancePageNo(currentUrlPattern);
            log.info("翻页后 nextUrl={}", nextUrl);

            CrawlerLog finished = new CrawlerLog();
            finished.setId(crawlerLog.getId());
            finished.setEndTime(LocalDateTime.now());
            finished.setRecordCount(jobs.size());
            if (jobs.isEmpty()) {
                finished.setStatus("FAILED");
                finished.setErrorMsg("已访问目标网页，但没有解析到岗位数据。请检查 URL 是否为岗位列表页，或目标页面结构是否变化。");
            } else {
                finished.setStatus("SUCCESS");
                int nextPage = nextUrl != null ? extractPageNo(nextUrl) : 1;
                finished.setErrorMsg("直采完成：解析 " + jobs.size() + " 条，新增清洗入库 " + cleaned + " 条。下次将从第 "
                        + nextPage + " 页继续。");
            }
            crawlerLogMapper.updateById(finished);

            // 持久化翻页进度到 urlPattern（使用 LambdaUpdateWrapper 精确更新，避免 updateById 字段策略问题）
            if (nextUrl != null && !nextUrl.equals(currentUrlPattern)) {
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrawlerTask> updateWrapper =
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
                updateWrapper.eq(CrawlerTask::getId, task.getId())
                        .set(CrawlerTask::getUrlPattern, nextUrl);
                int rows = crawlerTaskMapper.update(null, updateWrapper);
                log.info("官方公开岗位直采翻页进度已更新 taskId={}, oldUrl={}, newUrl={}, affectedRows={}",
                        task.getId(), currentUrlPattern, nextUrl, rows);
            } else if (nextUrl == null) {
                log.warn("官方公开岗位直采翻页失败：advancePageNo 返回 null，urlPattern={}", currentUrlPattern);
            } else {
                log.info("官方公开岗位直采翻页未变化：nextUrl 与当前 urlPattern 相同，跳过更新。urlPattern={}", currentUrlPattern);
            }

            log.info("官方公开岗位直采写入完成 taskId={}, parsed={}, cleaned={}", task.getId(), jobs.size(), cleaned);
        } catch (Exception e) {
            CrawlerLog failed = new CrawlerLog();
            failed.setId(crawlerLog.getId());
            failed.setEndTime(LocalDateTime.now());
            failed.setRecordCount(0);
            failed.setStatus("FAILED");
            failed.setErrorMsg(StrUtil.maxLength(StrUtil.blankToDefault(e.getMessage(), "官方公开岗位直采失败"), 480));
            crawlerLogMapper.updateById(failed);
            log.warn("官方公开岗位直采失败 taskId={}, url={}", task.getId(), task.getUrlPattern(), e);
        } finally {
            CrawlerTask stoppedTask = new CrawlerTask();
            stoppedTask.setId(task.getId());
            stoppedTask.setStatus(0);
            crawlerTaskMapper.updateById(stoppedTask);
        }
    }

    /**
     * 将 urlPattern 中的 pageNo 参数自增 1。
     * 例如 "...?pageNo=1&..." → "...?pageNo=2&..."
     * 支持 pageNo、pageno、page_no 等常见写法（大小写不敏感）。
     */
    private String advancePageNo(String urlPattern) {
        if (urlPattern == null || urlPattern.isEmpty()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([?&]page_?no=)(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(urlPattern);
        if (m.find()) {
            int page = Integer.parseInt(m.group(2));
            return urlPattern.substring(0, m.start(2)) + (page + 1) + urlPattern.substring(m.end(2));
        }
        // 如果没匹配到 pageNo 参数，尝试在 URL 末尾追加 pageNo=2
        if (urlPattern.contains("?")) {
            return urlPattern + "&pageNo=2";
        }
        return urlPattern + "?pageNo=2";
    }

    private int extractPageNo(String urlPattern) {
        if (urlPattern == null) return 1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([?&]pageNo=)(\\d+)").matcher(urlPattern);
        if (m.find()) {
            return Integer.parseInt(m.group(2));
        }
        return 1;
    }

    private List<JobDataMessage> collectOfficialJobsWithTimeout(OfficialPublicDirectCollector collector, String url) {
        CompletableFuture<List<JobDataMessage>> future = CompletableFuture.supplyAsync(() -> collector.collect(url));
        try {
            return future.get(OFFICIAL_DIRECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("官方公开岗位采集超过 " + OFFICIAL_DIRECT_TIMEOUT_SECONDS
                    + " 秒仍未返回，已主动终止本次任务。请稍后重试或换一个可访问的公开岗位列表 URL。", e);
        } catch (Exception e) {
            throw new IllegalStateException(StrUtil.blankToDefault(e.getMessage(), "官方公开岗位采集执行异常"), e);
        }
    }

    private void saveRawJob(JobDataMessage job) {
        RawJobData raw = new RawJobData();
        raw.setSource(job.getSource());
        raw.setSourceUrl(job.getSourceUrl());
        raw.setRawContent(job.getRawContent());
        raw.setFetchTime(job.getFetchTime());
        raw.setStatus("CLEANED");
        rawJobDataMapper.insert(raw);
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
        String targetUrl = url == null ? "" : url.trim();
        if (targetUrl.isEmpty()) {
            failLog(crawlerLog, "采集任务没有配置 URL");
            return null;
        }
        if (!RobotsRules.isAllowed(targetUrl)) {
            failLog(crawlerLog, "目标站点 robots.txt 不允许抓取 " + targetUrl);
            return null;
        }
        return new Request(targetUrl);
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

    private void runSpiderAndWriteBack(Long taskId, Long logId, Spider spider, AtomicInteger collectedCount,
                                       PageProcessor processor, Long tenantId) {
        CompletableFuture.runAsync(() -> {
            Exception failure = null;
            try {
                spider.run();
            } catch (Exception e) {
                failure = e;
                log.error("采集任务执行失败 taskId={}, logId={}", taskId, logId, e);
            } finally {
                writeBackCrawlResult(taskId, logId, collectedCount, processor, tenantId, failure);
            }
        });
    }

    private void writeBackCrawlResult(Long taskId, Long logId, AtomicInteger collectedCount,
                                      PageProcessor processor, Long tenantId, Exception failure) {
        try {
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            }
            CrawlerLog current = crawlerLogMapper.selectById(logId);
            if (current == null || !"RUNNING".equals(current.getStatus())) {
                return;
            }

            collectedCount.addAndGet(persistCollectedNews(processor, tenantId));
            int count = collectedCount.get();
            CrawlerLog finished = new CrawlerLog();
            finished.setId(logId);
            finished.setEndTime(LocalDateTime.now());
            finished.setRecordCount(count);
            if (failure != null) {
                finished.setStatus("FAILED");
                String message = StrUtil.blankToDefault(failure.getMessage(), failure.getClass().getSimpleName());
                finished.setErrorMsg(StrUtil.maxLength(message, 480));
            } else if (count <= 0 && !(processor instanceof NewsPageProcessor)) {
                finished.setStatus("FAILED");
                finished.setErrorMsg("本次采集完成但未解析到岗位数据，可能是目标站点不可访问、返回空页面、页面结构变化或被源站拦截。");
            } else {
                finished.setStatus("SUCCESS");
            }
            crawlerLogMapper.updateById(finished);

            CrawlerTask task = new CrawlerTask();
            task.setId(taskId);
            task.setStatus(0);
            crawlerTaskMapper.updateById(task);
            runningSpiders.remove(taskId);
            runningLogIds.remove(taskId);
        } catch (Exception e) {
            log.error("采集完成状态回写失败 taskId={}, logId={}", taskId, logId, e);
        } finally {
            TenantContextHolder.clear();
        }
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
            runSpiderAndWriteBack(taskId, logId, spider, collectedCount, processor, tenantId);
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
            case "STACK_OVERFLOW_SURVEY": {
                Map<String, String> params = parseUrlParams(task.getUrlPattern());
                String url = params.getOrDefault("url", task.getUrlPattern());
                int maxItems = Integer.parseInt(params.getOrDefault("maxItems", "200"));
                return new StackOverflowSurveyProcessor(url, maxItems);
            }
            case "NEWS_OSCHINA":
                return new OsChinaNewsProcessor();
            default:
                throw new BizException("暂不支持的采集源类型：" + task.getSourceType()
                        + "。当前可用：MOCK / OFFICIAL_PUBLIC / STACK_OVERFLOW_SURVEY / ZHAOPIN");
        }
    }

    private Map<String, String> parseUrlParams(String urlPattern) {
        Map<String, String> params = new LinkedHashMap<>();
        if (urlPattern == null || urlPattern.isEmpty()) {
            return params;
        }
        String raw = urlPattern.trim();
        if (raw.startsWith("url=")) {
            int optionStart = firstCrawlerOptionIndex(raw);
            params.put("url", optionStart > 0 ? raw.substring(4, optionStart).trim() : raw.substring(4).trim());
            if (optionStart <= 0) {
                return params;
            }
            raw = raw.substring(optionStart + 1);
        } else if (raw.startsWith("http://") || raw.startsWith("https://")) {
            int optionStart = firstCrawlerOptionIndex(raw);
            params.put("url", optionStart > 0 ? raw.substring(0, optionStart).trim() : raw);
            if (optionStart <= 0) {
                return params;
            }
            raw = raw.substring(optionStart + 1);
        }
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0].trim(), kv[1].trim());
            }
        }
        return params;
    }

    private int firstCrawlerOptionIndex(String raw) {
        int first = -1;
        String[] optionKeys = {"&maxItems=", "&maxPages="};
        for (String key : optionKeys) {
            int index = raw.indexOf(key);
            if (index >= 0 && (first < 0 || index < first)) {
                first = index;
            }
        }
        return first;
    }
}
