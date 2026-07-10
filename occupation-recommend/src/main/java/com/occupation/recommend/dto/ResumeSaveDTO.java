package com.occupation.recommend.dto;

import com.occupation.recommend.model.ResumeSection;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 简历保存入参
 * <p>
 * 三段经历直接收结构化数组，Service 负责序列化成 JSON 落库。
 * {@code @Valid} 会递归校验列表里的每个条目。
 *
 * @author occupation-team
 */
@Data
public class ResumeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 留空则回落到 sys_user.phone */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    /** 留空则回落到 sys_user.email */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String contactEmail;

    @Size(max = 100, message = "求职意向过长")
    private String jobIntention;

    @Size(max = 1000, message = "自我评价不能超过 1000 字")
    private String selfIntro;

    @Valid
    private List<ResumeSection.Education> educations;

    @Valid
    private List<ResumeSection.Project> projects;

    @Valid
    private List<ResumeSection.Internship> internships;

    private List<String> honors;
}
