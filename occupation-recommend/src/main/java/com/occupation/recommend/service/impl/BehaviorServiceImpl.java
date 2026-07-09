package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.recommend.entity.StudentBehavior;
import com.occupation.recommend.mapper.StudentBehaviorMapper;
import com.occupation.recommend.service.BehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生行为服务实现 — student_behavior 表（多租户自动隔离）
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorServiceImpl implements BehaviorService {

    private final StudentBehaviorMapper behaviorMapper;

    @Override
    public void record(Long userId, Long jobId, String action) {
        // VIEW 允许重复记录（反映活跃度）；其他行为幂等
        if (!"VIEW".equals(action)) {
            Long exists = behaviorMapper.selectCount(new LambdaQueryWrapper<StudentBehavior>()
                    .eq(StudentBehavior::getUserId, userId)
                    .eq(StudentBehavior::getJobId, jobId)
                    .eq(StudentBehavior::getAction, action));
            if (exists != null && exists > 0) {
                return;
            }
        }
        StudentBehavior behavior = new StudentBehavior();
        behavior.setUserId(userId);
        behavior.setJobId(jobId);
        behavior.setAction(action);
        behavior.setCreateTime(LocalDateTime.now());
        behaviorMapper.insert(behavior);
    }

    @Override
    public void removeFavorite(Long userId, Long jobId) {
        behaviorMapper.delete(new LambdaQueryWrapper<StudentBehavior>()
                .eq(StudentBehavior::getUserId, userId)
                .eq(StudentBehavior::getJobId, jobId)
                .eq(StudentBehavior::getAction, "FAVORITE"));
    }

    @Override
    public List<Long> listJobIdsByAction(Long userId, String action) {
        return behaviorMapper.selectList(new LambdaQueryWrapper<StudentBehavior>()
                        .eq(StudentBehavior::getUserId, userId)
                        .eq(StudentBehavior::getAction, action)
                        .orderByDesc(StudentBehavior::getCreateTime))
                .stream()
                .map(StudentBehavior::getJobId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> countByAction(Long userId) {
        List<StudentBehavior> all = behaviorMapper.selectList(
                new LambdaQueryWrapper<StudentBehavior>().eq(StudentBehavior::getUserId, userId));
        Map<String, Long> counts = new HashMap<>();
        for (StudentBehavior b : all) {
            counts.merge(b.getAction(), 1L, Long::sum);
        }
        return counts;
    }

    @Override
    public List<StudentBehavior> listByUser(Long userId, int limit) {
        return behaviorMapper.selectList(new LambdaQueryWrapper<StudentBehavior>()
                .eq(StudentBehavior::getUserId, userId)
                .orderByDesc(StudentBehavior::getCreateTime)
                .last("LIMIT " + Math.max(1, limit)));
    }

    @Override
    public List<StudentBehavior> listByJobIdsAndAction(Collection<Long> jobIds, String action) {
        if (jobIds == null || jobIds.isEmpty()) {
            return Collections.emptyList();
        }
        return behaviorMapper.selectList(new LambdaQueryWrapper<StudentBehavior>()
                .in(StudentBehavior::getJobId, jobIds)
                .eq(StudentBehavior::getAction, action)
                .orderByDesc(StudentBehavior::getCreateTime));
    }

    @Override
    public Map<String, Long> countByActionForTenant() {
        Map<String, Long> counts = new HashMap<>();
        for (StudentBehavior b : behaviorMapper.selectList(null)) {
            counts.merge(b.getAction(), 1L, Long::sum);
        }
        return counts;
    }

    @Override
    public Map<Long, Map<String, Long>> countByActionGroupedByUser(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Map<String, Long>> grouped = new HashMap<>();
        List<StudentBehavior> all = behaviorMapper.selectList(
                new LambdaQueryWrapper<StudentBehavior>().in(StudentBehavior::getUserId, userIds));
        for (StudentBehavior b : all) {
            grouped.computeIfAbsent(b.getUserId(), k -> new HashMap<>())
                    .merge(b.getAction(), 1L, Long::sum);
        }
        return grouped;
    }
}
