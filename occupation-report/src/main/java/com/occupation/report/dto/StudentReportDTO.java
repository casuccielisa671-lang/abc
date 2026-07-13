package com.occupation.report.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.List;

/**
 * 学生 AI 报告入参：预览（生成/多轮改）与保存共用。
 *
 * @author occupation-team
 */
@Data
public class StudentReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== 预览 / 多轮改 =====
    /** 修改指令（如「把薪资部分写详细点」）；首次生成可空 */
    private String instruction;
    /** 前端持有的多轮对话历史（role: user/assistant） */
    private List<Msg> history;

    // ===== 保存（定稿落库）=====
    /** 报告名称 */
    private String name;
    /** 定稿正文 */
    private String content;
    /** 导出格式 */
    @Pattern(regexp = "PDF|HTML", message = "学生 AI 报告支持 PDF/HTML")
    private String fileType = "PDF";

    @Data
    public static class Msg implements Serializable {
        private static final long serialVersionUID = 1L;
        private String role;
        private String content;
    }
}
