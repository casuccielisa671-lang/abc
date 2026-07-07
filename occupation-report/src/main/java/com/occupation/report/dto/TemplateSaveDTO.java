package com.occupation.report.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 报告模板新增/编辑入参
 *
 * @author occupation-team
 */
@Data
public class TemplateSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 编辑时必传，新增时为空 */
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    /** 适用行业，空表示通用 */
    private String industry;

    @NotBlank(message = "报告类型不能为空")
    @Pattern(regexp = "MONTHLY|QUARTERLY|YEARLY", message = "类型必须为 MONTHLY/QUARTERLY/YEARLY")
    private String type;

    /** 模板内容：Freemarker HTML 模板（可引用 dashboard / aiSummary / generateTime 等变量）；
     *  留空则使用系统内置默认模板 */
    private String templateContent;

    /** 1=启用 0=禁用 */
    private Integer status;
}
