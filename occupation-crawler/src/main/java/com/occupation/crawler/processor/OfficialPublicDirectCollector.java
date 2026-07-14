package com.occupation.crawler.processor;

import com.alibaba.fastjson.JSONObject;
import com.occupation.common.dto.JobDataMessage;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small synchronous collector for official public job pages.
 * Each call to collect() only parses the current page.
 * Pagination is handled externally by updating the pageNo in urlPattern after each run.
 */
@Slf4j
public class OfficialPublicDirectCollector {

    private static final String SOURCE = "OFFICIAL_PUBLIC";
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final Pattern SALARY_PATTERN = Pattern.compile("\\d+");
    private static final String[] SKILLS = {
            "Java", "Python", "Spring", "Vue", "React", "SQL", "MySQL", "Redis", "Docker",
            "Linux", "数据", "算法", "前端", "后端", "测试", "运维", "网络", "安全", "计算机"
    };

    private final String sourceName;
    private final int maxItems;

    public OfficialPublicDirectCollector(String sourceName, int maxItems) {
        this.sourceName = sourceName == null || sourceName.trim().isEmpty() ? "官方公开招聘" : sourceName.trim();
        this.maxItems = Math.max(1, maxItems);
    }

    public List<JobDataMessage> collect(String url) {
        String targetUrl = url == null ? "" : url.trim();
        if (targetUrl.isEmpty()) {
            throw new IllegalArgumentException("采集 URL 为空");
        }
        if (!RobotsRules.isAllowed(targetUrl)) {
            throw new IllegalStateException("目标站点 robots.txt 不允许抓取：" + targetUrl);
        }

        // 每次只采集当前页（不翻页），翻页由调用方通过更新 urlPattern 中的 pageNo 自增实现
        log.info("直采 当前页: {}", targetUrl);

        String html = fetchHtml(targetUrl);
        Document doc = Jsoup.parse(html, targetUrl);
        List<JobDataMessage> jobs = parseMohrssRows(doc, targetUrl);
        if (jobs.isEmpty()) {
            jobs = parseLinks(doc, targetUrl);
        }

        // 截断到 maxItems
        if (jobs.size() > maxItems) {
            jobs = jobs.subList(0, maxItems);
        }

        // 打印每条岗位的 sourceUrl 和 title，方便排查入库问题
        for (int i = 0; i < jobs.size(); i++) {
            JobDataMessage job = jobs.get(i);
            log.info("直采解析[{}] title={}, sourceUrl={}", i + 1, job.getRawContent().substring(0, Math.min(80, job.getRawContent().length())), job.getSourceUrl());
        }

        log.info("官方公开岗位直采完成 url={}, parsed={}", targetUrl, jobs.size());
        return jobs;
    }

    private String fetchHtml(String targetUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("目标网页返回 HTTP " + code);
            }
            try (InputStream input = connection.getInputStream()) {
                byte[] bytes = readAll(input);
                return new String(bytes, resolveCharset(connection.getContentType()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("访问目标网页失败：" + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private Charset resolveCharset(String contentType) {
        if (contentType != null) {
            Matcher matcher = Pattern.compile("charset=([^;]+)", Pattern.CASE_INSENSITIVE).matcher(contentType);
            if (matcher.find()) {
                try {
                    return Charset.forName(matcher.group(1).trim());
                } catch (Exception ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private List<JobDataMessage> parseMohrssRows(Document doc, String pageUrl) {
        Elements rows = doc.select("ul.list_show, .list_show, table tbody tr, tbody tr");
        List<JobDataMessage> jobs = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        // 去掉 pageUrl 中已有的锚点，用于后续拼接
        String basePageUrl = pageUrl.replaceAll("#.*$", "");
        for (Element row : rows) {
            if (jobs.size() >= maxItems) {
                break;
            }
            Element link = row.selectFirst("a[href]");
            String title = link == null ? firstUsefulText(row) : clean(link.text());
            if (title.isEmpty() || title.length() < 2) {
                continue;
            }
            // 过滤分页导航等非岗位行：标题包含"首页""上一页""下一页""尾页""当前"等分页关键词则跳过
            if (containsAny(title, "首页", "上一页", "下一页", "尾页", "当前", "共", "条")) {
                continue;
            }
            // 生成唯一 sourceUrl：优先用链接的绝对地址，但如果链接指向的是列表页本身（与 pageUrl 相同），
            // 则用 pageUrl + 行内 hash 来区分每条岗位
            String sourceUrl;
            if (link != null) {
                String absHref = link.absUrl("href").replaceAll("#.*$", "");
                if (absHref.equals(basePageUrl)) {
                    // 链接指向列表页自身，用行内容 hash 生成唯一标识
                    sourceUrl = basePageUrl + "#row-" + Math.abs(row.text().hashCode());
                } else {
                    sourceUrl = link.absUrl("href");
                }
            } else {
                sourceUrl = basePageUrl + "#row-" + Math.abs(row.text().hashCode());
            }
            if (!seen.add(sourceUrl)) {
                continue;
            }
            String rowText = clean(row.text());
            String company = pickByIndex(row, 1, sourceName);
            String city = pickCity(rowText);
            String salaryText = pickByIndex(row, 2, "面议");
            jobs.add(toMessage(sourceUrl, toRawJson(title, company, city, salaryText, rowText, sourceUrl)));
        }
        return jobs;
    }

    private List<JobDataMessage> parseLinks(Document doc, String pageUrl) {
        List<JobDataMessage> jobs = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element link : doc.select("a[href]")) {
            if (jobs.size() >= maxItems) {
                break;
            }
            String title = clean(link.text());
            if (!looksLikeJob(title)) {
                continue;
            }
            String sourceUrl = link.absUrl("href");
            if (sourceUrl.isEmpty() || !seen.add(sourceUrl)) {
                continue;
            }
            jobs.add(toMessage(sourceUrl, toRawJson(title, sourceName, pickCity(title), "面议", title, sourceUrl)));
        }
        return jobs;
    }

    private JobDataMessage toMessage(String sourceUrl, String rawContent) {
        return JobDataMessage.builder()
                .source(SOURCE)
                .sourceUrl(sourceUrl)
                .rawContent(rawContent)
                .fetchTime(LocalDateTime.now())
                .build();
    }

    private String toRawJson(String title, String company, String city, String salaryText, String text, String sourceUrl) {
        int[] salary = parseSalary(salaryText);
        JSONObject raw = new JSONObject(true);
        raw.put("title", title);
        raw.put("company", company == null || company.trim().isEmpty() ? sourceName : company);
        raw.put("city", city);
        raw.put("industry", inferIndustry(title + " " + text));
        raw.put("salaryMin", salary[0]);
        raw.put("salaryMax", salary[1]);
        raw.put("education", inferEducation(text));
        raw.put("experience", "不限");
        raw.put("skills", extractSkills(title + " " + text));
        raw.put("description", "来源于官方公开招聘页面，原文链接：" + sourceUrl + "。页面摘要：" + text);
        raw.put("publishDate", LocalDate.now().toString());
        return raw.toJSONString();
    }

    private String firstUsefulText(Element row) {
        for (Element item : row.select("li, td, span, p")) {
            String text = clean(item.text());
            if (!text.isEmpty()) {
                return text;
            }
        }
        return clean(row.text());
    }

    private String pickByIndex(Element row, int index, String fallback) {
        List<String> parts = new ArrayList<>();
        for (Element item : row.select("li, td, span, p")) {
            String text = clean(item.text());
            if (!text.isEmpty()) {
                parts.add(text);
            }
        }
        return parts.size() > index ? parts.get(index) : fallback;
    }

    private String pickCity(String text) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "苏州", "成都", "武汉", "西安",
                "长沙", "郑州", "济南", "青岛", "宁波", "厦门", "合肥", "福州", "南昌", "昆明",
                "贵阳", "南宁", "海口", "太原", "石家庄", "沈阳", "大连", "长春", "哈尔滨", "湖北"};
        for (String city : cities) {
            if (text != null && text.contains(city)) {
                return city;
            }
        }
        return "全国";
    }

    private int[] parseSalary(String salaryText) {
        Matcher matcher = SALARY_PATTERN.matcher(salaryText == null ? "" : salaryText);
        List<Integer> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(Integer.parseInt(matcher.group()));
        }
        if (values.isEmpty()) {
            return new int[]{0, 0};
        }
        int min = values.get(0);
        int max = values.get(values.size() - 1);
        return new int[]{Math.min(min, max), Math.max(min, max)};
    }

    private String inferIndustry(String text) {
        if (containsAny(text, "计算机", "软件", "网络", "信息", "数据", "人工智能", "通信")) {
            return "互联网/IT";
        }
        if (containsAny(text, "教师", "高校", "学院", "大学")) {
            return "教育/科研";
        }
        return "公共服务";
    }

    private String inferEducation(String text) {
        if (containsAny(text, "博士")) return "博士";
        if (containsAny(text, "硕士", "研究生")) return "硕士";
        if (containsAny(text, "本科")) return "本科";
        if (containsAny(text, "专科", "大专")) return "专科";
        return "不限";
    }

    private List<String> extractSkills(String text) {
        List<String> skills = new ArrayList<>();
        for (String skill : SKILLS) {
            if (text != null && text.toLowerCase().contains(skill.toLowerCase())) {
                skills.add(skill);
            }
        }
        return skills;
    }

    private boolean looksLikeJob(String title) {
        return title != null && title.length() >= 4
                && containsAny(title, "招聘", "岗位", "教师", "工程师", "计算机", "人才");
    }

    private boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
