package com.occupation.recommend.service;

import com.occupation.recommend.vo.SalaryBenchmarkVO;
import com.occupation.recommend.vo.TalentCompareVO;

import java.util.List;

/**
 * HR端工具箱服务 — 人才对比 / 薪资竞争力
 *
 * @author occupation-team
 */
public interface HrToolService {

    /**
     * 人才对比：选择 2~4 个候选人并排对比
     */
    TalentCompareVO compareTalents(List<Long> userIds);

    /**
     * 薪资竞争力分析：输入岗位，对比市场薪资分位
     */
    SalaryBenchmarkVO benchmarkSalary(String jobTitle);
}
