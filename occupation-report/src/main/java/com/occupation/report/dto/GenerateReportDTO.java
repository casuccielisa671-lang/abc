package com.occupation.report.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 报告生成入参
 *
 * @author occupation-team
 */
@Data
public class GenerateReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "模板 ID 不能为空")
    private Long templateId;

    /** 导出格式 */
    @Pattern(regexp = "PDF|WORD|HTML", message = "文件类型必须为 PDF/WORD/HTML")
    private String fileType = "HTML";
}
