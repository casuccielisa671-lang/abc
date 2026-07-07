package com.occupation.crawler.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建/更新采集任务请求
 */
@Data
public class CrawlerTaskCreateDTO {

    /** 采集源类型：BOSS_ZHIPIN / ZHAOPIN / COMPANY_OFFICIAL / MOCK */
    @NotBlank(message = "采集源类型不能为空")
    private String sourceType;

    /** 采集源名称 */
    @NotBlank(message = "采集源名称不能为空")
    private String sourceName;

    /** URL 或数据文件模式 */
    private String urlPattern;

    /** Cron 定时表达式 */
    private String cronExpr;

}
