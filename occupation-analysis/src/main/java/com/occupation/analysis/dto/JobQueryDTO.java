package com.occupation.analysis.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 职位查询入参 — 供推荐模块跨模块调用
 *
 * @author occupation-team
 */
@Data
public class JobQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 城市筛选 */
    private String city;

    /** 行业筛选 */
    private String industry;

    /** 薪资下限 */
    private Integer salaryMin;

    /** 薪资上限 */
    private Integer salaryMax;

    /** 学历要求 */
    private String education;

    /** 经验要求 */
    private String experience;

    /** 关键词搜索 */
    private String keyword;
}
