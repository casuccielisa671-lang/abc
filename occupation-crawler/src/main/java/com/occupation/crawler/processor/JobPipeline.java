package com.occupation.crawler.processor;

import com.occupation.common.dto.JobDataMessage;
import com.occupation.common.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.List;

/**
 * 职位数据管道 — 采集结果 → Kafka
 * <p>
 * 爬虫解析出的职位数据通过此管道发送到 Kafka（topic: raw-job-data），
 * 由 occupation-common 中的 KafkaConsumerService 消费入库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobPipeline implements Pipeline {

    private final KafkaProducerService kafkaProducerService;

    /**
     * WebMagic 每抓取到一个页面后回调此方法。
     * 从 ResultItems 中提取 JobDataMessage 列表，逐条发送到 Kafka。
     */
    @Override
    public void process(ResultItems resultItems, Task task) {
        @SuppressWarnings("unchecked")
        List<JobDataMessage> jobs = resultItems.get("jobs");
        if (jobs == null || jobs.isEmpty()) {
            return;
        }

        for (JobDataMessage job : jobs) {
            try {
                kafkaProducerService.send(job);
            } catch (Exception e) {
                log.error("Kafka 发送失败 — source={}, url={}", job.getSource(), job.getSourceUrl(), e);
            }
        }
        log.info("管道处理完成 — 发送 {} 条职位数据到 Kafka", jobs.size());
    }

}
