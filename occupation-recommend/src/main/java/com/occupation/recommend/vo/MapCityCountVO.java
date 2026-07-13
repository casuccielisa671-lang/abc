package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 地图城市计数 — 教师端地图的「学生意向城市」「投递去向城市」图层。
 * count 语义随图层：意向=学生人数，投递=投递次数。
 *
 * @author occupation-team
 */
@Data
public class MapCityCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cityName;
    private Double longitude;
    private Double latitude;
    private Integer count;
}
