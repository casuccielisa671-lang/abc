package com.occupation.analysis.service;

import com.occupation.analysis.vo.CityStatVO;
import com.occupation.analysis.vo.JobCityHeatVO;
import com.occupation.analysis.vo.RecommendJobVO;

import java.util.List;

/**
 * 首页 3D 地图数据服务
 */
public interface MapService {

    /** 推荐职业列表（按岗位数量降序） */
    List<RecommendJobVO> recommendJobs();

    /** 某职业在各城市的聚集度热力数据 */
    List<JobCityHeatVO> getJobCityHeat(String jobName);

    /** 全量城市岗位分布（岗位数 + 平均薪资 + 坐标），供 3D 柱状地图默认展示 */
    List<CityStatVO> cityDistribution();
}
