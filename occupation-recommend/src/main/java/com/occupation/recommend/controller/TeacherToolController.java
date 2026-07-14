package com.occupation.recommend.controller;

import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.service.TeacherToolService;
import com.occupation.recommend.vo.ClassCompareVO;
import com.occupation.recommend.vo.CourseMatchVO;
import com.occupation.recommend.vo.StudentAlertVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端工具箱接口 — 班级对比 / 学生预警 / 课程岗位匹配
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/teacher/tools")
@RequiredArgsConstructor
public class TeacherToolController {

    private final TeacherToolService teacherToolService;

    /** 班级就业对比：选择 2~5 个班级 ID */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/compare-classes")
    public Result<ClassCompareVO> compareClasses(@RequestBody List<Long> classIds) {
        log.info("班级就业对比: teacherId={}, classIds={}", UserContextHolder.getUserId(), classIds);
        return Result.ok(teacherToolService.compareClasses(classIds));
    }

    /** 学生预警看板：支持按类型、严重度、学生名筛选 */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/student-alerts")
    public Result<StudentAlertVO> studentAlerts(@RequestParam(required = false) String alertType,
                                                @RequestParam(required = false) String severity,
                                                @RequestParam(required = false) String search) {
        log.info("学生预警: teacherId={}, type={}, severity={}, search={}",
                UserContextHolder.getUserId(), alertType, severity, search);
        return Result.ok(teacherToolService.getStudentAlerts(alertType, severity, search));
    }

    /** 课程-岗位匹配：输入课程名称，查看关联岗位市场趋势 */
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/course-match")
    public Result<CourseMatchVO> courseMatch(@RequestParam String courseName) {
        log.info("课程岗位匹配: teacherId={}, course={}", UserContextHolder.getUserId(), courseName);
        return Result.ok(teacherToolService.matchCourse(courseName));
    }
}
