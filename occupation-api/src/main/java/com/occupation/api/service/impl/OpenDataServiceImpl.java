package com.occupation.api.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.api.service.OpenDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 开放数据服务实现 — 基于全平台共享表 job_detail 的聚合统计
 * <p>
 * 性能设计：聚合结果 Redis 缓存 10 分钟；开放 API 属只读接口，容忍分钟级延迟。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenDataServiceImpl implements OpenDataService {

    private final JobDetailMapper jobDetailMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "open-api:cache:";
    private static final long CACHE_TTL_MINUTES = 10;

    @Override
    public Map<String, Object> getStatsOverview() {
        String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + "overview");
        if (cached != null) {
            return JSON.parseObject(cached);
        }

        QueryWrapper<JobDetail> wrapper = new QueryWrapper<>();
        wrapper.select("COUNT(*) AS total_jobs",
                       "AVG((salary_min + salary_max) / 2) AS avg_salary",
                       "COUNT(DISTINCT city) AS city_count",
                       "COUNT(DISTINCT industry) AS industry_count");
        List<Map<String, Object>> rows = jobDetailMapper.selectMaps(wrapper);
        Map<String, Object> result = rows.isEmpty() ? new HashMap<>() : rows.get(0);

        redisTemplate.opsForValue().set(CACHE_PREFIX + "overview",
                JSON.toJSONString(result), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<Map<String, Object>> getSkillHot(int topN) {
        String cacheKey = CACHE_PREFIX + "skill-hot:" + topN;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JSON.parseObject(cached, List.class);
        }

        // skills 是 JSON 数组字段 → Java 内存词频统计（数据量万级，毫秒级完成）
        List<JobDetail> jobs = jobDetailMapper.selectList(
                new LambdaQueryWrapper<JobDetail>().select(JobDetail::getSkills));
        Map<String, Long> freq = new HashMap<>();
        for (JobDetail job : jobs) {
            if (job.getSkills() == null || job.getSkills().isEmpty()) {
                continue;
            }
            try {
                for (Object skill : JSON.parseArray(job.getSkills())) {
                    freq.merge(String.valueOf(skill), 1L, Long::sum);
                }
            } catch (Exception ignore) {
                // 脏数据跳过
            }
        }

        List<Map<String, Object>> result = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("skill", e.getKey());
                    item.put("jobCount", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result),
                CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<Map<String, Object>> getIndustryDist(int topN) {
        QueryWrapper<JobDetail> wrapper = new QueryWrapper<>();
        wrapper.select("industry AS name", "COUNT(*) AS job_count")
               .isNotNull("industry")
               .groupBy("industry")
               .orderByDesc("job_count")
               .last("LIMIT " + Math.max(1, topN));
        return jobDetailMapper.selectMaps(wrapper);
    }
}
