package com.occupation.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.report.dto.TemplateSaveDTO;
import com.occupation.report.entity.ReportTemplate;
import com.occupation.report.service.ReportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 报告模板管理接口（管理后台，ADMIN）
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/admin/report/template")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportTemplateController {

    private final ReportTemplateService templateService;

    /** 模板分页列表 */
    @GetMapping
    public Result<PageResult<ReportTemplate>> pageTemplates(@RequestParam(defaultValue = "1") int pageNum,
                                                            @RequestParam(defaultValue = "10") int pageSize) {
        Page<ReportTemplate> page = templateService.pageTemplates(pageNum, pageSize);
        return Result.ok(PageResult.of(page));
    }

    /** 模板详情 */
    @GetMapping("/{id}")
    public Result<ReportTemplate> getById(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    /** 新增模板 */
    @PostMapping
    public Result<Void> create(@RequestBody @Validated TemplateSaveDTO dto) {
        dto.setId(null);
        templateService.saveTemplate(dto);
        return Result.ok();
    }

    /** 编辑模板 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated TemplateSaveDTO dto) {
        dto.setId(id);
        templateService.saveTemplate(dto);
        return Result.ok();
    }

    /** 删除模板 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.ok();
    }
}
