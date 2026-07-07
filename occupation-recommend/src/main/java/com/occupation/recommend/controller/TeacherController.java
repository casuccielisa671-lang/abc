package com.occupation.recommend.controller;

import com.occupation.common.result.Result;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端接口 — 学生就业动态与教学建议
 * <p>
 * 数据范围：当前租户（本校）内的学生，多租户插件自动隔离。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherController {

    private final StudentProfileService profileService;
    private final BehaviorService behaviorService;

    /** 本校学生画像列表（就业意向概览） */
    @GetMapping("/students")
    public Result<List<SysStudentProfile>> listStudents() {
        // TODO(P5-C组): 关联 sys_user 补充学生姓名/学号（通过 auth 模块 UserService，
        //   需在 recommend/pom.xml 增加 occupation-auth 依赖），并改为分页返回
        return Result.ok(profileService.listAll());
    }

    /** 指定学生的求职行为统计 */
    @GetMapping("/students/{userId}/stats")
    public Result<Map<String, Long>> studentStats(@PathVariable Long userId) {
        return Result.ok(behaviorService.countByAction(userId));
    }

    /** 指定学生的行为明细（最近 50 条） */
    @GetMapping("/students/{userId}/behaviors")
    public Result<List<StudentBehavior>> studentBehaviors(@PathVariable Long userId) {
        return Result.ok(behaviorService.listByUser(userId, 50));
    }

    // TODO(P5-C组): GET /api/teacher/suggestions — 教学建议（技能缺口诊断）
    //   实现思路：① 汇总本校学生画像 skills 得到"掌握率"；② 读 analysis_result 中
    //   dimension=skill 的市场热度 Top50；③ 两者对比输出"市场热门但掌握率 < 30% 的技能清单"，
    //   每条附岗位数量证据。这是差异化亮点"技能缺口诊断"的最小可行版，纯 SQL/Java 即可实现。

    // TODO(P5-C组): GET /api/teacher/export — 导出学生就业数据 Excel（Hutool ExcelWriter 或 EasyExcel）
}
