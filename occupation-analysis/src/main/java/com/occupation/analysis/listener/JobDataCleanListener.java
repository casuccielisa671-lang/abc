package com.occupation.analysis.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.occupation.analysis.service.DataCleanService;
import com.occupation.common.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 职位数据清洗监听器 — 数据管道第二环
 * <p>
 * 与 common 模块的 KafkaConsumerService（负责原始数据归档到 raw_job_data）
 * 使用不同的 groupId 并行消费同一 Topic：
 * <pre>
 * 爬虫 → Kafka(raw-job-data) ─┬→ data-cleaner-group  → raw_job_data（原始归档，common）
 *                             └→ job-clean-group     → job_detail （清洗入库，本类）
 * </pre>
 * 好处：清洗逻辑故障不影响原始数据落地，可随时用存量补偿任务重放。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobDataCleanListener {

    private final DataCleanService dataCleanService;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_RAW_JOB_DATA,
                   groupId = "job-clean-group",
                   concurrency = "2")
    public void onMessage(String messageJson) {
        try {
            JSONObject msg = JSON.parseObject(messageJson);
            dataCleanService.cleanAndSave(
                    msg.getString("rawContent"),
                    msg.getString("source"),
                    msg.getString("sourceUrl"));
        } catch (Exception e) {
            // 只记录不抛出：单条脏数据不应阻塞消费位点
            log.error("清洗消息处理失败: {}", messageJson, e);
        }
    }
}
