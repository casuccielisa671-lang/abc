package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 薪资竞争力分析 VO
 *
 * @author occupation-team
 */
@Data
public class SalaryBenchmarkVO {

    /** P25 分位 */
    private Integer p25;
    /** P50 中位 */
    private Integer p50;
    /** P75 分位 */
    private Integer p75;
    /** P90 分位 */
    private Integer p90;

    /** 城市薪资对比 */
    private List<CityData> cityData;

    @Data
    public static class CityData {
        private String city;
        private Integer salary;
    }
}
