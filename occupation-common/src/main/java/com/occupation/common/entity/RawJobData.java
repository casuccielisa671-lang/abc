package com.occupation.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 原始职位数据实体 — 映射 raw_job_data 表
 * <p>
 * 注意：此表不含 tenant_id，属于全平台共享数据，不继承 BaseEntity。
 * 在 MyBatis-Plus 多租户插件中已配置忽略此表。
 *
 * @author occupation-team
 */
@Data
@TableName("raw_job_data")
public class RawJobData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据来源：BOSS_ZHIPIN / ZHAOPIN / COMPANY_OFFICIAL */
    private String source;

    /** 来源 URL */
    private String sourceUrl;

    /** 原始内容（JSON） */
    private String rawContent;

    /** 采集时间 */
    private LocalDateTime fetchTime;

    /** 状态：RAW=原始 / CLEANED=已清洗 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
