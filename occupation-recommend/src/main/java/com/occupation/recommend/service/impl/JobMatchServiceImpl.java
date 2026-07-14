package com.occupation.recommend.service.impl;

import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.exception.BizException;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.JobMatchService;
import com.occupation.recommend.service.SemanticMatchService;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.vo.MatchJobVO;
import com.occupation.recommend.vo.SemanticMatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位匹配实现 — 规则打分 + 语义匹配 + 行为反馈加权
 * <p>
 * 跨模块协作：调用 analysis 模块的 JobDetailService 拿候选职位（先按意向城市初筛，
 * 数量不足时放开城市限制），再逐条打分排序。
 * <p>
 * 打分由三部分组成：
 * <ol>
 *   <li><b>基础规则分 0~100</b>：技能 40 + 城市 25 + 薪资 20 + 学历 15；</li>
 *   <li><b>语义匹配 +0~15</b>：调用 DeepSeek 做 JD-简历语义相似度计算，弥补字符串匹配的不足；</li>
 *   <li><b>行为加权 -10~+10</b>：从学生历史 APPLY / FAVORITE / IGNORE 记录反推技能偏好，
 *       与候选职位的技能求交后加减分。已投递、已忽略的职位不再出现在推荐里。</li>
 * </ol>
 * 最终分裁剪回 0~100，保证前端展示的匹配分语义稳定。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {

    private final StudentProfileService profileService;
    private final JobDetailService jobDetailService;
    private final BehaviorService behaviorService;
    private final SemanticMatchService semanticMatchService;

    /** 候选集大小：先取 200 条再精排，避免全表打分 */
    private static final int CANDIDATE_SIZE = 200;

    /** 参与行为加权的历史行为条数上限（按时间倒序取） */
    private static final int BEHAVIOR_WINDOW = 100;

    /** 行为对技能偏好的贡献权重 */
    private static final int W_APPLY = 2;
    /** 自主联系与投递同级：学生愿意跳出平台去联系，意愿强度不弱于站内投递 */
    private static final int W_CONTACT = 2;
    private static final int W_FAVORITE = 1;
    private static final int W_IGNORE = -2;

    /** 行为加权对总分的影响上限（正负各 10 分），防止盖过基础规则分 */
    private static final int MAX_BEHAVIOR_BONUS = 10;

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
        List<String> mySkills = SkillUtils.parse(profile.getSkills());

        // 2. 行为反馈：技能偏好 + 需要排除的职位
        List<StudentBehavior> behaviors = behaviorService.listByUser(userId, BEHAVIOR_WINDOW);
        Map<String, Integer> skillAffinity = buildSkillAffinity(behaviors);
        // 已经处理过的职位不再推荐：投过、忽略过、或已经自主联系过
        Set<Long> excluded = behaviors.stream()
                .filter(b -> BehaviorAction.APPLY.equals(b.getAction())
                        || BehaviorAction.IGNORE.equals(b.getAction())
                        || BehaviorAction.CONTACT.equals(b.getAction()))
                .map(StudentBehavior::getJobId)
                .collect(Collectors.toSet());

        // 3. 候选职位：意向城市初筛；不足 20 条时放开城市重查
        List<JobDetailVO> candidates = queryCandidates(profile.getExpectedCity());
        if (candidates.size() < 20) {
            candidates = queryCandidates(null);
        }

        // 4. 逐条打分（跳过已投递/已忽略）
        List<MatchJobVO> results = new ArrayList<>();
        for (JobDetailVO job : candidates) {
            if (excluded.contains(job.getId())) {
                continue;
            }
            results.add(score(profile, mySkills, job, skillAffinity));
        }

        log.info("推荐打分完成: userId={}, 候选={}, 排除已投递/已忽略={}, 偏好技能={}",
                userId, candidates.size(), excluded.size(), skillAffinity.size());

        // 5. 降序取 Top N
        return results.stream()
                .sorted(Comparator.comparingInt(MatchJobVO::getScore).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    @Override
    public MatchJobVO scoreOne(Long userId, Long jobId) {
        SysStudentProfile profile = profileService.getByUserId(userId);
        if (profile == null) {
            throw new BizException("请先完善个人画像（专业、技能、意向城市等）");
        }
        JobDetailVO job = jobDetailService.getJobById(jobId);
        if (job == null) {
            throw new BizException("职位不存在或已下架");
        }
        Map<String, Integer> skillAffinity =
                buildSkillAffinity(behaviorService.listByUser(userId, BEHAVIOR_WINDOW));
        return score(profile, SkillUtils.parse(profile.getSkills()), job, skillAffinity);
    }

    /**
     * 从历史行为反推技能偏好：技能（小写）→ 净权重。
     * <p>
     * 投递过的职位所要求的技能加分、忽略过的减分。同一技能在多条行为中出现则累加，
     * 因此「反复投递 Java 岗」会显著抬高 Java 的权重。
     */
    private Map<String, Integer> buildSkillAffinity(List<StudentBehavior> behaviors) {
        Map<Long, Integer> weightByJob = new LinkedHashMap<>();
        for (StudentBehavior b : behaviors) {
            int w = weightOf(b.getAction());
            if (w != 0) {
                weightByJob.merge(b.getJobId(), w, Integer::sum);
            }
        }
        if (weightByJob.isEmpty()) {
            return Collections.emptyMap();
        }

        // 一次取回这些职位，避免逐条查库
        Map<Long, JobDetailVO> jobs = jobDetailService.listByIds(weightByJob.keySet()).stream()
                .collect(Collectors.toMap(JobDetailVO::getId, j -> j));

        Map<String, Integer> affinity = new HashMap<>();
        for (Map.Entry<Long, Integer> e : weightByJob.entrySet()) {
            JobDetailVO job = jobs.get(e.getKey());
            if (job == null) {
                continue;
            }
            for (String skill : SkillUtils.parse(job.getSkills())) {
                affinity.merge(skill.toLowerCase(), e.getValue(), Integer::sum);
            }
        }
        return affinity;
    }

    private int weightOf(String action) {
        if (BehaviorAction.APPLY.equals(action)) {
            return W_APPLY;
        }
        if (BehaviorAction.CONTACT.equals(action)) {
            return W_CONTACT;
        }
        if (BehaviorAction.FAVORITE.equals(action)) {
            return W_FAVORITE;
        }
        if (BehaviorAction.IGNORE.equals(action)) {
            return W_IGNORE;
        }
        return 0;
    }

    private List<JobDetailVO> queryCandidates(String city) {
        JobQueryDTO query = new JobQueryDTO();
        query.setCity(city);
        query.setPageNum(1);
        query.setPageSize(CANDIDATE_SIZE);
        return jobDetailService.queryJobs(query).getRecords();
    }

    /** 单个职位打分：基础规则（技能 40 + 城市 25 + 薪资 20 + 学历 15）+ 语义匹配（+15）+ 行为加权 ±10 */
    private MatchJobVO score(SysStudentProfile profile, List<String> mySkills, JobDetailVO job,
                             Map<String, Integer> skillAffinity) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // —— 技能匹配（40 分）：职位要求技能被学生覆盖的比例 ——
        List<String> jobSkills = SkillUtils.parse(job.getSkills());
        List<String> missing = new ArrayList<>();
        if (!jobSkills.isEmpty()) {
            long hit = jobSkills.stream().filter(s -> SkillUtils.containsIgnoreCase(mySkills, s)).count();
            missing = jobSkills.stream().filter(s -> !SkillUtils.containsIgnoreCase(mySkills, s))
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

        // —— 语义匹配（+15 分）：AI 语义相似度，弥补字符串匹配的不足 ——
        int semanticBonus = 0;
        try {
            String jdText = buildJdText(job);
            String resumeText = buildResumeText(profile, mySkills);
            SemanticMatchVO semantic = semanticMatchService.match(
                    profile.getUserId(), job.getId(), jdText, resumeText);
            if (semantic.isAiGenerated() && semantic.getSimilarity() != null) {
                semanticBonus = (int) Math.round(15.0 * semantic.getSimilarity() / 100.0);
                if (semanticBonus > 0) {
                    reasons.add("AI 语义匹配 +" + semanticBonus);
                }
            }
        } catch (Exception e) {
            log.debug("语义匹配跳过（AI 不可用或异常）: {}", e.getMessage());
        }

        // —— 行为反馈加权（±10 分） ——
        int bonus = behaviorBonus(jobSkills, skillAffinity);
        if (bonus > 0) {
            reasons.add("与你投递/收藏过的职位技能相近");
        } else if (bonus < 0) {
            reasons.add("与你忽略过的职位技能相近");
        }
        score = clamp(score + semanticBonus + bonus, 0, 100);

        MatchJobVO vo = new MatchJobVO();
        vo.setJob(job);
        vo.setScore(score);
        vo.setMatchReason(String.join("、", reasons));
        vo.setMissingSkills(missing);
        return vo;
    }

    /** 构建 JD 描述文本，供语义匹配使用 */
    private String buildJdText(JobDetailVO job) {
        StringBuilder sb = new StringBuilder();
        sb.append("岗位：").append(nvl(job.getTitle())).append('\n');
        sb.append("公司：").append(nvl(job.getCompany())).append('\n');
        sb.append("城市：").append(nvl(job.getCity())).append('\n');
        sb.append("学历要求：").append(nvl(job.getEducation())).append('\n');
        sb.append("薪资：").append(job.getSalaryMin()).append('-').append(job.getSalaryMax()).append('\n');
        sb.append("技能要求：").append(String.join("、", SkillUtils.parse(job.getSkills()))).append('\n');
        sb.append("岗位描述：").append(nvl(job.getDescription()));
        return sb.toString();
    }

    /** 构建简历文本，供语义匹配使用 */
    private String buildResumeText(SysStudentProfile profile, List<String> mySkills) {
        StringBuilder sb = new StringBuilder();
        sb.append("专业：").append(nvl(profile.getMajor())).append('\n');
        sb.append("学历：").append(nvl(profile.getEducationLevel())).append('\n');
        sb.append("意向城市：").append(nvl(profile.getExpectedCity())).append('\n');
        sb.append("意向行业：").append(nvl(profile.getExpectedIndustry())).append('\n');
        sb.append("掌握技能：").append(String.join("、", mySkills));
        return sb.toString();
    }

    private static String nvl(String s) {
        return s == null || s.isEmpty() ? "不限" : s;
    }

    /** 候选职位技能与偏好求交，裁剪到 ±MAX_BEHAVIOR_BONUS */
    private int behaviorBonus(List<String> jobSkills, Map<String, Integer> skillAffinity) {
        if (skillAffinity.isEmpty() || jobSkills.isEmpty()) {
            return 0;
        }
        int raw = 0;
        for (String s : jobSkills) {
            raw += skillAffinity.getOrDefault(s.toLowerCase(), 0);
        }
        return clamp(raw, -MAX_BEHAVIOR_BONUS, MAX_BEHAVIOR_BONUS);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
