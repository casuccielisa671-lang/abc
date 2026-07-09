package com.occupation.recommend.vo;

import com.occupation.recommend.entity.SysStudentProfile;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 人才卡片（脱敏视图）— HR 端可见的学生信息
 * <p>
 * 刻意不包含 userId、姓名、手机号、邮箱：HR 只能看到能力与意向，
 * 不能反查到具体是哪位学生。活跃度仅以聚合计数呈现。
 *
 * @author occupation-team
 */
@Data
public class TalentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String major;
    private String skills;
    private String expectedCity;
    private String expectedIndustry;
    private String educationLevel;
    private Integer expectedSalaryMin;
    private Integer expectedSalaryMax;

    /** 浏览职位次数 */
    private long viewCount;

    /** 投递次数 */
    private long applyCount;

    /**
     * @param behaviorCounts 该学生的 action → count，缺失按 0 计
     */
    public static TalentVO from(SysStudentProfile p, Map<String, Long> behaviorCounts) {
        TalentVO vo = new TalentVO();
        vo.major = p.getMajor();
        vo.skills = p.getSkills();
        vo.expectedCity = p.getExpectedCity();
        vo.expectedIndustry = p.getExpectedIndustry();
        vo.educationLevel = p.getEducationLevel();
        vo.expectedSalaryMin = p.getExpectedSalaryMin();
        vo.expectedSalaryMax = p.getExpectedSalaryMax();
        vo.viewCount = behaviorCounts.getOrDefault("VIEW", 0L);
        vo.applyCount = behaviorCounts.getOrDefault("APPLY", 0L);
        return vo;
    }
}
