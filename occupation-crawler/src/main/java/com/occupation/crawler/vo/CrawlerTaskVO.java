package com.occupation.crawler.vo;

import com.occupation.crawler.entity.CrawlerTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务响应
 */
@Data
@Builder
public class CrawlerTaskVO {

    private Long id;
    private Long tenantId;
    private String sourceType;
    private String sourceName;
    private String urlPattern;
    private String cronExpr;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 状态文本 */
    private String statusText;

    public static CrawlerTaskVO from(CrawlerTask entity) {
        return CrawlerTaskVO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .sourceType(entity.getSourceType())
                .sourceName(entity.getSourceName())
                .urlPattern(entity.getUrlPattern())
                .cronExpr(entity.getCronExpr())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .statusText(entity.getStatus() == 1 ? "运行中" : "已停止")
                .build();
    }

}
