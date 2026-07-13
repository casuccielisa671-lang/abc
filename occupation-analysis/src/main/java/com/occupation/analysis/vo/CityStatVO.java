package com.occupation.analysis.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 城市岗位分布 — 首页 3D 地图（ECharts GL bar3D）数据。
 * 每个有坐标的城市一条：岗位数 + 平均薪资。
 *
 * @author occupation-team
 */
@Data
public class CityStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cityName;
    private Double longitude;
    private Double latitude;
    private Integer jobCount;
    /** 平均薪资（元/月，(min+max)/2 的均值，四舍五入） */
    private Integer avgSalary;
}
