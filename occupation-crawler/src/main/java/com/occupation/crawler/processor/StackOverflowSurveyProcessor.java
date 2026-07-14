package com.occupation.crawler.processor;

import com.alibaba.fastjson.JSONObject;
import com.occupation.common.dto.JobDataMessage;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stack Overflow Developer Survey CSV 采集器。
 *
 * <p>年度调查不是招聘岗位，因此这里把每条有效答卷转成“开发者生态样本”：
 * DevType -> 职位标题，技术栈列 -> skills，ConvertedCompYearly -> 月薪估算。
 * 这样不新增表结构，也能直接进入现有 Dashboard 的技能热度、薪资趋势与行业统计。</p>
 */
@Slf4j
public class StackOverflowSurveyProcessor extends JobPageProcessor {

    private static final String SOURCE = "STACK_OVERFLOW_SURVEY";

    private final String csvUrl;
    private final int maxItems;
    private final String domain;

    public StackOverflowSurveyProcessor(String csvUrl, int maxItems) {
        super(SOURCE);
        this.csvUrl = csvUrl;
        this.maxItems = Math.max(1, maxItems);
        this.domain = resolveDomain(csvUrl);
    }

    @Override
    protected String getDomain() {
        return domain;
    }

    @Override
    protected int randomSleep() {
        return 1000;
    }

    @Override
    public Site getSite() {
        return super.getSite().setDomain(domain).setSleepTime(randomSleep()).setRetryTimes(2).setTimeOut(30000);
    }

    @Override
    public void process(Page page) {
        List<List<String>> rows = parseCsv(page.getRawText());
        if (rows.size() < 2) {
            flushJobs(page);
            log.warn("Stack Overflow 调查 CSV 为空或格式异常: {}", csvUrl);
            return;
        }

        Map<String, Integer> header = headerIndex(rows.get(0));
        int parsed = 0;
        for (int i = 1; i < rows.size() && parsed < maxItems; i++) {
            List<String> row = rows.get(i);
            String title = firstValue(row, header, "DevType", "DeveloperType");
            List<String> skills = collectSkills(row, header);
            if (title.isEmpty() || skills.isEmpty()) {
                continue;
            }

            String sourceUrl = csvUrl + "#row-" + (i + 1);
            JobDataMessage message = buildMessage(sourceUrl, toRawJson(row, header, title, skills, sourceUrl));
            addJob(message);
            parsed++;
        }

        flushJobs(page);
        log.info("Stack Overflow 调查 CSV 采集完成: url={}, parsed={}", csvUrl, parsed);
    }

    private String toRawJson(List<String> row, Map<String, Integer> header,
                             String title, List<String> skills, String sourceUrl) {
        Integer monthlySalary = monthlySalary(row, header);
        JSONObject raw = new JSONObject(true);
        raw.put("title", title);
        raw.put("company", "Stack Overflow Developer Survey");
        raw.put("city", "全球");
        raw.put("industry", "开发者生态");
        raw.put("salaryMin", monthlySalary == null ? 0 : monthlySalary);
        raw.put("salaryMax", monthlySalary == null ? 0 : monthlySalary);
        raw.put("education", normalizeEducation(firstValue(row, header, "EdLevel")));
        raw.put("experience", firstValue(row, header, "YearsCodePro", "YearsCode"));
        raw.put("skills", skills);
        raw.put("description", "来源于 Stack Overflow Developer Survey CSV，用于补充技术栈分布与开发者薪资趋势；原始位置：" + sourceUrl);
        raw.put("publishDate", LocalDate.now().toString());
        return raw.toJSONString();
    }

    private List<String> collectSkills(List<String> row, Map<String, Integer> header) {
        List<String> skills = new ArrayList<>();
        String[] columns = {
                "LanguageHaveWorkedWith",
                "DatabaseHaveWorkedWith",
                "PlatformHaveWorkedWith",
                "WebframeHaveWorkedWith",
                "ToolsTechHaveWorkedWith",
                "NEWCollabToolsHaveWorkedWith"
        };
        for (String column : columns) {
            for (String skill : firstValue(row, header, column).split(";")) {
                addDistinct(skills, skill);
                if (skills.size() >= 12) {
                    return skills;
                }
            }
        }
        return skills;
    }

    private void addDistinct(List<String> values, String value) {
        String cleaned = value == null ? "" : value.trim();
        if (!cleaned.isEmpty() && values.stream().noneMatch(v -> v.equalsIgnoreCase(cleaned))) {
            values.add(cleaned);
        }
    }

    private Integer monthlySalary(List<String> row, Map<String, Integer> header) {
        String annual = firstValue(row, header, "ConvertedCompYearly", "CompTotal");
        try {
            int value = (int) Math.round(Double.parseDouble(annual) / 12D);
            return value > 0 && value < 1_000_000 ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeEducation(String edLevel) {
        if (edLevel.contains("Bachelor")) return "本科";
        if (edLevel.contains("Master")) return "硕士";
        if (edLevel.contains("Doctoral") || edLevel.contains("Ph.D")) return "博士";
        if (edLevel.contains("Associate")) return "专科";
        return "不限";
    }

    private String firstValue(List<String> row, Map<String, Integer> header, String... names) {
        for (String name : names) {
            Integer index = header.get(name);
            if (index != null && index < row.size()) {
                String value = row.get(index).trim();
                if (!value.isEmpty() && !"NA".equalsIgnoreCase(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private Map<String, Integer> headerIndex(List<String> headerRow) {
        Map<String, Integer> header = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            header.put(headerRow.get(i).trim(), i);
        }
        return header;
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    private String resolveDomain(String url) {
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }
}
