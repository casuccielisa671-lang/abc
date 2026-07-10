package com.occupation.crawler.processor;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * robots.txt 校验
 * <p>
 * <b>WebMagic 不会自动遵守 robots.txt，本项目原先也完全没有这道关。</b>
 * 抓取前先问一句「人家允许吗」，这是最基本的礼貌，也是判断能不能抓的第一道门。
 * <p>
 * 实现刻意保守：
 * <ul>
 *   <li>只解析 {@code User-agent: *} 段落下的 {@code Disallow}，忽略 {@code Allow} 例外 ——
 *       宁可误判成「禁止」而放弃抓取，也不要误判成「允许」；</li>
 *   <li>拉不到 robots.txt（404/超时）视为<b>无限制</b>，这是通行做法；</li>
 *   <li>按域名缓存，不会每抓一页拉一次。</li>
 * </ul>
 * <p>
 * 支持 {@code *}（任意字符）与 {@code $}（结尾锚定）。例如 Boss 直聘的
 * {@code Disallow: /*?query=*} 会正确命中
 * {@code https://www.zhipin.com/web/geek/job?query=Java&city=101010100} ——
 * 这正是本项目原先 BossJobPageProcessor 拼出来的 URL，它踩在人家明令禁止的线上。
 *
 * @author occupation-team
 */
@Slf4j
public final class RobotsRules {

    /** 域名 → User-agent:* 下的 Disallow 规则（已编译成正则） */
    private static final ConcurrentHashMap<String, List<String>> CACHE = new ConcurrentHashMap<>();

    private static final int TIMEOUT_MS = 8000;

    private RobotsRules() {
    }

    /**
     * 目标 URL 是否被 robots.txt 允许抓取。
     *
     * @return true=允许（含拉不到 robots.txt 的情况）
     */
    public static boolean isAllowed(String url) {
        try {
            URL u = new URL(url);
            String origin = u.getProtocol() + "://" + u.getHost();
            // 用 getFile() 而不是 getPath()：Disallow 经常针对查询串，如 /*?query=*
            String path = u.getFile().isEmpty() ? "/" : u.getFile();

            for (String pattern : rulesOf(origin)) {
                if (path.matches(pattern)) {
                    log.warn("robots.txt 禁止抓取: {} 命中规则 {}", path, pattern);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("robots.txt 校验异常，保守起见按禁止处理: {} — {}", url, e.getMessage());
            return false;
        }
    }

    /** 清空缓存，仅供测试使用 */
    static void clearCache() {
        CACHE.clear();
    }

    private static List<String> rulesOf(String origin) {
        return CACHE.computeIfAbsent(origin, RobotsRules::fetchAndParse);
    }

    private static List<String> fetchAndParse(String origin) {
        List<String> disallows = new ArrayList<>();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(origin + "/robots.txt").openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; OccupationBot/1.0)");
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.info("{} 没有可用的 robots.txt（HTTP {}），视为无限制", origin, conn.getResponseCode());
                return disallows;
            }

            boolean inWildcardGroup = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = stripComment(line).trim();
                    int colon = line.indexOf(':');
                    if (line.isEmpty() || colon < 0) {
                        continue;
                    }
                    String key = line.substring(0, colon).trim().toLowerCase();
                    String value = line.substring(colon + 1).trim();

                    if ("user-agent".equals(key)) {
                        // 遇到新的 User-agent 段落，重新判断是不是 "*"
                        inWildcardGroup = "*".equals(value);
                    } else if (inWildcardGroup && "disallow".equals(key) && !value.isEmpty()) {
                        disallows.add(toRegex(value));
                    }
                }
            }
            log.info("{} 的 robots.txt 已解析，User-agent:* 下有 {} 条 Disallow", origin, disallows.size());
        } catch (Exception e) {
            log.info("拉取 {} 的 robots.txt 失败（{}），视为无限制", origin, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return disallows;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    /**
     * robots 路径模式 → 正则。
     * <p>
     * robots 的语义是<b>前缀匹配</b>，所以除非以 {@code $} 结尾锚定，否则末尾要补 {@code .*}。
     */
    static String toRegex(String pattern) {
        boolean anchored = pattern.endsWith("$");
        String p = anchored ? pattern.substring(0, pattern.length() - 1) : pattern;

        StringBuilder sb = new StringBuilder();
        for (char c : p.toCharArray()) {
            if (c == '*') {
                sb.append(".*");
            } else if ("\\.[]{}()+-?^$|".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return anchored ? sb.toString() : sb.append(".*").toString();
    }
}
