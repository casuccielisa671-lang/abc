package com.occupation.auth.service;

import com.occupation.common.entity.SysTenant;

/**
 * 租户服务接口
 *
 * @author occupation-team
 */
public interface TenantService {

    /**
     * 根据租户 ID 查询租户信息
     */
    SysTenant getById(Long tenantId);

    /**
     * 根据租户名称查询（用于登录时识别所属学校/企业）
     */
    SysTenant getByName(String name);

    /**
     * 创建租户
     */
    SysTenant create(SysTenant tenant);
}
