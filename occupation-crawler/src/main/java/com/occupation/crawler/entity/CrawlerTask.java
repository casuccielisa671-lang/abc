package com.occupation.crawler.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采集任务实体 — 映射 crawler_task 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_task")
public class CrawlerTask extends BaseEntity {

    /** 采集源类型：BOSS_ZHIPIN / ZHAOPIN / COMPANY_OFFICIAL */
    private String sourceType;

    /** 采集源名称 */
    private String sourceName;

    /** URL 匹配模式 */
    private String urlPattern;

    /** Cron 定时表达式 */
    private String cronExpr;

    /** 状态：0=停止 1=运行中 */
    private Integer status;

}
