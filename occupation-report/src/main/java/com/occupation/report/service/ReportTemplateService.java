package com.occupation.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.report.dto.TemplateSaveDTO;
import com.occupation.report.entity.ReportTemplate;

/**
 * 报告模板服务 — 模板 CRUD（按租户隔离）
 *
 * @author occupation-team
 */
public interface ReportTemplateService {

    /** 分页查询模板列表 */
    Page<ReportTemplate> pageTemplates(int pageNum, int pageSize);

    /** 查询模板详情 */
    ReportTemplate getById(Long id);

    /** 新增/编辑模板 */
    void saveTemplate(TemplateSaveDTO dto);

    /** 删除模板（逻辑删除） */
    void deleteTemplate(Long id);
}
