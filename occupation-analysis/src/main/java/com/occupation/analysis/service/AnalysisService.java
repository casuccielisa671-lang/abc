package com.occupation.analysis.service;

import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.analysis.vo.EmploymentVO;

import java.util.List;

/**
 * 分析服务接口 — 供 report / recommend 模块调用
 * <p>
 * 负责 Dashboard 数据查询、分析结果缓存刷新等。
 *
 * @author occupation-team
 */
public interface AnalysisService {

    /**
     * 获取 Dashboard 分析数据
     *
     * @param query 筛选条件（维度、时间范围、租户ID）
     * @return Dashboard 视图数据（各维度 Top 数据 + 趋势）
     */
    DashboardVO getDashboard(DashboardQueryDTO query);

    /**
     * 市场热门技能排行（dimension=skill，metric=job_count，按岗位数降序）
     * <p>
     * 供教师端「技能缺口诊断」对比学生掌握率使用。数据范围受多租户插件限制在当前租户。
     *
     * @param limit 取前 N 条
     */
    List<DashboardVO.DimensionItem> topSkills(int limit);

    /**
     * 就业分析：投递漏斗 / 供需错配 / 自主求职流向。
     * <p>
     * 数据由 recommend 模块的 {@link AnalysisContributor} 在 {@code runAll()} 时写入
     * {@code analysis_result}。若从未重算过，各字段为空集合而非 null。
     */
    EmploymentVO getEmployment();
}
