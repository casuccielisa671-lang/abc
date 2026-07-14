package com.occupation.recommend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * JD 优化请求体
 *
 * @author occupation-team
 */
@Data
public class JdOptimizeRequest {

    /** 职位描述原文 */
    @NotBlank(message = "请输入职位描述内容")
    private String jdText;
}
