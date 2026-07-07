package com.occupation.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka Topic 定义
 * <p>
 * 参考 Real-Time-Recruitment-System 的 Topic 划分思路：
 * raw-job-data → 采集原始数据入口
 * cleaned-job-data → 清洗后数据（P2 阶段启用）
 *
 * @author occupation-team
 * @see <a href="https://github.com/Hamza88-coder/Real-Time-Recruitment-System">参考</a>
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.auto-create-topics", havingValue = "true", matchIfMissing = false)
public class KafkaTopicConfig {

    /** 原始职位数据 Topic — 爬虫 → 数据清洗管道 */
    public static final String TOPIC_RAW_JOB_DATA = "raw-job-data";

    /** 清洗后职位数据 Topic — 清洗服务 → 分析引擎（P2 启用） */
    public static final String TOPIC_CLEANED_JOB_DATA = "cleaned-job-data";

    /**
     * 自动创建 raw-job-data Topic（开发环境）
     * 生产环境建议手动创建并配置分区数/副本因子
     */
    @Bean
    public NewTopic rawJobDataTopic() {
        return new NewTopic(TOPIC_RAW_JOB_DATA, 3, (short) 1);
    }

    /**
     * 自动创建 cleaned-job-data Topic（P2 阶段启用）
     */
    @Bean
    public NewTopic cleanedJobDataTopic() {
        return new NewTopic(TOPIC_CLEANED_JOB_DATA, 3, (short) 1);
    }
}
