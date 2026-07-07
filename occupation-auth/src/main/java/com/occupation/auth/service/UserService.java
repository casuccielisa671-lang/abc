package com.occupation.auth.service;

import com.occupation.auth.entity.SysUser;

/**
 * 用户服务接口
 *
 * @author occupation-team
 */
public interface UserService {

    /**
     * 根据用户名和租户 ID 查询用户（联合唯一键）
     */
    SysUser getByUsername(String username, Long tenantId);

    /**
     * 根据用户 ID 查询
     */
    SysUser getById(Long userId);
}
