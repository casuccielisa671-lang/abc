package com.occupation.analysis.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.occupation.analysis.service.DataCleanService;
import com.occupation.common.config.KafkaTopicConfig;
import com.occupation.common.dto.JobDataMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 职位数据清洗监听器。
 *
 * <p>兼容 Kafka JsonDeserializer 反序列化出来的对象和历史字符串消息，
 * 避免出现 raw_job_data 已落库但 job_detail 未清洗的环境差异。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobDataCleanListener {

    private final DataCleanService dataCleanService;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_RAW_JOB_DATA,
                   groupId = "job-clean-group",
                   concurrency = "2")
    public void onMessage(Object payload) {
        try {
            JobDataMessage message = toMessage(payload);
            dataCleanService.cleanAndSave(
                    message.getRawContent(),
                    message.getSource(),
                    message.getSourceUrl());
        } catch (Exception e) {
            log.error("清洗消息处理失败: {}", payload, e);
        }
    }

    private JobDataMessage toMessage(Object payload) {
        if (payload instanceof JobDataMessage) {
            return (JobDataMessage) payload;
        }
        if (payload instanceof String) {
            JSONObject msg = JSON.parseObject((String) payload);
            return JobDataMessage.builder()
                    .rawContent(msg.getString("rawContent"))
                    .source(msg.getString("source"))
                    .sourceUrl(msg.getString("sourceUrl"))
                    .build();
        }
        return JSON.parseObject(JSON.toJSONString(payload), JobDataMessage.class);
    }
}
