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
 * 官方公开招聘公告采集器。
 *
 * <p>定位：抓取政府/高校/公共就业服务等公开页面中的招聘公告链接，作为平台演示和分析数据集补充。
 * 为了稳妥合规，本处理器只解析公开列表页的链接标题，不进入附件、不采集联系方式、不绕过登录或验证码。</p>
 */
@Slf4j
public class OfficialPublicJobProcessor extends JobPageProcessor {

    private static final String SOURCE = "OFFICIAL_PUBLIC";
    private static final String MOHRSS_JOB_HOST = "job.mohrss.gov.cn";
    private static final String MOHRSS_JOB_PATH = "/cjobs/lkysudi";
    private static final Pattern PAGE_NO_PATTERN = Pattern.compile("([?&]pageNo=)(\\d+)");
    private static final Pattern CITY_PATTERN = Pattern.compile(
            "(北京|上海|天津|重庆|杭州|广州|深圳|南京|苏州|成都|武汉|西安|长沙|郑州|济南|青岛|宁波|厦门|合肥|福州|南昌|昆明|贵阳|南宁|海口|太原|石家庄|沈阳|大连|长春|哈尔滨)");
    private static final String[] TITLE_KEYWORDS = {
            "招聘", "招录", "招考", "人才", "岗位", "事业单位", "教师", "工程师", "实习"
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
        this.seedUrl = seedUrl;
        this.sourceName = sourceName == null || sourceName.trim().isEmpty() ? "官方公开招聘公告" : sourceName.trim();
        this.maxItems = Math.max(1, maxItems);
        this.domain = resolveDomain(seedUrl);
    }

    @Override
    protected String getDomain() {
        return domain;
    }

    @Override
    protected int randomSleep() {
        return 8000 + (int) (Math.random() * 5000);
    }

    @Override
    public Site getSite() {
        return super.getSite().setDomain(domain).setSleepTime(randomSleep()).setRetryTimes(2);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (!RobotsRules.isAllowed(url)) {
            log.warn("robots.txt 不允许抓取官方公开页，已跳过: {}", url);
            return;
        }

        Document doc = Jsoup.parse(page.getRawText(), url);
        if (isMohrssJobList(url)) {
            parseMohrssJobList(page, doc, url);
            return;
        }

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
            JobDataMessage message = buildMessage(href, toRawJson(title, href));
            addJob(message);
            parsed++;
        }
        flushJobs(page);
        log.info("官方公开招聘公告采集完成: seed={}, parsed={}", seedUrl, parsed);
    }

    /**
     * 人社部高校毕业生就业服务平台岗位列表页：
     * http://job.mohrss.gov.cn/cjobs/lkysudi?pageNo=1&GJ=&job=计算机
     *
     * <p>该页面不是“招聘公告标题列表”，而是岗位表格。每一行依次包含：
     * 岗位名称、岗位月薪、单位名称、地区。因此这里单独解析表格行，
     * 再转成下游清洗服务已经支持的标准职位 JSON。</p>
     */
    private void parseMohrssJobList(Page page, Document doc, String url) {
        Elements rows = doc.select(".list_list ul.list_show");
        int parsed = 0;

        for (Element row : rows) {
            if (emittedCount >= maxItems) {
                break;
            }
            Elements cells = row.select("li");
            if (cells.size() < 4) {
                continue;
            }

            Element titleLink = cells.get(0).selectFirst("a[href]");
            String title = normalize(titleLink != null ? titleLink.text() : cells.get(0).text());
            String href = titleLink == null ? "" : titleLink.absUrl("href");
            String salaryText = cellValue(cells.get(1));
            String company = cellValue(cells.get(2));
            String city = cellValue(cells.get(3));

            if (title.isEmpty() || company.isEmpty()) {
                continue;
            }

            String sourceUrl = href.isEmpty() ? url + "#row-" + (emittedCount + 1) : href;
            JobDataMessage message = buildMessage(sourceUrl,
                    toMohrssRawJson(title, company, city, salaryText, sourceUrl));
            addJob(message);
            parsed++;
            emittedCount++;
        }

        String nextUrl = nextMohrssPageUrl(doc, url);
        if (emittedCount < maxItems && nextUrl != null && RobotsRules.isAllowed(nextUrl)) {
            page.addTargetRequest(nextUrl);
        }

        flushJobs(page);
        log.info("人社部公开岗位列表采集完成: url={}, parsed={}, emitted={}/{}",
                url, parsed, emittedCount, maxItems);
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
        raw.put("description", "来源于人社部高校毕业生就业服务平台公开岗位列表；岗位月薪："
                + salaryText + "；地区：" + city + "；原文链接：" + href);
        raw.put("publishDate", LocalDate.now().toString());
        return raw.toJSONString();
    }

    private String toRawJson(String title, String href) {
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
        if (title.length() < 6) {
            return false;
        }
        for (String keyword : TITLE_KEYWORDS) {
            if (title.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractCity(String title) {
        Matcher matcher = CITY_PATTERN.matcher(title);
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
        return "公共就业服务";
    }

    private int[] parseSalaryRange(String salaryText) {
        Matcher matcher = Pattern.compile("\\d+").matcher(salaryText == null ? "" : salaryText);
        List<Integer> values = new ArrayList<>();
        while (matcher.find()) {
            try {
                values.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {
                // 跳过异常数字片段，保留默认 0 值。
            }
        }
        if (values.isEmpty()) {
            return new int[]{0, 0};
        }
        int min = values.get(0);
        int max = values.get(values.size() - 1);
        return new int[]{Math.min(min, max), Math.max(min, max)};
    }

    private String cellValue(Element cell) {
        String title = normalize(cell.attr("title"));
        return title.isEmpty() ? normalize(cell.text()) : title;
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
            return matcher.replaceFirst("$1" + (current + 1));
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
}
