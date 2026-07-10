package com.occupation.recommend.service;

import com.occupation.recommend.entity.JobApplication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 投递记录服务
 *
 * @author occupation-team
 */
public interface JobApplicationService {

    /**
     * 学生投递。已投过则幂等返回旧记录，不重复插入（唯一索引也兜着）。
     *
     * @param publisherId 职位发布者，调用方需保证职位可投递（publisherId 非空）
     */
    JobApplication apply(Long userId, Long jobId, Long publisherId);

    /** 某 HR 收到的全部投递，按投递时间倒序 */
    List<JobApplication> listByPublisher(Long publisherId);

    /** 某学生投出去的全部投递，按投递时间倒序 */
    List<JobApplication> listByUser(Long userId);

    /**
     * HR 变更投递状态。
     * <p>
     * 校验两件事：① 该投递确实属于当前 HR；② 状态流转合法（不能回退、终态不可再改）。
     * 任一不满足抛 {@code BizException}。
     */
    void changeStatus(Long applicationId, Long operatorId, String newStatus, String hrNote);

    /**
     * HR 打开某学生详情时，把他 SUBMITTED 的投递自动标记为 VIEWED。
     * <p>
     * 返回被更新的条数。已经推进过的状态不会被拉回。
     */
    int markViewed(Long publisherId, Long applicantUserId);

    /** userId → 该学生投给某 HR 的投递记录（HR 端列表用，避免 N+1） */
    Map<Long, List<JobApplication>> groupByApplicant(Long publisherId, Collection<Long> userIds);
}
