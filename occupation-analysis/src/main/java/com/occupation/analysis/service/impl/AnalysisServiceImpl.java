package com.occupation.analysis.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.entity.AnalysisResult;
import com.occupation.analysis.mapper.AnalysisResultMapper;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.analysis.vo.EmploymentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分析服务实现 — Dashboard 数据查询
 *
 * @author occupation-team
 */
@Slf4j
@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Autowired
    private AnalysisResultMapper analysisResultMapper;

    @Override
    public DashboardVO getDashboard(DashboardQueryDTO query) {
        log.info("查询 Dashboard 数据, tenantId={}, startDate={}, endDate={}",
                query.getTenantId(), query.getStartDate(), query.getEndDate());

        DashboardVO vo = new DashboardVO();

        // 1. 行业 Top 10
        vo.setIndustryTop(queryDimension("industry", 10, query));

        // 2. 城市分布
        vo.setCityDist(queryDimension("city", null, query));

        // 3. 技能热度 Top 20
        vo.setSkillHot(queryDimension("skill", 20, query));

        // 4. 学历分布
        vo.setEducationDist(queryDimension("education", null, query));

        // 5. 趋势数据
        vo.setTrend(queryTrend(query));

        log.info("Dashboard 数据查询完成, 行业Top={}, 城市数={}, 技能数={}, 学历数={}, 趋势点={}",
                sizeOf(vo.getIndustryTop()), sizeOf(vo.getCityDist()),
                sizeOf(vo.getSkillHot()), sizeOf(vo.getEducationDist()),
                sizeOf(vo.getTrend()));

        return vo;
    }

    @Override
    public List<DashboardVO.DimensionItem> topSkills(int limit) {
        return queryDimension("skill", limit, new DashboardQueryDTO());
    }

    @Override
    public EmploymentVO getEmployment() {
        EmploymentVO vo = new EmploymentVO();
        vo.setFunnel(buildFunnel());
        vo.setStudentCity(readDimension("student_city", "student_count", null));
        vo.setStudentIndustry(readDimension("student_industry", "student_count", null));
        // 薪资分桶要按桶的顺序展示，不能按数量降序 —— 否则 X 轴是乱的
        vo.setStudentSalary(sortBySalaryBucket(readDimension("student_salary", "student_count", null)));
        vo.setCityGap(buildCityGap());
        vo.setSalaryGap(buildSalaryGap());
        vo.setContactCity(readDimension("contact_city", "contact_count", null));
        vo.setContactIndustry(readDimension("contact_industry", "contact_count", null));
        return vo;
    }

    // ==================== 就业分析：从 analysis_result 组装 ====================

    private EmploymentVO.Funnel buildFunnel() {
        Map<String, BigDecimal> counts = readAsMap("apply_funnel", "application_count");
        Map<String, BigDecimal> resp = readAsMap("apply_response", "application_count");

        EmploymentVO.Funnel f = new EmploymentVO.Funnel();
        f.setTotal(longOf(counts.get("TOTAL")));
        f.setSubmitted(longOf(counts.get("SUBMITTED")));
        f.setViewed(longOf(counts.get("VIEWED")));
        f.setInterview(longOf(counts.get("INTERVIEW")));
        f.setOffer(longOf(counts.get("OFFER")));
        f.setRejected(longOf(counts.get("REJECTED")));
        f.setResponded(longOf(resp.get("responded")));
        f.setUnresponded(longOf(resp.get("unresponded")));
        f.setMedianResponseHours(readSingle("apply_response", "median_hours", "hours"));

        long total = f.getTotal();
        f.setViewRate(percent(f.getResponded(), total));
        f.setInterviewRate(percent(f.getInterview() + f.getOffer(), total));
        f.setOfferRate(percent(f.getOffer(), total));
        return f;
    }

    private List<EmploymentVO.CityGap> buildCityGap() {
        Map<String, BigDecimal> studentRatio = readAsMap("gap_city", "student_ratio");
        Map<String, BigDecimal> jobRatio = readAsMap("gap_city", "job_ratio");
        Map<String, BigDecimal> gapRatio = readAsMap("gap_city", "gap_ratio");

        return studentRatio.entrySet().stream()
                .map(e -> {
                    EmploymentVO.CityGap g = new EmploymentVO.CityGap();
                    g.setCity(e.getKey());
                    g.setStudentRatio(e.getValue());
                    g.setJobRatio(jobRatio.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    g.setGapRatio(gapRatio.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return g;
                })
                .sorted((a, b) -> b.getStudentRatio().compareTo(a.getStudentRatio()))
                .collect(Collectors.toList());
    }

    private EmploymentVO.SalaryGap buildSalaryGap() {
        EmploymentVO.SalaryGap g = new EmploymentVO.SalaryGap();
        g.setStudentMedian(readSingle("gap_salary", "overall", "student_median"));
        g.setMarketMedian(readSingle("gap_salary", "overall", "market_median"));
        g.setDeviationPercent(readSingle("gap_salary", "overall", "deviation_percent"));
        return g;
    }

    /** 通用读：某维度下指定指标的全部记录，按值降序 */
    private List<DashboardVO.DimensionItem> readDimension(String dimension, String metric, Integer limit) {
        QueryWrapper<AnalysisResult> wrapper = new QueryWrapper<>();
        wrapper.eq("dimension", dimension).eq("metric_name", metric).orderByDesc("metric_value");
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return analysisResultMapper.selectList(wrapper).stream().map(r -> {
            DashboardVO.DimensionItem item = new DashboardVO.DimensionItem();
            item.setName(r.getDimensionValue());
            item.setValue(r.getMetricValue());
            item.setCount(r.getMetricValue() == null ? 0L : r.getMetricValue().longValue());
            return item;
        }).collect(Collectors.toList());
    }

    private Map<String, BigDecimal> readAsMap(String dimension, String metric) {
        QueryWrapper<AnalysisResult> wrapper = new QueryWrapper<>();
        wrapper.eq("dimension", dimension).eq("metric_name", metric);
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (AnalysisResult r : analysisResultMapper.selectList(wrapper)) {
            map.put(r.getDimensionValue(), r.getMetricValue());
        }
        return map;
    }

    private BigDecimal readSingle(String dimension, String dimensionValue, String metric) {
        QueryWrapper<AnalysisResult> wrapper = new QueryWrapper<>();
        wrapper.eq("dimension", dimension)
               .eq("dimension_value", dimensionValue)
               .eq("metric_name", metric)
               .last("LIMIT 1");
        List<AnalysisResult> list = analysisResultMapper.selectList(wrapper);
        return list.isEmpty() ? BigDecimal.ZERO : list.get(0).getMetricValue();
    }

    /** 薪资分桶按金额从低到高排，而不是按人数 */
    private List<DashboardVO.DimensionItem> sortBySalaryBucket(List<DashboardVO.DimensionItem> items) {
        List<String> order = java.util.Arrays.asList(
                "6000以下", "6000-8000", "8000-10000", "10000-15000", "15000以上");
        items.sort(java.util.Comparator.comparingInt(i -> order.indexOf(i.getName())));
        return items;
    }

    private long longOf(BigDecimal v) {
        return v == null ? 0L : v.longValue();
    }

    private BigDecimal percent(long part, long total) {
        return total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    /**
     * 通用维度查询：查某个维度下 job_count 指标，按 metric_value 降序
     */
    private List<DashboardVO.DimensionItem> queryDimension(String dimension, Integer limit,
                                                            DashboardQueryDTO query) {
        QueryWrapper<AnalysisResult> wrapper = new QueryWrapper<>();
        wrapper.eq("dimension", dimension)
               .eq("metric_name", "job_count");

        // 时间范围过滤（存在时才添加）
        applyDateFilter(wrapper, query);

        wrapper.orderByDesc("metric_value");
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }

        List<AnalysisResult> results = analysisResultMapper.selectList(wrapper);

        return results.stream().map(r -> {
            DashboardVO.DimensionItem item = new DashboardVO.DimensionItem();
            item.setName(r.getDimensionValue());
            item.setValue(r.getMetricValue());
            item.setCount(r.getMetricValue() != null ? r.getMetricValue().longValue() : 0L);
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 趋势数据查询：按 period_value 升序，每个时间点含 job_count 和 avg_salary
     */
    private List<DashboardVO.TrendItem> queryTrend(DashboardQueryDTO query) {
        // 查所有 trend 维度的数据
        QueryWrapper<AnalysisResult> wrapper = new QueryWrapper<>();
        wrapper.eq("dimension", "trend");
        applyDateFilter(wrapper, query);
        wrapper.orderByAsc("period_value");

        List<AnalysisResult> allTrendData = analysisResultMapper.selectList(wrapper);

        // 按 period_value 分组
        Map<String, List<AnalysisResult>> grouped = allTrendData.stream()
                .collect(Collectors.groupingBy(AnalysisResult::getPeriodValue,
                         LinkedHashMap::new, Collectors.toList()));

        List<DashboardVO.TrendItem> trendItems = new ArrayList<>();
        for (Map.Entry<String, List<AnalysisResult>> entry : grouped.entrySet()) {
            DashboardVO.TrendItem item = new DashboardVO.TrendItem();
            item.setPeriod(entry.getKey());

            long jobCount = 0L;
            BigDecimal totalSalary = BigDecimal.ZERO;
            int salaryCount = 0;

            for (AnalysisResult r : entry.getValue()) {
                if (r.getMetricValue() == null) {
                    continue;
                }
                if ("job_count".equals(r.getMetricName())) {
                    jobCount = r.getMetricValue().longValue();
                } else if ("avg_salary_min".equals(r.getMetricName())
                        || "avg_salary_max".equals(r.getMetricName())
                        || "avg_salary".equals(r.getMetricName())) {
                    totalSalary = totalSalary.add(r.getMetricValue());
                    salaryCount++;
                }
            }

            item.setJobCount(jobCount);
            if (salaryCount > 0) {
                item.setAvgSalary(totalSalary.divide(new BigDecimal(salaryCount), 2, RoundingMode.HALF_UP));
            } else {
                item.setAvgSalary(BigDecimal.ZERO);
            }

            trendItems.add(item);
        }

        return trendItems;
    }

    /**
     * 为 QueryWrapper 附加时间范围过滤条件
     */
    private void applyDateFilter(QueryWrapper<AnalysisResult> wrapper, DashboardQueryDTO query) {
        if (StrUtil.isNotBlank(query.getStartDate())) {
            wrapper.ge("period_value", query.getStartDate());
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le("period_value", query.getEndDate());
        }
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
