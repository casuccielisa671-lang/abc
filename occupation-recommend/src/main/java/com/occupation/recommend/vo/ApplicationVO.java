package com.occupation.recommend.vo;

import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收到的投递（HR 端）— 一条 APPLY 行为 + 被投递职位 + 投递人脱敏画像
 * <p>
 * 与 {@link TalentVO} 一致，不返回学生姓名与联系方式。
 * 学生画像可能为空（学生未填画像就直接投递），此时能力字段为 null。
 *
 * @author occupation-team
 */
@Data
public class ApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 被投递的职位 */
    private Long jobId;
    private String jobTitle;
    private String jobCity;

    /** 投递时间 */
    private LocalDateTime applyTime;

    /** 投递人画像（脱敏） */
    private String major;
    private String skills;
    private String educationLevel;
    private String expectedCity;
    private Integer expectedSalaryMin;
    private Integer expectedSalaryMax;

    /** 该投递人是否已填写画像 */
    private boolean profileCompleted;

    public static ApplicationVO of(StudentBehavior behavior, JobDetailVO job, SysStudentProfile profile) {
        ApplicationVO vo = new ApplicationVO();
        vo.applyTime = behavior.getCreateTime();
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
        return vo;
    }
}
