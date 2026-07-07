package com.occupation.analysis.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.vo.JobDetailVO;

/**
 * 职位查询服务接口 — 供 recommend 模块调用
 * <p>
 * 提供清洗后的职位数据，供推荐引擎匹配使用。
 *
 * @author occupation-team
 */
public interface JobDetailService {

    /**
     * 分页查询职位列表
     *
     * @param query 筛选条件（城市、行业、薪资范围、学历、经验等）
     * @return 分页职位视图
     */
    Page<JobDetailVO> queryJobs(JobQueryDTO query);
}
