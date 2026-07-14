package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * JD 优化分析 VO
 *
 * @author occupation-team
 */
@Data
public class JdOptimizeVO {

    /** 综合评分 (0-100) */
    private Integer score;

    /** 各维度评分 */
    private List<Dimension> dimensions;

    /** 优化建议 */
    private List<String> suggestions;

    @Data
    public static class Dimension {
        private String name;
        private Integer score;
    }
}
