package com.occupation.auth.controller;

import com.occupation.auth.entity.TeacherScope;
import com.occupation.auth.service.TeacherScopeService;
import com.occupation.common.exception.BizException;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 教师可见范围管理接口（管理后台）
 * <p>
 * 仅 ADMIN 可访问。管理员在此把某教师配置为：班主任（CLASS，值=班级id）/
 * 专业老师（MAJOR，值=专业名）/ 届老师（GRADE，值=入学年级）。一个教师可多条。
 * 数据自动限定在当前租户内（多租户插件）。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/admin/teacher-scopes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TeacherScopeController {

    private static final List<String> VALID_TYPES =
            Arrays.asList(TeacherScope.CLASS, TeacherScope.MAJOR, TeacherScope.GRADE);

    private final TeacherScopeService scopeService;

    /** 某教师的全部范围配置 */
    @GetMapping
    public Result<List<TeacherScope>> list(@RequestParam Long teacherId) {
        return Result.ok(scopeService.listByTeacher(teacherId));
    }

    /** 新增/编辑一条范围 */
    @PostMapping
    public Result<Void> save(@RequestBody TeacherScope scope) {
        if (scope.getTeacherId() == null || scope.getScopeType() == null || scope.getScopeValue() == null) {
            throw new BizException("教师、范围类型、范围值均不能为空");
        }
        if (!VALID_TYPES.contains(scope.getScopeType())) {
            throw new BizException("范围类型只能是 CLASS / MAJOR / GRADE");
        }
        // 租户由多租户插件在插入时填充，不信任客户端传入
        scope.setTenantId(null);
        scopeService.saveScope(scope);
        return Result.ok();
    }

    /** 删除一条范围 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scopeService.deleteScope(id);
        return Result.ok();
    }
}
