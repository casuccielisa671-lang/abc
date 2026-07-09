package com.occupation.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.exception.BizException;
import com.occupation.report.dto.TemplateSaveDTO;
import com.occupation.report.entity.ReportTemplate;
import com.occupation.report.mapper.ReportTemplateMapper;
import com.occupation.report.service.ReportTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 报告模板服务实现 — report_template 表 CRUD
 * <p>
 * 含 tenant_id，多租户插件自动隔离，代码无需手动过滤。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportTemplateServiceImpl implements ReportTemplateService {

    private final ReportTemplateMapper templateMapper;

    @Override
    public Page<ReportTemplate> pageTemplates(int pageNum, int pageSize) {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ReportTemplate::getCreateTime);
        return templateMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ReportTemplate getById(Long id) {
        ReportTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException("报告模板不存在");
        }
        return template;
    }

    @Override
    public ReportTemplate findById(Long id) {
        return id == null ? null : templateMapper.selectById(id);
    }

    @Override
    public void saveTemplate(TemplateSaveDTO dto) {
        ReportTemplate template;
        if (dto.getId() != null) {
            template = getById(dto.getId());
        } else {
            template = new ReportTemplate();
            template.setStatus(1);
        }
        template.setName(dto.getName());
        template.setIndustry(dto.getIndustry());
        template.setType(dto.getType());
        template.setTemplateContent(dto.getTemplateContent());
        if (dto.getStatus() != null) {
            template.setStatus(dto.getStatus());
        }

        if (dto.getId() != null) {
            templateMapper.updateById(template);
        } else {
            templateMapper.insert(template);
        }
    }

    @Override
    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }
}
