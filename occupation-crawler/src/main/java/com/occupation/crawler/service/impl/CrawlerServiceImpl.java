package com.occupation.crawler.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.crawler.entity.CrawlerLog;
import com.occupation.crawler.entity.CrawlerTask;
import com.occupation.crawler.mapper.CrawlerLogMapper;
import com.occupation.crawler.mapper.CrawlerTaskMapper;
import com.occupation.crawler.processor.MockJobPageProcessor;
import com.occupation.crawler.processor.RobotsRules;
import com.occupation.crawler.processor.ZhaopinJobPageProcessor;
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

    /** MOCK 任务未指定数据文件时使用的默认文件（位于 classpath 的 mock/ 目录下） */
    private static final String DEFAULT_MOCK_FILE = "mock-jobs.json";

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
            // 单线程：真实站点是别人家的服务器。配合 Processor 里 5~10 秒的随机间隔，
            // 抓取压力可以忽略。原来是 thread(3)，三个线程同时打一个站，不合适。
            Spider spider = Spider.create(processor)
                    .addPipeline(jobPipeline)
                    .addPipeline(new ConsolePipeline())
                    .thread(1);

            // robots.txt 校验不通过时直接返回 FAILED 日志，而不是抛异常。
            // 本方法带 @Transactional(rollbackFor = Exception.class)，而 BizException 是
            // RuntimeException —— 一抛就会把上面刚写的 FAILED 日志一起回滚掉，
            // 采集日志页什么都看不到，用户只会得到一句莫名其妙的报错。
            Request seed = resolveSeedRequest(task, processor, crawlerLog);
            if (seed == null) {
                task.setStatus(0);
                crawlerTaskMapper.updateById(task);
                return crawlerLog;   // 已被 failLog 标记为 FAILED，原因写在 error_msg 里
            }
            spider.addRequest(seed);

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

    /**
     * 解析本次采集的起始请求，顺带做 robots.txt 校验。
     *
     * @return null 表示不允许抓取，日志已被标记为 FAILED
     */
    private Request resolveSeedRequest(CrawlerTask task, PageProcessor processor, CrawlerLog crawlerLog) {
        if (processor instanceof ZhaopinJobPageProcessor) {
            // urlPattern 存的是参数串而非 URL，必须走 seedRequest：
            // 它会解开 301（robots 允许的是跳转后的路径式地址）并逐段校验 robots.txt
            Map<String, String> params = parseUrlParams(task.getUrlPattern());
            String keyword = params.getOrDefault("kw", params.getOrDefault("query", "Java"));
            String city = params.getOrDefault("jl", params.getOrDefault("city", "530"));
            Request seed = ZhaopinJobPageProcessor.seedRequest(keyword, city);
            if (seed == null) {
                failLog(crawlerLog, "目标站点的 robots.txt 不允许抓取该地址，已放弃本次采集");
            }
            return seed;
        }

        String url = task.getUrlPattern();
        if (url == null || url.trim().isEmpty()) {
            failLog(crawlerLog, "采集任务没有配置 URL");
            return null;
        }
        if (!RobotsRules.isAllowed(url)) {
            failLog(crawlerLog, "目标站点的 robots.txt 不允许抓取: " + url);
            return null;
        }
        return new Request(url);
    }

    /**
     * 把这条采集日志标记为失败。
     * <p>
     * createLog 已经写了一条 RUNNING 的记录，若直接抛异常返回，
     * 那条记录会永远停在 RUNNING，采集日志页看起来像是「跑着没结束」。
     */
    private void failLog(CrawlerLog crawlerLog, String reason) {
        crawlerLog.setEndTime(LocalDateTime.now());
        crawlerLog.setStatus("FAILED");
        crawlerLog.setErrorMsg(reason);
        crawlerLogMapper.updateById(crawlerLog);
        log.warn("采集失败: taskId={}, {}", crawlerLog.getTaskId(), reason);
    }

    private PageProcessor createProcessor(CrawlerTask task) {
        switch (task.getSourceType()) {
            case "MOCK": {
                // 模拟爬虫：从 classpath 加载 mock 数据（InputStream 方式，兼容 JAR 内运行）。
                // urlPattern 为空时回落到默认文件 —— 否则会拼出 "mock/null" 而后静默采到 0 条。
                String dataFile = StrUtil.blankToDefault(task.getUrlPattern(), DEFAULT_MOCK_FILE);
                return new MockJobPageProcessor("mock/" + dataFile, true);
            }
            case "BOSS_ZHIPIN":
                // BossJobPageProcessor 已于 2026-07-10 删除。
                // 它拼出的列表页地址形如 /web/geek/job?query=Java&city=101010100，
                // 而 www.zhipin.com/robots.txt 里明文写着：
                //     Disallow: /*?query=*
                //     Disallow: *?city=*
                // 这不是技术问题，是人家写在门口的。何况页面已改成 JS 渲染 + 行为验证，
                // 靠随机 UA 也过不去。真实采集请用 ZHAOPIN。
                throw new BizException("Boss 直聘的 robots.txt 明确禁止抓取职位列表页，"
                        + "本项目不再支持该采集源。请改用 ZHAOPIN（真实采集）或 MOCK（本地样例）");

            case "ZHAOPIN": {
                // 真实采集：智联的城市参数是 jl（如 530=杭州），与 BOSS 的 city 编码不同
                Map<String, String> params = parseUrlParams(task.getUrlPattern());
                String keyword = params.getOrDefault("kw", params.getOrDefault("query", "Java"));
                String cityCode = params.getOrDefault("jl", params.getOrDefault("city", "530"));
                int maxPages = Integer.parseInt(params.getOrDefault("maxPages", "3"));
                return new ZhaopinJobPageProcessor(keyword, cityCode, maxPages);
            }
            default:
                // 抛 BizException 而不是 IllegalArgumentException：后者会被兜底处理器变成
                // 500「系统内部错误」，用户根本不知道自己选错了采集源。
                // 种子里的 COMPANY_OFFICIAL 任务就会走到这里 —— 它只有表结构没有实现。
                throw new BizException("暂不支持的采集源类型：" + task.getSourceType()
                        + "。当前可用：MOCK（本地样例，不访问外网）/ ZHAOPIN（真实采集）");
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
