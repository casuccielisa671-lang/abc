package com.occupation.crawler.processor;

import com.occupation.common.dto.JobDataMessage;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 职位采集页面处理器（抽象基类）
 * <p>
 * 不同采集源（BOSS 直聘、智联招聘、企业官网等）分别实现 parse() 方法。
 * 基类统一处理站点配置、反爬策略、结果收集。
 */
public abstract class JobPageProcessor implements PageProcessor {

    /** 采集源标识 */
    protected final String source;

    /** 采集结果缓存 */
    protected final List<JobDataMessage> collectedJobs = new ArrayList<>();

    protected JobPageProcessor(String source) {
        this.source = source;
    }

    /**
     * 配置站点信息（UA、重试、间隔等反爬策略）
     */
    @Override
    public Site getSite() {
        return Site.me()
                .setDomain(getDomain())
                .setUserAgent(randomUserAgent())
                .setSleepTime(randomSleep())
                .setRetryTimes(3)
                .setTimeOut(10000)
                .setCharset("UTF-8");
    }

    /**
     * 页面解析逻辑（子类实现）
     */
    @Override
    public abstract void process(Page page);

    /**
     * 返回采集结果并清空缓存
     */
    public List<JobDataMessage> drainCollectedJobs() {
        List<JobDataMessage> drained = new ArrayList<>(collectedJobs);
        collectedJobs.clear();
        return drained;
    }

    /**
     * 获取已采集数量
     */
    public int getCollectedCount() {
        return collectedJobs.size();
    }

    /**
     * 构建标准 JobDataMessage
     */
    protected JobDataMessage buildMessage(String sourceUrl, String rawContent) {
        return JobDataMessage.builder()
                .source(this.source)
                .sourceUrl(sourceUrl)
                .rawContent(rawContent)
                .fetchTime(LocalDateTime.now())
                .build();
    }

    protected void addJob(JobDataMessage job) {
        this.collectedJobs.add(job);
    }

    protected void flushJobs(Page page) {
        page.putField("jobs", drainCollectedJobs());
    }

    // ---- 反爬辅助 ----

    private static final String[] UA_POOL = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118.0.0.0 Safari/537.36"
    };

    protected String randomUserAgent() {
        return UA_POOL[(int) (Math.random() * UA_POOL.length)];
    }

    /**
     * 请求间隔随机化（3-8 秒），子类可覆盖
     */
    protected int randomSleep() {
        return 3000 + (int) (Math.random() * 5000);
    }

    /**
     * 子类返回目标域名
     */
    protected abstract String getDomain();

}
