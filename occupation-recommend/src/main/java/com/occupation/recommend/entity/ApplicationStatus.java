package com.occupation.recommend.entity;

import java.util.Arrays;

/**
 * 投递状态机
 * <p>
 * 只允许<b>向前推进或直接拒绝</b>，不允许把 REJECTED 改回 INTERVIEW 这类回退 ——
 * 学生那边已经看到「不合适」了，再改回去只会制造困惑。
 * HR 改错了只能靠备注说明，这是刻意的：状态变更对学生可见，就该是不可撤销的。
 *
 * @author occupation-team
 */
public enum ApplicationStatus {

    /** 学生刚投递，HR 还没看 */
    SUBMITTED("已投递"),
    /** HR 打开过简历 */
    VIEWED("已查看"),
    /** 进入面试流程 */
    INTERVIEW("邀请面试"),
    /** 终态：录用 */
    OFFER("已录用"),
    /** 终态：不合适 */
    REJECTED("不合适");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return this == OFFER || this == REJECTED;
    }

    public static boolean isValid(String name) {
        return name != null && Arrays.stream(values()).anyMatch(s -> s.name().equals(name));
    }

    /**
     * 能否从 from 流转到 to。
     * <p>
     * 规则：终态不可再变；其余状态可以推进到任意<b>更靠后</b>的状态，也可以直接 REJECTED。
     * 不允许原地不动（前端不该发一次无意义的请求）。
     */
    public static boolean canTransit(ApplicationStatus from, ApplicationStatus to) {
        if (from == to || from.isTerminal()) {
            return false;
        }
        return to == REJECTED || to.ordinal() > from.ordinal();
    }
}
