package com.occupation.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 *
 * @author occupation-team
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 租户名称（登录时指定所属学校/企业） */
    @NotBlank(message = "租户名称不能为空")
    private String tenantName;
}
