package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 班级就业对比 VO
 *
 * @author occupation-team
 */
@Data
public class ClassCompareVO {

    /** 各班数据 */
    private List<ClassItem> classes;

    /** 对比总结 */
    private Comparison comparison;

    @Data
    public static class ClassItem {
        private Long id;
        private String name;
        private Integer studentCount;
        private Double employmentRate;
        private Integer avgSalary;
        /** 去向分布 Top 3 */
        private List<Destination> topDestinations;
    }

    @Data
    public static class Destination {
        private String jobCategory;
        private Double ratio;
    }

    @Data
    public static class Comparison {
        private String highestEmployment;
        private String lowestEmployment;
        private List<String> commonDestinations;
    }
}
