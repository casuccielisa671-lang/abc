package com.occupation.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.occupation.auth.entity.SysClass;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.mapper.SysClassMapper;
import com.occupation.auth.mapper.SysUserMapper;
import com.occupation.auth.service.ClassService;
import com.occupation.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 班级服务实现（多租户自动隔离）。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final SysClassMapper classMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<SysClass> listAll() {
        return classMapper.selectList(new LambdaQueryWrapper<SysClass>()
                .orderByAsc(SysClass::getMajor)
                .orderByAsc(SysClass::getEnrollYear)
                .orderByAsc(SysClass::getClassName));
    }

    @Override
    public Map<Long, SysClass> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return classMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysClass::getId, c -> c));
    }

    @Override
    public List<Long> idsByMajor(String major) {
        return classMapper.selectList(new LambdaQueryWrapper<SysClass>()
                        .select(SysClass::getId)
                        .eq(SysClass::getMajor, major))
                .stream().map(SysClass::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> idsByYear(Integer enrollYear) {
        return classMapper.selectList(new LambdaQueryWrapper<SysClass>()
                        .select(SysClass::getId)
                        .eq(SysClass::getEnrollYear, enrollYear))
                .stream().map(SysClass::getId).collect(Collectors.toList());
    }

    @Override
    public List<String> listMajors() {
        return listAll().stream().map(SysClass::getMajor).distinct().collect(Collectors.toList());
    }

    @Override
    public List<Integer> listYears() {
        return listAll().stream().map(SysClass::getEnrollYear)
                .distinct().sorted(Collections.reverseOrder()).collect(Collectors.toList());
    }

    @Override
    public Map<Long, Long> studentCountByClass() {
        List<Map<String, Object>> rows = userMapper.selectMaps(new QueryWrapper<SysUser>()
                .select("class_id AS classId", "COUNT(*) AS cnt")
                .eq("role", "STUDENT")
                .isNotNull("class_id")
                .groupBy("class_id"));
        Map<Long, Long> result = new java.util.HashMap<>();
        for (Map<String, Object> r : rows) {
            Object cid = r.get("classId");
            Object cnt = r.get("cnt");
            if (cid != null) {
                result.put(((Number) cid).longValue(), ((Number) cnt).longValue());
            }
        }
        return result;
    }

    @Override
    public SysClass saveClass(SysClass c) {
        if (c.getMajor() == null || c.getEnrollYear() == null || c.getClassName() == null) {
            throw new BizException("专业、入学年级、班级名不能为空");
        }
        c.setCode(c.getMajor() + "-" + c.getEnrollYear() + "-" + c.getClassName());
        if (c.getStatus() == null) {
            c.setStatus(1);
        }
        if (c.getId() == null) {
            classMapper.insert(c);
            log.info("新增班级: {}", c.getCode());
        } else {
            classMapper.updateById(c);
            log.info("编辑班级: id={}, {}", c.getId(), c.getCode());
        }
        return c;
    }

    @Override
    public void deleteClass(Long id) {
        long students = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, id));
        if (students > 0) {
            throw new BizException("该班级仍有 " + students + " 名学生，请先转移学生再删除");
        }
        classMapper.deleteById(id);
        log.info("删除班级: id={}", id);
    }

    @Override
    public void assignStudents(Long classId, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        // 校验班级存在（且在当前租户内，多租户插件保证）
        SysClass clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BizException("班级不存在");
        }
        // 仅对当前租户内 role=STUDENT 的用户生效；updateWrapper 也被多租户插件限定
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getClassId, classId)
                .in(SysUser::getId, userIds)
                .eq(SysUser::getRole, "STUDENT");
        int rows = userMapper.update(null, update);
        log.info("班级分配: classId={}, 影响学生 {} 人", classId, rows);
    }

    @Override
    public Set<Long> studentIdsByMajorYear(String major, Integer enrollYear) {
        boolean hasMajor = StrUtil.isNotBlank(major);
        boolean hasYear = enrollYear != null;
        if (!hasMajor && !hasYear) {
            return null;
        }
        List<Long> classIds = classMapper.selectList(new LambdaQueryWrapper<SysClass>()
                        .select(SysClass::getId)
                        .eq(hasMajor, SysClass::getMajor, major)
                        .eq(hasYear, SysClass::getEnrollYear, enrollYear))
                .stream().map(SysClass::getId).collect(Collectors.toList());
        if (classIds.isEmpty()) {
            return Collections.emptySet();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .eq(SysUser::getRole, "STUDENT")
                        .in(SysUser::getClassId, classIds))
                .stream().map(SysUser::getId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> studentIdsInClass(Long classId) {
        if (classId == null) {
            return Collections.emptySet();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .eq(SysUser::getRole, "STUDENT")
                        .eq(SysUser::getClassId, classId))
                .stream().map(SysUser::getId).collect(Collectors.toSet());
    }

    @Override
    public Set<Long> allStudentIds() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .eq(SysUser::getRole, "STUDENT"))
                .stream().map(SysUser::getId).collect(Collectors.toSet());
    }
}
