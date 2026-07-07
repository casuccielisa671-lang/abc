package com.occupation.analysis.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.entity.AnalysisResult;
import com.occupation.analysis.mapper.AnalysisResultMapper;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
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
