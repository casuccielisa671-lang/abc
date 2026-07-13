package com.occupation.recommend.service;

import com.occupation.recommend.vo.EmploymentReportData;

/**
 * 学生就业数据报告服务 — 按 专业/年级/班级 scope 聚合就业指标。
 * <p>
 * 供 occupation-report 生成 EMPLOYMENT 类报告调用（report 依赖 recommend）。
 * 三个 scope 参数均可空：给 classId 按班级；给 major/enrollYear 按专业/届；全空则按全校。
 *
 * @author occupation-team
 */
public interface EmploymentReportService {

    EmploymentReportData build(String major, Integer enrollYear, Long classId);
}
