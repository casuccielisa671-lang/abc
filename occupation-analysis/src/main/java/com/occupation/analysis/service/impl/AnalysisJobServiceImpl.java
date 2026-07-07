package com.occupation.analysis.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.occupation.analysis.entity.AnalysisResult;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.AnalysisResultMapper;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.analysis.service.AnalysisJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统计分析 Job 实现 — SQL 聚合 + Java 内存计算
 * <p>
 * job_detail 为全平台共享表（无租户隔离），统计结果写入 analysis_result 时
 * 由多租户插件按当前 TenantContextHolder 注入 tenant_id——
 * 调度器（AnalysisScheduler）会遍历所有租户分别执行。
 * <p>
 * 若课程要求演示 Spark 技术点：保持本接口不变，把各 analyzeXxx() 的实现
 * 替换为 SparkSession(local[*]) 读 MySQL → groupBy 聚合 → 写回，即插即换。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisJobServiceImpl implements AnalysisJobService {

    private final JobDetailMapper jobDetailMapper;
    private final AnalysisResultMapper analysisResultMapper;

    /** 本期周期值：按月统计（如 "2026-07"） */
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int runAll() {
        int total = 0;
        total += analyzeIndustry();
        total += analyzeCity();
        total += analyzeEducation();
        total += analyzeSkill();
        total += analyzeTrend();
        log.info("统计分析全量重算完成，共写入 {} 条结果", total);
        return total;
    }

    @Override
    public int analyzeIndustry() {
        // SELECT industry, COUNT(*), AVG((salary_min+salary_max)/2) FROM job_detail GROUP BY industry
        QueryWrapper<JobDetail> wrapper = new QueryWrapper<>();
        wrapper.select("industry AS name",
                       "COUNT(*) AS job_count",
                       "AVG((salary_min + salary_max) / 2) AS avg_salary")
               .isNotNull("industry")
               .groupBy("industry");
        return saveGroupResult("industry", jobDetailMapper.selectMaps(wrapper));
    }

    @Override
    public int analyzeCity() {
        QueryWrapper<JobDetail> wrapper = new QueryWrapper<>();
        wrapper.select("city AS name",
                       "COUNT(*) AS job_count",
                       "AVG((salary_min + salary_max) / 2) AS avg_salary")
               .isNotNull("city")
               .groupBy("city");
        return saveGroupResult("city", jobDetailMapper.selectMaps(wrapper));
    }

    @Override
    public int analyzeEducation() {
        QueryWrapper<JobDetail> wrapper = new QueryWrapper<>();
        wrapper.select("education AS name",
                       "COUNT(*) AS job_count",
                       "AVG((salary_min + salary_max) / 2) AS avg_salary")
               .isNotNull("education")
               .groupBy("education");
        return saveGroupResult("education", jobDetailMapper.selectMaps(wrapper));
    }

    @Override
    public int analyzeSkill() {
        // 技能是 JSON 数组字段，SQL 无法直接聚合 → Java 内存词频统计
        List<JobDetail> jobs = jobDetailMapper.selectList(
                new LambdaQueryWrapper<JobDetail>().select(JobDetail::getSkills));

        Map<String, Long> freq = new HashMap<>();
        for (JobDetail job : jobs) {
            if (job.getSkills() == null || job.getSkills().isEmpty()) {
                continue;
            }
            try {
                for (Object skill : JSON.parseArray(job.getSkills())) {
                    freq.merge(String.valueOf(skill), 1L, Long::sum);
                }
            } catch (Exception ignore) {
                // 单条脏数据跳过
            }
        }

        // 取 Top 100 写入结果表
        clearOldResults("skill");
        String period = LocalDateTime.now().format(MONTH_FMT);
        int count = 0;
        List<Map.Entry<String, Long>> top = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(100)
                .collect(java.util.stream.Collectors.toList());
        for (Map.Entry<String, Long> e : top) {
            insertResult("skill", e.getKey(), "job_count", BigDecimal.valueOf(e.getValue()), period);
            count++;
        }
        log.info("技能热度统计完成: 共 {} 个技能，写入 Top {}", freq.size(), count);
        return count;
    }

    @Override
    public int analyzeTrend() {
        // 按发布月份聚合：SELECT DATE_FORMAT(publish_date,'%Y-%m'), COUNT(*), AVG(...)
        QueryWrapper<JobDetail> wrapper = new QueryWrapper<>();
        wrapper.select("DATE_FORMAT(publish_date, '%Y-%m') AS name",
                       "COUNT(*) AS job_count",
                       "AVG((salary_min + salary_max) / 2) AS avg_salary")
               .isNotNull("publish_date")
               .groupBy("DATE_FORMAT(publish_date, '%Y-%m')");

        clearOldResults("trend");
        int count = 0;
        for (Map<String, Object> row : jobDetailMapper.selectMaps(wrapper)) {
            String month = String.valueOf(row.get("name"));
            insertResult("trend", month, "job_count", toDecimal(row.get("job_count")), month);
            insertResult("trend", month, "avg_salary", toDecimal(row.get("avg_salary")), month);
            count += 2;
        }
        return count;
    }

    // ==================== 私有工具 ====================

    /** 通用分组结果落库：每组写 job_count 与 avg_salary 两条指标 */
    private int saveGroupResult(String dimension, List<Map<String, Object>> rows) {
        clearOldResults(dimension);
        String period = LocalDateTime.now().format(MONTH_FMT);
        int count = 0;
        for (Map<String, Object> row : rows) {
            String name = String.valueOf(row.get("name"));
            insertResult(dimension, name, "job_count", toDecimal(row.get("job_count")), period);
            insertResult(dimension, name, "avg_salary", toDecimal(row.get("avg_salary")), period);
            count += 2;
        }
        log.info("{} 维度统计完成: {} 组，写入 {} 条", dimension, rows.size(), count);
        return count;
    }

    /** 删除该维度旧数据（tenant_id 条件由多租户插件自动注入） */
    private void clearOldResults(String dimension) {
        analysisResultMapper.delete(
                new LambdaQueryWrapper<AnalysisResult>().eq(AnalysisResult::getDimension, dimension));
    }

    private void insertResult(String dimension, String value, String metricName,
                              BigDecimal metricValue, String periodValue) {
        AnalysisResult r = new AnalysisResult();
        r.setDimension(dimension);
        r.setDimensionValue(value);
        r.setMetricName(metricName);
        r.setMetricValue(metricValue == null
                ? BigDecimal.ZERO
                : metricValue.setScale(2, RoundingMode.HALF_UP));
        r.setPeriodType("MONTH");
        r.setPeriodValue(periodValue);
        r.setCalcTime(LocalDateTime.now());
        analysisResultMapper.insert(r);
    }

    private BigDecimal toDecimal(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        return new BigDecimal(String.valueOf(val));
    }
}
