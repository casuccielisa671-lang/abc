package com.occupation.recommend.model;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 简历中的可重复条目 — 教育 / 项目 / 实习
 * <p>
 * 这三类既是入参（学生编辑）也是出参（HR 查看、AI 诊断），
 * 放在共享 model 包里，避免在 DTO 和 VO 里各抄一份、字段一改就漏。
 * <p>
 * 落库时由 Service 序列化成 JSON 数组字符串存进 {@code student_resume} 的 TEXT 列；
 * <b>HTTP 层始终收发结构化数组</b> —— 不重蹈画像 {@code skills} 让前端手动
 * {@code JSON.stringify} 的覆辙。
 *
 * @author occupation-team
 */
public final class ResumeSection {

    private ResumeSection() {
    }

    /** 教育经历 */
    @Data
    public static class Education implements Serializable {
        private static final long serialVersionUID = 1L;

        @Size(max = 100, message = "学校名称过长")
        private String school;

        @Size(max = 100, message = "专业名称过长")
        private String major;

        /** 专科/本科/硕士/博士，与画像 educationLevel 同一套取值 */
        @Size(max = 20)
        private String degree;

        /** yyyy-MM */
        @Size(max = 10)
        private String startDate;

        @Size(max = 10)
        private String endDate;

        @Size(max = 20)
        private String gpa;
    }

    /** 项目经历 */
    @Data
    public static class Project implements Serializable {
        private static final long serialVersionUID = 1L;

        @Size(max = 100, message = "项目名称过长")
        private String name;

        @Size(max = 50)
        private String role;

        @Size(max = 10)
        private String startDate;

        @Size(max = 10)
        private String endDate;

        @Size(max = 2000, message = "项目描述过长")
        private String description;

        /** 该项目用到的技能 */
        private List<String> skills;
    }

    /** 实习经历 */
    @Data
    public static class Internship implements Serializable {
        private static final long serialVersionUID = 1L;

        @Size(max = 100, message = "公司名称过长")
        private String company;

        @Size(max = 100)
        private String position;

        @Size(max = 10)
        private String startDate;

        @Size(max = 10)
        private String endDate;

        @Size(max = 2000, message = "实习描述过长")
        private String description;
    }
}
