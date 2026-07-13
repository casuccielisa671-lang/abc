package com.occupation.auth.service;

import com.occupation.auth.entity.SysClass;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 班级服务 — 学院内组织结构（专业-入学年级-班级）。
 * <p>
 * 全部查询经多租户插件自动限定在当前租户内。
 *
 * @author occupation-team
 */
public interface ClassService {

    /** 当前租户全部班级（含停用），按专业+入学年级排序 */
    List<SysClass> listAll();

    /** 批量按 id 取回：id → SysClass，供教师端 StudentVO 展示班级信息，避免 N+1 */
    Map<Long, SysClass> mapByIds(Collection<Long> ids);

    /** 指定专业下的班级 id（教师"专业老师"范围解析用） */
    List<Long> idsByMajor(String major);

    /** 指定入学年级下的班级 id（教师"届老师"范围解析用） */
    List<Long> idsByYear(Integer enrollYear);

    /** 当前租户去重的专业列表（筛选下拉） */
    List<String> listMajors();

    /** 当前租户去重的入学年级列表（筛选下拉），倒序 */
    List<Integer> listYears();

    /** 各班级的在册学生数：classId → 学生数（管理端班级列表展示），一次分组查询 */
    Map<Long, Long> studentCountByClass();

    /**
     * 新增或编辑班级。code 由 专业-入学年级-班级 自动拼接；租户内 code 唯一（DB 约束兜底）。
     */
    SysClass saveClass(SysClass c);

    /** 删除班级；若仍有学生归属于该班则拒绝（避免产生悬空 class_id） */
    void deleteClass(Long id);

    /** 把一批学生分配到某班级（仅对当前租户内 role=STUDENT 的用户生效） */
    void assignStudents(Long classId, Collection<Long> userIds);

    /**
     * 按专业/入学年级（均可选）解析出的学生 userId 集合，用于教师端在其可见范围内二次筛选。
     *
     * @return {@code null}=未提供任何筛选（调用方不据此缩小范围）；否则为匹配班级下的
     *         学生 userId 集合（可能为空集，表示筛选无匹配）
     */
    Set<Long> studentIdsByMajorYear(String major, Integer enrollYear);

    /** 指定班级下的学生 userId 集合（就业报告按班级出数据时用） */
    Set<Long> studentIdsInClass(Long classId);

    /** 当前租户内全部 role=STUDENT 的 userId 集合（报告「发给全体」时用） */
    Set<Long> allStudentIds();
}
