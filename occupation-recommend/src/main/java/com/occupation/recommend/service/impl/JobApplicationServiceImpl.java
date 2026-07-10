package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.occupation.common.exception.BizException;
import com.occupation.recommend.entity.ApplicationStatus;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.mapper.JobApplicationMapper;
import com.occupation.recommend.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投递记录服务实现 — job_application 表（多租户自动隔离）
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationMapper applicationMapper;

    @Override
    public JobApplication apply(Long userId, Long jobId, Long publisherId) {
        JobApplication exists = findByUserAndJob(userId, jobId);
        if (exists != null) {
            return exists;
        }
        JobApplication app = new JobApplication();
        app.setUserId(userId);
        app.setJobId(jobId);
        app.setPublisherId(publisherId);
        app.setStatus(ApplicationStatus.SUBMITTED.name());
        app.setAppliedAt(LocalDateTime.now());
        try {
            applicationMapper.insert(app);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时过了上面的 exists 检查，唯一索引兜住。重查一次返回既有记录
            log.debug("重复投递被唯一索引拦下: userId={}, jobId={}", userId, jobId);
            return findByUserAndJob(userId, jobId);
        }
        log.info("投递已创建: userId={}, jobId={}, publisherId={}", userId, jobId, publisherId);
        return app;
    }

    @Override
    public List<JobApplication> listByPublisher(Long publisherId) {
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getPublisherId, publisherId)
                .orderByDesc(JobApplication::getAppliedAt));
    }

    @Override
    public List<JobApplication> listByUser(Long userId) {
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .orderByDesc(JobApplication::getAppliedAt));
    }

    @Override
    public void changeStatus(Long applicationId, Long operatorId, String newStatus, String hrNote) {
        if (!ApplicationStatus.isValid(newStatus)) {
            throw new BizException("非法的投递状态: " + newStatus);
        }
        JobApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BizException("投递记录不存在");
        }
        // 归属校验：只能改自己发布的职位上的投递
        if (!operatorId.equals(app.getPublisherId())) {
            log.warn("拒绝越权变更投递状态: applicationId={}, publisherId={}, operatorId={}",
                    applicationId, app.getPublisherId(), operatorId);
            throw new BizException(403, "无权操作该投递：它不属于你发布的职位");
        }

        ApplicationStatus from = ApplicationStatus.valueOf(app.getStatus());
        ApplicationStatus to = ApplicationStatus.valueOf(newStatus);
        if (!ApplicationStatus.canTransit(from, to)) {
            throw new BizException(String.format("不能从「%s」变更为「%s」%s",
                    from.getLabel(), to.getLabel(),
                    from.isTerminal() ? "：该投递已处于终态" : ""));
        }

        app.setStatus(to.name());
        app.setStatusChangedAt(LocalDateTime.now());
        if (hrNote != null) {
            app.setHrNote(hrNote.trim().isEmpty() ? null : hrNote.trim());
        }
        applicationMapper.updateById(app);
        log.info("投递状态变更: id={}, {} -> {}", applicationId, from, to);
    }

    @Override
    public int markViewed(Long publisherId, Long applicantUserId) {
        // 只推进 SUBMITTED；已经 INTERVIEW/OFFER/REJECTED 的不能被拉回 VIEWED
        LambdaUpdateWrapper<JobApplication> wrapper = new LambdaUpdateWrapper<JobApplication>()
                .eq(JobApplication::getPublisherId, publisherId)
                .eq(JobApplication::getUserId, applicantUserId)
                .eq(JobApplication::getStatus, ApplicationStatus.SUBMITTED.name())
                .set(JobApplication::getStatus, ApplicationStatus.VIEWED.name())
                .set(JobApplication::getStatusChangedAt, LocalDateTime.now());
        return applicationMapper.update(null, wrapper);
    }

    @Override
    public Map<Long, List<JobApplication>> groupByApplicant(Long publisherId, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getPublisherId, publisherId)
                        .in(JobApplication::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(JobApplication::getUserId));
    }

    private JobApplication findByUserAndJob(Long userId, Long jobId) {
        return applicationMapper.selectOne(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .eq(JobApplication::getJobId, jobId));
    }
}
