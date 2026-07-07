package com.occupation.recommend.service.impl;

import com.alibaba.fastjson.JSON;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.exception.BizException;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.JobMatchService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.MatchJobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位匹配实现 — 规则打分模型
 * <p>
 * 跨模块协作：调用 analysis 模块的 JobDetailService 拿候选职位（先按意向城市初筛，
 * 数量不足时放开城市限制），再逐条打分排序。
 * <p>
 * TODO(P4-进阶): 行为反馈加权——读取 student_behavior 中 APPLY/IGNORE 记录，
 * 对相似技能职位加权/降权；语义匹配（向量化）作为课程加分项另行评估。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {

    private final StudentProfileService profileService;
    private final JobDetailService jobDetailService;

    /** 候选集大小：先取 200 条再精排，避免全表打分 */
    private static final int CANDIDATE_SIZE = 200;

    /** 学历等级表（用于"学历 ≥ 要求"的比较） */
    private static final Map<String, Integer> EDU_LEVEL = new HashMap<>();
    static {
        EDU_LEVEL.put("不限", 0);
        EDU_LEVEL.put("专科", 1);
        EDU_LEVEL.put("本科", 2);
        EDU_LEVEL.put("硕士", 3);
        EDU_LEVEL.put("博士", 4);
    }

    @Override
    public List<MatchJobVO> match(Long userId, int topN) {
        // 1. 学生画像
        SysStudentProfile profile = profileService.getByUserId(userId);
        if (profile == null) {
            throw new BizException("请先完善个人画像（专业、技能、意向城市等）");
        }
        List<String> mySkills = parseSkills(profile.getSkills());

        // 2. 候选职位：意向城市初筛；不足 20 条时放开城市重查
        List<JobDetailVO> candidates = queryCandidates(profile.getExpectedCity());
        if (candidates.size() < 20) {
            candidates = queryCandidates(null);
        }

        // 3. 逐条打分
        List<MatchJobVO> results = new ArrayList<>();
        for (JobDetailVO job : candidates) {
            results.add(score(profile, mySkills, job));
        }

        // 4. 降序取 Top N
        return results.stream()
                .sorted(Comparator.comparingInt(MatchJobVO::getScore).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    private List<JobDetailVO> queryCandidates(String city) {
        JobQueryDTO query = new JobQueryDTO();
        query.setCity(city);
        query.setPageNum(1);
        query.setPageSize(CANDIDATE_SIZE);
        return jobDetailService.queryJobs(query).getRecords();
    }

    /** 单个职位打分：技能 40 + 城市 25 + 薪资 20 + 学历 15 */
    private MatchJobVO score(SysStudentProfile profile, List<String> mySkills, JobDetailVO job) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // —— 技能匹配（40 分）：职位要求技能被学生覆盖的比例 ——
        List<String> jobSkills = parseSkills(job.getSkills());
        List<String> missing = new ArrayList<>();
        if (!jobSkills.isEmpty()) {
            long hit = jobSkills.stream().filter(s -> containsIgnoreCase(mySkills, s)).count();
            missing = jobSkills.stream().filter(s -> !containsIgnoreCase(mySkills, s))
                               .collect(Collectors.toList());
            int skillScore = (int) Math.round(40.0 * hit / jobSkills.size());
            score += skillScore;
            reasons.add(String.format("技能匹配 %d/%d", hit, jobSkills.size()));
        }

        // —— 城市匹配（25 分） ——
        if (profile.getExpectedCity() != null && profile.getExpectedCity().equals(job.getCity())) {
            score += 25;
            reasons.add("城市一致");
        }

        // —— 薪资匹配（20 分）：区间有交集给满分 ——
        if (profile.getExpectedSalaryMin() != null && job.getSalaryMax() != null
                && job.getSalaryMin() != null) {
            int expectMax = profile.getExpectedSalaryMax() == null
                    ? Integer.MAX_VALUE : profile.getExpectedSalaryMax();
            boolean overlap = profile.getExpectedSalaryMin() <= job.getSalaryMax()
                    && expectMax >= job.getSalaryMin();
            if (overlap) {
                score += 20;
                reasons.add("薪资符合预期");
            }
        }

        // —— 学历匹配（15 分）：学生学历 ≥ 职位要求满分，低一档 5 分 ——
        Integer myEdu = EDU_LEVEL.get(profile.getEducationLevel());
        Integer jobEdu = EDU_LEVEL.get(job.getEducation());
        if (myEdu != null && jobEdu != null) {
            if (myEdu >= jobEdu) {
                score += 15;
                reasons.add("学历符合");
            } else if (jobEdu - myEdu == 1) {
                score += 5;
            }
        }

        MatchJobVO vo = new MatchJobVO();
        vo.setJob(job);
        vo.setScore(score);
        vo.setMatchReason(String.join("、", reasons));
        vo.setMissingSkills(missing);
        return vo;
    }

    /** 技能 JSON 数组解析，容错脏数据 */
    private List<String> parseSkills(String skillsJson) {
        if (skillsJson == null || skillsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(skillsJson, String.class);
        } catch (Exception e) {
            // 兼容逗号分隔的旧格式
            return Arrays.asList(skillsJson.split("[,，]"));
        }
    }

    private boolean containsIgnoreCase(List<String> list, String target) {
        return list.stream().anyMatch(s -> s.equalsIgnoreCase(target.trim()));
    }
}
