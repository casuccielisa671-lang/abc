package com.occupation.common.utils;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 技能字段解析工具
 * <p>
 * 库中的 skills 列（job_detail / sys_student_profile）标准格式是 JSON 数组
 * {@code ["Java","MySQL"]}，但历史数据里存在逗号/顿号分隔的旧格式。
 * 解析入口统一收敛到这里，避免各模块各写一份容错逻辑。
 *
 * @author occupation-team
 */
public final class SkillUtils {

    private SkillUtils() {
    }

    /**
     * 解析技能字段，兼容 JSON 数组与「逗号/顿号分隔」旧格式；空值返回空列表。
     * 结果已去空白、去空串。
     */
    public static List<String> parse(String skills) {
        if (skills == null || skills.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> raw;
        try {
            raw = JSON.parseArray(skills, String.class);
        } catch (Exception e) {
            raw = new ArrayList<>();
            for (String s : skills.split("[,，、]")) {
                raw.add(s);
            }
        }
        if (raw == null) {
            return Collections.emptyList();
        }
        List<String> cleaned = new ArrayList<>(raw.size());
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (!t.isEmpty()) {
                cleaned.add(t);
            }
        }
        return cleaned;
    }

    /**
     * 序列化为 JSON 数组字符串（入库标准格式）；空集合返回 {@code "[]"}。
     */
    public static String toJson(List<String> skills) {
        return skills == null || skills.isEmpty() ? "[]" : JSON.toJSONString(skills);
    }

    /**
     * 解析并按不区分大小写去重，保留首次出现顺序。
     */
    public static Set<String> parseDistinct(String skills) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> lowered = new LinkedHashSet<>();
        for (String s : parse(skills)) {
            if (lowered.add(s.toLowerCase())) {
                seen.add(s);
            }
        }
        return seen;
    }

    /**
     * 集合中是否包含目标技能（忽略大小写与首尾空白）。
     */
    public static boolean containsIgnoreCase(Iterable<String> skills, String target) {
        if (target == null) {
            return false;
        }
        String t = target.trim();
        for (String s : skills) {
            if (s != null && s.trim().equalsIgnoreCase(t)) {
                return true;
            }
        }
        return false;
    }
}
