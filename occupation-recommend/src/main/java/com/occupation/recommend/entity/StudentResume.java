package com.occupation.recommend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 学生简历实体 — 映射 student_resume 表
 * <p>
 * 与 {@link SysStudentProfile} 的分工：画像是喂给推荐算法的结构化匹配依据，
 * 简历是给 HR 和大模型读的自我陈述。三段经历以 JSON 数组字符串存储。
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student_resume")
public class StudentResume extends BaseEntity {

    /** 关联用户 ID */
    private Long userId;

    /** 求职手机号（为空时前端展示 sys_user.phone） */
    private String contactPhone;

    /** 求职邮箱（为空时前端展示 sys_user.email） */
    private String contactEmail;

    /** 求职意向岗位 */
    private String jobIntention;

    /** 自我评价 */
    private String selfIntro;

    /** 教育经历 JSON 数组 */
    private String educations;

    /** 项目经历 JSON 数组 */
    private String projects;

    /** 实习经历 JSON 数组 */
    private String internships;

    /** 获奖与证书 JSON 数组 */
    private String honors;

    /** 最近一次 AI 诊断结果（JSON），缓存下来避免重复调用大模型 */
    private String aiReview;

    /** AI 诊断时间 */
    private LocalDateTime aiReviewTime;
}
