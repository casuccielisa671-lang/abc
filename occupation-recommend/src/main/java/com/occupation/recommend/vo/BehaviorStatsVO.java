package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.occupation.recommend.entity.BehaviorAction;
import com.occupation.recommend.entity.StudentBehavior;

/**
 * 单个学生的求职行为统计
 * <p>
 * 过去接口直接返回 {@code Map<String,Long>}（键是 VIEW/FAVORITE/APPLY），
 * 前端却按 viewCount/favoriteCount/applyCount 读，永远是 0。这里改成明确的字段。
 *
 * @author occupation-team
 */
@Data
public class BehaviorStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long viewCount;
    private long favoriteCount;
    private long applyCount;
    private long ignoreCount;
    /** 自主联系（对采集职位表达求职意向）的次数 */
    private long contactCount;

    /** 最近一次行为时间；无任何行为时为 null */
    private LocalDateTime lastActiveTime;

    public static BehaviorStatsVO of(Map<String, Long> counts, List<StudentBehavior> recent) {
        BehaviorStatsVO vo = new BehaviorStatsVO();
        vo.viewCount = counts.getOrDefault(BehaviorAction.VIEW, 0L);
        vo.favoriteCount = counts.getOrDefault(BehaviorAction.FAVORITE, 0L);
        vo.applyCount = counts.getOrDefault(BehaviorAction.APPLY, 0L);
        vo.ignoreCount = counts.getOrDefault(BehaviorAction.IGNORE, 0L);
        vo.contactCount = counts.getOrDefault(BehaviorAction.CONTACT, 0L);
        // recent 已按时间倒序，第一条即最近一次
        vo.lastActiveTime = recent.isEmpty() ? null : recent.get(0).getCreateTime();
        return vo;
    }
}
