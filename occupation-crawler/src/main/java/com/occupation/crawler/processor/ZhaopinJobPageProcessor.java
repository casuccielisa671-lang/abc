package com.occupation.crawler.processor;

import com.alibaba.fastjson.JSONObject;
import com.occupation.common.dto.JobDataMessage;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.selector.Selectable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智联招聘职位采集处理器
 * <p>
 * 结构与 {@link BossJobPageProcessor} 一致：列表页取详情页链接并翻页，详情页解析字段。
 * 反爬策略沿用基类（UA 池轮换、请求间隔、失败重试），<b>不做任何检测规避</b>。
 * <p>
 * ⚠️ 与 BOSS 采集器一样，本处理器默认不启用：站点 DOM 结构会随改版失效，
 * 且高频抓取可能违反目标站点的服务条款。日常开发与演示请使用 {@code MOCK} 数据源。
 * 仅在获得授权、且已确认目标站点的 robots.txt 与服务条款允许的前提下使用。
 *
 * @author occupation-team
 */
@Slf4j
public class ZhaopinJobPageProcessor extends JobPageProcessor {

    private static final String SOURCE = "ZHAOPIN";
    private static final String DOMAIN = "www.zhaopin.com";

    private static final Pattern LIST_PATTERN =
            Pattern.compile("https?://sou\\.zhaopin\\.com/.*");

    private static final Pattern DETAIL_PATTERN =
            Pattern.compile("https?://jobs\\.zhaopin\\.com/[a-zA-Z0-9]+\\.htm.*");

    private static final Pattern PAGE_PATTERN = Pattern.compile("[?&]p=(\\d+)");

    /**
     * 智联薪资写法比 BOSS 多样：{@code 1.5万-2万}、{@code 8千-1.2万}、{@code 15K-25K}。
     * 捕获「数字 + 可选单位」两段，单位缺失时沿用后一段的单位（如「1.5-2万」）。
     */
    private static final Pattern SALARY_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([万千kK]?)\\s*[-~]\\s*(\\d+(?:\\.\\d+)?)\\s*([万千kK]?)");

    private final String keyword;
    private final String cityCode;
    private final int maxPages;

    public ZhaopinJobPageProcessor(String keyword, String cityCode, int maxPages) {
        super(SOURCE);
        this.keyword = keyword;
        this.cityCode = cityCode;
        this.maxPages = maxPages;
    }

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
        if (DETAIL_PATTERN.matcher(url).find()) {
            processDetailPage(page);
        } else if (LIST_PATTERN.matcher(url).find()) {
            processListPage(page);
        }
    }

    // ---- 列表页 ----

    private void processListPage(Page page) {
        Selectable html = page.getHtml();

        List<Selectable> cards = html.$(".joblist-box__item").nodes();
        if (cards.isEmpty()) {
            cards = html.$(".job-list-box .joblist-box__item").nodes();
        }
        if (cards.isEmpty()) {
            String sample = html.toString();
            log.warn("智联列表页未解析到职位卡片（站点 DOM 可能已改版） — url={}, htmlSample={}",
                    page.getUrl(), sample.substring(0, Math.min(200, sample.length())));
            page.setSkip(true);
            return;
        }

        List<String> detailUrls = new ArrayList<>();
        for (Selectable card : cards) {
            String href = card.$("a.jobinfo__name", "href").get();
            if (href == null || href.isEmpty()) {
                href = card.$("a", "href").get();
            }
            if (href != null && !href.isEmpty()) {
                detailUrls.add(href.startsWith("http") ? href : "https:" + href);
            }
        }
        page.addTargetRequests(detailUrls);
        log.info("智联列表页解析完成 — 发现 {} 个职位", detailUrls.size());

        int currentPage = extractPageNum(page.getUrl().toString());
        if (currentPage < maxPages) {
            page.addTargetRequest(buildListUrl(currentPage + 1));
            log.info("智联添加下一页 — page={}", currentPage + 1);
        }
    }

    // ---- 详情页 ----

    private void processDetailPage(Page page) {
        Selectable html = page.getHtml();

        String title = extractText(html, ".summary-plane__title", ".job-summary .title");
        String company = extractText(html, ".company__title", ".company-name");
        String salaryStr = extractText(html, ".summary-plane__salary", ".job-salary");
        String city = extractText(html, ".summary-plane__info li:nth-child(1)", ".job-location");
        String experience = extractText(html, ".summary-plane__info li:nth-child(2)", ".job-experience");
        String education = extractText(html, ".summary-plane__info li:nth-child(3)", ".job-education");
        String description = extractText(html, ".describtion__detail-content", ".job-detail");
        List<String> skills = extractSkills(html);

        int[] salary = parseSalary(salaryStr);

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

        List<JobDataMessage> batch = new ArrayList<>();
        batch.add(message);
        page.putField("jobs", batch);

        log.info("智联详情页解析完成 — title={}, company={}", title, company);
    }

    // ---- 辅助 ----

    private String extractText(Selectable html, String primarySelector, String fallbackSelector) {
        String text = html.$(primarySelector, "text").get();
        if (text == null || text.trim().isEmpty()) {
            text = html.$(fallbackSelector, "text").get();
        }
        return text != null ? text.trim() : "";
    }

    private List<String> extractSkills(Selectable html) {
        List<String> skills = new ArrayList<>();
        List<Selectable> tags = html.$(".summary-plane__tags .tag").nodes();
        if (tags.isEmpty()) {
            tags = html.$(".job-tags span").nodes();
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
     * 解析薪资为 [min, max]，单位统一为元/月。
     * <p>
     * 「1.5万-2万」→ [15000, 20000]；「8千-1.2万」→ [8000, 12000]；「15K-25K」→ [15000, 25000]。
     * 「1.5-2万」这种只在后段标单位的写法，前段沿用后段单位。
     */
    int[] parseSalary(String salaryStr) {
        if (salaryStr == null || salaryStr.isEmpty()) {
            return new int[]{0, 0};
        }
        Matcher m = SALARY_PATTERN.matcher(salaryStr);
        if (!m.find()) {
            return new int[]{0, 0};
        }
        String minUnit = m.group(2);
        String maxUnit = m.group(4);
        // 「1.5-2万」：前段无单位时沿用后段；「15K-25K」：两段都有
        if (minUnit.isEmpty()) {
            minUnit = maxUnit;
        }
        if (maxUnit.isEmpty()) {
            maxUnit = minUnit;
        }
        int min = toYuan(m.group(1), minUnit);
        int max = toYuan(m.group(3), maxUnit);
        return new int[]{min, max};
    }

    private int toYuan(String number, String unit) {
        BigDecimal value = new BigDecimal(number);
        BigDecimal multiplier;
        if ("万".equals(unit)) {
            multiplier = BigDecimal.valueOf(10000);
        } else if ("千".equals(unit) || "k".equalsIgnoreCase(unit)) {
            multiplier = BigDecimal.valueOf(1000);
        } else {
            // 无单位：按「元」原样处理
            multiplier = BigDecimal.ONE;
        }
        return value.multiply(multiplier).intValue();
    }

    private int extractPageNum(String url) {
        Matcher m = PAGE_PATTERN.matcher(url);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    private String buildListUrl(int pageNum) {
        return String.format("https://sou.zhaopin.com/?jl=%s&kw=%s&p=%d", cityCode, keyword, pageNum);
    }

    /** 生成列表页种子请求 */
    public static Request seedRequest(String keyword, String cityCode) {
        return new Request(String.format("https://sou.zhaopin.com/?jl=%s&kw=%s&p=1", cityCode, keyword));
    }
}
