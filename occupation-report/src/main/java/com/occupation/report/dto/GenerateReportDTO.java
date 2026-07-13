package com.occupation.report.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 报告生成入参（无模板：直接选大类 + 范围 + 格式）
 *
 * @author occupation-team
 */
@Data
public class GenerateReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报告大类：MARKET=市场行业 / EMPLOYMENT=学生就业 */
    @Pattern(regexp = "MARKET|EMPLOYMENT", message = "大类必须为 MARKET/EMPLOYMENT")
    private String category = "MARKET";

    /** 导出格式 */
    @Pattern(regexp = "PDF|WORD|HTML", message = "文件类型必须为 PDF/WORD/HTML")
    private String fileType = "HTML";

    // ===== EMPLOYMENT 类报告的范围（均可空：给 classId 按班级；给 major/enrollYear 按专业/届；全空按全校）=====
    /** 专业 */
    private String major;
    /** 入学年级 */
    private Integer enrollYear;
    /** 班级 ID */
    private Long classId;
}
