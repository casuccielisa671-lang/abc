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
    /** 发布者用户 ID（仅 HR_PUBLISH 职位有值） */
    private Long publisherId;
    private LocalDateTime createTime;

    /**
     * 是否支持站内投递。
     * <p>
     * 采集来的职位（Boss/智联/Mock）在本平台上<b>没有主人</b>：真正的招聘方根本不知道
     * 这个平台存在，学生投了也没有任何 HR 能看到。所以只有 HR 在站内发布的职位才可投递。
     * <p>
     * 这是<b>派生值</b>而不是数据库字段 —— 归属关系的唯一真相是 {@code publisher_id}，
     * 多存一列就多一处可能不同步的地方。Lombok 的 {@code @Data} 不会覆盖它，
     * Jackson 会把它序列化成 {@code "applicable": true/false}。
     */
    public boolean isApplicable() {
        return publisherId != null;
    }
}
