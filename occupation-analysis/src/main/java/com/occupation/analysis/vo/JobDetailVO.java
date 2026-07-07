package com.occupation.analysis.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 职位详情出参 — 供推荐模块跨模块调用
 *
 * @author occupation-team
 */
@Data
public class JobDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String company;
    private String city;
    private String industry;
    private Integer salaryMin;
    private Integer salaryMax;
    private String education;
    private String experience;
    private String skills;
    private String description;
    private LocalDate publishDate;
    private String source;
    private String sourceUrl;
    private LocalDateTime createTime;
}
