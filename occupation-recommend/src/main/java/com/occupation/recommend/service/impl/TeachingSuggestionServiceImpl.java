package com.occupation.recommend.service.impl;

import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.common.utils.SkillUtils;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.StudentProfileService;
import com.occupation.recommend.service.TeachingSuggestionService;
import com.occupation.recommend.vo.TeachingSuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 技能缺口诊断实现
 * <p>
 * ① 汇总本租户学生画像的 skills，得到每个技能的掌握人数；
 * ② 取 analysis_result 中 dimension=skill 的市场热度排行；
 * ③ 两者对比，输出「市场热但掌握率低」的技能，附岗位数与掌握人数作为证据。
 * <p>
 * 纯 SQL/Java 计算，无随机数、无预置文案。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingSuggestionServiceImpl implements TeachingSuggestionService {

    /** 掌握率低于该阈值才算「缺口」，与需求文档一致 */
    private static final int MASTERY_THRESHOLD = 30;

    /** 缺口分档：≥50 严重（HIGH），≥20 中等（MEDIUM），否则轻微（LOW） */
    private static final int GAP_SEVERE = 50;
    private static final int GAP_MODERATE = 20;

    /** 课程改革建议的条目上限 */
    private static final int MAX_COURSE_SUGGESTIONS = 5;

    private final StudentProfileService profileService;
    private final AnalysisService analysisService;

    @Override
    public TeachingSuggestionVO diagnose(int topSkills, int maxGapItems) {
        return diagnose(topSkills, maxGapItems, null);
    }

    @Override
    public TeachingSuggestionVO diagnose(int topSkills, int maxGapItems, java.util.Collection<Long> restrictUserIds) {
        TeachingSuggestionVO vo = new TeachingSuggestionVO();

        // ① 学生掌握情况：技能（小写）→ 掌握人数（限定在范围内的学生）
        List<SysStudentProfile> profiles = profileService.listByUserIds(restrictUserIds);
        int studentCount = profiles.size();
        vo.setStudentsWithProfile(studentCount);

        Map<String, Long> masteredBySkill = new HashMap<>();
        for (SysStudentProfile p : profiles) {
            // 同一学生的同一技能只计一次
            Set<String> distinct = SkillUtils.parseDistinct(p.getSkills());
            for (String s : distinct) {
                masteredBySkill.merge(s.toLowerCase(), 1L, Long::sum);
            }
        }

        // ② 市场热度
        List<DashboardVO.DimensionItem> market = analysisService.topSkills(topSkills);
        if (market.isEmpty()) {
            log.info("技能缺口诊断：analysis_result 无 skill 维度数据，请先执行「手动重算分析数据」");
            vo.setSkillGaps(Collections.emptyList());
            vo.setCourseSuggestions(Collections.emptyList());
            return vo;
        }

        // 相对热度基准：最热技能的岗位数（topSkills 已按岗位数降序）
        long maxJobCount = market.get(0).getCount() == null ? 0L : market.get(0).getCount();
        if (maxJobCount <= 0) {
            vo.setSkillGaps(Collections.emptyList());
            vo.setCourseSuggestions(Collections.emptyList());
            return vo;
        }

        // ③ 对比
        List<TeachingSuggestionVO.SkillGap> gaps = new ArrayList<>();
        for (DashboardVO.DimensionItem item : market) {
            long jobCount = item.getCount() == null ? 0L : item.getCount();
            long mastered = masteredBySkill.getOrDefault(item.getName().toLowerCase(), 0L);

            int marketDemand = (int) Math.round(100.0 * jobCount / maxJobCount);
            int studentRate = studentCount == 0 ? 0 : (int) Math.round(100.0 * mastered / studentCount);
            if (studentRate >= MASTERY_THRESHOLD) {
                continue;
            }
            int gap = Math.max(0, marketDemand - studentRate);
            if (gap == 0) {
                continue;
            }

            TeachingSuggestionVO.SkillGap g = new TeachingSuggestionVO.SkillGap();
            g.setSkill(item.getName());
            g.setMarketDemand(marketDemand);
            g.setStudentRate(studentRate);
            g.setGap(gap);
            g.setJobCount(jobCount);
            g.setMasteredCount(mastered);
            g.setSuggestion(buildSuggestion(item.getName(), jobCount, mastered, studentCount, gap));
            gaps.add(g);
        }

        gaps.sort((a, b) -> Integer.compare(b.getGap(), a.getGap()));
        if (gaps.size() > maxGapItems) {
            gaps = new ArrayList<>(gaps.subList(0, maxGapItems));
        }
        vo.setSkillGaps(gaps);
        vo.setCourseSuggestions(buildCourseSuggestions(gaps, studentCount));

        log.info("技能缺口诊断完成: 学生数={}, 市场技能数={}, 缺口条目={}",
                studentCount, market.size(), gaps.size());
        return vo;
    }

    private String buildSuggestion(String skill, long jobCount, long mastered, int studentCount, int gap) {
        if (studentCount == 0) {
            return String.format("市场有 %d 个岗位要求「%s」，但本校尚无学生填写画像，无法评估掌握率。", jobCount, skill);
        }
        String severity = gap >= GAP_SEVERE ? "严重缺口" : gap >= GAP_MODERATE ? "中等缺口" : "轻微缺口";
        return String.format("市场 %d 个岗位要求「%s」，本校 %d/%d 名学生掌握，%s（相差 %d 个百分点），建议在相关课程中补强。",
                jobCount, skill, mastered, studentCount, severity, gap);
    }

    private List<TeachingSuggestionVO.CourseSuggestion> buildCourseSuggestions(
            List<TeachingSuggestionVO.SkillGap> gaps, int studentCount) {
        List<TeachingSuggestionVO.CourseSuggestion> list = new ArrayList<>();
        int limit = Math.min(gaps.size(), MAX_COURSE_SUGGESTIONS);
        for (int i = 0; i < limit; i++) {
            TeachingSuggestionVO.SkillGap g = gaps.get(i);
            TeachingSuggestionVO.CourseSuggestion cs = new TeachingSuggestionVO.CourseSuggestion();
            cs.setId(i + 1L);
            cs.setTitle(String.format("增设或强化「%s」相关课程", g.getSkill()));
            cs.setDescription(String.format("市场需求相对热度 %d%%（%d 个岗位），学生掌握率仅 %d%%（%d/%d 人），缺口 %d 个百分点。",
                    g.getMarketDemand(), g.getJobCount(), g.getStudentRate(),
                    g.getMasteredCount(), studentCount, g.getGap()));
            cs.setPriority(g.getGap() >= GAP_SEVERE ? "HIGH" : g.getGap() >= GAP_MODERATE ? "MEDIUM" : "LOW");
            list.add(cs);
        }
        return list;
    }
}
