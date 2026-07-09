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

    /** 查询模板详情；不存在时抛业务异常 */
    ReportTemplate getById(Long id);

    /**
     * 查询模板详情，不存在返回 null。
     * <p>
     * 报告记录列表要关联模板名称，而模板可能已被删除 ——
     * 用 {@link #getById} 会让整个列表接口因为一条脏数据而失败。
     */
    ReportTemplate findById(Long id);

    /** 新增/编辑模板 */
    void saveTemplate(TemplateSaveDTO dto);

    /** 删除模板（逻辑删除） */
    void deleteTemplate(Long id);
}
