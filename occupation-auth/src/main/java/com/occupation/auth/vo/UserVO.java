package com.occupation.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息出参 — 不含密码等敏感字段
 *
 * @author occupation-team
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String role;
    private String realName;
    private String phone;
    private String email;
    /** 1=启用 0=禁用 */
    private Integer status;
    private LocalDateTime createTime;
}
