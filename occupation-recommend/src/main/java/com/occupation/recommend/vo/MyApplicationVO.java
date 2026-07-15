package com.occupation.recommend.vo;

import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.recommend.entity.ApplicationStatus;
import com.occupation.recommend.entity.JobApplication;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 我的投递（学生端）— 投递记录 + 职位快照 + HR 处理进度
 * <p>
 * <b>不返回 hrNote</b>：那是 HR 的内部备注，学生不该看到。
 *
 * @author occupation-team
 */
@Data
public class MyApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long applicationId;

    private Long jobId;
    private String jobTitle;
    private String company;
    private String jobCity;
    private Integer salaryMin;
    private Integer salaryMax;

    /** SUBMITTED / VIEWED / INTERVIEW / OFFER / REJECTED */
    private String status;
    /** 状态的中文名，前端直接展示，避免各端各写一份映射表 */
    private String statusLabel;
    /** 是否已到终态（录用/不合适），前端据此决定要不要显示「等待回复」 */
    private boolean terminal;

    private LocalDateTime appliedAt;
    private LocalDateTime statusChangedAt;

    /** 面试信息（status=INTERVIEW 时非空），学生端渲染成面试通知卡 */
    private LocalDateTime interviewTime;
    private String interviewPlace;
    private String interviewContact;
    private String interviewContent;

    public static MyApplicationVO of(JobApplication app, JobDetailVO job) {
        MyApplicationVO vo = new MyApplicationVO();
        vo.applicationId = app.getId();
        vo.jobId = app.getJobId();
        vo.appliedAt = app.getAppliedAt();
        vo.statusChangedAt = app.getStatusChangedAt();
        vo.status = app.getStatus();
        vo.interviewTime = app.getInterviewTime();
        vo.interviewPlace = app.getInterviewPlace();
        vo.interviewContact = app.getInterviewContact();
        vo.interviewContent = app.getInterviewContent();

        ApplicationStatus st = ApplicationStatus.valueOf(app.getStatus());
        vo.statusLabel = st.getLabel();
        vo.terminal = st.isTerminal();

        // 职位可能已被 HR 物理删除（job_detail 没有 deleted 列），此时只剩投递记录
        if (job != null) {
            vo.jobTitle = job.getTitle();
            vo.company = job.getCompany();
            vo.jobCity = job.getCity();
            vo.salaryMin = job.getSalaryMin();
            vo.salaryMax = job.getSalaryMax();
        }
        return vo;
    }
}
