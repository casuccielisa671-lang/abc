package com.occupation.recommend.service;

import com.occupation.recommend.vo.JobChecklistVO;
import com.occupation.recommend.vo.JobCompareVO;
import com.occupation.recommend.vo.SalaryCalcVO;
import com.occupation.recommend.vo.SkillRoiVO;

import java.util.List;

/**
 * 学生端工具箱服务 — 多岗位对比 / 技能ROI / 期望薪资 / 求职清单
 *
 * @author occupation-team
 */
public interface StudentToolService {

    /**
     * 多岗位对比：选择 2~4 个岗位，从薪资、技能、学历、城市等多维度并排对比
     */
    JobCompareVO compareJobs(List<Long> jobIds);

    /**
     * 技能 ROI 分析：量化学习某项技能的薪资回报与市场需求
     */
    SkillRoiVO analyzeSkillRoi(String skillName);

    /**
     * 期望薪资计算器：基于城市、岗位、学历、经验给出合理期望薪资范围
     */
    SalaryCalcVO calcSalary(String city, String keyword, String education, String experience);

    /**
     * 求职清单生成器：对比岗位要求与个人技能，生成差距分析与学习路径建议
     */
    JobChecklistVO generateChecklist(Long jobId, Long userId);
}
