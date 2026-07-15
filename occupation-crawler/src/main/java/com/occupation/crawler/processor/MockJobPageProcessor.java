package com.occupation.crawler.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.occupation.common.dto.JobDataMessage;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模拟爬虫 — 从本地 JSON 文件读取职位数据，模拟完整采集流程
 * <p>
 * 用于开发测试阶段验证整条链路：爬虫 → Kafka → 入库。
 * 不需要访问真实招聘网站，避免反爬封禁。
 */
@Slf4j
public class MockJobPageProcessor extends JobPageProcessor {

    private static final String SOURCE = "MOCK";

    /** 模拟数据文件路径（文件系统路径或 classpath 资源路径） */
    private final String dataFilePath;

    /** 是否从 classpath 加载 */
    private final boolean fromClasspath;

    /** 每次处理的条数 */
    private final int batchSize;

    /** 批次间隔（毫秒），模拟请求延迟 */
    private final long batchIntervalMs;

    private List<JSONObject> allRecords;
    private int currentIndex = 0;

    public MockJobPageProcessor(String dataFilePath) {
        this(dataFilePath, false, 10, 1000L);
    }

    public MockJobPageProcessor(String dataFilePath, boolean fromClasspath) {
        this(dataFilePath, fromClasspath, 10, 1000L);
    }

    public MockJobPageProcessor(String dataFilePath, int batchSize, long batchIntervalMs) {
        this(dataFilePath, false, batchSize, batchIntervalMs);
    }

    public MockJobPageProcessor(String dataFilePath, boolean fromClasspath, int batchSize, long batchIntervalMs) {
        super(SOURCE);
        this.dataFilePath = dataFilePath;
        this.fromClasspath = fromClasspath;
        this.batchSize = batchSize;
        this.batchIntervalMs = batchIntervalMs;
    }

    /**
     * 初始化：加载本地 JSON 文件，构造分页请求
     */
    @Override
    public void process(Page page) {
        if (allRecords == null) {
            loadData();
        }

        // 提取当前页号
        int pageNum = extractPageNum(page.getRequest());
        int start = (pageNum - 1) * batchSize;
        int end = Math.min(start + batchSize, allRecords.size());

        if (start >= allRecords.size()) {
            page.setSkip(true);
            return;
        }

        List<JobDataMessage> jobs = new ArrayList<>();
        for (int i = start; i < end; i++) {
            JSONObject record = allRecords.get(i);
            String rawContent = JSON.toJSONString(record);

            JobDataMessage message = buildMessage(
                    record.getString("sourceUrl"),
                    rawContent
            );
            jobs.add(message);
            addJob(message);
        }

        // 将当前批次的结果放入 page，由 JobPipeline 处理
        page.putField("jobs", jobs);

        // 模拟延迟
        if (batchIntervalMs > 0) {
            try {
                Thread.sleep(batchIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("模拟采集第 {} 页完成 — 本页 {} 条，累计 {} 条",
                pageNum, jobs.size(), getCollectedCount());

        // 构造下一页请求
        if (end < allRecords.size()) {
            page.addTargetRequest(buildPageRequest(pageNum + 1));
        }
    }

    @Override
    protected String getDomain() {
        return "mock.local";
    }

    // ---- 内部方法 ----

    private void loadData() {
        try {
            String content;
            if (fromClasspath) {
                // 从 classpath 读取（兼容 JAR 包内运行）
                ClassLoader classLoader = getClass().getClassLoader();
                try (InputStream is = classLoader.getResourceAsStream(dataFilePath);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    content = reader.lines().collect(Collectors.joining("\n"));
                }
            } else {
                // 从文件系统读取
                content = new String(Files.readAllBytes(Paths.get(dataFilePath)), StandardCharsets.UTF_8);
            }
            JSONArray array = JSON.parseArray(content);
            allRecords = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                allRecords.add(array.getJSONObject(i));
            }
            log.info("模拟数据加载完成 — 共 {} 条记录", allRecords.size());
        } catch (IOException e) {
            log.error("加载模拟数据文件失败: {}", dataFilePath, e);
            allRecords = new ArrayList<>();
        }
    }

    private int extractPageNum(Request request) {
        String url = request.getUrl();
        try {
            return Integer.parseInt(url.substring(url.lastIndexOf("=") + 1));
        } catch (Exception e) {
            return 1;
        }
    }

    private Request buildPageRequest(int pageNum) {
        return new Request("http://mock.local/api/jobs?page=" + pageNum + "&size=" + batchSize);
    }

    /**
     * 生成第一页请求，供 CrawlerService 调度使用
     */
    public static Request firstPageRequest() {
        return new Request("http://mock.local/api/jobs?page=1&size=10");
    }

    /**
     * 加载并构建全部 mock 职位消息，不发送 Kafka。
     * 供 CrawlerService 同步清洗入库使用（绕开 Kafka 异步链路，采集完立即可查）。
     */
    public List<JobDataMessage> collectAll() {
        if (allRecords == null) {
            loadData();
        }
        List<JobDataMessage> jobs = new ArrayList<>();
        if (allRecords.isEmpty()) {
            log.warn("模拟数据为空，跳过采集");
            return jobs;
        }
        for (JSONObject record : allRecords) {
            String rawContent = JSON.toJSONString(record);
            JobDataMessage message = buildMessage(record.getString("sourceUrl"), rawContent);
            jobs.add(message);
            addJob(message);
        }
        return jobs;
    }

    /**
     * 直接处理所有 mock 数据（不通过 Spider 框架）
     * 加载 JSON 数据 → 构建 JobDataMessage → 通过 Pipeline 发送到 Kafka
     */
    public void processAll(JobPipeline pipeline) {
        if (allRecords == null) {
            loadData();
        }

        if (allRecords.isEmpty()) {
            log.warn("模拟数据为空，跳过采集");
            return;
        }

        List<JobDataMessage> jobs = new ArrayList<>();
        for (JSONObject record : allRecords) {
            String rawContent = JSON.toJSONString(record);
            JobDataMessage message = buildMessage(
                    record.getString("sourceUrl"),
                    rawContent
            );
            jobs.add(message);
            addJob(message);
        }

        // 通过 Pipeline 发送到 Kafka
        try {
            // 使用反射或直接调用 KafkaProducerService
            // 由于 JobPipeline.process 需要 ResultItems，这里直接构建
            us.codecraft.webmagic.ResultItems resultItems = new us.codecraft.webmagic.ResultItems();
            resultItems.put("jobs", jobs);
            pipeline.process(resultItems, null);
            log.info("Mock 采集完成 — 共 {} 条数据已发送到 Kafka", jobs.size());
        } catch (Exception e) {
            log.error("Mock 数据发送 Kafka 失败", e);
        }
    }

}
