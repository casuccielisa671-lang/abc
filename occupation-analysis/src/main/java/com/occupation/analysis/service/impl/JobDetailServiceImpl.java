package com.occupation.analysis.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 职位查询服务实现 — 全平台共享数据，不含租户隔离
 * <p>
 * job_detail 表不包含 tenant_id，多租户插件已配置忽略此表。
 *
 * @author occupation-team
 */
@Slf4j
@Service
public class JobDetailServiceImpl implements JobDetailService {

    @Autowired
    private JobDetailMapper jobDetailMapper;

    @Override
    public Page<JobDetailVO> queryJobs(JobQueryDTO query) {
        log.info("查询职位列表, pageNum={}, pageSize={}, city={}, industry={}, keyword={}",
                query.getPageNum(), query.getPageSize(),
                query.getCity(), query.getIndustry(), query.getKeyword());

        // 1. 构建查询条件
        QueryWrapper<JobDetail> wrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(query.getCity())) {
            wrapper.eq("city", query.getCity());
        }
        if (StrUtil.isNotBlank(query.getIndustry())) {
            wrapper.eq("industry", query.getIndustry());
        }
        // 薪资区间交集：用户期望薪资下限 ≤ 职位薪资上限，且用户期望薪资上限 ≥ 职位薪资下限
        if (query.getSalaryMin() != null) {
            wrapper.ge("salary_max", query.getSalaryMin());
        }
        if (query.getSalaryMax() != null) {
            wrapper.le("salary_min", query.getSalaryMax());
        }
        if (StrUtil.isNotBlank(query.getEducation())) {
            wrapper.eq("education", query.getEducation());
        }
        if (StrUtil.isNotBlank(query.getExperience())) {
            wrapper.eq("experience", query.getExperience());
        }
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.and(w -> w.like("title", query.getKeyword())
                              .or()
                              .like("company", query.getKeyword()));
        }

        // 按发布日期降序
        wrapper.orderByDesc("publish_date");

        // 2. 分页查询
        Page<JobDetail> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<JobDetail> entityPage = jobDetailMapper.selectPage(page, wrapper);

        // 3. Entity → VO 转换
        Page<JobDetailVO> voPage = new Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<JobDetailVO> voList = entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        log.info("职位查询结果: total={}, currentPage={}", voPage.getTotal(), voPage.getCurrent());
        return voPage;
    }

    /**
     * JobDetail Entity → JobDetailVO
     */
    private JobDetailVO toVO(JobDetail entity) {
        JobDetailVO vo = new JobDetailVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setCompany(entity.getCompany());
        vo.setCity(entity.getCity());
        vo.setIndustry(entity.getIndustry());
        vo.setSalaryMin(entity.getSalaryMin());
        vo.setSalaryMax(entity.getSalaryMax());
        vo.setEducation(entity.getEducation());
        vo.setExperience(entity.getExperience());
        vo.setSkills(entity.getSkills());
        vo.setDescription(entity.getDescription());
        vo.setPublishDate(entity.getPublishDate());
        vo.setSource(entity.getSource());
        vo.setSourceUrl(entity.getSourceUrl());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
