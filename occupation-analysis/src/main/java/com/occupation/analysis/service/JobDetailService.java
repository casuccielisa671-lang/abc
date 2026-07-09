package com.occupation.analysis.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.dto.JobSaveDTO;
import com.occupation.analysis.vo.JobDetailVO;

import java.util.Collection;
import java.util.List;

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
     * 批量按 ID 查询（收藏列表、行为加权推荐用），一次查库避免 N+1。
     * <p>
     * 返回顺序与传入的 ids 一致；不存在的 ID 被跳过。ids 为空时返回空列表。
     */
    List<JobDetailVO> listByIds(Collection<Long> ids);

    /**
     * 新增/编辑职位（HR 端平台内发布，source 固定为 HR_PUBLISH）
     * <p>
     * 新增时以 {@code operatorId} 作为 publisher_id 落库；编辑时校验该职位确由
     * {@code operatorId} 发布，防止 HR 之间越权改动彼此的职位。
     *
     * @param operatorId 当前操作人用户 ID
     * @return 职位 ID
     */
    Long saveJob(JobSaveDTO dto, Long operatorId);

    /**
     * 下架职位（HR 端；物理删除仅限 HR_PUBLISH 来源、且由 {@code operatorId} 发布的职位）
     *
     * @param operatorId 当前操作人用户 ID
     */
    void removeJob(Long id, Long operatorId);
}
