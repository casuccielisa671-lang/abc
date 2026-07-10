package com.occupation.recommend.service;

import com.occupation.recommend.dto.ResumeSaveDTO;
import com.occupation.recommend.entity.StudentResume;
import com.occupation.recommend.vo.ResumeVO;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 学生简历服务
 *
 * @author occupation-team
 */
public interface ResumeService {

    /** 查看某人的简历（未创建时返回 exists=false 的空壳，而非 null） */
    ResumeVO getByUserId(Long userId);

    /** 原始实体，供 AI 诊断等内部逻辑使用；未创建返回 null */
    StudentResume findByUserId(Long userId);

    /** 保存/更新简历 */
    void save(Long userId, ResumeSaveDTO dto);

    /** 缓存 AI 诊断结果，避免每次打开都重新调大模型 */
    void saveAiReview(Long userId, String reviewJson);

    /**
     * 批量判断哪些学生已填简历（HR 投递列表用，避免 N+1）。
     * userIds 为空时返回空集合。
     */
    Set<Long> filterUsersWithResume(Collection<Long> userIds);

    /** 批量取简历实体：userId → resume。userIds 为空时返回空 Map */
    Map<Long, StudentResume> mapByUserIds(Collection<Long> userIds);
}
