package com.occupation.analysis.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard 出参 — 各维度分析数据
 *
 * @author occupation-team
 */
@Data
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 行业 Top 排行 */
    private List<DimensionItem> industryTop;

    /** 城市热度分布 */
    private List<DimensionItem> cityDist;

    /** 技能热度排行 */
    private List<DimensionItem> skillHot;

    /** 学历分布 */
    private List<DimensionItem> educationDist;

    /** 趋势数据（按时间） */
    private List<TrendItem> trend;

    // ---- 内部类 ----

    @Data
    public static class DimensionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private BigDecimal value;
        private Long count;
    }

    @Data
    public static class TrendItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String period;
        private Long jobCount;
        private BigDecimal avgSalary;
    }
}
