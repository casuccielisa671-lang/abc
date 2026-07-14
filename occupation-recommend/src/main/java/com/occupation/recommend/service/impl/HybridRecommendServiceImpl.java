package com.occupation.recommend.service.impl;

import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.recommend.service.*;
import com.occupation.recommend.vo.MatchJobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合推荐引擎实现 — 加权融合多种推荐策略。
 * <p>
 * 推荐管线：
 * <pre>
 * 用户请求
 *   ├─ 70% 规则打分 + 语义匹配（JobMatchService）→ 精准匹配
 *   ├─ 15% 协同过滤（CollaborativeFilterService）→ 相似用户偏好
 *   └─ 15% 内容推荐（ContentBasedRecommendService）→ 技能相似职位
 *        ↓
 *     加权合并 → 去重 → 排序 → Top N
 * </pre>
 * <p>
 * 权重设计理由：
 * <ul>
 *   <li>规则打分占主导：技能/城市/薪资/学历是求职核心维度，不可替代；</li>
 *   <li>协同过滤补充：发现"同类学生都投了但我没注意到"的职位；</li>
 *   <li>内容推荐补充：发现"技能要求相似但我没投过的"职位。</li>
 * </ul>
 * <p>
 * 冷启动处理：新用户无行为数据时，协同过滤和内容推荐返回空，自动回退到纯规则打分。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRecommendServiceImpl implements HybridRecommendService {

    private final JobMatchService jobMatchService;
    private final CollaborativeFilterService collaborativeFilterService;
    private final ContentBasedRecommendService contentBasedRecommendService;

    /** 规则打分权重 */
    private static final double W_RULE = 0.70;
    /** 协同过滤权重 */
    private static final double W_CF = 0.15;
    /** 内容推荐权重 */
    private static final double W_CB = 0.15;

    /** 各渠道取的数量倍数（取 topN * factor 条再合并去重） */
    private static final int FETCH_FACTOR = 2;

    @Override
    public List<MatchJobVO> recommend(Long userId, int topN) {
        int fetchSize = topN * FETCH_FACTOR;

        // 1. 规则打分 + 语义匹配（主通道）
        List<MatchJobVO> ruleResults = Collections.emptyList();
        try {
            ruleResults = jobMatchService.match(userId, fetchSize);
        } catch (Exception e) {
            log.warn("规则打分失败，跳过: {}", e.getMessage());
        }

        // 2. 协同过滤
        List<JobDetailVO> cfJobs = Collections.emptyList();
        try {
            cfJobs = collaborativeFilterService.recommend(userId, fetchSize);
        } catch (Exception e) {
            log.warn("协同过滤失败，跳过: {}", e.getMessage());
        }

        // 3. 基于内容的推荐
        List<JobDetailVO> cbJobs = Collections.emptyList();
        try {
            cbJobs = contentBasedRecommendService.recommendByUserHistory(userId, fetchSize);
        } catch (Exception e) {
            log.warn("内容推荐失败，跳过: {}", e.getMessage());
        }

        // 4. 加权合并
        // 规则结果：已有分数（0-100），保持原分 × W_RULE
        Map<Long, MatchJobVO> merged = new LinkedHashMap<>();
        Set<Long> ruleJobIds = new HashSet<>();

        for (MatchJobVO vo : ruleResults) {
            Long jobId = vo.getJob().getId();
            ruleJobIds.add(jobId);
            // 规则分缩放到 0-100 后加权
            int scaledScore = (int) Math.round(vo.getScore() * W_RULE);
            vo.setScore(scaledScore);
            merged.put(jobId, vo);
        }

        // 协同过滤结果：给一个基础分 80（表示"相似用户投过"），再乘以权重
        int cfRank = cfJobs.size();
        for (int i = 0; i < cfJobs.size(); i++) {
            JobDetailVO job = cfJobs.get(i);
            if (ruleJobIds.contains(job.getId())) {
                // 规则已有，加分
                MatchJobVO existing = merged.get(job.getId());
                if (existing != null) {
                    int bonus = (int) Math.round(80 * W_CF * (cfRank - i) / (double) cfRank);
                    existing.setScore(Math.min(100, existing.getScore() + bonus));
                    String reason = existing.getMatchReason();
                    if (reason == null || reason.isEmpty()) {
                        existing.setMatchReason("相似用户也投递了该职位");
                    } else if (!reason.contains("相似用户")) {
                        existing.setMatchReason(reason + "、相似用户也投递了该职位");
                    }
                }
            } else {
                // 新职位：基础分 = 80 * W_CF * 排名衰减
                int score = (int) Math.round(80 * W_CF * (cfRank - i) / (double) cfRank);
                MatchJobVO vo = new MatchJobVO();
                vo.setJob(job);
                vo.setScore(score);
                vo.setMatchReason("相似用户也投递了该职位");
                vo.setMissingSkills(Collections.emptyList());
                merged.put(job.getId(), vo);
            }
        }

        // 内容推荐结果：给一个基础分 70（表示"技能相似"），再乘以权重
        int cbRank = cbJobs.size();
        for (int i = 0; i < cbJobs.size(); i++) {
            JobDetailVO job = cbJobs.get(i);
            if (ruleJobIds.contains(job.getId())) {
                MatchJobVO existing = merged.get(job.getId());
                if (existing != null) {
                    int bonus = (int) Math.round(70 * W_CB * (cbRank - i) / (double) cbRank);
                    existing.setScore(Math.min(100, existing.getScore() + bonus));
                }
            } else if (!merged.containsKey(job.getId())) {
                int score = (int) Math.round(70 * W_CB * (cbRank - i) / (double) cbRank);
                MatchJobVO vo = new MatchJobVO();
                vo.setJob(job);
                vo.setScore(score);
                vo.setMatchReason("技能要求与你匹配");
                vo.setMissingSkills(Collections.emptyList());
                merged.put(job.getId(), vo);
            }
        }

        // 5. 按最终得分降序取 Top N
        List<MatchJobVO> result = merged.values().stream()
                .sorted(Comparator.comparingInt(MatchJobVO::getScore).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        log.info("混合推荐完成: userId={}, 规则={}, CF={}, CB={}, 合并后={}, 最终={}",
                userId, ruleResults.size(), cfJobs.size(), cbJobs.size(),
                merged.size(), result.size());

        return result;
    }
}
