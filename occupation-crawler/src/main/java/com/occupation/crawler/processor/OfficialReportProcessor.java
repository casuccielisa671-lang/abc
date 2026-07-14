package com.occupation.crawler.processor;

import cn.hutool.core.util.StrUtil;

import java.net.URI;

/**
 * 官方公开报告/政策资讯采集器。
 *
 * <p>该采集器面向 RSS/Atom 等公开订阅源，结果写入 news 表，
 * 不伪装成岗位数据，避免报告类资料污染职位分析口径。</p>
 */
public class OfficialReportProcessor extends NewsPageProcessor {

    private static final String DEFAULT_SOURCE_NAME = "官方公开报告";
    private static final String DEFAULT_RSS_URL = "https://www.oschina.net/news/rss";
    private static final int DEFAULT_MAX_ITEMS = 20;

    private final String rssUrl;
    private final String sourceName;
    private final String domain;
    private final int maxItems;

    public OfficialReportProcessor(String rssUrl, String sourceName, int maxItems) {
        this.rssUrl = StrUtil.blankToDefault(rssUrl, DEFAULT_RSS_URL);
        this.sourceName = StrUtil.blankToDefault(sourceName, DEFAULT_SOURCE_NAME);
        this.domain = parseDomain(this.rssUrl);
        this.maxItems = maxItems > 0 ? maxItems : DEFAULT_MAX_ITEMS;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public String getRssUrl() {
        return rssUrl;
    }

    @Override
    protected String getDomain() {
        return domain;
    }

    @Override
    protected int getMaxItems() {
        return maxItems;
    }

    private String parseDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            return StrUtil.blankToDefault(host, "official-report.local");
        } catch (Exception e) {
            return "official-report.local";
        }
    }
}
