package com.occupation.analysis.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 职位发布入参 — 供 HR 端在平台内发布职位（recommend 模块跨模块调用）
 *
 * @author occupation-team
 */
@Data
public class JobSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 编辑时必传，新增时为空 */
    private Long id;

    @NotBlank(message = "职位标题不能为空")
    private String title;

    @NotBlank(message = "公司名称不能为空")
    private String company;

    @NotBlank(message = "城市不能为空")
    private String city;

    private String industry;

    private Integer salaryMin;

    private Integer salaryMax;

    private String education;

    private String experience;

    /** 技能标签（JSON 数组字符串，如 ["Java","MySQL"]） */
    private String skills;

    private String description;
}
