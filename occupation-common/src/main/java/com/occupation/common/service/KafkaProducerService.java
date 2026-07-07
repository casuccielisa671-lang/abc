package com.occupation.common.service;

import com.occupation.common.config.KafkaTopicConfig;
import com.occupation.common.dto.JobDataMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Kafka 生产者服务
 * <p>
 * 爬虫模块调用此服务，将采集到的职位数据发送到 Kafka。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送职位数据消息到 raw-job-data Topic
     *
     * @param message 职位数据消息体
     */
    public void send(JobDataMessage message) {
        kafkaTemplate.send(KafkaTopicConfig.TOPIC_RAW_JOB_DATA, message)
                .addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                    @Override
                    public void onSuccess(SendResult<String, Object> result) {
                        log.info("Kafka 消息发送成功: source={}, offset={}",
                                message.getSource(),
                                result.getRecordMetadata().offset());
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("Kafka 消息发送失败: source={}, url={}",
                                message.getSource(), message.getSourceUrl(), ex);
                    }
                });
    }
}
