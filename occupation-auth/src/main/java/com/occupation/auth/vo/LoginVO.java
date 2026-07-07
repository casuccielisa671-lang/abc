package com.occupation.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO
 *
 * @author occupation-team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT Token */
    private String token;

    /** Token 过期时间戳（毫秒） */
    private Long expiresAt;

    /** 用户角色 */
    private String role;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 租户名称 */
    private String tenantName;
}
