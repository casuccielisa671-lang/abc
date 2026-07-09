package com.occupation.recommend.service;

import com.occupation.recommend.entity.StudentBehavior;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 学生行为服务 — 推荐反馈闭环的数据基础
 * <p>
 * 行为类型：VIEW（浏览）/ FAVORITE（收藏）/ APPLY（投递）/ IGNORE（忽略）。
 * 数据用途：① 个人求职统计；② 教师端学生动态；③ 匹配算法反馈加权（P4 进阶）。
 *
 * @author occupation-team
 */
public interface BehaviorService {

    /** 记录一次行为（同一职位同一行为只记一次，VIEW 除外） */
    void record(Long userId, Long jobId, String action);

    /** 取消收藏（删除 FAVORITE 记录） */
    void removeFavorite(Long userId, Long jobId);

    /** 我的某类行为的职位 ID 列表（收藏列表/投递列表用） */
    List<Long> listJobIdsByAction(Long userId, String action);

    /** 个人求职统计：各行为计数（浏览 N 次、投递 N 个...） */
    Map<String, Long> countByAction(Long userId);

    /** 指定学生的行为明细（教师端） */
    List<StudentBehavior> listByUser(Long userId, int limit);

    /**
     * 指定职位集合上的某类行为记录（HR 端「收到的投递」）。
     * jobIds 为空时返回空列表，不会退化成全表扫描。
     */
    List<StudentBehavior> listByJobIdsAndAction(Collection<Long> jobIds, String action);

    /**
     * 当前租户内各行为类型的总数（教师端概览：总浏览数 / 总投递数）。
     */
    Map<String, Long> countByActionForTenant();

    /**
     * 按用户分组的行为计数：userId → (action → count)。
     * 一次查询取回，避免逐个学生查库的 N+1。userIds 为空时返回空 Map。
     */
    Map<Long, Map<String, Long>> countByActionGroupedByUser(Collection<Long> userIds);
}
