package com.occupation.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.analysis.entity.AnalysisResult;
import com.occupation.analysis.mapper.AnalysisResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * analysis_result 的写入门面 — 供 {@link AnalysisContributor} 实现类使用
 * <p>
 * 把「先清旧维度、再逐条写入、统一周期与精度」这套动作收在一处，
 * 免得每个扩展点各写一遍 tenant_id / period_value / 小数位。
 * <p>
 * tenant_id 由多租户插件自动注入，调用方不用关心。
 *
 * @author occupation-team
 */
@Component
@RequiredArgsConstructor
public class AnalysisResultWriter {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AnalysisResultMapper analysisResultMapper;

    /** 清空某个维度的旧结果（当前租户内） */
    public void clear(String dimension) {
        analysisResultMapper.delete(
                new LambdaQueryWrapper<AnalysisResult>().eq(AnalysisResult::getDimension, dimension));
    }

    /** 写一条指标。null 按 0 处理，统一保留两位小数 */
    public void write(String dimension, String dimensionValue, String metricName, BigDecimal metricValue) {
        AnalysisResult r = new AnalysisResult();
        r.setDimension(dimension);
        r.setDimensionValue(dimensionValue);
        r.setMetricName(metricName);
        r.setMetricValue(metricValue == null
                ? BigDecimal.ZERO
                : metricValue.setScale(2, RoundingMode.HALF_UP));
        r.setPeriodType("MONTH");
        r.setPeriodValue(LocalDateTime.now().format(MONTH_FMT));
        r.setCalcTime(LocalDateTime.now());
        analysisResultMapper.insert(r);
    }

    /** 便捷重载：整数指标 */
    public void write(String dimension, String dimensionValue, String metricName, long metricValue) {
        write(dimension, dimensionValue, metricName, BigDecimal.valueOf(metricValue));
    }
}
