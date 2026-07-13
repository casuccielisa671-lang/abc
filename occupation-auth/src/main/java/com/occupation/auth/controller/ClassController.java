package com.occupation.auth.controller;

import com.occupation.auth.entity.SysClass;
import com.occupation.auth.service.ClassService;
import com.occupation.auth.vo.ClassVO;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班级管理接口（管理后台）
 * <p>
 * 仅 ADMIN 可访问；数据自动限定在当前租户内（多租户插件）。
 * 班级采用「专业-入学年级-班级」统一命名，code 由服务端拼接、租户内唯一。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ClassController {

    private final ClassService classService;

    /** 班级列表（含在册学生数） */
    @GetMapping
    public Result<List<ClassVO>> list() {
        Map<Long, Long> counts = classService.studentCountByClass();
        List<ClassVO> vos = classService.listAll().stream()
                .map(c -> ClassVO.of(c, counts.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());
        return Result.ok(vos);
    }

    /** 新增/编辑班级（id 为空=新增） */
    @PostMapping
    public Result<SysClass> save(@RequestBody SysClass clazz) {
        // 租户由多租户插件在插入时填充，不信任客户端传入
        clazz.setTenantId(null);
        return Result.ok(classService.saveClass(clazz));
    }

    /** 删除班级（仍有学生归属时拒绝） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.deleteClass(id);
        return Result.ok();
    }

    /** 把一批学生分配到该班级 */
    @PostMapping("/{id}/students")
    public Result<Void> assignStudents(@PathVariable Long id, @RequestBody List<Long> userIds) {
        classService.assignStudents(id, userIds);
        return Result.ok();
    }

    /** 筛选项：当前租户的专业 / 入学年级 */
    @GetMapping("/filters")
    public Result<Map<String, Object>> filters() {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("majors", classService.listMajors());
        data.put("years", classService.listYears());
        return Result.ok(data);
    }
}
