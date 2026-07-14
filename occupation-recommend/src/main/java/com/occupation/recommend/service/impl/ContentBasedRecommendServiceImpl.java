package com.occupation.recommend.service.impl;

import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.ContentBasedRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于内容的推荐实现 — TF-IDF + 余弦相似度。
 * <p>
 * 核心思路：
 * <ol>
 *   <li>将所有职位的技能标签视为文档集合，每个职位是一个文档；</li>
 *   <li>计算每个技能词的 TF-IDF 权重，构建职位向量；</li>
 *   <li>计算目标职位与其他职位的余弦相似度；</li>
 *   <li>返回 Top N 相似职位。</li>
 * </ol>
 * <p>
 * 性能说明：技能标签维度通常不超过 500 个不同词，全量职位通常不超过 5000 条，
 * 实时计算完全可行，无需离线预计算。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentBasedRecommendServiceImpl implements ContentBasedRecommendService {

    private final JobDetailService jobDetailService;
    private final BehaviorService behaviorService;

    /** 参与计算的候选职位上限（取最新发布的 N 条） */
    private static final int CANDIDATE_POOL_SIZE = 2000;

    @Override
    public List<JobDetailVO> similarJobs(Long jobId, int topN) {
        JobDetailVO seed = jobDetailService.getJobById(jobId);
        if (seed == null) {
            return Collections.emptyList();
        }

        List<JobDetailVO> pool = fetchCandidatePool();
        if (pool.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 TF-IDF 向量
        List<List<String>> docs = pool.stream()
                .map(j -> SkillUtils.parse(j.getSkills()))
                .collect(Collectors.toList());
        Map<String, Double> idf = computeIdf(docs);

        // 种子职位向量
        List<String> seedSkills = SkillUtils.parse(seed.getSkills());
        Map<String, Double> seedVec = tfidfVector(seedSkills, idf);

        // 计算相似度并排序
        return pool.stream()
                .filter(j -> !j.getId().equals(jobId))
                .map(j -> new JobSimilarity(j, cosineSimilarity(seedVec,
                        tfidfVector(SkillUtils.parse(j.getSkills()), idf))))
                .filter(js -> js.similarity > 0.01)
                .sorted(Comparator.comparingDouble((JobSimilarity js) -> js.similarity).reversed())
                .limit(topN)
                .map(js -> js.job)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobDetailVO> recommendByUserHistory(Long userId, int topN) {
        // 取用户投递+收藏过的职位
        List<Long> applyJobIds = behaviorService.listJobIdsByAction(userId, BehaviorAction.APPLY);
        List<Long> favJobIds = behaviorService.listJobIdsByAction(userId, BehaviorAction.FAVORITE);
        Set<Long> historyJobIds = new LinkedHashSet<>();
        historyJobIds.addAll(applyJobIds);
        historyJobIds.addAll(favJobIds);

        if (historyJobIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 取这些职位的详情
        List<JobDetailVO> historyJobs = jobDetailService.listByIds(historyJobIds);
        if (historyJobs.isEmpty()) {
            return Collections.emptyList();
        }

        List<JobDetailVO> pool = fetchCandidatePool();
        if (pool.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 TF-IDF
        List<List<String>> docs = pool.stream()
                .map(j -> SkillUtils.parse(j.getSkills()))
                .collect(Collectors.toList());
        Map<String, Double> idf = computeIdf(docs);

        // 用户画像向量：历史职位技能向量的平均值
        Map<String, Double> userVec = new HashMap<>();
        for (JobDetailVO hj : historyJobs) {
            Map<String, Double> jv = tfidfVector(SkillUtils.parse(hj.getSkills()), idf);
            jv.forEach((k, v) -> userVec.merge(k, v, Double::sum));
        }
        // 归一化：除以历史职位数
        int count = historyJobs.size();
        userVec.replaceAll((k, v) -> v / count);

        // 排除已交互过的职位
        Set<Long> excludeIds = new HashSet<>(historyJobIds);
        // 也排除 IGNORE 的
        excludeIds.addAll(behaviorService.listJobIdsByAction(userId, BehaviorAction.IGNORE));

        return pool.stream()
                .filter(j -> !excludeIds.contains(j.getId()))
                .map(j -> new JobSimilarity(j, cosineSimilarity(userVec,
                        tfidfVector(SkillUtils.parse(j.getSkills()), idf))))
                .filter(js -> js.similarity > 0.01)
                .sorted(Comparator.comparingDouble((JobSimilarity js) -> js.similarity).reversed())
                .limit(topN)
                .map(js -> js.job)
                .collect(Collectors.toList());
    }

    // ==================== TF-IDF 计算 ====================

    /**
     * 计算 IDF：log(总文档数 / 包含该词的文档数)
     */
    private Map<String, Double> computeIdf(List<List<String>> docs) {
        int N = docs.size();
        Map<String, Integer> docFreq = new HashMap<>();
        for (List<String> doc : docs) {
            Set<String> unique = new HashSet<>(doc);
            for (String word : unique) {
                docFreq.merge(word.toLowerCase(), 1, Integer::sum);
            }
        }
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> e : docFreq.entrySet()) {
            idf.put(e.getKey(), Math.log((double) N / (1 + e.getValue())));
        }
        return idf;
    }

    /**
     * 计算单个文档的 TF-IDF 向量。
     * TF 使用对数归一化：1 + log(词频)
     */
    private Map<String, Double> tfidfVector(List<String> skills, Map<String, Double> idf) {
        Map<String, Integer> tf = new HashMap<>();
        for (String s : skills) {
            tf.merge(s.toLowerCase(), 1, Integer::sum);
        }
        Map<String, Double> vec = new HashMap<>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            double idfVal = idf.getOrDefault(e.getKey(), 0.0);
            vec.put(e.getKey(), (1.0 + Math.log(e.getValue())) * idfVal);
        }
        return vec;
    }

    /**
     * 余弦相似度
     */
    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<String, Double> e : a.entrySet()) {
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

    // ==================== 辅助 ====================

    private List<JobDetailVO> fetchCandidatePool() {
        try {
            // 取最新发布的职位作为候选池
            com.occupation.analysis.dto.JobQueryDTO query =
                    new com.occupation.analysis.dto.JobQueryDTO();
            query.setPageNum(1);
            query.setPageSize(CANDIDATE_POOL_SIZE);
            return jobDetailService.queryJobs(query).getRecords();
        } catch (Exception e) {
            log.warn("获取候选职位池失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 内部类：职位 + 相似度
     */
    private static class JobSimilarity {
        final JobDetailVO job;
        final double similarity;

        JobSimilarity(JobDetailVO job, double similarity) {
            this.job = job;
            this.similarity = similarity;
        }
    }
}
