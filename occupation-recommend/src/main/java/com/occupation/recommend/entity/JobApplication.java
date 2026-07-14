package com.occupation.recommend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 投递记录实体 — 映射 job_application 表
 * <p>
 * 与 {@link StudentBehavior} 的 {@code action='APPLY'} 是<b>同一次投递的两个视角</b>：
 * behavior 是不可变的行为埋点（喂推荐算法），本表是可流转的业务实体（HR 处理简历）。
 * 投递时两边都写。
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job_application")
public class JobApplication extends BaseEntity {

    /** 投递学生的用户 ID */
    private Long userId;

    /** 被投递的职位 ID */
    private Long jobId;

    /** 职位发布者（HR）的用户 ID，投递时固化，职位下架也不丢 */
    private Long publisherId;

    /** 见 {@link ApplicationStatus} */
    private String status;

    /** HR 备注，仅 HR 可见 */
    private String hrNote;

    /** 面试时间（HR 邀请面试时填，其余状态为 null） */
    private LocalDateTime interviewTime;

    /** 面试地点/方式：线下地址或线上会议链接 */
    private String interviewPlace;

    /** 面试官/联系人 */
    private String interviewContact;

    /** 面试内容/环节说明 */
    private String interviewContent;

    private LocalDateTime appliedAt;

    private LocalDateTime statusChangedAt;
}
