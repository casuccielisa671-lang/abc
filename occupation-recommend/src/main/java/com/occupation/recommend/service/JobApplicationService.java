package com.occupation.recommend.service;

import com.occupation.recommend.dto.ApplicationStatusDTO;
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
     * HR 变更投递状态，并按状态给学生发一条站内通知。
     * <p>
     * 校验两件事：① 该投递确实属于当前 HR；② 状态流转合法（不能回退、终态不可再改）。
     * 任一不满足抛 {@code BizException}。{@code status=INTERVIEW} 时还会落面试信息并要求时间/地点非空。
     * 通知内容按 INTERVIEW / OFFER / REJECTED 三套模板生成，学生看得到，但看不到 hrNote。
     */
    void changeStatus(Long applicationId, Long operatorId, ApplicationStatusDTO dto);

    /**
     * HR 打开某学生详情时，把他 SUBMITTED 的投递自动标记为 VIEWED。
     * <p>
     * 返回被更新的条数。已经推进过的状态不会被拉回。
     */
    int markViewed(Long publisherId, Long applicantUserId);

    /** userId → 该学生投给某 HR 的投递记录（HR 端列表用，避免 N+1） */
    Map<Long, List<JobApplication>> groupByApplicant(Long publisherId, Collection<Long> userIds);

    /**
     * 学生接收某条 OFFER 投递为正式录用（OFFER → ACCEPTED）。
     * <p>校验：该投递属于本人、状态为 OFFER、且本人尚未接收过别的 offer（一人只能入职一处）。
     * 这是<b>学生动作</b>，HR 的 changeStatus 无法产生 ACCEPTED。
     */
    void acceptOffer(Long applicationId, Long userId);

    /** 该学生是否已就业（有 ACCEPTED 投递）。用于「已就业不能再投递/联系」的守卫 */
    boolean isEmployed(Long userId);

    /** 学生就业状态：EMPLOYED=已就业 / OFFERED=收到录用待接收 / SEEKING=求职中 / IDLE=待业 */
    String employmentStatus(Long userId);

    /** 批量：这些学生里哪些已就业（有 ACCEPTED），教师列表用，避免 N+1 */
    java.util.Set<Long> employedUserIds(Collection<Long> userIds);

    /** 批量：userId → 就业状态（EMPLOYED/OFFERED/SEEKING/IDLE），一次查回，教师列表用 */
    Map<Long, String> employmentStatusByUsers(Collection<Long> userIds);

    /** 本租户内已就业（有 ACCEPTED）的学生数 —— 教师/管理员概览「已就业」用 */
    long countEmployedInTenant();
}
