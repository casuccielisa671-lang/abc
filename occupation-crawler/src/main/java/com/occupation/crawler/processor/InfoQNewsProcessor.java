package com.occupation.crawler.processor;

/**
 * InfoQ 中文站资讯采集处理器
 * <p>
 * 通过 RSS 订阅地址抓取 InfoQ 中文站的最新文章，
 * 自动分类后写入 news 表。
 *
 * @author occupation-team
 */
public class InfoQNewsProcessor extends NewsPageProcessor {

    private static final String RSS_URL = "https://www.infoq.cn/feed";
    private static final String SOURCE_NAME = "InfoQ 中文站";
    private static final String DOMAIN = "www.infoq.cn";

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
