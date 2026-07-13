package com.occupation.recommend.service;

import com.occupation.recommend.vo.TeachingSuggestionVO;

import java.util.Collection;

/**
 * 教学建议服务 — 技能缺口诊断
 * <p>
 * 对比「市场热门技能」与「本校学生掌握率」，输出市场需求高但掌握率低的技能清单，
 * 每条附岗位数量与掌握人数作为证据。
 *
 * @author occupation-team
 */
public interface TeachingSuggestionService {

    /**
     * 生成当前租户的技能缺口诊断与课程改革建议。
     *
     * @param topSkills   参与对比的市场热门技能数量
     * @param maxGapItems 返回的缺口条目上限
     */
    TeachingSuggestionVO diagnose(int topSkills, int maxGapItems);

    /**
     * 生成技能缺口诊断，学生样本限定在 {@code restrictUserIds}（教师端范围过滤）。
     *
     * @param restrictUserIds 学生 userId 集合：{@code null}=当前租户全部；空集=无学生样本
     */
    TeachingSuggestionVO diagnose(int topSkills, int maxGapItems, Collection<Long> restrictUserIds);
}
