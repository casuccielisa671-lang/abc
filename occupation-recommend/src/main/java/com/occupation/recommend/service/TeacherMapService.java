package com.occupation.recommend.service;

import com.occupation.recommend.vo.MapCityCountVO;

import java.util.List;

/**
 * 教师端地图数据 — 学生侧的城市分布图层（按当前教师可见范围过滤；ADMIN 看整租户）。
 *
 * @author occupation-team
 */
public interface TeacherMapService {

    /** 学生求职意向城市分布（画像 expected_city 聚合） */
    List<MapCityCountVO> studentIntentCities();

    /** 投递去向城市分布（job_application 关联职位所在城市聚合） */
    List<MapCityCountVO> applicationCities();
}
