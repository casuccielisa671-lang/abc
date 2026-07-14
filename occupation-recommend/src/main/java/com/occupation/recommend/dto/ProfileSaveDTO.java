package com.occupation.recommend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 学生画像保存入参
 *
 * @author occupation-team
 */
@Data
public class ProfileSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "专业不能为空")
    private String major;

    /** 技能列表（JSON 数组字符串，如 ["Java","MySQL"]） */
    private String skills;

    private String expectedCity;

    private String expectedIndustry;

    private Integer expectedSalaryMin;

    private Integer expectedSalaryMax;

    /** 专科/本科/硕士/博士 */
    private String educationLevel;

    /** 证件照URL（上传后由后端填充） */
    private String avatarUrl;
}
