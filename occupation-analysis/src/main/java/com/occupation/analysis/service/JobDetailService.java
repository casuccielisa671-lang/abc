package com.occupation.analysis.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.dto.JobSaveDTO;
import com.occupation.analysis.vo.JobDetailVO;

/**
 * 职位查询服务接口 — 供 recommend / api 模块调用
 * <p>
 * 提供清洗后的职位数据，供推荐引擎匹配、开放 API 查询使用；
 * 同时提供 HR 端平台内发布职位的写入口（source=HR_PUBLISH）。
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

    /**
     * 按 ID 查询职位详情
     *
     * @return 职位视图，不存在返回 null
     */
    JobDetailVO getJobById(Long id);

    /**
     * 新增/编辑职位（HR 端平台内发布，source 固定为 HR_PUBLISH）
     *
     * @return 职位 ID
     */
    Long saveJob(JobSaveDTO dto);

    /**
     * 下架职位（HR 端；物理删除仅限 HR_PUBLISH 来源的职位）
     */
    void removeJob(Long id);
}
