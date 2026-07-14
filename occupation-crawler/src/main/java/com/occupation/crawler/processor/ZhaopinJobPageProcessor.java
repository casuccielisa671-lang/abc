package com.occupation.crawler.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.occupation.common.dto.JobDataMessage;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智联招聘采集器 —— 解析列表页里服务端注入的 {@code window.__INITIAL_STATE__}
 * <p>
 * <b>为什么不解析 HTML 标签</b>：写死 XPath 的做法在页面改版当天就失效。智联把整页数据以
 * JSON 形式塞在 {@code __INITIAL_STATE__.positionList} 里，字段齐全（含 industryName /
 * publishTime / jobSkillTags / salaryReal），只要这个数据结构不变，换皮不影响解析。
 * <p>
 * <b>只抓列表页，不进详情页。</b>列表页的公开字段已经够用；详情页可能带出 HR 手机号一类的
 * 个人信息，抓下来就会存进 {@code raw_job_data.raw_content}。从源头避免。
 * <p>
 * <b>合规</b>：
 * <ul>
 *   <li>抓取前用 {@link RobotsRules} 校验 robots.txt，入口与 301 跳转目标都要通过；</li>
 *   <li>{@code www.zhaopin.com/robots.txt} 有 {@code Disallow: /*?*}（禁一切带查询串的 URL），
 *       而跳转后的路径式地址 {@code /sou/jl653/kwXXXX} 不带查询串，故被允许 ——
 *       这正是必须先把 301 解开的原因；</li>
 *   <li>单线程 + 5~10 秒随机间隔，见 {@link #randomSleep()}。</li>
 * </ul>
 *
 * @author occupation-team
 */
@Slf4j
public class ZhaopinJobPageProcessor extends JobPageProcessor {

    private static final String SOURCE = "ZHAOPIN";
    private static final String DOMAIN = "www.zhaopin.com";

    /** 入口：kw 是明文关键词，jl 是城市编码（653=杭州）。它会 301 到路径式 URL */
    private static final String ENTRY = "https://sou.zhaopin.com/?kw=%s&jl=%s";

    /** 服务端注入的状态对象，整页数据都在里面 */
    private static final Pattern STATE_PATTERN =
            Pattern.compile("__INITIAL_STATE__\\s*=\\s*(\\{.*)", Pattern.DOTALL);

    /** 路径式 URL 末尾的页码，如 /sou/jl653/kw01500O80EO062/p2 */
    private static final Pattern PAGE_SUFFIX = Pattern.compile("/p(\\d+)$");

    /** salaryReal 的形态：11001-18000 */
    private static final Pattern SALARY_REAL = Pattern.compile("^(\\d+)-(\\d+)$");

    /**
     * 智联薪资展示串写法多样：{@code 1.5万-2万}、{@code 8千-1.2万}、{@code 15K-25K·13薪}。
     * 捕获「数字 + 可选单位」两段；前段单位缺失时沿用后段单位（如「1.5-2万」）。
     */
    private static final Pattern SALARY_TEXT =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([万千kK]?)\\s*[-~至]\\s*(\\d+(?:\\.\\d+)?)\\s*([万千kK]?)");

    private final int maxPages;

    public ZhaopinJobPageProcessor(String keyword, String cityCode, int maxPages) {
        super(SOURCE);
        this.maxPages = Math.max(1, maxPages);
        log.info("智联采集器初始化: keyword={}, cityCode={}, maxPages={}", keyword, cityCode, this.maxPages);
    }

    @Override
    protected String getDomain() {
        return DOMAIN;
    }

    /** 真实站点要抓得慢。5~10 秒一次，配合单线程，对目标站点的压力可以忽略 */
    @Override
    protected int randomSleep() {
        return 5000 + (int) (Math.random() * 5000);
    }

    @Override
    public Site getSite() {
        return super.getSite().setDomain(DOMAIN);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (!RobotsRules.isAllowed(url)) {
            log.error("robots.txt 不允许抓取，已跳过: {}", url);
            return;
        }

        JSONObject state = extractState(page.getRawText());
        if (state == null) {
            log.warn("页面里没有 __INITIAL_STATE__，可能被风控拦截或页面已改版: {}", url);
            return;
        }

        JSONArray positions = state.getJSONArray("positionList");
        if (positions == null || positions.isEmpty()) {
            log.info("本页没有职位数据，采集结束: {}", url);
            return;
        }

        int parsed = 0;
        for (int i = 0; i < positions.size(); i++) {
            JobDataMessage msg = toMessage(positions.getJSONObject(i));
            if (msg != null) {
                addJob(msg);
                parsed++;
            }
        }
        log.info("智联采集: {} 解析 {}/{} 条", url, parsed, positions.size());

        flushJobs(page);
        addNextPage(page, url);
    }

    /** 翻页：路径式 URL 末尾追加或递增 /pN */
    private void addNextPage(Page page, String url) {
        int current = 1;
        String base = url;
        Matcher m = PAGE_SUFFIX.matcher(url);
        if (m.find()) {
            current = Integer.parseInt(m.group(1));
            base = url.substring(0, m.start());
        }
        if (current >= maxPages) {
            return;
        }
        String next = base + "/p" + (current + 1);
        if (RobotsRules.isAllowed(next)) {
            page.addTargetRequest(next);
        }
    }

    /**
     * positionList 里的一条 → 清洗链路认识的 JobDataMessage。
     * <p>
     * 键名必须与 {@code DataCleanServiceImpl.cleanAndSave} 读取的一致：title / company /
     * city / industry / salaryMin / salaryMax / education / experience / skills /
     * description / publishDate。
     */
    private JobDataMessage toMessage(JSONObject p) {
        String title = p.getString("name");
        String company = p.getString("companyName");
        String positionUrl = p.getString("positionURL");
        // 清洗环节会丢弃缺标题或公司的记录，这里提前跳过，省得脏数据白跑一趟 Kafka
        if (isBlank(title) || isBlank(company) || isBlank(positionUrl)) {
            return null;
        }

        int[] salary = resolveSalary(p);

        JSONObject raw = new JSONObject(true);
        raw.put("title", title);
        raw.put("company", company);
        raw.put("city", p.getString("workCity"));
        raw.put("industry", p.getString("industryName"));
        raw.put("salaryMin", salary[0]);
        raw.put("salaryMax", salary[1]);
        raw.put("education", p.getString("education"));
        raw.put("experience", p.getString("workingExp"));
        raw.put("skills", skillsOf(p));
        // 列表页的 jobSummary 常为空。留空不影响：SkillDictionary 会从标题里补技能标签
        raw.put("description", nullToEmpty(p.getString("jobSummary")));
        raw.put("publishDate", dateOf(p.getString("publishTime")));

        return buildMessage(positionUrl, raw.toJSONString());
    }

    /**
     * 薪资：优先用 {@code salaryReal}（"11001-18000"，纯数字区间），
     * 缺失时退回解析 {@code salary60}（"1.1-1.8万·13薪" 这类展示串）。
     * 「面议」两者都给不出数字，返回 {@code {0,0}}，与 MOCK 数据的处理一致。
     */
    private int[] resolveSalary(JSONObject p) {
        String real = p.getString("salaryReal");
        if (real != null) {
            Matcher m = SALARY_REAL.matcher(real.trim());
            if (m.matches()) {
                return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
            }
        }
        return parseSalary(p.getString("salary60"));
    }

    /** jobSkillTags 优先（结构化），退回 skillLabel */
    private List<String> skillsOf(JSONObject p) {
        List<String> skills = new ArrayList<>();
        JSONArray tags = p.getJSONArray("jobSkillTags");
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                String name = tags.getJSONObject(i).getString("name");
                if (!isBlank(name)) {
                    skills.add(name);
                }
            }
        }
        if (skills.isEmpty()) {
            JSONArray labels = p.getJSONArray("skillLabel");
            if (labels != null) {
                for (int i = 0; i < labels.size(); i++) {
                    String v = labels.getJSONObject(i).getString("value");
                    if (!isBlank(v)) {
                        skills.add(v);
                    }
                }
            }
        }
        return skills;
    }

    /** "2026-07-10 11:04:36" → "2026-07-10" */
    private String dateOf(String publishTime) {
        if (isBlank(publishTime)) {
            return null;
        }
        int space = publishTime.indexOf(' ');
        return space > 0 ? publishTime.substring(0, space) : publishTime;
    }

    /**
     * 从整页 HTML 里抠出 {@code __INITIAL_STATE__} 的 JSON。
     * <p>
     * 大括号配平截取，而不是贪婪正则 —— 后面还有别的 script，正则会一路吃到页尾。
     */
    JSONObject extractState(String html) {
        if (isBlank(html)) {
            return null;
        }
        Matcher m = STATE_PATTERN.matcher(html);
        if (!m.find()) {
            return null;
        }
        String tail = m.group(1);
        int depth = 0;
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                try {
                    return JSON.parseObject(tail.substring(0, i + 1));
                } catch (Exception e) {
                    log.warn("__INITIAL_STATE__ 解析失败: {}", e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    // ==================== 入口构造 ====================

    /**
     * 构造种子请求。
     * <p>
     * 必须先把 {@code sou.zhaopin.com/?kw=X&jl=Y} 的 301 解开：目标域 {@code www.zhaopin.com}
     * 的 robots.txt 禁止一切带查询串的 URL，而跳转后的路径式地址不带查询串，才是被允许的那个。
     *
     * @return 已解析到最终地址的请求；robots 不允许时返回 {@code null}，调用方应放弃本次采集
     */
    public static Request seedRequest(String keyword, String cityCode) {
        String entry = String.format(ENTRY, encode(keyword), encode(cityCode));
        if (!RobotsRules.isAllowed(entry)) {
            log.error("入口地址被 robots.txt 禁止: {}", entry);
            return null;
        }

        String resolved = resolveRedirect(entry);
        if (resolved == null) {
            log.warn("无法解析 301 目标，退回入口地址（只能抓一页）: {}", entry);
            resolved = entry;
        }
        if (!RobotsRules.isAllowed(resolved)) {
            log.error("跳转目标被 robots.txt 禁止: {}", resolved);
            return null;
        }
        log.info("智联采集入口: {} → {}", entry, resolved);
        return new Request(resolved);
    }

    /** 只发一次请求，不跟随跳转，读 Location 头 */
    private static String resolveRedirect(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36");
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                return conn.getHeaderField("Location");
            }
            // 没有跳转也算成功：直接抓入口地址
            return code == HttpURLConnection.HTTP_OK ? url : null;
        } catch (Exception e) {
            log.warn("解析跳转失败: {} — {}", url, e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    // ==================== 薪资展示串解析（salaryReal 缺失时的兜底） ====================

    /**
     * 解析「1.5万-2万」「8千-1.2万」「15K-25K·13薪」这类展示串。
     * 「面议」/空值返回 {@code {0,0}}。
     * <p>
     * 包级可见，由 {@code ZhaopinSalaryParseTest} 直接测试。
     */
    int[] parseSalary(String text) {
        if (isBlank(text)) {
            return new int[]{0, 0};
        }
        Matcher m = SALARY_TEXT.matcher(text);
        if (!m.find()) {
            return new int[]{0, 0};
        }
        String minUnit = m.group(2);
        String maxUnit = m.group(4);
        // 前段省略单位时沿用后段单位：「1.5-2万」的 1.5 也是万
        if (minUnit.isEmpty()) {
            minUnit = maxUnit;
        }
        return new int[]{toYuan(m.group(1), minUnit), toYuan(m.group(3), maxUnit)};
    }

    private int toYuan(String num, String unit) {
        double v = Double.parseDouble(num);
        if ("万".equals(unit)) {
            v *= 10000;
        } else if ("千".equals(unit) || "k".equalsIgnoreCase(unit)) {
            v *= 1000;
        }
        return (int) Math.round(v);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
