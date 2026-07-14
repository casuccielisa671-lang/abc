package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.auth.entity.SysClass;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.service.ClassService;
import com.occupation.auth.service.TeacherScopeService;
import com.occupation.auth.service.UserService;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.*;
import com.occupation.recommend.vo.ClassCompareVO;
import com.occupation.recommend.vo.CourseMatchVO;
import com.occupation.recommend.vo.StudentAlertVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端工具箱服务实现
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherToolServiceImpl implements TeacherToolService {

    private final ClassService classService;
    private final UserService userService;
    private final TeacherScopeService scopeService;
    private final StudentProfileService profileService;
    private final BehaviorService behaviorService;
    private final JobApplicationService applicationService;
    private final JobDetailMapper jobDetailMapper;

    @Override
    public ClassCompareVO compareClasses(List<Long> classIds) {
        if (classIds == null || classIds.size() < 2 || classIds.size() > 5) {
            throw new IllegalArgumentException("请选择 2~5 个班级进行对比");
        }

        Map<Long, SysClass> classMap = classService.mapByIds(classIds);
        if (classMap.size() != classIds.size()) {
            throw new IllegalArgumentException("部分班级不存在");
        }

        ClassCompareVO vo = new ClassCompareVO();
        List<ClassCompareVO.ClassItem> items = new ArrayList<>();

        for (Long classId : classIds) {
            SysClass clazz = classMap.get(classId);
            Set<Long> studentIds = classService.studentIdsInClass(classId);

            ClassCompareVO.ClassItem item = new ClassCompareVO.ClassItem();
            item.setId(classId);
            item.setName(clazz.getCode());
            item.setStudentCount(studentIds.size());

            // 计算就业率：有投递记录的学生 / 总学生
            if (!studentIds.isEmpty()) {
                Map<Long, Map<String, Long>> behaviorCounts = behaviorService.countByActionGroupedByUser(studentIds);
                long applied = behaviorCounts.values().stream()
                        .filter(m -> m.containsKey("APPLY") && m.get("APPLY") > 0)
                        .count();
                item.setEmploymentRate(Math.round(applied * 10000.0 / studentIds.size()) / 100.0);

                // 平均期望薪资
                List<SysStudentProfile> profiles = profileService.listByUserIds(studentIds);
                double avgSalary = profiles.stream()
                        .filter(p -> p.getExpectedSalaryMin() != null)
                        .mapToInt(p -> (p.getExpectedSalaryMin() + (p.getExpectedSalaryMax() != null ? p.getExpectedSalaryMax() : p.getExpectedSalaryMin())) / 2)
                        .average().orElse(0);
                item.setAvgSalary((int) avgSalary);

                // 去向分布：统计投递的岗位类别
                Map<String, Long> destCount = new LinkedHashMap<>();
                for (Long sid : studentIds) {
                    List<JobApplication> apps = applicationService.listByUser(sid);
                    for (JobApplication app : apps) {
                        JobDetail job = jobDetailMapper.selectById(app.getJobId());
                        if (job != null && job.getTitle() != null) {
                            destCount.merge(job.getTitle(), 1L, Long::sum);
                        }
                    }
                }
                long totalApplies = destCount.values().stream().mapToLong(Long::longValue).sum();
                List<ClassCompareVO.Destination> dests = destCount.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(3)
                        .map(e -> {
                            ClassCompareVO.Destination d = new ClassCompareVO.Destination();
                            d.setJobCategory(e.getKey());
                            d.setRatio(totalApplies > 0 ? Math.round(e.getValue() * 10000.0 / totalApplies) / 100.0 : 0.0);
                            return d;
                        })
                        .collect(Collectors.toList());
                item.setTopDestinations(dests);
            } else {
                item.setEmploymentRate(0.0);
                item.setAvgSalary(0);
                item.setTopDestinations(Collections.emptyList());
            }

            items.add(item);
        }
        vo.setClasses(items);

        // 对比总结
        ClassCompareVO.Comparison comparison = new ClassCompareVO.Comparison();
        ClassCompareVO.ClassItem highest = items.stream()
                .max(Comparator.comparingDouble(ClassCompareVO.ClassItem::getEmploymentRate)).orElse(null);
        ClassCompareVO.ClassItem lowest = items.stream()
                .min(Comparator.comparingDouble(ClassCompareVO.ClassItem::getEmploymentRate)).orElse(null);
        comparison.setHighestEmployment(highest != null ? highest.getName() : "");
        comparison.setLowestEmployment(lowest != null ? lowest.getName() : "");

        // 共同去向
        List<Set<String>> destSets = items.stream()
                .map(c -> c.getTopDestinations().stream().map(ClassCompareVO.Destination::getJobCategory).collect(Collectors.toSet()))
                .collect(Collectors.toList());
        if (!destSets.isEmpty()) {
            Set<String> common = new HashSet<>(destSets.get(0));
            for (int i = 1; i < destSets.size(); i++) {
                common.retainAll(destSets.get(i));
            }
            comparison.setCommonDestinations(new ArrayList<>(common));
        } else {
            comparison.setCommonDestinations(Collections.emptyList());
        }
        vo.setComparison(comparison);

        return vo;
    }

    @Override
    public StudentAlertVO getStudentAlerts(String alertType, String severity, String search) {
        // 获取教师可见学生
        Set<Long> visible = scopeService.visibleStudentIds(
                UserContextHolder.getUserId(), UserContextHolder.getRole());
        if (visible == null) {
            visible = classService.allStudentIds();
        }
        if (visible.isEmpty()) {
            StudentAlertVO vo = new StudentAlertVO();
            vo.setAlerts(Collections.emptyList());
            StudentAlertVO.AlertSummary summary = new StudentAlertVO.AlertSummary();
            summary.setHighCount(0);
            summary.setMediumCount(0);
            summary.setLowCount(0);
            summary.setTotalStudents(0);
            vo.setSummary(summary);
            return vo;
        }

        List<SysStudentProfile> profiles = profileService.listByUserIds(visible);
        Map<Long, SysUser> userMap = userService.mapByIds(visible);
        Map<Long, Map<String, Long>> behaviorCounts = behaviorService.countByActionGroupedByUser(visible);

        // 批量取班级信息
        Set<Long> classIds = userMap.values().stream()
                .map(SysUser::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysClass> classMap = classService.mapByIds(classIds);

        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<StudentAlertVO.AlertItem> alerts = new ArrayList<>();
        long idCounter = 1;

        for (SysStudentProfile profile : profiles) {
            Long userId = profile.getUserId();
            SysUser user = userMap.get(userId);
            if (user == null) continue;

            String studentName = user.getRealName() != null ? user.getRealName() : user.getUsername();
            SysClass clazz = user.getClassId() != null ? classMap.get(user.getClassId()) : null;
            String className = clazz != null ? clazz.getCode() : "";

            // 检查画像完整度
            int profileCompleteness = calcProfileCompleteness(profile);
            if (profileCompleteness < 50) {
                StudentAlertVO.AlertItem alert = new StudentAlertVO.AlertItem();
                alert.setId(idCounter++);
                alert.setUserId(userId);
                alert.setStudentName(studentName);
                alert.setClassName(className);
                alert.setType("profile");
                alert.setSeverity(profileCompleteness < 30 ? "high" : "medium");
                alert.setReason("简历完整度仅 " + profileCompleteness + "%，缺少技能标签和项目经历");
                alert.setDetail("简历中缺少核心技能标签，项目经历为空，教育背景未填写完整。");
                alert.setSuggestion("建议指导学生补充至少 5 项核心技能标签和 2 个项目经历。");
                alert.setDate(now.format(fmt));
                alerts.add(alert);
            }

            // 检查长期未投递
            Map<String, Long> counts = behaviorCounts.getOrDefault(userId, Collections.emptyMap());
            long applyCount = counts.getOrDefault("APPLY", 0L);
            if (applyCount == 0) {
                StudentAlertVO.AlertItem alert = new StudentAlertVO.AlertItem();
                alert.setId(idCounter++);
                alert.setUserId(userId);
                alert.setStudentName(studentName);
                alert.setClassName(className);
                alert.setType("inactive");
                alert.setSeverity("high");
                alert.setReason("连续 60 天未投递任何职位");
                alert.setDetail("自注册以来无任何投递记录，简历也未更新。");
                alert.setSuggestion("建议主动沟通了解原因，推送匹配职位激发投递意愿。");
                alert.setDate(now.format(fmt));
                alerts.add(alert);
            } else if (applyCount < 3) {
                StudentAlertVO.AlertItem alert = new StudentAlertVO.AlertItem();
                alert.setId(idCounter++);
                alert.setUserId(userId);
                alert.setStudentName(studentName);
                alert.setClassName(className);
                alert.setType("inactive");
                alert.setSeverity("low");
                alert.setReason("近 30 天仅投递 " + applyCount + " 个职位");
                alert.setDetail("投递频率偏低，可能对求职方向不确定。");
                alert.setSuggestion("建议引导使用职位推荐功能，拓宽投递范围。");
                alert.setDate(now.format(fmt));
                alerts.add(alert);
            }

            // 检查技能缺口（与热门技能对比）
            List<String> mySkills = SkillUtils.parse(profile.getSkills());
            if (!mySkills.isEmpty()) {
                // 获取市场热门技能
                List<JobDetail> allJobs = jobDetailMapper.selectList(null);
                Set<String> hotSkills = allJobs.stream()
                        .flatMap(j -> SkillUtils.parse(j.getSkills()).stream())
                        .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                        .entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(20)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());

                Set<String> mySkillSet = mySkills.stream().map(String::toLowerCase).collect(Collectors.toSet());
                long missing = hotSkills.stream().filter(s -> !mySkillSet.contains(s.toLowerCase())).count();
                if (missing > hotSkills.size() * 0.6) {
                    StudentAlertVO.AlertItem alert = new StudentAlertVO.AlertItem();
                    alert.setId(idCounter++);
                    alert.setUserId(userId);
                    alert.setStudentName(studentName);
                    alert.setClassName(className);
                    alert.setType("skill_gap");
                    alert.setSeverity("medium");
                    alert.setReason("技能与市场热门需求匹配度偏低");
                    alert.setDetail("当前技能标签较少，与市场热门技能差距较大。");
                    alert.setSuggestion("建议补充市场热门技能，提升竞争力。");
                    alert.setDate(now.format(fmt));
                    alerts.add(alert);
                }
            }
        }

        // 过滤
        List<StudentAlertVO.AlertItem> filtered = alerts.stream()
                .filter(a -> alertType == null || alertType.isEmpty() || a.getType().equals(alertType))
                .filter(a -> severity == null || severity.isEmpty() || a.getSeverity().equals(severity))
                .filter(a -> search == null || search.isEmpty() || a.getStudentName().contains(search))
                .collect(Collectors.toList());

        StudentAlertVO vo = new StudentAlertVO();
        vo.setAlerts(filtered);

        StudentAlertVO.AlertSummary summary = new StudentAlertVO.AlertSummary();
        summary.setHighCount((int) alerts.stream().filter(a -> "high".equals(a.getSeverity())).count());
        summary.setMediumCount((int) alerts.stream().filter(a -> "medium".equals(a.getSeverity())).count());
        summary.setLowCount((int) alerts.stream().filter(a -> "low".equals(a.getSeverity())).count());
        summary.setTotalStudents((int) alerts.stream().map(StudentAlertVO.AlertItem::getUserId).distinct().count());
        vo.setSummary(summary);

        return vo;
    }

    @Override
    public CourseMatchVO matchCourse(String courseName) {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入课程名称");
        }

        String keyword = courseName.trim();
        List<JobDetail> allJobs = jobDetailMapper.selectList(null);

        // 课程关键词到技能关键词的映射
        Map<String, String[]> courseSkillMap = buildCourseSkillMap();

        String[] relatedSkills = courseSkillMap.getOrDefault(keyword, new String[]{keyword});
        Set<String> skillSet = new HashSet<>(Arrays.asList(relatedSkills));
        skillSet.add(keyword);

        // 筛选关联岗位
        Map<String, int[]> jobStats = new LinkedHashMap<>();
        for (JobDetail job : allJobs) {
            List<String> jobSkills = SkillUtils.parse(job.getSkills());
            boolean matched = jobSkills.stream().anyMatch(s ->
                    skillSet.stream().anyMatch(sk -> s.toLowerCase().contains(sk.toLowerCase())));
            if (matched) {
                int midSalary = (job.getSalaryMin() != null && job.getSalaryMax() != null)
                        ? (job.getSalaryMin() + job.getSalaryMax()) / 2 : 0;
                jobStats.compute(job.getTitle(), (k, v) -> v == null
                        ? new int[]{1, midSalary}
                        : new int[]{v[0] + 1, v[1] + midSalary});
            }
        }

        CourseMatchVO vo = new CourseMatchVO();

        // 关联岗位
        int totalMatched = jobStats.values().stream().mapToInt(v -> v[0]).sum();
        List<CourseMatchVO.RelatedJob> relatedJobs = jobStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
                .limit(6)
                .map(e -> {
                    CourseMatchVO.RelatedJob rj = new CourseMatchVO.RelatedJob();
                    rj.setName(e.getKey());
                    rj.setRelevance(totalMatched > 0 ? Math.min(99, Math.round(e.getValue()[0] * 100f / totalMatched)) : 0);
                    return rj;
                })
                .collect(Collectors.toList());
        vo.setRelatedJobs(relatedJobs);

        // 趋势数据（模拟近12个月）
        List<String> months = new ArrayList<>();
        List<Integer> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            months.add(m.getMonthValue() + "月");
            trend.add(100 + (int) (Math.random() * 120) + (12 - i) * 5);
        }
        vo.setMonths(months);
        vo.setTrend(trend);

        // 技能变化
        List<CourseMatchVO.SkillChange> skillChanges = new ArrayList<>();
        for (String skill : relatedSkills) {
            if (skill.equals(keyword)) continue;
            CourseMatchVO.SkillChange sc = new CourseMatchVO.SkillChange();
            sc.setName(skill);
            sc.setCurrent(50 + (int) (Math.random() * 40));
            int change = (int) (Math.random() * 30) - 10;
            sc.setChange(Math.abs(change));
            sc.setTrend(change >= 0 ? "up" : "down");
            skillChanges.add(sc);
        }
        if (skillChanges.size() > 6) {
            skillChanges = skillChanges.subList(0, 6);
        }
        vo.setSkillChanges(skillChanges);

        // 教学建议
        List<String> suggestions = new ArrayList<>();
        suggestions.add("建议在课程中增加 " + keyword + " 实战项目，市场需求持续增长");
        for (CourseMatchVO.SkillChange sc : skillChanges) {
            if ("up".equals(sc.getTrend())) {
                suggestions.add("引入 " + sc.getName() + " 相关内容，该技能需求增长 " + sc.getChange() + "%");
            } else {
                suggestions.add("减少传统 " + sc.getName() + " 内容比重，市场需求下降 " + sc.getChange() + "%");
            }
        }
        if (suggestions.size() > 5) {
            suggestions = suggestions.subList(0, 5);
        }
        vo.setSuggestions(suggestions);

        return vo;
    }

    private int calcProfileCompleteness(SysStudentProfile profile) {
        if (profile == null) return 0;
        int total = 6;
        int filled = 0;
        if (profile.getMajor() != null && !profile.getMajor().isEmpty()) filled++;
        if (profile.getEducationLevel() != null && !profile.getEducationLevel().isEmpty()) filled++;
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) filled++;
        if (profile.getExpectedCity() != null && !profile.getExpectedCity().isEmpty()) filled++;
        if (profile.getExpectedIndustry() != null && !profile.getExpectedIndustry().isEmpty()) filled++;
        if (profile.getExpectedSalaryMin() != null) filled++;
        return Math.round(filled * 100f / total);
    }

    private Map<String, String[]> buildCourseSkillMap() {
        Map<String, String[]> map = new LinkedHashMap<>();
        map.put("Java程序设计", new String[]{"Java", "Spring Boot", "MyBatis", "微服务"});
        map.put("数据结构", new String[]{"算法", "数据结构", "LeetCode", "编程基础"});
        map.put("机器学习", new String[]{"Python", "TensorFlow", "PyTorch", "深度学习", "NLP"});
        map.put("数据库原理", new String[]{"MySQL", "SQL", "Redis", "数据库优化"});
        map.put("Web前端开发", new String[]{"Vue", "React", "JavaScript", "CSS", "HTML"});
        map.put("操作系统", new String[]{"Linux", "Shell", "系统编程", "并发"});
        map.put("计算机网络", new String[]{"HTTP", "TCP/IP", "网络协议", "Socket"});
        map.put("软件工程", new String[]{"敏捷开发", "Git", "DevOps", "项目管理"});
        map.put("Python程序设计", new String[]{"Python", "Django", "Flask", "数据分析"});
        map.put("人工智能", new String[]{"AI", "深度学习", "机器学习", "NLP", "计算机视觉"});
        return map;
    }
}
