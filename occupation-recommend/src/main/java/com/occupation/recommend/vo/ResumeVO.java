package com.occupation.recommend.vo;

import com.occupation.recommend.model.ResumeSection;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历出参 — 学生端编辑回显、HR 端查看
 * <p>
 * JSON 列已在 Service 里解析成数组，前端直接渲染，无需自己 parse。
 *
 * @author occupation-team
 */
@Data
public class ResumeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 简历是否已创建。false 时其余字段为空，前端展示引导填写 */
    private boolean exists;

    private String contactPhone;
    private String contactEmail;
    private String jobIntention;
    private String selfIntro;

    private List<ResumeSection.Education> educations;
    private List<ResumeSection.Project> projects;
    private List<ResumeSection.Internship> internships;
    private List<String> honors;

    /** 最近一次 AI 诊断结果，未诊断过为 null */
    private ResumeReviewVO aiReview;

    private LocalDateTime aiReviewTime;

    private LocalDateTime updateTime;
}
