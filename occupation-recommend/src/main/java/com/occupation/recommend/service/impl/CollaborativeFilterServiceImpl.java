package com.occupation.recommend.service.impl;

import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.CollaborativeFilterService;
import com.occupation.recommend.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User-Based 协同过滤实现。
 * <p>
 * 核心流程：
 * <ol>
 *   <li>构建用户-职位交互矩阵（仅考虑 APPLY + FAVORITE 正向行为）；</li>
 *   <li>计算目标用户与其他用户的余弦相似度；</li>
 *   <li>选取 Top K 个最相似邻居；</li>
 *   <li>聚合邻居交互过但目标用户未接触的职位，按邻居相似度加权得分排序；</li>
 *   <li>排除目标用户已 APPLY / FAVORITE / IGNORE 的职位。</li>
 * </ol>
 * <p>
 * 冷启动处理：新用户（无行为记录）或行为数据不足时返回空列表，
 * 由混合推荐引擎回退到内容推荐 + 规则打分。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborativeFilterServiceImpl implements CollaborativeFilterService {

    private final BehaviorService behaviorService;
    private final StudentProfileService profileService;
    private final JobDetailService jobDetailService;

    /** 邻居数量 K */
    private static final int K_NEIGHBORS = 20;

    /** 参与相似度计算的用户上限（取最近活跃的 N 个用户） */
    private static final int MAX_USERS = 500;

    /** 最小共同交互数：低于此值视为不可靠邻居 */
    private static final int MIN_COMMON_ITEMS = 2;

    @Override
    public List<JobDetailVO> recommend(Long userId, int topN) {
        // 1. 获取目标用户的交互职位集合
        Set<Long> targetApply = new HashSet<>(
                behaviorService.listJobIdsByAction(userId, BehaviorAction.APPLY));
        Set<Long> targetFav = new HashSet<>(
                behaviorService.listJobIdsByAction(userId, BehaviorAction.FAVORITE));
        Set<Long> targetIgnore = new HashSet<>(
                behaviorService.listJobIdsByAction(userId, BehaviorAction.IGNORE));
        Set<Long> targetItems = new HashSet<>();
        targetItems.addAll(targetApply);
        targetItems.addAll(targetFav);

        // 冷启动：无行为数据
        if (targetItems.isEmpty()) {
            log.debug("协同过滤跳过：用户 {} 无行为数据", userId);
            return Collections.emptyList();
        }

        // 2. 获取候选邻居用户列表（排除目标用户自身）
        List<SysStudentProfile> allProfiles = profileService.listAll();
        List<Long> candidateUsers = allProfiles.stream()
                .map(SysStudentProfile::getUserId)
                .filter(uid -> !uid.equals(userId))
                .limit(MAX_USERS)
                .collect(Collectors.toList());

        if (candidateUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 批量获取候选用户的行为计数（一次查询避免 N+1）
        Map<Long, Map<String, Long>> behaviorCounts =
                behaviorService.countByActionGroupedByUser(candidateUsers);

        // 4. 构建目标用户的交互向量（职位ID → 权重：APPLY=2, FAVORITE=1）
        Map<Long, Double> targetVec = new HashMap<>();
        for (Long jid : targetApply) {
            targetVec.put(jid, 2.0);
        }
        for (Long jid : targetFav) {
            targetVec.merge(jid, 1.0, Double::sum);
        }

        // 5. 计算邻居相似度
        List<Neighbor> neighbors = new ArrayList<>();
        for (Long neighborId : candidateUsers) {
            Map<String, Long> counts = behaviorCounts.get(neighborId);
            if (counts == null || counts.isEmpty()) {
                continue;
            }

            // 获取邻居的交互职位
            Set<Long> neighborApply = new HashSet<>(
                    behaviorService.listJobIdsByAction(neighborId, BehaviorAction.APPLY));
            Set<Long> neighborFav = new HashSet<>(
                    behaviorService.listJobIdsByAction(neighborId, BehaviorAction.FAVORITE));

            // 构建邻居向量
            Map<Long, Double> neighborVec = new HashMap<>();
            for (Long jid : neighborApply) {
                neighborVec.put(jid, 2.0);
            }
            for (Long jid : neighborFav) {
                neighborVec.merge(jid, 1.0, Double::sum);
            }

            // 计算共同交互数
            Set<Long> common = new HashSet<>(targetVec.keySet());
            common.retainAll(neighborVec.keySet());

            if (common.size() < MIN_COMMON_ITEMS) {
                continue;
            }

            double sim = cosineSimilarity(targetVec, neighborVec);
            if (sim > 0.01) {
                neighbors.add(new Neighbor(neighborId, sim, neighborVec));
            }
        }

        if (neighbors.isEmpty()) {
            log.debug("协同过滤跳过：用户 {} 无相似邻居", userId);
            return Collections.emptyList();
        }

        // 6. 按相似度降序取 Top K
        neighbors.sort(new Comparator<Neighbor>() {
            @Override
            public int compare(Neighbor a, Neighbor b) {
                return Double.compare(b.similarity, a.similarity);
            }
        });
        if (neighbors.size() > K_NEIGHBORS) {
            neighbors = neighbors.subList(0, K_NEIGHBORS);
        }

        // 7. 聚合邻居职位：按邻居相似度加权
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Neighbor n : neighbors) {
            for (Map.Entry<Long, Double> e : n.itemVec.entrySet()) {
                Long jobId = e.getKey();
                if (targetItems.contains(jobId) || targetIgnore.contains(jobId)) {
                    continue; // 排除已交互和已忽略
                }
                candidateScores.merge(jobId, n.similarity * e.getValue(), Double::sum);
            }
        }

        if (candidateScores.isEmpty()) {
            return Collections.emptyList();
        }

        // 8. 按加权得分降序取 Top N
        List<Long> topJobIds = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 9. 批量查询职位详情
        List<JobDetailVO> jobs = jobDetailService.listByIds(new HashSet<>(topJobIds));
        // 按得分顺序排列
        Map<Long, JobDetailVO> jobMap = jobs.stream()
                .collect(Collectors.toMap(JobDetailVO::getId, j -> j));
        List<JobDetailVO> result = new ArrayList<>();
        for (Long jid : topJobIds) {
            JobDetailVO job = jobMap.get(jid);
            if (job != null) {
                result.add(job);
            }
        }

        log.info("协同过滤完成: userId={}, 邻居数={}, 推荐职位数={}",
                userId, neighbors.size(), result.size());
        return result;
    }

    /**
     * 两个用户向量的余弦相似度
     */
    private double cosineSimilarity(Map<Long, Double> a, Map<Long, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<Long, Double> e : a.entrySet()) {
            double va = e.getValue();
            normA += va * va;
            Double vb = b.get(e.getKey());
            if (vb != null) {
                dot += va * vb;
            }
        }
        for (double vb : b.values()) {
            normB += vb * vb;
        }

        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dot / denom;
    }

    /**
     * 邻居记录：用户ID + 相似度 + 交互向量
     */
    private static class Neighbor {
        final Long userId;
        final double similarity;
        final Map<Long, Double> itemVec;

        Neighbor(Long userId, double similarity, Map<Long, Double> itemVec) {
            this.userId = userId;
            this.similarity = similarity;
            this.itemVec = itemVec;
        }
    }
}
