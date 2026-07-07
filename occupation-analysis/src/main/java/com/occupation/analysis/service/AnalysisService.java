package com.occupation.analysis.service;

import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.vo.DashboardVO;

/**
 * 分析服务接口 — 供 report 模块调用
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
}
