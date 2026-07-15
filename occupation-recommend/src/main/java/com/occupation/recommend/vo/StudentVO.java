package com.occupation.recommend.vo;

import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.SysStudentProfile;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 教师端学生视图 — 画像 + sys_user 的姓名/学号 + 行为计数
 * <p>
 * 与 HR 端的 {@link TalentVO} 不同：教师对本校学生有管理职责，可以看到真实姓名与学号。
 *
 * @author occupation-team
 */
@Data
public class StudentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    /** 学号 / 登录名 */
    private String username;
    /** 真实姓名；用户已删除或数据不一致时为 null */
    private String realName;

    private String major;
    private String skills;
    private String educationLevel;

    /** 所属班级ID（未入班为 null） */
    private Long classId;
    /** 班级统一命名：专业-入学年级-班级（未入班为 null） */
    private String classCode;
    /** 入学年级（未入班为 null） */
    private Integer enrollYear;
    private String expectedCity;
    private String expectedIndustry;
    private Integer expectedSalaryMin;
    private Integer expectedSalaryMax;

    private long viewCount;
    /** 自主联系（对采集职位表达求职意向）次数 */
    private long contactCount;
    private long favoriteCount;
    private long applyCount;

    /** 就业状态：EMPLOYED=已就业 / OFFERED=收到录用待接收 / SEEKING=求职中 / IDLE=待业 */
    private String employmentStatus;

    public static StudentVO of(SysStudentProfile p, String username, String realName,
                               Map<String, Long> behaviorCounts) {
        StudentVO vo = new StudentVO();
        vo.userId = p.getUserId();
        vo.username = username;
        vo.realName = realName;
        vo.major = p.getMajor();
        vo.skills = p.getSkills();
        vo.educationLevel = p.getEducationLevel();
        vo.expectedCity = p.getExpectedCity();
        vo.expectedIndustry = p.getExpectedIndustry();
        vo.expectedSalaryMin = p.getExpectedSalaryMin();
        vo.expectedSalaryMax = p.getExpectedSalaryMax();
        vo.viewCount = behaviorCounts.getOrDefault(BehaviorAction.VIEW, 0L);
        vo.favoriteCount = behaviorCounts.getOrDefault(BehaviorAction.FAVORITE, 0L);
        vo.applyCount = behaviorCounts.getOrDefault(BehaviorAction.APPLY, 0L);
        vo.contactCount = behaviorCounts.getOrDefault(BehaviorAction.CONTACT, 0L);
        return vo;
    }
}
