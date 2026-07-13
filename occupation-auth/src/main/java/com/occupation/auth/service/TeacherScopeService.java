package com.occupation.auth.service;

import com.occupation.auth.entity.TeacherScope;

import java.util.List;
import java.util.Set;

/**
 * 教师可见范围服务 — 把班主任/专业老师/届老师的范围解析成可见学生集合。
 * <p>
 * 这是教师端数据隔离的安全核心：所有教师端接口都必须先经此解析，再据以过滤，
 * 否则任何教师都能看到全校学生（类比 HR 端的投递人归属校验）。
 *
 * @author occupation-team
 */
public interface TeacherScopeService {

    /**
     * 解析指定教师能看到的学生 userId 集合。
     *
     * @param teacherId 教师用户ID
     * @param role      当前用户角色
     * @return {@code null} = 不受限（ADMIN，看当前租户全部）；
     *         空集 = 看不到任何学生（无任何范围配置的教师）；
     *         否则为可见学生的 userId 集合。
     */
    Set<Long> visibleStudentIds(Long teacherId, String role);

    /** 某教师的全部范围配置（管理端展示/编辑用） */
    List<TeacherScope> listByTeacher(Long teacherId);

    /** 新增/编辑一条范围 */
    void saveScope(TeacherScope scope);

    /** 删除一条范围 */
    void deleteScope(Long id);
}
