package com.occupation.auth.service;

import com.occupation.common.entity.SysTenant;

import java.util.List;

/**
 * 租户服务接口
 *
 * @author occupation-team
 */
public interface TenantService {

    /**
     * 全部启用状态的租户，按名称排序。
     * <p>
     * 供登录页的学校/企业下拉联想使用（无鉴权接口），因此只返回 status=1 的租户，
     * 已禁用的租户不出现在列表里。
     */
    List<SysTenant> listActive();

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
