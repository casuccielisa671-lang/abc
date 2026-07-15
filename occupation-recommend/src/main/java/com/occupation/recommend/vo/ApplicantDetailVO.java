package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 投递人详情（HR 端）— 身份 + 联系方式 + 画像 + 简历 + 投递过的职位
 * <p>
 * <b>只能通过 {@code GET /api/hr/applicants/{userId}} 获取，且服务端会校验：
 * 该学生确实投递过当前 HR 发布的职位。</b>没有投递关系的学生一律 403 ——
 * 否则任何 HR 只要枚举 userId 就能拖走全校学生的联系方式。
 *
 * @author occupation-team
 */
@Data
public class ApplicantDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- 身份 ----
    private Long userId;
    /** 学号 */
    private String username;
    private String realName;
    private String phone;
    private String email;

    // ---- 画像 ----
    private boolean profileCompleted;
    private String major;
    private String skills;
    private String educationLevel;
    private String expectedCity;
    private String expectedIndustry;
    private Integer expectedSalaryMin;
    private Integer expectedSalaryMax;

    // ---- 简历（exists=false 表示学生尚未填写） ----
    private ResumeVO resume;

    // ---- 求职活跃度 ----
    private long viewCount;
    private long favoriteCount;
    private long applyCount;

    /** 该学生是否已在别处入职（有 ACCEPTED 投递）；true 时 HR 端提示「已入职他处」、无法再录用 */
    private boolean employedElsewhere;

    /** 该学生投递到「本 HR 职位」的记录 */
    private List<AppliedJob> appliedJobs;

    /** 一条投递：投了我的哪个职位、什么时候、现在处理到哪一步 */
    @Data
    public static class AppliedJob implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 变更状态时回传这个 ID */
        private Long applicationId;

        private Long jobId;
        private String jobTitle;
        private String jobCity;
        private LocalDateTime applyTime;

        /** SUBMITTED / VIEWED / INTERVIEW / OFFER / REJECTED */
        private String status;
        private String statusLabel;
        /** 终态（录用/不合适）不能再改 */
        private boolean terminal;
        /** HR 备注，仅 HR 可见 */
        private String hrNote;

        /** 面试信息（status=INTERVIEW 时非空），HR 端回显已发出的安排 */
        private LocalDateTime interviewTime;
        private String interviewPlace;
        private String interviewContact;
        private String interviewContent;
    }
}
