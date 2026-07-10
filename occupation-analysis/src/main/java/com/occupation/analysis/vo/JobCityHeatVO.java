package com.occupation.analysis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 某职业在各城市的聚集度（3D 地图热力点）
 */
@Data
public class JobCityHeatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cityName;
    private Double longitude;
    private Double latitude;
    /** 聚集度：该城市该职业岗位占比/数量，越高地图越红 */
    private BigDecimal gatherValue;
}
