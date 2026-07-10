package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 教师端班级概览统计
 * <p>
 * totalStudents 统计的是本租户 STUDENT 角色的账号数，withProfile 是其中已填写画像的人数，
 * 两者之差即「尚未完善画像」的学生数。
 *
 * @author occupation-team
 */
@Data
public class TeacherOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学生账号总数 */
    private long totalStudents;

    /** 已填写画像的学生数 */
    private long withProfile;

    /** 全部学生的浏览行为总数 */
    private long totalViews;

    /** 全部学生的投递行为总数 */
    private long totalApplies;

    /** 自主联系总数：学生对采集来的外部岗位表达的求职意向 */
    private long totalContacts;
}
