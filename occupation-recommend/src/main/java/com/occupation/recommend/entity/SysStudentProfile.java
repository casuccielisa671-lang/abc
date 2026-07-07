package com.occupation.recommend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生画像实体 — 映射 sys_student_profile 表
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_student_profile")
public class SysStudentProfile extends BaseEntity {

    /** 关联用户 ID */
    private Long userId;

    /** 专业 */
    private String major;

    /** 技能列表（JSON 数组或逗号分隔） */
    private String skills;

    /** 意向城市 */
    private String expectedCity;

    /** 意向行业 */
    private String expectedIndustry;

    /** 期望薪资下限（元） */
    private Integer expectedSalaryMin;

    /** 期望薪资上限（元） */
    private Integer expectedSalaryMax;

    /** 学历：专科/本科/硕士/博士 */
    private String educationLevel;
}
