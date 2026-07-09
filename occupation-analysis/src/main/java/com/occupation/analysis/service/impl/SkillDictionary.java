package com.occupation.analysis.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 技能词库 — 从职位描述文本中提取技能标签
 * <p>
 * 匹配规则按技能名的字符类型分两种，这是本类存在的全部理由：
 * <ul>
 *   <li><b>ASCII 技能</b>（Java、Go、C++）：必须前后无标识符字符才算命中。
 *       否则 {@code "Django"} 会命中 {@code "Go"}、{@code "JavaScript"} 会命中 {@code "Java"}。
 *       边界用 lookaround 手写而非 {@code \b}，因为 {@code \b} 处理不了 {@code C++} / {@code C#} 结尾的符号。</li>
 *   <li><b>中文技能</b>（数据分析、机器学习）：中文无词边界，直接子串匹配即可。</li>
 * </ul>
 * 词库与 init.sql 种子数据中出现的技能保持一致，新增技能只需往 {@link #CANONICAL} 里加。
 *
 * @author occupation-team
 */
final class SkillDictionary {

    private SkillDictionary() {
    }

    /** 标准技能名 → 别名（含标准名自身的其他写法）；顺序即输出顺序 */
    private static final Map<String, List<String>> CANONICAL = new LinkedHashMap<>();

    static {
        // —— 编程语言 ——
        put("Java", "Java");
        put("Python", "Python");
        put("JavaScript", "JavaScript", "JS");
        put("TypeScript", "TypeScript", "TS");
        put("Go", "Go", "Golang", "Go语言");
        put("C++", "C++");
        put("SQL", "SQL");
        // —— 框架 / 中间件 ——
        put("Spring Boot", "Spring Boot", "SpringBoot", "Spring-Boot");
        put("Vue", "Vue", "Vue.js", "Vue3", "Vue2");
        put("Redis", "Redis");
        put("MySQL", "MySQL");
        put("Kafka", "Kafka");
        put("微服务", "微服务");
        put("分布式", "分布式");
        // —— 大数据 ——
        put("Spark", "Spark");
        put("Hadoop", "Hadoop");
        put("Hive", "Hive");
        put("Flink", "Flink");
        // —— 运维 / 基础设施 ——
        put("Linux", "Linux");
        put("Docker", "Docker");
        put("Kubernetes", "Kubernetes", "K8s");
        put("Git", "Git");
        // —— 人工智能 ——
        put("机器学习", "机器学习");
        put("深度学习", "深度学习");
        put("TensorFlow", "TensorFlow");
        put("PyTorch", "PyTorch");
        put("NLP", "NLP", "自然语言处理");
        put("计算机视觉", "计算机视觉", "CV");
        // —— 前端 / 其他工程 ——
        put("CSS", "CSS");
        put("Selenium", "Selenium");
        put("Unity", "Unity");
        put("单片机", "单片机");
        put("网络安全", "网络安全");
        put("数据结构", "数据结构");
        // —— 数据 / 产品 / 通用 ——
        put("数据分析", "数据分析");
        put("数据建模", "数据建模");
        put("数据标注", "数据标注");
        put("数理统计", "数理统计");
        put("Excel", "Excel");
        put("Tableau", "Tableau");
        put("Axure", "Axure");
        put("产品设计", "产品设计");
        put("项目管理", "项目管理");
        put("用户运营", "用户运营");
        put("文案策划", "文案策划");
        put("团队协作", "团队协作");
        put("沟通能力", "沟通能力");
    }

    private static void put(String canonical, String... aliases) {
        CANONICAL.put(canonical, Arrays.asList(aliases));
    }

    /** 标准技能名 → 该技能全部别名合并成的一个正则 */
    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();

    static {
        for (Map.Entry<String, List<String>> e : CANONICAL.entrySet()) {
            List<String> alts = new ArrayList<>();
            for (String alias : e.getValue()) {
                alts.add(boundedRegex(alias));
            }
            PATTERNS.put(e.getKey(), Pattern.compile(String.join("|", alts), Pattern.CASE_INSENSITIVE));
        }
    }

    /**
     * 纯 ASCII 别名加标识符边界；含中文的别名直接字面匹配。
     * 边界字符集包含 {@code + # .}，使 "C++" 不会命中 "C++11" 之外的场景，
     * 也使 "Java" 不会命中 "JavaScript"。
     */
    private static String boundedRegex(String alias) {
        String quoted = Pattern.quote(alias);
        if (isAscii(alias)) {
            return "(?<![A-Za-z0-9+#.])" + quoted + "(?![A-Za-z0-9+#.])";
        }
        return quoted;
    }

    private static boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从文本中提取技能标签，返回标准技能名（去重，按词库顺序）。
     * text 为空时返回空集合。
     */
    static Set<String> extract(String text) {
        Set<String> hits = new LinkedHashSet<>();
        if (text == null || text.isEmpty()) {
            return hits;
        }
        for (Map.Entry<String, Pattern> e : PATTERNS.entrySet()) {
            Matcher m = e.getValue().matcher(text);
            if (m.find()) {
                hits.add(e.getKey());
            }
        }
        return hits;
    }
}
