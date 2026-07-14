package com.occupation.crawler.processor;

/**
 * 开源中国（OSCHINA）资讯采集处理器
 * <p>
 * 通过 RSS 订阅地址抓取 OSCHINA 的最新资讯，
 * 自动分类后写入 news 表。
 *
 * @author occupation-team
 */
public class OsChinaNewsProcessor extends NewsPageProcessor {

    private static final String RSS_URL = "https://www.oschina.net/news/rss";
    private static final String SOURCE_NAME = "开源中国";
    private static final String DOMAIN = "www.oschina.net";

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String getRssUrl() {
        return RSS_URL;
    }

    @Override
    protected String getDomain() {
        return DOMAIN;
    }
}
