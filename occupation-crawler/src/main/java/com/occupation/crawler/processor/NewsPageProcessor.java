package com.occupation.crawler.processor;

import com.occupation.recommend.entity.News;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 资讯采集页面处理器（抽象基类）
 * <p>
 * 专门用于解析 RSS/XML 格式的行业资讯，提取标题、链接、摘要等信息，
 * 写入 news 表（type=EXTERNAL）。不同资讯源（InfoQ、OSCHINA 等）
 * 分别实现 {@link #getRssUrl()} 和 {@link #getSourceName()}。
 *
 * @author occupation-team
 */
@Slf4j
public abstract class NewsPageProcessor implements PageProcessor {

    /** 封面色块样式池 */
    private static final String[] COVERS = {"blue", "green", "purple", "amber"};

    /** 采集结果缓存 */
    protected final List<News> collectedNews = new ArrayList<>();

    /** 标题最大长度 */
    private static final int MAX_TITLE_LEN = 300;

    /** 摘要最大长度 */
    private static final int MAX_SUMMARY_LEN = 600;

    @Override
    public Site getSite() {
        return Site.me()
                .setDomain(getDomain())
                .setUserAgent(randomUserAgent())
                .setSleepTime(randomSleep())
                .setRetryTimes(2)
                .setTimeOut(10000)
                .setCharset("UTF-8");
    }

    @Override
    public void process(Page page) {
        try {
            String rawText = page.getRawText();
            if (rawText == null || rawText.trim().isEmpty()) {
                log.warn("[{}] 页面内容为空，跳过", getSourceName());
                page.setSkip(true);
                return;
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(rawText.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() == 0) {
                // 部分 RSS 用 <entry> 标签（Atom 格式）
                items = doc.getElementsByTagName("entry");
            }

            int limit = Math.min(items.getLength(), getMaxItems());
            for (int i = 0; i < limit; i++) {
                Element item = (Element) items.item(i);
                News news = parseItem(item);
                if (news != null) {
                    collectedNews.add(news);
                }
            }
            log.info("[{}] 解析完成，共 {} 条资讯", getSourceName(), collectedNews.size());
        } catch (Exception e) {
            log.error("[{}] RSS 解析失败: {}", getSourceName(), e.getMessage(), e);
        }
        page.setSkip(true);
    }

    /**
     * 解析单条 RSS item → News 实体
     */
    protected News parseItem(Element item) {
        String title = text(item, "title");
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        title = title.trim();
        if (title.length() > MAX_TITLE_LEN) {
            title = title.substring(0, MAX_TITLE_LEN);
        }

        String link = text(item, "link");
        // Atom 格式 link 可能在 href 属性中
        if (link == null) {
            Element linkEl = firstChild(item, "link");
            if (linkEl != null && linkEl.hasAttribute("href")) {
                link = linkEl.getAttribute("href");
            }
        }

        String description = text(item, "description");
        if (description == null) {
            description = text(item, "summary");
        }
        if (description != null && description.length() > MAX_SUMMARY_LEN) {
            description = description.substring(0, MAX_SUMMARY_LEN);
        }

        String pubDate = text(item, "pubDate");
        if (pubDate == null) {
            pubDate = text(item, "published");
        }

        News news = new News();
        news.setType("EXTERNAL");
        news.setTitle(title);
        news.setSourceUrl(link);
        news.setSummary(description);
        news.setSource(getSourceName());
        news.setCategory(detectCategory(title));
        news.setCoverStyle(COVERS[(int) (Math.random() * COVERS.length)]);
        news.setViewCount(0);
        news.setFeatured(0);
        news.setStatus(1);
        news.setPublishTime(LocalDateTime.now());
        return news;
    }

    /**
     * 根据标题关键词自动分类
     */
    protected String detectCategory(String title) {
        if (title == null) {
            return null;
        }
        String lower = title.toLowerCase();
        if (containsAny(lower, "java", "spring", "mybatis", "微服务", "后端", "golang", "go语言", "rust")) {
            return "backend";
        }
        if (containsAny(lower, "vue", "react", "angular", "javascript", "typescript", "前端", "css", "html", "node.js", "nodejs")) {
            return "frontend";
        }
        if (containsAny(lower, "docker", "kubernetes", "k8s", "devops", "ci/cd", "jenkins", "运维", "linux", "nginx")) {
            return "devops";
        }
        if (containsAny(lower, "测试", "自动化测试", "selenium", "junit", "test", "质量")) {
            return "test";
        }
        if (containsAny(lower, "spark", "flink", "hadoop", "大数据", "数据仓库", "kafka", "hive", "数据湖")) {
            return "bigdata";
        }
        return null;
    }

    /**
     * 返回采集结果并清空缓存
     */
    public List<News> drainCollectedNews() {
        List<News> drained = new ArrayList<>(collectedNews);
        collectedNews.clear();
        return drained;
    }

    public int getCollectedCount() {
        return collectedNews.size();
    }

    // ---- 子类必须实现 ----

    /** 资讯源名称（如 "InfoQ 中文站"） */
    public abstract String getSourceName();

    /** RSS 订阅地址 */
    public abstract String getRssUrl();

    /** 目标域名 */
    protected abstract String getDomain();

    /** 单次最大解析条数，避免大型 RSS/Atom 源导致手动采集耗时过长。 */
    protected int getMaxItems() {
        return 30;
    }

    // ---- 工具方法 ----

    protected String text(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) {
            return null;
        }
        String content = nl.item(0).getTextContent();
        return (content != null && !content.isEmpty()) ? content : null;
    }

    protected Element firstChild(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        return nl.getLength() == 0 ? null : (Element) nl.item(0);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    // ---- 反爬辅助 ----

    private static final String[] UA_POOL = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118.0.0.0 Safari/537.36"
    };

    protected String randomUserAgent() {
        return UA_POOL[(int) (Math.random() * UA_POOL.length)];
    }

    protected int randomSleep() {
        return 2000 + (int) (Math.random() * 3000);
    }
}
