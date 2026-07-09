package com.occupation.recommend.service;

import com.occupation.recommend.vo.TeachingSuggestionVO;

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
}
