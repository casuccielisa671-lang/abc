package com.occupation.analysis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 首页地图 — 职业聚集度 SQL
 */
@Mapper
public interface MapJobMapper {

    @Select("SELECT title AS jobName, COUNT(*) AS jobCount FROM job_detail " +
            "WHERE title IS NOT NULL AND title <> '' " +
            "GROUP BY title ORDER BY jobCount DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecommendJobs(@Param("limit") int limit);

    @Select("SELECT city AS cityName, COUNT(*) AS gatherCnt FROM job_detail " +
            "WHERE title = #{jobName} AND city IS NOT NULL AND city <> '' " +
            "GROUP BY city ORDER BY gatherCnt DESC")
    List<Map<String, Object>> selectCityGatherByJob(@Param("jobName") String jobName);

    @Select("SELECT city AS cityName, COUNT(*) AS gatherCnt FROM job_detail " +
            "WHERE title LIKE CONCAT('%', #{keyword}, '%') AND city IS NOT NULL AND city <> '' " +
            "GROUP BY city ORDER BY gatherCnt DESC LIMIT 40")
    List<Map<String, Object>> selectCityGatherByJobLike(@Param("keyword") String keyword);

    /** 全量城市分布：岗位数 + 平均薪资（(min+max)/2 的均值） */
    @Select("SELECT city AS cityName, COUNT(*) AS jobCount, " +
            "ROUND(AVG((COALESCE(salary_min,0)+COALESCE(salary_max,0))/2)) AS avgSalary " +
            "FROM job_detail WHERE city IS NOT NULL AND city <> '' " +
            "GROUP BY city ORDER BY jobCount DESC")
    List<Map<String, Object>> selectCityDistribution();
}
