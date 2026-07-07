package com.occupation.analysis.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.JobQueryDTO;
import com.occupation.analysis.dto.JobSaveDTO;
import com.occupation.analysis.entity.JobDetail;
import com.occupation.analysis.mapper.JobDetailMapper;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Override
    public JobDetailVO getJobById(Long id) {
        JobDetail entity = jobDetailMapper.selectById(id);
        return entity == null ? null : toVO(entity);
    }

    @Override
    public Long saveJob(JobSaveDTO dto) {
        JobDetail job;
        if (dto.getId() != null) {
            job = jobDetailMapper.selectById(dto.getId());
            if (job == null) {
                throw new BizException("职位不存在");
            }
            if (!"HR_PUBLISH".equals(job.getSource())) {
                throw new BizException("仅允许编辑 HR 发布的职位，采集数据不可修改");
            }
        } else {
            job = new JobDetail();
            job.setSource("HR_PUBLISH");
            // HR 发布的职位无外部 URL，用内部标识占位保证去重逻辑不冲突
            job.setSourceUrl("hr://job/" + System.nanoTime());
            job.setPublishDate(LocalDate.now());
            job.setCreateTime(LocalDateTime.now());
        }
        job.setTitle(dto.getTitle());
        job.setCompany(dto.getCompany());
        job.setCity(dto.getCity());
        job.setIndustry(dto.getIndustry());
        job.setSalaryMin(dto.getSalaryMin());
        job.setSalaryMax(dto.getSalaryMax());
        job.setEducation(dto.getEducation());
        job.setExperience(dto.getExperience());
        job.setSkills(dto.getSkills() == null ? "[]" : dto.getSkills());
        job.setDescription(dto.getDescription());
        job.setUpdateTime(LocalDateTime.now());

        if (dto.getId() != null) {
            jobDetailMapper.updateById(job);
        } else {
            jobDetailMapper.insert(job);
        }
        return job.getId();
    }

    @Override
    public void removeJob(Long id) {
        JobDetail job = jobDetailMapper.selectById(id);
        if (job == null) {
            return;
        }
        if (!"HR_PUBLISH".equals(job.getSource())) {
            throw new BizException("仅允许下架 HR 发布的职位");
        }
        jobDetailMapper.deleteById(id);
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
