package com.occupation.report.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生个人 AI 分析报告的预览结果（尚未落库）。
 *
 * @author occupation-team
 */
@Data
public class StudentAiReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报告标题 */
    private String title;
    /** 正文（AI 生成或规则化兜底） */
    private String content;
    /** 是否由 AI 生成（false=AI 未启用/失败，走规则化文字） */
    private boolean aiGenerated;

    public static StudentAiReportVO of(String title, String content, boolean aiGenerated) {
        StudentAiReportVO vo = new StudentAiReportVO();
        vo.title = title;
        vo.content = content;
        vo.aiGenerated = aiGenerated;
        return vo;
    }
}
