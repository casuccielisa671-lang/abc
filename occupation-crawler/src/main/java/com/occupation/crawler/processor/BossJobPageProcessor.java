package com.occupation.crawler.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.occupation.common.dto.JobDataMessage;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.selector.Selectable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BOSS 直聘职位采集处理器
 * <p>
 * 采集流程：
 * 1. 列表页：解析职位卡片（标题、公司、薪资、城市、学历要求、经验要求、职位链接）
 * 2. 详情页：解析职位描述和技能标签
 * 3. 分页翻页：自动遍历列表页
 * <p>
 * 反爬策略：
 * - User-Agent 池随机轮换（继承自基类）
 * - 请求间隔随机化 5-15 秒
 * - 失败重试 3 次
 * - 列表页与详情页使用不同域名请求间隔
 */
@Slf4j
public class BossJobPageProcessor extends JobPageProcessor {

    private static final String SOURCE = "BOSS_ZHIPIN";
    private static final String DOMAIN = "www.zhipin.com";

    /** 列表页 URL 模式 */
    private static final Pattern LIST_PATTERN =
            Pattern.compile("https?://www\\.zhipin\\.com/web/geek/job\\?.*");

    /** 详情页 URL 模式 */
    private static final Pattern DETAIL_PATTERN =
            Pattern.compile("https?://www\\.zhipin\\.com/job_detail/[a-zA-Z0-9]+\\.html.*");

    /** 薪资范围正则：xxK-xxK */
    private static final Pattern SALARY_PATTERN =
            Pattern.compile("(\\d+)[kK]-(\\d+)[kK]");

    /** 页码参数提取 */
    private static final Pattern PAGE_PATTERN =
            Pattern.compile("[?&]page=(\\d+)");

    /** 搜索关键词（必填） */
    private final String keyword;

    /** 目标城市编码（如 101010100=北京） */
    private final String cityCode;

    /** 最大采集页数 */
    private final int maxPages;

    public BossJobPageProcessor(String keyword, String cityCode, int maxPages) {
        super(SOURCE);
        this.keyword = keyword;
        this.cityCode = cityCode;
        this.maxPages = maxPages;
    }

    /**
     * 详情页请求间隔（更长，避免触发反爬）
     */
    @Override
    protected int randomSleep() {
        return 5000 + (int) (Math.random() * 10000);
    }

    @Override
    protected String getDomain() {
        return DOMAIN;
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();

        if (isDetailPage(url)) {
            processDetailPage(page);
        } else if (isListPage(url)) {
            processListPage(page);
        }
    }

    // ---- 列表页解析 ----

    private void processListPage(Page page) {
        Selectable html = page.getHtml();

        // 解析职位卡片列表
        List<Selectable> jobCards = html.$("li.job-card-wrapper").nodes();
        if (jobCards.isEmpty()) {
            // BOSS 直聘可能使用不同的 DOM 结构，尝试备选选择器
            jobCards = html.$(".job-list-box .job-card").nodes();
        }

        if (jobCards.isEmpty()) {
            log.warn("列表页未解析到职位卡片 — url={}, htmlSample={}",
                    page.getUrl(), html.toString().substring(0, Math.min(200, html.toString().length())));
            page.setSkip(true);
            return;
        }

        List<String> detailUrls = new ArrayList<>();
        for (Selectable card : jobCards) {
            // 提取详情页链接
            String detailUrl = extractDetailUrl(card);
            if (detailUrl != null) {
                detailUrls.add(detailUrl);
            }
        }

        // 将详情页链接加入爬取队列
        page.addTargetRequests(detailUrls);
        log.info("列表页解析完成 — 发现 {} 个职位，将爬取详情页", detailUrls.size());

        // 处理分页
        String currentUrl = page.getUrl().toString();
        int currentPage = extractPageNum(currentUrl);
        if (currentPage < maxPages) {
            String nextUrl = buildListUrl(currentPage + 1);
            page.addTargetRequest(nextUrl);
            log.info("添加下一页 — page={}", currentPage + 1);
        }
    }

    // ---- 详情页解析 ----

    private void processDetailPage(Page page) {
        Selectable html = page.getHtml();

        String title = extractText(html, ".name h1", ".job-name .name");
        String company = extractText(html, ".company-info .name a", ".company-name");
        String salaryStr = extractText(html, ".job-banner .salary", ".job-salary");
        String city = extractText(html, ".job-banner .location", ".job-location");
        String experience = extractText(html, ".job-banner .experience", ".job-experience");
        String education = extractText(html, ".job-banner .education", ".job-education");
        String description = extractText(html, ".job-sec .text", ".job-detail .detail-text");
        List<String> skills = extractSkills(html);

        // 解析薪资范围
        int[] salary = parseSalary(salaryStr);

        // 构建 JSON 内容
        JSONObject jobJson = new JSONObject();
        jobJson.put("title", title);
        jobJson.put("company", company);
        jobJson.put("city", city);
        jobJson.put("salaryMin", salary[0]);
        jobJson.put("salaryMax", salary[1]);
        jobJson.put("salaryStr", salaryStr);
        jobJson.put("experience", experience);
        jobJson.put("education", education);
        jobJson.put("skills", skills);
        jobJson.put("description", description);
        jobJson.put("publishDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        JobDataMessage message = buildMessage(page.getUrl().toString(), jobJson.toJSONString());
        addJob(message);

        // 放入 page 供 Pipeline 处理
        List<JobDataMessage> batch = new ArrayList<>();
        batch.add(message);
        page.putField("jobs", batch);

        log.info("详情页解析完成 — title={}, company={}", title, company);
    }

    // ---- 辅助方法 ----

    private boolean isListPage(String url) {
        return LIST_PATTERN.matcher(url).find();
    }

    private boolean isDetailPage(String url) {
        return DETAIL_PATTERN.matcher(url).find();
    }

    private String extractDetailUrl(Selectable card) {
        String href = card.$("a.job-card-left", "href").get();
        if (href == null || href.isEmpty()) {
            href = card.$("a.job-name", "href").get();
        }
        if (href != null && !href.startsWith("http")) {
            href = "https://" + DOMAIN + href;
        }
        return href;
    }

    private String extractText(Selectable html, String primarySelector, String fallbackSelector) {
        String text = html.$(primarySelector, "text").get();
        if (text == null || text.trim().isEmpty()) {
            text = html.$(fallbackSelector, "text").get();
        }
        return text != null ? text.trim() : "";
    }

    private List<String> extractSkills(Selectable html) {
        List<String> skills = new ArrayList<>();
        // 尝试多种技能标签选择器
        List<Selectable> tags = html.$(".job-tag-list .tag-item").nodes();
        if (tags.isEmpty()) {
            tags = html.$(".tag-container .tag").nodes();
        }
        if (tags.isEmpty()) {
            tags = html.$(".skill-tags span").nodes();
        }
        for (Selectable tag : tags) {
            String skill = tag.xpath("//text()").get();
            if (skill != null && !skill.trim().isEmpty()) {
                skills.add(skill.trim());
            }
        }
        return skills;
    }

    /**
     * 解析薪资字符串为 [min, max]（单位：元/月）
     * 格式：15K-25K → [15000, 25000]
     */
    private int[] parseSalary(String salaryStr) {
        if (salaryStr == null) return new int[]{0, 0};
        Matcher matcher = SALARY_PATTERN.matcher(salaryStr);
        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1)) * 1000;
            int max = Integer.parseInt(matcher.group(2)) * 1000;
            return new int[]{min, max};
        }
        return new int[]{0, 0};
    }

    private int extractPageNum(String url) {
        Matcher matcher = PAGE_PATTERN.matcher(url);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }

    private String buildListUrl(int pageNum) {
        return String.format(
                "https://www.zhipin.com/web/geek/job?query=%s&city=%s&page=%d",
                keyword, cityCode, pageNum
        );
    }

    /**
     * 生成列表页种子请求
     */
    public static Request seedRequest(String keyword, String cityCode) {
        String url = String.format(
                "https://www.zhipin.com/web/geek/job?query=%s&city=%s&page=1",
                keyword, cityCode
        );
        return new Request(url);
    }

}
