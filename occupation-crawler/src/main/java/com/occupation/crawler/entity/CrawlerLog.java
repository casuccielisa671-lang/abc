package com.occupation.crawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集日志实体 — 映射 crawler_log 表
 */
@Data
@TableName("crawler_log")
public class CrawlerLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联采集任务ID */
    private Long taskId;

    /** 所属租户ID */
    private Long tenantId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 采集记录数 */
    private Integer recordCount;

    /** 状态：RUNNING / SUCCESS / FAILED */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

}
