package com.occupation.web.controller;

import com.occupation.common.dto.JobDataMessage;
import com.occupation.common.result.Result;
import com.occupation.common.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Kafka 管道测试控制器
 * <p>
 * 用于验证 Kafka 生产→消费→入库的完整链路。
 * P1 开发阶段使用，P2 后可移除。需要 Kafka 服务运行才能正常工作。
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/test/kafka")
@RequiredArgsConstructor
public class KafkaTestController {

    private final KafkaProducerService kafkaProducerService;

    /**
     * 发送测试职位数据到 Kafka
     * <p>
     * POST /api/test/kafka/send
     * Body: 可选，JSON 格式的 JobDataMessage 字段
     * 如果不传 Body，使用默认测试数据。
     */
    @PostMapping("/send")
    public Result<String> sendTestMessage(@RequestBody(required = false) JobDataMessage message) {
        JobDataMessage msg = (message != null) ? message : buildTestMessage();
        try {
            kafkaProducerService.send(msg);
            log.info("测试消息已发送: source={}, url={}", msg.getSource(), msg.getSourceUrl());
            return Result.ok("消息已发送到 Kafka Topic [raw-job-data]");
        } catch (Exception e) {
            log.error("发送测试消息失败", e);
            return Result.error("发送失败: " + e.getMessage());
        }
    }

    private JobDataMessage buildTestMessage() {
        return JobDataMessage.builder()
                .source("TEST")
                .sourceUrl("https://example.com/job/test")
                .rawContent("{\"title\":\"Java开发工程师\",\"company\":\"测试科技\",\"city\":\"深圳\",\"salary\":\"15-25K\"}")
                .fetchTime(LocalDateTime.now())
                .build();
    }
}
