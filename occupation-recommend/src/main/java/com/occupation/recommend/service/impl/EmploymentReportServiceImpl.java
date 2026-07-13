package com.occupation.recommend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.auth.entity.SysClass;
import com.occupation.auth.service.ClassService;
import com.occupation.auth.service.UserService;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.ApplicationStatus;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.mapper.JobApplicationMapper;
import com.occupation.recommend.service.EmploymentReportService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.EmploymentReportData;
import com.occupation.recommend.vo.EmploymentReportData.DimItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生就业数据报告聚合实现（多租户自动隔离）。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmploymentReportServiceImpl implements EmploymentReportService {

    /** 期望薪资分桶（元/月），左闭右开——与就业分析口径一致 */
    private static final int[] SALARY_BUCKETS = {0, 6000, 8000, 10000, 15000, Integer.MAX_VALUE};
    private static final String[] SALARY_LABELS =
            {"6000以下", "6000-8000", "8000-10000", "10000-15000", "15000以上"};

    private static final int TOP_CITY = 10;
    private static final int TOP_INDUSTRY = 10;
    private static final int TOP_SKILLS = 15;

    private final ClassService classService;
    private final UserService userService;
    private final StudentProfileService profileService;
    private final JobApplicationMapper applicationMapper;

    @Override
    public EmploymentReportData build(String major, Integer enrollYear, Long classId) {
        EmploymentReportData d = new EmploymentReportData();

        // ① 解析 scope → 学生 userId 集合（null=全校）
        Set<Long> ids;
        if (classId != null) {
            ids = classService.studentIdsInClass(classId);
            SysClass c = classService.mapByIds(Collections.singletonList(classId)).get(classId);
            d.setScopeLabel(c != null ? c.getCode() : "班级#" + classId);
        } else if (StrUtil.isNotBlank(major) || enrollYear != null) {
            ids = classService.studentIdsByMajorYear(major, enrollYear);
            d.setScopeLabel(buildLabel(major, enrollYear));
        } else {
            ids = null;
            d.setScopeLabel("全校");
        }

        d.setStudentCount(ids != null ? ids.size() : (int) userService.countByRole("STUDENT"));

        // ② 画像（ids=null 走全校）
        List<SysStudentProfile> profiles = profileService.listByUserIds(ids);
        d.setProfiledCount(profiles.size());

        // ③ 投递（只统计 job_application）
        List<JobApplication> apps;
        if (ids == null) {
            apps = applicationMapper.selectList(null);
        } else if (ids.isEmpty()) {
            apps = Collections.emptyList();
        } else {
            apps = applicationMapper.selectList(
                    new LambdaQueryWrapper<JobApplication>().in(JobApplication::getUserId, ids));
        }
        Map<String, Long> byStatus = apps.stream()
                .collect(Collectors.groupingBy(JobApplication::getStatus, Collectors.counting()));
        long offer = byStatus.getOrDefault(ApplicationStatus.OFFER.name(), 0L);
        d.setApplicationCount(apps.size());
        d.setAppliedStudentCount((int) apps.stream().map(JobApplication::getUserId).distinct().count());
        d.setOfferCount((int) offer);
        d.setOfferRate(apps.isEmpty() ? 0 : round1(100.0 * offer / apps.size()));

        List<DimItem> funnel = new ArrayList<>();
        for (ApplicationStatus st : ApplicationStatus.values()) {
            funnel.add(new DimItem(statusLabel(st), byStatus.getOrDefault(st.name(), 0L)));
        }
        d.setFunnel(funnel);

        // ④ 意向分布 + 技能
        d.setIntentCity(topGroup(profiles, SysStudentProfile::getExpectedCity, TOP_CITY));
        d.setIntentIndustry(topGroup(profiles, SysStudentProfile::getExpectedIndustry, TOP_INDUSTRY));
        d.setSalaryBuckets(salaryBuckets(profiles));
        d.setTopSkills(topSkills(profiles));

        log.info("就业报告聚合: scope={}, 学生={}, 画像={}, 投递={}",
                d.getScopeLabel(), d.getStudentCount(), d.getProfiledCount(), d.getApplicationCount());
        return d;
    }

    // ==================== 工具 ====================

    private String buildLabel(String major, Integer enrollYear) {
        if (StrUtil.isNotBlank(major) && enrollYear != null) {
            return major + " " + enrollYear + " 届";
        }
        if (StrUtil.isNotBlank(major)) {
            return major + " 专业";
        }
        return enrollYear + " 届";
    }

    private String statusLabel(ApplicationStatus st) {
        switch (st) {
            case SUBMITTED: return "已投递";
            case VIEWED: return "已查看";
            case INTERVIEW: return "邀请面试";
            case OFFER: return "已录用";
            case REJECTED: return "不合适";
            default: return st.name();
        }
    }

    private List<DimItem> topGroup(List<SysStudentProfile> profiles, Function<SysStudentProfile, String> key, int topN) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (SysStudentProfile p : profiles) {
            String v = key.apply(p);
            if (v != null && !v.trim().isEmpty()) {
                map.merge(v.trim(), 1L, Long::sum);
            }
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .map(e -> new DimItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<DimItem> salaryBuckets(List<SysStudentProfile> profiles) {
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
        return buckets.entrySet().stream()
                .map(e -> new DimItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<DimItem> topSkills(List<SysStudentProfile> profiles) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (SysStudentProfile p : profiles) {
            for (String s : SkillUtils.parseDistinct(p.getSkills())) {
                map.merge(s, 1L, Long::sum);
            }
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_SKILLS)
                .map(e -> new DimItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private String bucketOf(int salary) {
        for (int i = 0; i < SALARY_LABELS.length; i++) {
            if (salary >= SALARY_BUCKETS[i] && salary < SALARY_BUCKETS[i + 1]) {
                return SALARY_LABELS[i];
            }
        }
        return SALARY_LABELS[SALARY_LABELS.length - 1];
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
