package com.occupation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Kafka 消息体 — 原始职位数据
 * <p>
 * 爬虫采集到职位数据后，封装为此消息发送到 Kafka topic {@code raw-job-data}。
 * 下游消费者接收消息后存入 raw_job_data 表。
 *
 * @author occupation-team
 * @see <a href="https://github.com/Hamza88-coder/Real-Time-Recruitment-System">参考 Kafka+Spark 管道设计</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDataMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据来源：BOSS_ZHIPIN / ZHAOPIN / COMPANY_OFFICIAL */
    private String source;

    /** 来源 URL */
    private String sourceUrl;

    /** 原始内容（JSON 字符串，包含职位标题/公司/薪资等未清洗字段） */
    private String rawContent;

    /** 采集时间 */
    private LocalDateTime fetchTime;
}
