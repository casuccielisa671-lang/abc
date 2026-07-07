package com.occupation.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 用户新增/编辑入参（管理员用户管理）
 *
 * @author occupation-team
 */
@Data
public class UserSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 编辑时必传，新增时为空 */
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 新增时必填；编辑时留空表示不修改密码 */
    private String password;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "STUDENT|TEACHER|ADMIN|HR", message = "角色必须为 STUDENT/TEACHER/ADMIN/HR")
    private String role;

    private String realName;

    private String phone;

    private String email;
}
