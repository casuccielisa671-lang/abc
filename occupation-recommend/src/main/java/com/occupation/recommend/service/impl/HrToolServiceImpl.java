package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.service.UserService;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.HrToolService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.SalaryBenchmarkVO;
import com.occupation.recommend.vo.TalentCompareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HR端工具箱服务实现
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrToolServiceImpl implements HrToolService {

    private final JobDetailMapper jobDetailMapper;
    private final UserService userService;
    private final StudentProfileService profileService;

    @Override
    public TalentCompareVO compareTalents(List<Long> userIds) {
        if (userIds == null || userIds.size() < 2 || userIds.size() > 4) {
            throw new IllegalArgumentException("请选择 2~4 个候选人进行对比");
        }

        Map<Long, SysUser> userMap = userService.mapByIds(new HashSet<>(userIds));
        if (userMap.size() != userIds.size()) {
            throw new IllegalArgumentException("部分候选人不存在");
        }

        List<SysStudentProfile> profiles = profileService.listByUserIds(new HashSet<>(userIds));
        Map<Long, SysStudentProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(SysStudentProfile::getUserId, p -> p, (a, b) -> a));

        TalentCompareVO vo = new TalentCompareVO();
        List<TalentCompareVO.TalentItem> talents = new ArrayList<>();

        for (Long userId : userIds) {
            SysUser user = userMap.get(userId);
            SysStudentProfile profile = profileMap.get(userId);

            TalentCompareVO.TalentItem item = new TalentCompareVO.TalentItem();
            item.setId(userId);
            item.setName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            item.setEducation(profile != null && profile.getEducationLevel() != null ? profile.getEducationLevel() : "未知");
            item.setExperience(profile != null && profile.getEducationLevel() != null
                    ? profile.getEducationLevel() : "未知");

            // 薪资格式化
            if (profile != null && profile.getExpectedSalaryMin() != null && profile.getExpectedSalaryMax() != null) {
                item.setSalary((profile.getExpectedSalaryMin() / 1000) + "K-" + (profile.getExpectedSalaryMax() / 1000) + "K");
            } else {
                item.setSalary("未填写");
            }

            // 技能
            item.setSkills(profile != null ? SkillUtils.parse(profile.getSkills()) : Collections.emptyList());

            // 综合评分：基于技能数量、学历、期望薪资等
            item.setScore(calcTalentScore(profile, userId));

            talents.add(item);
        }
        vo.setTalents(talents);

        // 对比总结
        TalentCompareVO.Summary summary = new TalentCompareVO.Summary();
        TalentCompareVO.TalentItem best = talents.stream()
                .max(Comparator.comparingInt(TalentCompareVO.TalentItem::getScore)).orElse(null);
        TalentCompareVO.TalentItem lowestSalary = talents.stream()
                .filter(t -> !"未填写".equals(t.getSalary()))
                .min(Comparator.comparing(t -> {
                    try { return Integer.parseInt(t.getSalary().split("K")[0]); } catch (Exception e) { return Integer.MAX_VALUE; }
                })).orElse(null);

        summary.setBest(best != null ? best.getName() : "");
        summary.setLowestSalary(lowestSalary != null ? lowestSalary.getName() : "");

        // 共同技能
        List<Set<String>> skillSets = talents.stream()
                .map(t -> new HashSet<>(t.getSkills()))
                .collect(Collectors.toList());
        if (!skillSets.isEmpty()) {
            Set<String> common = new HashSet<>(skillSets.get(0));
            for (int i = 1; i < skillSets.size(); i++) {
                common.retainAll(skillSets.get(i));
            }
            summary.setCommonSkills(new ArrayList<>(common));
        } else {
            summary.setCommonSkills(Collections.emptyList());
        }
        vo.setSummary(summary);

        return vo;
    }

    @Override
    public SalaryBenchmarkVO benchmarkSalary(String jobTitle) {
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入岗位名称");
        }

        String keyword = jobTitle.trim();
        List<JobDetail> allJobs = jobDetailMapper.selectList(null);

        // 筛选匹配岗位
        List<JobDetail> matched = allJobs.stream()
                .filter(j -> j.getTitle() != null && j.getTitle().contains(keyword))
                .collect(Collectors.toList());

        SalaryBenchmarkVO vo = new SalaryBenchmarkVO();

        if (!matched.isEmpty()) {
            List<Integer> midSalaries = matched.stream()
                    .map(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                            ? (j.getSalaryMin() + j.getSalaryMax()) / 2
                            : (j.getSalaryMin() != null ? j.getSalaryMin() : (j.getSalaryMax() != null ? j.getSalaryMax() : 0)))
                    .filter(s -> s > 0)
                    .sorted()
                    .collect(Collectors.toList());

            if (!midSalaries.isEmpty()) {
                int size = midSalaries.size();
                vo.setP25(midSalaries.get(size / 4));
                vo.setP50(midSalaries.get(size / 2));
                vo.setP75(midSalaries.get(size * 3 / 4));
                vo.setP90(midSalaries.get(size * 9 / 10));
            }
        } else {
            // 从全部数据中给出合理默认值
            List<Integer> allSalaries = allJobs.stream()
                    .map(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                            ? (j.getSalaryMin() + j.getSalaryMax()) / 2
                            : (j.getSalaryMin() != null ? j.getSalaryMin() : (j.getSalaryMax() != null ? j.getSalaryMax() : 0)))
                    .filter(s -> s > 0)
                    .sorted()
                    .collect(Collectors.toList());
            if (!allSalaries.isEmpty()) {
                int size = allSalaries.size();
                vo.setP25(allSalaries.get(size / 4));
                vo.setP50(allSalaries.get(size / 2));
                vo.setP75(allSalaries.get(size * 3 / 4));
                vo.setP90(allSalaries.get(size * 9 / 10));
            }
        }

        // 城市薪资对比
        Map<String, List<Integer>> citySalaries = new LinkedHashMap<>();
        for (JobDetail j : matched) {
            if (j.getCity() == null) continue;
            int mid = (j.getSalaryMin() != null && j.getSalaryMax() != null)
                    ? (j.getSalaryMin() + j.getSalaryMax()) / 2 : 0;
            if (mid > 0) {
                citySalaries.computeIfAbsent(j.getCity(), k -> new ArrayList<>()).add(mid);
            }
        }

        List<SalaryBenchmarkVO.CityData> cityData = citySalaries.entrySet().stream()
                .map(e -> {
                    SalaryBenchmarkVO.CityData cd = new SalaryBenchmarkVO.CityData();
                    cd.setCity(e.getKey());
                    cd.setSalary((int) e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0));
                    return cd;
                })
                .sorted((a, b) -> Integer.compare(b.getSalary(), a.getSalary()))
                .collect(Collectors.toList());
        vo.setCityData(cityData);

        return vo;
    }

    // ==================== 人才评分辅助方法 ====================

    private int calcTalentScore(SysStudentProfile profile, Long userId) {
        if (profile == null) return 50;
        int score = 50;
        List<String> skills = SkillUtils.parse(profile.getSkills());
        score += Math.min(30, skills.size() * 5);
        if (profile.getEducationLevel() != null) {
            String edu = profile.getEducationLevel();
            if (edu.contains("硕士") || edu.contains("博士")) score += 10;
            else if (edu.contains("本科")) score += 5;
        }
        // 与市场热门技能匹配度
        List<JobDetail> allJobs = jobDetailMapper.selectList(null);
        Set<String> hotSkills = allJobs.stream()
                .flatMap(j -> SkillUtils.parse(j.getSkills()).stream())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        long matched = skills.stream().filter(s -> hotSkills.stream().anyMatch(h -> h.toLowerCase().contains(s.toLowerCase()))).count();
        score += Math.min(10, (int) matched * 3);
        return Math.min(100, score);
    }
}
