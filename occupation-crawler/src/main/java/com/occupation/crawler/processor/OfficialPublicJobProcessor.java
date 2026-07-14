package com.occupation.crawler.processor;

import com.alibaba.fastjson.JSONObject;
import com.occupation.common.dto.JobDataMessage;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 人社部等官方公开岗位信息采集器。
 *
 * <p>这个页面经常改版，所以同时支持表格行、列表项、卡片式布局，避免页面小改版后直接采不到数据。</p>
 */
@Slf4j
public class OfficialPublicJobProcessor extends JobPageProcessor {

    private static final String SOURCE = "OFFICIAL_PUBLIC";
    private static final String MOHRSS_JOB_HOST = "job.mohrss.gov.cn";
    private static final String MOHRSS_JOB_PATH = "/cjobs/lkysudi";
    private static final String[] MOHRSS_ROW_SELECTORS = {
            ".list_list ul.list_show",
            ".list_list tr",
            ".list_list li",
            "table tbody tr",
            "tbody tr",
            ".job-list li",
            ".list li",
            ".item"
    };
    private static final Pattern PAGE_NO_PATTERN = Pattern.compile("([?&]pageNo=)(\\d+)");
    private static final Pattern CITY_PATTERN = Pattern.compile(
            "(北京|上海|天津|重庆|杭州|广州|深圳|南京|苏州|成都|武汉|西安|长沙|郑州|济南|青岛|宁波|厦门|合肥|福州|南昌|昆明|贵阳|南宁|海口|太原|石家庄|沈阳|大连|长春|哈尔滨)");
    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*([万千kK]?)\\s*[-~至]\\s*(\\d+(?:\\.\\d+)?)\\s*([万千kK]?)");
    private static final String[] TITLE_KEYWORDS = {
            "招聘", "招录", "人才", "岗位", "事业单位", "教师", "工程师", "实习"
    };
    private static final String[] SKILL_KEYWORDS = {
            "Java", "Python", "前端", "后端", "数据", "大数据", "人工智能", "算法", "运维", "网络", "安全", "测试"
    };

    private final String seedUrl;
    private final String sourceName;
    private final int maxItems;
    private final String domain;
    private int emittedCount = 0;

    public OfficialPublicJobProcessor(String seedUrl, String sourceName, int maxItems) {
        super(SOURCE);
        this.seedUrl = seedUrl == null ? "" : seedUrl.trim();
        this.sourceName = sourceName == null || sourceName.trim().isEmpty()
                ? "官方公开招聘公告" : sourceName.trim();
        this.maxItems = Math.max(1, maxItems);
        this.domain = resolveDomain(this.seedUrl);
    }

    @Override
    protected String getDomain() {
        return domain;
    }

    @Override
    protected int randomSleep() {
        return 3000 + (int) (Math.random() * 3000);
    }

    @Override
    public Site getSite() {
        return super.getSite()
                .setDomain(domain)
                .setSleepTime(randomSleep())
                .setRetryTimes(2);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (!RobotsRules.isAllowed(url)) {
            log.warn("robots.txt 不允许抓取官方公开页面，已跳过: {}", url);
            return;
        }

        Document doc = Jsoup.parse(page.getRawText(), url);
        if (isMohrssJobList(url)) {
            parseMohrssJobList(page, doc, url);
            return;
        }

        parseGenericLinks(page, doc, url);
    }

    private void parseMohrssJobList(Page page, Document doc, String url) {
        List<Element> rows = collectMohrssRows(doc);
        int parsed = 0;

        if (rows.isEmpty()) {
            log.warn("人社部列表页没有匹配到可解析的行结构: {}", url);
        }

        for (Element row : rows) {
            if (emittedCount >= maxItems) {
                break;
            }
            MohrssRow item = extractMohrssRow(row, url);
            if (item == null) {
                continue;
            }
            String sourceUrl = item.href.isEmpty()
                    ? url + "#row-" + (emittedCount + 1)
                    : item.href;
            addJob(buildMessage(sourceUrl,
                    toMohrssRawJson(item.title, item.company, item.city, item.salaryText, sourceUrl)));
            parsed++;
            emittedCount++;
        }

        if (parsed == 0) {
            parsed += fallbackByLinkScan(page, doc, url);
        }

        String nextUrl = nextMohrssPageUrl(doc, url);
        if (emittedCount < maxItems && nextUrl != null && RobotsRules.isAllowed(nextUrl)) {
            page.addTargetRequest(nextUrl);
        }

        flushJobs(page);
        log.info("人社部公开岗位采集完成: url={}, parsed={}, emitted={}/{}",
                url, parsed, emittedCount, maxItems);
    }

    private int fallbackByLinkScan(Page page, Document doc, String url) {
        Elements links = doc.select("a[href]");
        int parsed = 0;
        Set<String> seen = new LinkedHashSet<>();

        for (Element link : links) {
            if (emittedCount >= maxItems) {
                break;
            }
            String title = normalize(link.text());
            String href = link.absUrl("href");
            if (title.isEmpty() || href.isEmpty() || !seen.add(href)) {
                continue;
            }
            if (!isRecruitmentTitle(title) && !containsAny(title, CITY_PATTERN, TITLE_KEYWORDS)) {
                continue;
            }

            addJob(buildMessage(href, toGenericRawJson(title, href)));
            parsed++;
            emittedCount++;
        }

        if (parsed > 0) {
            log.info("人社部列表页启用链接兜底解析，补采 {} 条", parsed);
        }
        return parsed;
    }

    private void parseGenericLinks(Page page, Document doc, String url) {
        Elements links = doc.select("a[href]");
        int parsed = 0;
        Set<String> seenUrls = new LinkedHashSet<>();

        for (Element link : links) {
            if (parsed >= maxItems) {
                break;
            }
            String title = normalize(link.text());
            String href = link.absUrl("href");
            if (!isRecruitmentTitle(title) || href.isEmpty() || !seenUrls.add(href)) {
                continue;
            }
            addJob(buildMessage(href, toGenericRawJson(title, href)));
            parsed++;
        }

        flushJobs(page);
        log.info("官方公开页面采集完成: seed={}, parsed={}", seedUrl, parsed);
    }

    private List<Element> collectMohrssRows(Document doc) {
        for (String selector : MOHRSS_ROW_SELECTORS) {
            Elements rows = doc.select(selector);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return new ArrayList<>();
    }

    private MohrssRow extractMohrssRow(Element row, String fallbackUrl) {
        String rowText = normalize(row.text());
        if (rowText.isEmpty()) {
            return null;
        }

        Element titleLink = row.selectFirst("a[href]");
        String href = titleLink == null ? "" : titleLink.absUrl("href");
        String title = titleLink == null ? "" : normalize(titleLink.text());

        List<String> cells = new ArrayList<>();
        Elements cellElements = row.select("td, li, span, p");
        for (Element cell : cellElements) {
            String text = normalize(cell.text());
            if (!text.isEmpty()) {
                cells.add(text);
            }
        }

        if (title.isEmpty()) {
            title = guessTitle(cells, rowText);
        }
        if (title.isEmpty()) {
            return null;
        }

        String salaryText = guessSalary(cells, rowText);
        String company = guessCompany(cells, rowText);
        String city = guessCity(cells, rowText);

        if (company.isEmpty()) {
            company = sourceName;
        }
        if (city.isEmpty()) {
            city = "全国";
        }
        if (salaryText.isEmpty()) {
            salaryText = "面议";
        }
        if (href.isEmpty()) {
            href = fallbackUrl + "#row-" + Math.abs(rowText.hashCode());
        }

        return new MohrssRow(title, company, city, salaryText, href);
    }

    private String guessTitle(List<String> cells, String rowText) {
        for (String cell : cells) {
            if (isRecruitmentTitle(cell)) {
                return trimTitle(cell);
            }
        }
        for (String keyword : TITLE_KEYWORDS) {
            if (rowText.contains(keyword)) {
                return trimTitle(rowText);
            }
        }
        return "";
    }

    private String trimTitle(String text) {
        String cleaned = normalize(text);
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    private String guessSalary(List<String> cells, String rowText) {
        for (String cell : cells) {
            String salary = guessSalaryFromText(cell);
            if (!salary.isEmpty()) {
                return salary;
            }
        }
        return guessSalaryFromText(rowText);
    }

    private String guessSalaryFromText(String text) {
        Matcher matcher = SALARY_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group() : "";
    }

    private String guessCompany(List<String> cells, String rowText) {
        for (String cell : cells) {
            if (cell.length() >= 2 && !isRecruitmentTitle(cell) && guessSalaryFromText(cell).isEmpty()) {
                if (!looksLikeCity(cell)) {
                    return cell;
                }
            }
        }
        if (rowText.length() > 8) {
            return rowText;
        }
        return "";
    }

    private String guessCity(List<String> cells, String rowText) {
        for (String cell : cells) {
            String city = extractCity(cell);
            if (!"其他".equals(city)) {
                return city;
            }
        }
        return extractCity(rowText);
    }

    private boolean looksLikeCity(String text) {
        return CITY_PATTERN.matcher(text).find();
    }

    private String toMohrssRawJson(String title, String company, String city, String salaryText, String href) {
        int[] salary = parseSalaryRange(salaryText);
        JSONObject raw = new JSONObject(true);
        raw.put("title", title);
        raw.put("company", company);
        raw.put("city", city);
        raw.put("industry", inferIndustry(title));
        raw.put("salaryMin", salary[0]);
        raw.put("salaryMax", salary[1]);
        raw.put("education", extractEducation(title));
        raw.put("experience", "不限");
        raw.put("skills", extractSkills(title));
        raw.put("description", "来源于人社部公开岗位页面，岗位月薪：" + salaryText + "，地区：" + city + "，原文链接：" + href);
        raw.put("publishDate", LocalDate.now().toString());
        return raw.toJSONString();
    }

    private String toGenericRawJson(String title, String href) {
        JSONObject raw = new JSONObject(true);
        raw.put("title", title);
        raw.put("company", sourceName);
        raw.put("city", extractCity(title));
        raw.put("industry", "公共服务/事业单位");
        raw.put("salaryMin", 0);
        raw.put("salaryMax", 0);
        raw.put("education", extractEducation(title));
        raw.put("experience", "不限");
        raw.put("skills", extractSkills(title));
        raw.put("description", "来源于官方公开招聘公告列表，原文链接：" + href);
        raw.put("publishDate", LocalDate.now().toString());
        return raw.toJSONString();
    }

    private boolean isRecruitmentTitle(String title) {
        if (title == null || title.length() < 4) {
            return false;
        }
        for (String keyword : TITLE_KEYWORDS) {
            if (title.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractCity(String text) {
        Matcher matcher = CITY_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : "其他";
    }

    private String extractEducation(String title) {
        if (title.contains("博士")) return "博士";
        if (title.contains("硕士") || title.contains("研究生")) return "硕士";
        if (title.contains("本科")) return "本科";
        if (title.contains("专科") || title.contains("大专")) return "专科";
        return "不限";
    }

    private List<String> extractSkills(String title) {
        List<String> skills = new ArrayList<>();
        String lower = title.toLowerCase();
        for (String skill : SKILL_KEYWORDS) {
            if (lower.contains(skill.toLowerCase())) {
                skills.add(skill);
            }
        }
        return skills;
    }

    private String inferIndustry(String title) {
        if (title.contains("计算机") || title.contains("网络") || title.contains("软件")
                || title.contains("信息") || title.contains("通信")) {
            return "互联网/IT";
        }
        if (title.contains("数控") || title.contains("设备") || title.contains("装配")
                || title.contains("制造")) {
            return "智能制造";
        }
        return "公共服务";
    }

    private int[] parseSalaryRange(String salaryText) {
        Matcher matcher = Pattern.compile("\\d+").matcher(salaryText == null ? "" : salaryText);
        List<Integer> values = new ArrayList<>();
        while (matcher.find()) {
            try {
                values.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {
                // 保持默认 0
            }
        }
        if (values.isEmpty()) {
            return new int[]{0, 0};
        }
        int min = values.get(0);
        int max = values.get(values.size() - 1);
        return new int[]{Math.min(min, max), Math.max(min, max)};
    }

    private boolean containsAny(String text, Pattern pattern, String[] keywords) {
        if (text == null) {
            return false;
        }
        if (pattern.matcher(text).find()) {
            return true;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMohrssJobList(String url) {
        try {
            URL parsed = new URL(url);
            return MOHRSS_JOB_HOST.equalsIgnoreCase(parsed.getHost())
                    && parsed.getPath().startsWith(MOHRSS_JOB_PATH);
        } catch (Exception e) {
            return false;
        }
    }

    private String nextMohrssPageUrl(Document doc, String url) {
        int current = parseInt(doc.selectFirst("input#nowpage"), "value", 1);
        int total = parseInt(doc.selectFirst("input#allpage"), "value", current);
        if (current >= total) {
            return null;
        }
        Matcher matcher = PAGE_NO_PATTERN.matcher(url);
        if (matcher.find()) {
            return url.substring(0, matcher.start(2)) + (current + 1) + url.substring(matcher.end(2));
        }
        return url + (url.contains("?") ? "&" : "?") + "pageNo=" + (current + 1);
    }

    private int parseInt(Element element, String attr, int fallback) {
        if (element == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(element.attr(attr));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String resolveDomain(String url) {
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private static final class MohrssRow {
        private final String title;
        private final String company;
        private final String city;
        private final String salaryText;
        private final String href;

        private MohrssRow(String title, String company, String city, String salaryText, String href) {
            this.title = title;
            this.company = company;
            this.city = city;
            this.salaryText = salaryText;
            this.href = href;
        }
    }
}
