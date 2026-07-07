package com.occupation.crawler.vo;

import com.occupation.crawler.entity.CrawlerLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集日志响应
 */
@Data
@Builder
public class CrawlerLogVO {

    private Long id;
    private Long taskId;
    private Long tenantId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer recordCount;
    private String status;
    private String errorMsg;
    private LocalDateTime createTime;

    /** 耗时描述（可选） */
    private String duration;

    public static CrawlerLogVO from(CrawlerLog entity) {
        CrawlerLogVOBuilder builder = CrawlerLogVO.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .tenantId(entity.getTenantId())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .recordCount(entity.getRecordCount())
                .status(entity.getStatus())
                .errorMsg(entity.getErrorMsg())
                .createTime(entity.getCreateTime());

        // 计算耗时
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            long seconds = java.time.Duration.between(entity.getStartTime(), entity.getEndTime()).getSeconds();
            builder.duration(seconds + "s");
        }
        return builder.build();
    }

}
