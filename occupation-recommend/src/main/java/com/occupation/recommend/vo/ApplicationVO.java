package com.occupation.recommend.vo;

import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.recommend.entity.ApplicationStatus;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收到的投递（HR 端）— 一条 APPLY 行为 + 被投递职位 + 投递人画像
 * <p>
 * <b>可见性边界</b>：学生主动投递到本 HR 发布的职位，视为授权该 HR 查看其身份，
 * 因此这里返回 userId 与姓名。<b>联系方式与简历正文不在列表里</b>，
 * 需再调 {@code GET /api/hr/applicants/{userId}} 单独拉取 —— 列表页不该批量泄露联系方式。
 * <p>
 * 与之相对，{@link TalentVO}（人才浏览）面向的是<b>没有投递关系</b>的全校学生，
 * 保持全脱敏，不含 userId、姓名与联系方式。两者的差别就是这一层授权关系。
 * <p>
 * 学生画像可能为空（学生未填画像就直接投递），此时能力字段为 null。
 *
 * @author occupation-team
 */
@Data
public class ApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 投递记录 ID，状态流转接口用它 */
    private Long applicationId;

    /** SUBMITTED / VIEWED / INTERVIEW / OFFER / REJECTED */
    private String status;
    /** 状态中文名，前端直接展示 */
    private String statusLabel;
    /** 已到终态（录用/不合适）则不能再改 */
    private boolean terminal;
    /** HR 内部备注 */
    private String hrNote;
    private LocalDateTime statusChangedAt;

    /** 被投递的职位 */
    private Long jobId;
    private String jobTitle;
    private String jobCity;

    /** 投递时间 */
    private LocalDateTime applyTime;

    /** 投递人身份（投递即授权本 HR 查看） */
    private Long userId;
    private String realName;

    /** 投递人画像 */
    private String major;
    private String skills;
    private String educationLevel;
    private String expectedCity;
    private Integer expectedSalaryMin;
    private Integer expectedSalaryMax;

    /** 该投递人是否已填写画像 */
    private boolean profileCompleted;

    /** 该投递人是否已填写简历（决定前端「查看简历」按钮是否可点） */
    private boolean hasResume;

    /**
     * 由 {@link JobApplication}（业务实体）构造 —— HR 端从这里读，能拿到状态与备注。
     */
    public static ApplicationVO of(JobApplication app, JobDetailVO job, SysStudentProfile profile,
                                   String realName, boolean hasResume) {
        ApplicationVO vo = new ApplicationVO();
        vo.applicationId = app.getId();
        vo.applyTime = app.getAppliedAt();
        vo.userId = app.getUserId();
        vo.statusChangedAt = app.getStatusChangedAt();
        vo.hrNote = app.getHrNote();

        ApplicationStatus st = ApplicationStatus.valueOf(app.getStatus());
        vo.status = st.name();
        vo.statusLabel = st.getLabel();
        vo.terminal = st.isTerminal();

        vo.realName = realName;
        vo.hasResume = hasResume;
        fillJobAndProfile(vo, job, profile);
        return vo;
    }

    /**
     * 由 {@link StudentBehavior}（行为埋点）构造。
     * <p>
     * 保留它是为了兼容「有 APPLY 行为但没有 job_application 记录」的历史数据 ——
     * 升级脚本只会把落在站内职位上的 APPLY 回填成投递实体，落在采集职位上的幽灵投递不回填。
     */
    public static ApplicationVO of(StudentBehavior behavior, JobDetailVO job, SysStudentProfile profile,
                                   String realName, boolean hasResume) {
        ApplicationVO vo = new ApplicationVO();
        vo.applyTime = behavior.getCreateTime();
        vo.userId = behavior.getUserId();
        vo.realName = realName;
        vo.hasResume = hasResume;
        vo.status = ApplicationStatus.SUBMITTED.name();
        vo.statusLabel = ApplicationStatus.SUBMITTED.getLabel();
        fillJobAndProfile(vo, job, profile);
        return vo;
    }

    private static void fillJobAndProfile(ApplicationVO vo, JobDetailVO job, SysStudentProfile profile) {
        if (job != null) {
            vo.jobId = job.getId();
            vo.jobTitle = job.getTitle();
            vo.jobCity = job.getCity();
        }
        vo.profileCompleted = profile != null;
        if (profile != null) {
            vo.major = profile.getMajor();
            vo.skills = profile.getSkills();
            vo.educationLevel = profile.getEducationLevel();
            vo.expectedCity = profile.getExpectedCity();
            vo.expectedSalaryMin = profile.getExpectedSalaryMin();
            vo.expectedSalaryMax = profile.getExpectedSalaryMax();
        }
    }
}
