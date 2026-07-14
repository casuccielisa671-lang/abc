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
import com.occupation.recommend.vo.JdOptimizeVO;
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
    public JdOptimizeVO optimizeJd(String jdText) {
        if (jdText == null || jdText.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入职位描述内容");
        }

        String text = jdText.trim();
        JdOptimizeVO vo = new JdOptimizeVO();

        // 各维度评分
        List<JdOptimizeVO.Dimension> dimensions = new ArrayList<>();
        dimensions.add(scoreDimension("吸引力", scoreAttractiveness(text)));
        dimensions.add(scoreDimension("完整性", scoreCompleteness(text)));
        dimensions.add(scoreDimension("关键词覆盖", scoreKeywordCoverage(text)));
        dimensions.add(scoreDimension("结构清晰度", scoreStructure(text)));
        dimensions.add(scoreDimension("薪资竞争力", scoreSalaryCompetitiveness(text)));
        vo.setDimensions(dimensions);

        // 综合评分 = 各维度平均
        int total = dimensions.stream().mapToInt(JdOptimizeVO.Dimension::getScore).sum();
        vo.setScore(total / dimensions.size());

        // 生成优化建议
        List<String> suggestions = generateSuggestions(text, dimensions);
        vo.setSuggestions(suggestions);

        return vo;
    }

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

    // ==================== JD 分析辅助方法 ====================

    private JdOptimizeVO.Dimension scoreDimension(String name, int score) {
        JdOptimizeVO.Dimension d = new JdOptimizeVO.Dimension();
        d.setName(name);
        d.setScore(score);
        return d;
    }

    private int scoreAttractiveness(String text) {
        int score = 50;
        if (text.contains("福利") || text.contains("假期") || text.contains("补贴")) score += 10;
        if (text.contains("团建") || text.contains("氛围") || text.contains("文化")) score += 10;
        if (text.contains("发展") || text.contains("晋升") || text.contains("成长")) score += 10;
        if (text.contains("期权") || text.contains("股权") || text.contains("年终")) score += 10;
        if (text.contains("培训") || text.contains("学习") || text.contains("导师")) score += 10;
        return Math.min(100, score);
    }

    private int scoreCompleteness(String text) {
        int score = 50;
        if (text.contains("职责") || text.contains("负责") || text.contains("工作内容")) score += 10;
        if (text.contains("要求") || text.contains("任职") || text.contains("条件")) score += 10;
        if (text.contains("学历") || text.contains("本科") || text.contains("硕士")) score += 10;
        if (text.contains("经验") || text.contains("年限") || text.matches(".*\\d+年.*")) score += 10;
        if (text.contains("薪资") || text.contains("薪酬") || text.matches(".*\\d+[kK].*")) score += 10;
        return Math.min(100, score);
    }

    private int scoreKeywordCoverage(String text) {
        String lower = text.toLowerCase();
        int score = 50;
        // 从数据库中取热门技能关键词进行匹配
        List<JobDetail> allJobs = jobDetailMapper.selectList(null);
        Set<String> hotSkills = allJobs.stream()
                .flatMap(j -> SkillUtils.parse(j.getSkills()).stream())
                .filter(s -> s.length() >= 2)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        long matched = hotSkills.stream().filter(s -> lower.contains(s.toLowerCase())).count();
        if (matched >= 10) score += 50;
        else if (matched >= 5) score += 30;
        else if (matched >= 2) score += 10;
        return Math.min(100, score);
    }

    private int scoreStructure(String text) {
        int score = 50;
        if (text.contains("【") || text.contains("】") || text.contains("##") || text.contains("1.")) score += 15;
        if (text.split("\\n").length >= 8) score += 15;
        if (text.length() >= 300) score += 10;
        if (text.contains("加分项") || text.contains("优先") || text.contains("熟悉")) score += 10;
        return Math.min(100, score);
    }

    private int scoreSalaryCompetitiveness(String text) {
        int score = 50;
        if (text.matches(".*\\d+[kK]-\\d+[kK].*") || text.matches(".*\\d+[kK].*")) score += 20;
        // 从市场数据判断薪资是否处于合理区间
        List<JobDetail> allJobs = jobDetailMapper.selectList(null);
        double avgMarket = allJobs.stream()
                .mapToInt(j -> (j.getSalaryMin() != null && j.getSalaryMax() != null)
                        ? (j.getSalaryMin() + j.getSalaryMax()) / 2 : 0)
                .filter(s -> s > 0).average().orElse(0);
        // 尝试从文本提取薪资
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)[kK]").matcher(text);
        if (m.find()) {
            int salary = Integer.parseInt(m.group(1)) * 1000;
            if (salary >= avgMarket * 0.9) score += 20;
            else if (salary >= avgMarket * 0.6) score += 10;
        }
        return Math.min(100, score);
    }

    private List<String> generateSuggestions(String text, List<JdOptimizeVO.Dimension> dims) {
        List<String> suggestions = new ArrayList<>();
        for (JdOptimizeVO.Dimension d : dims) {
            if (d.getScore() < 60) {
                switch (d.getName()) {
                    case "吸引力":
                        suggestions.add("开头增加公司亮点和团队文化描述，提升职位吸引力");
                        break;
                    case "完整性":
                        suggestions.add("补充具体的技术栈要求（如 Spring Boot、Vue 等），避免模糊表述");
                        break;
                    case "关键词覆盖":
                        suggestions.add("增加更多行业标准技能关键词，提高职位搜索匹配度");
                        break;
                    case "结构清晰度":
                        suggestions.add("将\"任职要求\"和\"加分项\"分开列出，结构更清晰");
                        break;
                    case "薪资竞争力":
                        suggestions.add("明确薪资范围，当前 JD 未提及薪资信息或薪资低于市场水平");
                        break;
                }
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("JD 质量较高，建议定期回顾并根据市场变化微调");
        }
        suggestions.add("增加职业发展路径说明，让候选人看到成长空间");
        if (text.length() < 200) {
            suggestions.add("JD 内容偏短，建议补充岗位职责和团队情况描述");
        }
        return suggestions;
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
