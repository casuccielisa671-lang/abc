package com.occupation.recommend.vo;

import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.recommend.entity.StudentBehavior;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生行为明细（教师端）— 行为记录 + 关联职位的标题/公司
 * <p>
 * student_behavior 表只存 job_id，直接返回实体的话教师端表格里
 * 「职位名称 / 公司」两列永远是空的。这里补齐。
 *
 * @author occupation-team
 */
@Data
public class BehaviorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long jobId;
    /** 行为类型：VIEW / FAVORITE / APPLY / IGNORE */
    private String action;
    private LocalDateTime createTime;

    /** 关联职位；职位已被下架时为 null */
    private String jobTitle;
    private String jobCompany;
    private String jobCity;

    public static BehaviorVO of(StudentBehavior b, JobDetailVO job) {
        BehaviorVO vo = new BehaviorVO();
        vo.jobId = b.getJobId();
        vo.action = b.getAction();
        vo.createTime = b.getCreateTime();
        if (job != null) {
            vo.jobTitle = job.getTitle();
            vo.jobCompany = job.getCompany();
            vo.jobCity = job.getCity();
        }
        return vo;
    }
}
