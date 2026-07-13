package com.occupation.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.entity.TeacherScope;
import com.occupation.auth.mapper.SysUserMapper;
import com.occupation.auth.mapper.TeacherScopeMapper;
import com.occupation.auth.service.ClassService;
import com.occupation.auth.service.TeacherScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师可见范围服务实现。
 * <p>
 * scope 解析链：teacher_scope → 班级 id 集合 → 学生 userId 集合，全程多租户自动隔离。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherScopeServiceImpl implements TeacherScopeService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_STUDENT = "STUDENT";

    private final TeacherScopeMapper scopeMapper;
    private final SysUserMapper userMapper;
    private final ClassService classService;

    @Override
    public Set<Long> visibleStudentIds(Long teacherId, String role) {
        // 管理员不受范围限制（维持原有"看整租户"语义）
        if (ROLE_ADMIN.equals(role)) {
            return null;
        }

        List<TeacherScope> scopes = listByTeacher(teacherId);
        Set<Long> classIds = new HashSet<>();
        for (TeacherScope s : scopes) {
            if (s.getScopeValue() == null) {
                continue;
            }
            String val = s.getScopeValue().trim();
            switch (s.getScopeType()) {
                case TeacherScope.CLASS:
                    parseLong(val).ifPresent(classIds::add);
                    break;
                case TeacherScope.MAJOR:
                    classIds.addAll(classService.idsByMajor(val));
                    break;
                case TeacherScope.GRADE:
                    parseInt(val).ifPresent(y -> classIds.addAll(classService.idsByYear(y)));
                    break;
                default:
                    log.warn("未知的教师范围类型: {}", s.getScopeType());
            }
        }

        // 无任何有效范围 → 看不到任何学生（fail-closed）
        if (classIds.isEmpty()) {
            return Collections.emptySet();
        }

        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .eq(SysUser::getRole, ROLE_STUDENT)
                        .in(SysUser::getClassId, classIds))
                .stream().map(SysUser::getId).collect(Collectors.toSet());
    }

    @Override
    public List<TeacherScope> listByTeacher(Long teacherId) {
        return scopeMapper.selectList(new LambdaQueryWrapper<TeacherScope>()
                .eq(TeacherScope::getTeacherId, teacherId));
    }

    @Override
    public void saveScope(TeacherScope scope) {
        if (scope.getId() == null) {
            scopeMapper.insert(scope);
        } else {
            scopeMapper.updateById(scope);
        }
    }

    @Override
    public void deleteScope(Long id) {
        scopeMapper.deleteById(id);
    }

    private static java.util.Optional<Long> parseLong(String s) {
        try {
            return java.util.Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    private static java.util.Optional<Integer> parseInt(String s) {
        try {
            return java.util.Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
