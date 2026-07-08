package com.occupation.common.service;

import com.occupation.common.config.KafkaTopicConfig;
import com.occupation.common.dto.JobDataMessage;
import com.occupation.common.entity.RawJobData;
import com.occupation.common.mapper.RawJobDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Kafka 消费者服务
 * <p>
 * 监听 raw-job-data Topic，接收爬虫发送的原始职位数据并持久化到数据库。
 * 消峰填谷：爬虫突发采集高峰时，消息暂存 Kafka，消费者平滑写入 MySQL。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final RawJobDataMapper rawJobDataMapper;

    /**
     * 消费 raw-job-data Topic 消息
     * <p>
     * 接收到的消息为 JSON 格式的 JobDataMessage，解析后写入 raw_job_data 表，
     * 初始状态设为 RAW，后续由 Spark 清洗任务处理。
     */
    @KafkaListener(topics = KafkaTopicConfig.TOPIC_RAW_JOB_DATA,
                   groupId = "data-cleaner-group",
                   concurrency = "3")
    public void consume(JobDataMessage message) {
        log.info("收到 Kafka 消息: source={}, sourceUrl={}",
                message.getSource(), message.getSourceUrl());

        try {
            RawJobData data = new RawJobData();
            data.setSource(message.getSource());
            data.setSourceUrl(message.getSourceUrl());
            data.setRawContent(message.getRawContent());
            data.setFetchTime(message.getFetchTime() != null
                    ? message.getFetchTime() : LocalDateTime.now());
            data.setStatus("RAW");
            data.setCreateTime(LocalDateTime.now());

            int rows = rawJobDataMapper.insert(data);
            log.info("职位数据入库成功: source={}, rows={}, id={}",
                    data.getSource(), rows, data.getId());

        } catch (Exception e) {
            log.error("Kafka 消息消费失败: source={}, sourceUrl={}",
                    message.getSource(), message.getSourceUrl(), e);
        }
    }
}
