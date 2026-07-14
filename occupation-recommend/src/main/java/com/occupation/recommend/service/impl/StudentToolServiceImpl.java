package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.service.StudentToolService;
import com.occupation.recommend.vo.JobChecklistVO;
import com.occupation.recommend.vo.JobCompareVO;
import com.occupation.recommend.vo.SalaryCalcVO;
import com.occupation.recommend.vo.SkillRoiVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生端工具箱服务实现
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentToolServiceImpl implements StudentToolService {

    private final JobDetailMapper jobDetailMapper;
    private final StudentProfileService profileService;

    @Override
    public JobCompareVO compareJobs(List<Long> jobIds) {
        if (jobIds == null || jobIds.size() < 2 || jobIds.size() > 4) {
            throw new IllegalArgumentException("请选择 2~4 个岗位进行对比");
        }

        List<JobDetail> jobs = jobDetailMapper.selectBatchIds(jobIds);
        if (jobs.size() != jobIds.size()) {
            throw new IllegalArgumentException("部分岗位不存在");
        }

        JobCompareVO vo = new JobCompareVO();

        List<JobCompareVO.JobItem> items = jobs.stream().map(j -> {
            JobCompareVO.JobItem item = new JobCompareVO.JobItem();
            item.setId(j.getId());
            item.setTitle(j.getTitle());
            item.setCompany(j.getCompany());
            item.setCity(j.getCity());
            item.setIndustry(j.getIndustry());
            item.setSalaryMin(j.getSalaryMin());
            item.setSalaryMax(j.getSalaryMax());
            item.setSalaryRange(formatSalary(j.getSalaryMin(), j.getSalaryMax()));
            item.setEducation(j.getEducation());
            item.setExperience(j.getExperience());
            item.setSkills(SkillUtils.parse(j.getSkills()));
            item.setPublishDate(j.getPublishDate() != null ? j.getPublishDate().toString() : null);
            return item;
        }).collect(Collectors.toList());
        vo.setJobs(items);

        // 对比总结
        JobCompareVO.CompareSummary summary = new JobCompareVO.CompareSummary();
        JobCompareVO.JobItem highest = items.stream()
                .max(Comparator.comparingInt(j -> j.getSalaryMax() != null ? j.getSalaryMax() : 0)).orElse(null);
        JobCompareVO.JobItem lowest = items.stream()
                .min(Comparator.comparingInt(j -> j.getSalaryMin() != null ? j.getSalaryMin() : Integer.MAX_VALUE)).orElse(null);
        summary.setHighestSalary(highest != null ? highest.getTitle() : "");
        summary.setLowestSalary(lowest != null ? lowest.getTitle() : "");

        // 共同技能
        List<Set<String>> skillSets = items.stream()
                .map(j -> new HashSet<>(j.getSkills()))
                .collect(Collectors.toList());
        Set<String> common = new HashSet<>(skillSets.get(0));
        for (int i = 1; i < skillSets.size(); i++) {
            common.retainAll(skillSets.get(i));
        }
        summary.setCommonSkills(new ArrayList<>(common));

        // 各岗位独有技能
        Map<String, List<String>> uniqueMap = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            Set<String> unique = new HashSet<>(items.get(i).getSkills());
            for (int j = 0; j < items.size(); j++) {
                if (i != j) {
                    unique.removeAll(items.get(j).getSkills());
                }
            }
            uniqueMap.put(items.get(i).getTitle(), new ArrayList<>(unique));
        }
        summary.setUniqueSkills(uniqueMap);
        vo.setSummary(summary);

        return vo;
    }

    @Override
    public SkillRoiVO analyzeSkillRoi(String skillName) {
        if (skillName == null || skillName.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入技能名称");
        }

        String keyword = skillName.trim();
        List<JobDetail> allJobs = jobDetailMapper.selectList(null);
        if (allJobs.isEmpty()) {
            throw new IllegalStateException("暂无职位数据");
        }

        // 筛选要求该技能的职位
        List<JobDetail> matched = allJobs.stream()
                .filter(j -> {
                    List<String> skills = SkillUtils.parse(j.getSkills());
                    return skills.stream().anyMatch(s -> s.toLowerCase().contains(keyword.toLowerCase()));
                })
                .collect(Collectors.toList());

        SkillRoiVO vo = new SkillRoiVO();
        vo.setSkillName(keyword);
        vo.setJobCount(matched.size());
        vo.setMarketShare(allJobs.isEmpty() ? 0 :
                Math.round(matched.size() * 10000.0 / allJobs.size()) / 100.0);

        if (!matched.isEmpty()) {
            List<Integer> salaries = matched.stream()
                    .map(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                            ? (j.getSalaryMin() + j.getSalaryMax()) / 2
                            : (j.getSalaryMin() != null ? j.getSalaryMin() : (j.getSalaryMax() != null ? j.getSalaryMax() : 0)))
                    .filter(s -> s > 0)
                    .sorted()
                    .collect(Collectors.toList());

            if (!salaries.isEmpty()) {
                vo.setAvgSalary((int) salaries.stream().mapToInt(Integer::intValue).average().orElse(0));
                vo.setMedianSalary(salaries.get(salaries.size() / 2));
            }

            // 薪资溢价：有该技能 vs 无该技能
            List<Integer> withoutSkill = allJobs.stream()
                    .filter(j -> {
                        List<String> skills = SkillUtils.parse(j.getSkills());
                        return skills.stream().noneMatch(s -> s.toLowerCase().contains(keyword.toLowerCase()));
                    })
                    .map(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                            ? (j.getSalaryMin() + j.getSalaryMax()) / 2
                            : (j.getSalaryMin() != null ? j.getSalaryMin() : (j.getSalaryMax() != null ? j.getSalaryMax() : 0)))
                    .filter(s -> s > 0)
                    .collect(Collectors.toList());

            int withAvg = salaries.isEmpty() ? 0 : (int) salaries.stream().mapToInt(Integer::intValue).average().orElse(0);
            int withoutAvg = withoutSkill.isEmpty() ? 0 : (int) withoutSkill.stream().mapToInt(Integer::intValue).average().orElse(0);
            vo.setSalaryPremium(withAvg - withoutAvg);
        }

        // 相关岗位 Top 5
        Map<String, int[]> jobStats = new LinkedHashMap<>();
        for (JobDetail j : matched) {
            String title = j.getTitle();
            int midSalary = (j.getSalaryMin() != null && j.getSalaryMax() != null)
                    ? (j.getSalaryMin() + j.getSalaryMax()) / 2 : 0;
            jobStats.compute(title, (k, v) -> v == null
                    ? new int[]{1, midSalary}
                    : new int[]{v[0] + 1, v[1] + midSalary});
        }
        List<SkillRoiVO.RelatedJob> relatedJobs = jobStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(e -> {
                    SkillRoiVO.RelatedJob rj = new SkillRoiVO.RelatedJob();
                    rj.setTitle(e.getKey());
                    rj.setJobCount(e.getValue()[0]);
                    rj.setAvgSalary(e.getValue()[1] / e.getValue()[0]);
                    return rj;
                })
                .collect(Collectors.toList());
        vo.setRelatedJobs(relatedJobs);

        // 学习建议
        List<String> suggestions = new ArrayList<>();
        if (vo.getSalaryPremium() > 3000) {
            suggestions.add("该技能薪资溢价明显（+" + (vo.getSalaryPremium() / 1000) + "k），强烈建议深入学习");
        } else if (vo.getSalaryPremium() > 0) {
            suggestions.add("该技能有一定薪资加成（+" + (vo.getSalaryPremium() / 1000) + "k），建议掌握基础");
        } else {
            suggestions.add("该技能为市场基础要求，建议作为必备技能掌握");
        }
        if (vo.getMarketShare() > 30) {
            suggestions.add("超过 " + String.format("%.1f", vo.getMarketShare()) + "% 的岗位要求该技能，市场需求旺盛");
        } else if (vo.getMarketShare() > 10) {
            suggestions.add("约 " + String.format("%.1f", vo.getMarketShare()) + "% 的岗位要求该技能，有一定市场需求");
        } else if (vo.getMarketShare() > 0) {
            suggestions.add("该技能为差异化竞争力，建议作为加分项学习");
        }
        vo.setSuggestions(suggestions);

        return vo;
    }

    @Override
    public SalaryCalcVO calcSalary(String city, String keyword, String education, String experience) {
        LambdaQueryWrapper<JobDetail> wrapper = new LambdaQueryWrapper<>();
        if (city != null && !city.trim().isEmpty()) {
            wrapper.eq(JobDetail::getCity, city.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(JobDetail::getTitle, keyword.trim());
        }
        if (education != null && !education.trim().isEmpty()) {
            wrapper.eq(JobDetail::getEducation, education.trim());
        }
        if (experience != null && !experience.trim().isEmpty()) {
            wrapper.eq(JobDetail::getExperience, experience.trim());
        }

        List<JobDetail> jobs = jobDetailMapper.selectList(wrapper);
        if (jobs.isEmpty()) {
            // 放宽条件：只用关键词
            wrapper = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.like(JobDetail::getTitle, keyword.trim());
            }
            jobs = jobDetailMapper.selectList(wrapper);
        }

        SalaryCalcVO vo = new SalaryCalcVO();
        vo.setSampleCount(jobs.size());

        if (jobs.isEmpty()) {
            vo.setSuggestedMin(0);
            vo.setSuggestedMax(0);
            vo.setMarketMedian(0);
            vo.setMarketP25(0);
            vo.setMarketP75(0);
            vo.setCityBreakdown(Collections.emptyList());
            vo.setEducationBreakdown(Collections.emptyList());
            return vo;
        }

        List<Integer> midSalaries = jobs.stream()
                .map(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                        ? (j.getSalaryMin() + j.getSalaryMax()) / 2
                        : (j.getSalaryMin() != null ? j.getSalaryMin() : (j.getSalaryMax() != null ? j.getSalaryMax() : 0)))
                .filter(s -> s > 0)
                .sorted()
                .collect(Collectors.toList());

        if (!midSalaries.isEmpty()) {
            int size = midSalaries.size();
            vo.setMarketP25(midSalaries.get(size / 4));
            vo.setMarketMedian(midSalaries.get(size / 2));
            vo.setMarketP75(midSalaries.get(size * 3 / 4));
            // 建议范围：P25 ~ P75
            vo.setSuggestedMin(vo.getMarketP25());
            vo.setSuggestedMax(vo.getMarketP75());
        }

        // 按城市分组
        Map<String, List<JobDetail>> byCity = jobs.stream()
                .filter(j -> j.getCity() != null)
                .collect(Collectors.groupingBy(JobDetail::getCity));
        List<SalaryCalcVO.CitySalary> cityBreakdown = byCity.entrySet().stream()
                .map(e -> {
                    SalaryCalcVO.CitySalary cs = new SalaryCalcVO.CitySalary();
                    cs.setCity(e.getKey());
                    cs.setJobCount(e.getValue().size());
                    cs.setAvgSalary((int) e.getValue().stream()
                            .mapToInt(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                                    ? (j.getSalaryMin() + j.getSalaryMax()) / 2 : 0)
                            .filter(s -> s > 0)
                            .average().orElse(0));
                    return cs;
                })
                .sorted((a, b) -> Integer.compare(b.getAvgSalary(), a.getAvgSalary()))
                .limit(6)
                .collect(Collectors.toList());
        vo.setCityBreakdown(cityBreakdown);

        // 按学历分组
        Map<String, List<JobDetail>> byEdu = jobs.stream()
                .filter(j -> j.getEducation() != null)
                .collect(Collectors.groupingBy(JobDetail::getEducation));
        List<SalaryCalcVO.EducationSalary> eduBreakdown = byEdu.entrySet().stream()
                .map(e -> {
                    SalaryCalcVO.EducationSalary es = new SalaryCalcVO.EducationSalary();
                    es.setEducation(e.getKey());
                    es.setJobCount(e.getValue().size());
                    es.setAvgSalary((int) e.getValue().stream()
                            .mapToInt(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                                    ? (j.getSalaryMin() + j.getSalaryMax()) / 2 : 0)
                            .filter(s -> s > 0)
                            .average().orElse(0));
                    return es;
                })
                .collect(Collectors.toList());
        vo.setEducationBreakdown(eduBreakdown);

        return vo;
    }

    @Override
    public JobChecklistVO generateChecklist(Long jobId, Long userId) {
        JobDetail job = jobDetailMapper.selectById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }

        SysStudentProfile profile = profileService.getByUserId(userId);

        JobChecklistVO vo = new JobChecklistVO();
        vo.setJobTitle(job.getTitle());

        List<String> jobSkills = SkillUtils.parse(job.getSkills());
        List<String> mySkills = profile != null ? SkillUtils.parse(profile.getSkills()) : Collections.emptyList();
        Set<String> mySkillSet = mySkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // 技能差距分析
        List<JobChecklistVO.SkillGap> gaps = new ArrayList<>();
        int matched = 0;
        for (String skill : jobSkills) {
            JobChecklistVO.SkillGap gap = new JobChecklistVO.SkillGap();
            gap.setSkill(skill);
            gap.setRequirement("required");
            boolean possessed = mySkillSet.contains(skill.toLowerCase());
            gap.setPossessed(possessed);
            if (possessed) {
                matched++;
                gap.setDescription("已掌握");
            } else {
                gap.setDescription("建议学习");
            }
            gaps.add(gap);
        }
        vo.setSkillGaps(gaps);
        vo.setMatchScore(jobSkills.isEmpty() ? 50 : Math.round(matched * 100f / jobSkills.size()));

        // 学习路径建议
        List<JobChecklistVO.LearningStep> learningPath = new ArrayList<>();
        List<String> missingSkills = gaps.stream()
                .filter(g -> !g.isPossessed())
                .map(JobChecklistVO.SkillGap::getSkill)
                .collect(Collectors.toList());

        int order = 1;
        if (!missingSkills.isEmpty()) {
            JobChecklistVO.LearningStep step1 = new JobChecklistVO.LearningStep();
            step1.setOrder(order++);
            step1.setTitle("补齐核心技能");
            step1.setDescription("优先学习以下技能：" + String.join("、", missingSkills.subList(0, Math.min(3, missingSkills.size()))));
            step1.setEstimatedTime("4-8 周");
            learningPath.add(step1);
        }

        JobChecklistVO.LearningStep step2 = new JobChecklistVO.LearningStep();
        step2.setOrder(order++);
        step2.setTitle("项目实战");
        step2.setDescription("用学到的技能完成 1-2 个完整项目，发布到 GitHub 并写入简历");
        step2.setEstimatedTime("2-4 周");
        learningPath.add(step2);

        JobChecklistVO.LearningStep step3 = new JobChecklistVO.LearningStep();
        step3.setOrder(order++);
        step3.setTitle("简历优化");
        step3.setDescription("将新技能和项目经历更新到简历中，突出与目标岗位的匹配度");
        step3.setEstimatedTime("1-2 天");
        learningPath.add(step3);

        JobChecklistVO.LearningStep step4 = new JobChecklistVO.LearningStep();
        step4.setOrder(order++);
        step4.setTitle("模拟面试");
        step4.setDescription("针对目标岗位常见面试题进行模拟练习，准备项目介绍和技术问答");
        step4.setEstimatedTime("1-2 周");
        learningPath.add(step4);
        vo.setLearningPath(learningPath);

        // 简历优化建议
        List<String> resumeTips = new ArrayList<>();
        if (profile == null || profile.getSkills() == null || profile.getSkills().isEmpty()) {
            resumeTips.add("建议完善个人画像中的技能标签");
        }
        if (profile == null || profile.getExpectedCity() == null) {
            resumeTips.add("建议填写意向城市，提高职位匹配精度");
        }
        if (profile == null || profile.getExpectedSalaryMin() == null) {
            resumeTips.add("建议填写期望薪资范围");
        }
        resumeTips.add("在简历中突出与" + job.getTitle() + "相关的技能和项目经验");
        resumeTips.add("使用岗位描述中的关键词优化简历，提高匹配度");
        vo.setResumeTips(resumeTips);

        // 推荐学习资源
        List<String> resources = new ArrayList<>();
        resources.add("在线课程平台：慕课网、B站相关技术教程");
        resources.add("实战平台：GitHub 搜索相关开源项目参与贡献");
        resources.add("刷题平台：LeetCode、牛客网");
        if (!missingSkills.isEmpty()) {
            resources.add("推荐搜索：" + String.join("、", missingSkills.subList(0, Math.min(3, missingSkills.size()))) + " 入门教程");
        }
        vo.setResources(resources);

        return vo;
    }

    private String formatSalary(Integer min, Integer max) {
        if (min == null && max == null) return "薪资面议";
        if (max == null) return (min / 1000) + "k 起";
        if (min == null) return "至 " + (max / 1000) + "k";
        return (min / 1000) + "k-" + (max / 1000) + "k";
    }
}
