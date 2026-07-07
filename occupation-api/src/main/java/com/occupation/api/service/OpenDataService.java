package com.occupation.api.service;

import java.util.List;
import java.util.Map;

/**
 * 开放数据服务 — 对外提供的统计数据（脱敏、只读、聚合级）
 * <p>
 * 数据来源均为全平台共享表（job_detail），不涉及租户私有数据与个人数据。
 *
 * @author occupation-team
 */
public interface OpenDataService {

    /**
     * 就业大盘统计：岗位总量、平均薪资、覆盖城市数、覆盖行业数
     */
    Map<String, Object> getStatsOverview();

    /**
     * 热门技能排行 Top N（从 job_detail.skills 实时聚合，Redis 缓存 10 分钟）
     */
    List<Map<String, Object>> getSkillHot(int topN);

    /**
     * 行业岗位分布 Top N
     */
    List<Map<String, Object>> getIndustryDist(int topN);
}
