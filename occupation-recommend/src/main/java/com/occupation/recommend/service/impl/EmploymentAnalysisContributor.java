package com.occupation.recommend.service.impl;

import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.service.AnalysisContributor;
import com.occupation.analysis.service.AnalysisResultWriter;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.recommend.entity.ApplicationStatus;
import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.mapper.JobApplicationMapper;
import com.occupation.recommend.mapper.StudentBehaviorMapper;
import com.occupation.recommend.mapper.SysStudentProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 就业分析扩展点 — 把学生侧数据写进 {@code analysis_result}
 * <p>
 * 内置维度只统计 {@code job_detail}（市场有什么岗位）。本类补上另外三个视角：
 * <ol>
 *   <li><b>投递转化漏斗</b>（{@code apply_funnel} / {@code apply_response}）：投出去有没有结果</li>
 *   <li><b>供需错配</b>（{@code student_*} / {@code gap_*}）：学生想要的 vs 市场供给的</li>
 *   <li><b>自主求职流向</b>（{@code contact_*}）：学生在平台外往哪儿投</li>
 * </ol>
 * <p>
 * <b>漏斗只统计 job_application</b>，不碰 {@code student_behavior} 里的 APPLY。
 * 后者包含历史上投在采集职位（无主）上的「幽灵投递」—— 它们连被查看的可能都没有，
 * 混进转化率只会让数字难看，而这个难看不反映任何真实问题。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmploymentAnalysisContributor implements AnalysisContributor {

    // ---- 维度名 ----
    private static final String DIM_FUNNEL = "apply_funnel";
    private static final String DIM_RESPONSE = "apply_response";
    private static final String DIM_STUDENT_CITY = "student_city";
    private static final String DIM_STUDENT_INDUSTRY = "student_industry";
    private static final String DIM_STUDENT_SALARY = "student_salary";
    private static final String DIM_GAP_CITY = "gap_city";
    private static final String DIM_GAP_SALARY = "gap_salary";
    private static final String DIM_CONTACT_CITY = "contact_city";
    private static final String DIM_CONTACT_INDUSTRY = "contact_industry";

    /** 供需错配只看学生扎堆的前 N 个城市，尾部城市样本太小，比值没有意义 */
    private static final int GAP_TOP_CITIES = 10;

    /** 一次取回的职位上限。当前数据量百级，真上量要改成 SQL 聚合 */
    private static final int JOB_SCAN_LIMIT = 5000;

    /** 期望薪资分桶（元/月），左闭右开 */
    private static final int[] SALARY_BUCKETS = {0, 6000, 8000, 10000, 15000, Integer.MAX_VALUE};
    private static final String[] SALARY_LABELS =
            {"6000以下", "6000-8000", "8000-10000", "10000-15000", "15000以上"};

    private final SysStudentProfileMapper profileMapper;
    private final StudentBehaviorMapper behaviorMapper;
    private final JobApplicationMapper applicationMapper;
    private final AnalysisResultWriter writer;
    private final AnalysisService analysisService;
    private final JobDetailService jobDetailService;

    @Override
    public String name() {
        return "就业分析（漏斗/错配/自主求职）";
    }

    @Override
    public int contribute() {
        int n = 0;
        n += analyzeFunnel();
        n += analyzeStudentIntent();
        n += analyzeGap();
        n += analyzeContactFlow();
        return n;
    }

    // ==================== 一、投递转化漏斗 ====================

    /**
     * 各状态当前计数 + HR 响应时长。
     * <p>
     * <b>刻意不假装成「到达过某阶段」的漏斗</b>：状态机允许 VIEWED 直接跳到 OFFER，
     * 从当前状态推不出它有没有经过 INTERVIEW。所以这里给的是<b>状态分布</b>，
     * 转化率由前端用「非 SUBMITTED 数 / 总数」这类明确口径算。
     */
    private int analyzeFunnel() {
        writer.clear(DIM_FUNNEL);
        writer.clear(DIM_RESPONSE);

        List<JobApplication> apps = applicationMapper.selectList(null);
        Map<String, Long> byStatus = apps.stream()
                .collect(Collectors.groupingBy(JobApplication::getStatus, Collectors.counting()));

        int count = 0;
        for (ApplicationStatus st : ApplicationStatus.values()) {
            writer.write(DIM_FUNNEL, st.name(), "application_count", byStatus.getOrDefault(st.name(), 0L));
            count++;
        }
        writer.write(DIM_FUNNEL, "TOTAL", "application_count", apps.size());
        count++;

        // HR 响应时长：SUBMITTED → 首次状态变更 的间隔。只统计已被处理的投递
        List<Long> responseHours = apps.stream()
                .filter(a -> a.getStatusChangedAt() != null && a.getAppliedAt() != null)
                .map(a -> Duration.between(a.getAppliedAt(), a.getStatusChangedAt()).toHours())
                .filter(h -> h >= 0)
                .sorted()
                .collect(Collectors.toList());

        long unresponded = apps.stream()
                .filter(a -> ApplicationStatus.SUBMITTED.name().equals(a.getStatus()))
                .count();

        writer.write(DIM_RESPONSE, "responded", "application_count", responseHours.size());
        writer.write(DIM_RESPONSE, "unresponded", "application_count", unresponded);
        writer.write(DIM_RESPONSE, "median_hours", "hours", median(responseHours));
        count += 3;

        log.debug("投递漏斗: 总 {} 条，已响应 {}，未响应 {}", apps.size(), responseHours.size(), unresponded);
        return count;
    }

    // ==================== 二、学生求职意向分布 ====================

    private int analyzeStudentIntent() {
        writer.clear(DIM_STUDENT_CITY);
        writer.clear(DIM_STUDENT_INDUSTRY);
        writer.clear(DIM_STUDENT_SALARY);

        List<SysStudentProfile> profiles = profileMapper.selectList(null);
        int count = 0;
        count += writeGroupCount(DIM_STUDENT_CITY, "student_count",
                groupCount(profiles, SysStudentProfile::getExpectedCity));
        count += writeGroupCount(DIM_STUDENT_INDUSTRY, "student_count",
                groupCount(profiles, SysStudentProfile::getExpectedIndustry));

        // 薪资分桶
        Map<String, Long> buckets = new LinkedHashMap<>();
        for (String label : SALARY_LABELS) {
            buckets.put(label, 0L);
        }
        for (SysStudentProfile p : profiles) {
            if (p.getExpectedSalaryMin() == null) {
                continue;
            }
            buckets.merge(bucketOf(p.getExpectedSalaryMin()), 1L, Long::sum);
        }
        count += writeGroupCount(DIM_STUDENT_SALARY, "student_count", buckets);
        return count;
    }

    // ==================== 三、供需错配 ====================

    /**
     * 城市错配比 = 学生意向占比 ÷ 岗位供给占比。
     * <p>
     * 大于 1 表示学生扎堆（想去的人比岗位多），小于 1 表示岗位过剩。
     * 岗位分布直接读内置维度刚写进 analysis_result 的 city 结果 —— 口径与看板完全一致。
     */
    private int analyzeGap() {
        writer.clear(DIM_GAP_CITY);
        writer.clear(DIM_GAP_SALARY);

        List<SysStudentProfile> profiles = profileMapper.selectList(null);
        if (profiles.isEmpty()) {
            return 0;
        }

        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setTenantId(TenantContextHolder.getTenantId());
        DashboardVO dashboard = analysisService.getDashboard(query);

        Map<String, Long> jobByCity = toCountMap(dashboard.getCityDist());
        long totalJobs = jobByCity.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Long> studentByCity = groupCount(profiles, SysStudentProfile::getExpectedCity);
        long totalStudents = studentByCity.values().stream().mapToLong(Long::longValue).sum();

        int count = 0;
        if (totalJobs > 0 && totalStudents > 0) {
            List<Map.Entry<String, Long>> top = studentByCity.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(GAP_TOP_CITIES)
                    .collect(Collectors.toList());

            for (Map.Entry<String, Long> e : top) {
                String city = e.getKey();
                BigDecimal studentRatio = ratio(e.getValue(), totalStudents);
                BigDecimal jobRatio = ratio(jobByCity.getOrDefault(city, 0L), totalJobs);
                // 岗位占比为 0 时比值无穷大，写 999 作为「该城市几乎没有岗位」的哨兵值
                BigDecimal gap = jobRatio.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.valueOf(999)
                        : studentRatio.divide(jobRatio, 2, RoundingMode.HALF_UP);

                writer.write(DIM_GAP_CITY, city, "student_ratio", studentRatio.multiply(BigDecimal.valueOf(100)));
                writer.write(DIM_GAP_CITY, city, "job_ratio", jobRatio.multiply(BigDecimal.valueOf(100)));
                writer.write(DIM_GAP_CITY, city, "gap_ratio", gap);
                count += 3;
            }
        }

        // 薪资错配：学生期望中位数 vs 市场中位数（用 (min+max)/2 作为单个职位的代表值）
        List<Long> studentSalaries = profiles.stream()
                .filter(p -> p.getExpectedSalaryMin() != null && p.getExpectedSalaryMax() != null)
                .map(p -> (long) ((p.getExpectedSalaryMin() + p.getExpectedSalaryMax()) / 2))
                .sorted().collect(Collectors.toList());
        List<Long> marketSalaries = scanJobs().stream()
                .filter(j -> j.getSalaryMin() != null && j.getSalaryMax() != null)
                .map(j -> (long) ((j.getSalaryMin() + j.getSalaryMax()) / 2))
                .sorted().collect(Collectors.toList());

        BigDecimal studentMedian = median(studentSalaries);
        BigDecimal marketMedian = median(marketSalaries);
        writer.write(DIM_GAP_SALARY, "overall", "student_median", studentMedian);
        writer.write(DIM_GAP_SALARY, "overall", "market_median", marketMedian);
        // 期望偏差百分比：正数表示学生期望高于市场
        BigDecimal deviation = marketMedian.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : studentMedian.subtract(marketMedian)
                        .divide(marketMedian, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
        writer.write(DIM_GAP_SALARY, "overall", "deviation_percent", deviation);
        count += 3;

        return count;
    }

    // ==================== 四、自主求职流向 ====================

    /** 学生对哪些外部岗位表达了意向 —— 反映他们真实的求职方向，而非平台内那点岗位 */
    private int analyzeContactFlow() {
        writer.clear(DIM_CONTACT_CITY);
        writer.clear(DIM_CONTACT_INDUSTRY);

        List<StudentBehavior> contacts = behaviorMapper.selectList(
                new LambdaQueryWrapper<StudentBehavior>()
                        .eq(StudentBehavior::getAction, BehaviorAction.CONTACT));
        if (contacts.isEmpty()) {
            return 0;
        }

        List<Long> jobIds = contacts.stream()
                .map(StudentBehavior::getJobId).distinct().collect(Collectors.toList());
        Map<Long, JobDetailVO> jobs = jobDetailService.listByIds(jobIds).stream()
                .collect(Collectors.toMap(JobDetailVO::getId, j -> j));

        Map<String, Long> byCity = new LinkedHashMap<>();
        Map<String, Long> byIndustry = new LinkedHashMap<>();
        for (StudentBehavior b : contacts) {
            JobDetailVO job = jobs.get(b.getJobId());
            if (job == null) {
                continue;   // 职位已被删除
            }
            mergeIfPresent(byCity, job.getCity());
            mergeIfPresent(byIndustry, job.getIndustry());
        }

        int count = writeGroupCount(DIM_CONTACT_CITY, "contact_count", byCity);
        count += writeGroupCount(DIM_CONTACT_INDUSTRY, "contact_count", byIndustry);
        return count;
    }

    // ==================== 工具 ====================

    private List<JobDetailVO> scanJobs() {
        com.occupation.analysis.dto.JobQueryDTO q = new com.occupation.analysis.dto.JobQueryDTO();
        q.setPageNum(1);
        q.setPageSize(JOB_SCAN_LIMIT);
        return jobDetailService.queryJobs(q).getRecords();
    }

    private int writeGroupCount(String dimension, String metric, Map<String, Long> groups) {
        groups.forEach((k, v) -> writer.write(dimension, k, metric, v));
        return groups.size();
    }

    private <T> Map<String, Long> groupCount(List<T> list, java.util.function.Function<T, String> key) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (T t : list) {
            mergeIfPresent(map, key.apply(t));
        }
        return map;
    }

    private void mergeIfPresent(Map<String, Long> map, String key) {
        if (key != null && !key.trim().isEmpty()) {
            map.merge(key.trim(), 1L, Long::sum);
        }
    }

    private Map<String, Long> toCountMap(List<DashboardVO.DimensionItem> items) {
        if (items == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> map = new LinkedHashMap<>();
        for (DashboardVO.DimensionItem i : items) {
            map.put(i.getName(), i.getValue() == null ? 0L : i.getValue().longValue());
        }
        return map;
    }

    private BigDecimal ratio(long part, long total) {
        return total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(part).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    /** 中位数。空列表返回 0，偶数个取中间两个的均值 */
    private BigDecimal median(List<Long> sorted) {
        if (sorted.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int n = sorted.size();
        if (n % 2 == 1) {
            return BigDecimal.valueOf(sorted.get(n / 2));
        }
        return BigDecimal.valueOf(sorted.get(n / 2 - 1) + sorted.get(n / 2))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private String bucketOf(int salary) {
        for (int i = 0; i < SALARY_LABELS.length; i++) {
            if (salary >= SALARY_BUCKETS[i] && salary < SALARY_BUCKETS[i + 1]) {
                return SALARY_LABELS[i];
            }
        }
        return SALARY_LABELS[SALARY_LABELS.length - 1];
    }
}
