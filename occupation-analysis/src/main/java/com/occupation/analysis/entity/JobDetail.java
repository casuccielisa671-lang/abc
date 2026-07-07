package com.occupation.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 清洗后职位详情实体 — 映射 job_detail 表
 * <p>
 * 注意：此表不含 tenant_id，属于全平台共享数据，不继承 BaseEntity。
 */
@Data
@TableName("job_detail")
public class JobDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职位标题 */
    private String title;

    /** 公司名称 */
    private String company;

    /** 城市 */
    private String city;

    /** 行业 */
    private String industry;

    /** 薪资最低值（元） */
    private Integer salaryMin;

    /** 薪资最高值（元） */
    private Integer salaryMax;

    /** 学历要求 */
    private String education;

    /** 经验要求 */
    private String experience;

    /** 技能标签（JSON 数组） */
    private String skills;

    /** 职位描述 */
    private String description;

    /** 发布日期 */
    private LocalDate publishDate;

    /** 数据来源 */
    private String source;

    /** 来源 URL */
    private String sourceUrl;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
