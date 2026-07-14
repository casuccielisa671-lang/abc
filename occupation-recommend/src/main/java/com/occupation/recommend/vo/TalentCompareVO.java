package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 人才对比 VO
 *
 * @author occupation-team
 */
@Data
public class TalentCompareVO {

    /** 候选人列表 */
    private List<TalentItem> talents;

    /** 对比总结 */
    private Summary summary;

    @Data
    public static class TalentItem {
        private Long id;
        private String name;
        private Integer score;
        private String education;
        private String experience;
        private String salary;
        private List<String> skills;
    }

    @Data
    public static class Summary {
        private String best;
        private String lowestSalary;
        private List<String> commonSkills;
    }
}
