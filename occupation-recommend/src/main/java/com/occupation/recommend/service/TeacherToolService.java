package com.occupation.recommend.service;

import com.occupation.recommend.vo.ClassCompareVO;
import com.occupation.recommend.vo.CourseMatchVO;
import com.occupation.recommend.vo.StudentAlertVO;

import java.util.List;

/**
 * 教师端工具箱服务 — 班级就业对比 / 学生预警 / 课程岗位匹配
 *
 * @author occupation-team
 */
public interface TeacherToolService {

    /**
     * 班级就业对比：选择 2~5 个班级，从就业率、薪资、去向维度并排对比
     */
    ClassCompareVO compareClasses(List<Long> classIds);

    /**
     * 学生预警看板：自动标记画像不完整、长期未投递、技能缺口大的学生
     */
    StudentAlertVO getStudentAlerts(String alertType, String severity, String search);

    /**
     * 课程-岗位匹配：输入课程名称，查看关联岗位的市场需求与技能变化趋势
     */
    CourseMatchVO matchCourse(String courseName);
}
