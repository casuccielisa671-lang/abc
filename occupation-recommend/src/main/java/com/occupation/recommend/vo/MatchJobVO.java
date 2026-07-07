package com.occupation.recommend.vo;

import com.occupation.analysis.vo.JobDetailVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 匹配结果出参 — 职位 + 匹配得分 + 匹配理由/差距分析
 *
 * @author occupation-team
 */
@Data
public class MatchJobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 职位信息 */
    private JobDetailVO job;

    /** 综合匹配得分（0~100） */
    private Integer score;

    /** 匹配理由（如 "技能匹配 3/5、城市一致、薪资符合预期"） */
    private String matchReason;

    /** 差距分析：职位要求但学生缺失的技能（个性化学习建议的依据） */
    private List<String> missingSkills;
}
