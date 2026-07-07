package com.occupation.recommend.service;

import com.occupation.recommend.entity.StudentBehavior;

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
}
