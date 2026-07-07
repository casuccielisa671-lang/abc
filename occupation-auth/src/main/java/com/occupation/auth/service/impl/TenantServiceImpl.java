package com.occupation.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.auth.service.TenantService;
import com.occupation.common.entity.SysTenant;
import com.occupation.common.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 租户服务实现
 *
 * @author occupation-team
 */
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final SysTenantMapper sysTenantMapper;

    @Override
    public SysTenant getById(Long tenantId) {
        return sysTenantMapper.selectById(tenantId);
    }

    @Override
    public SysTenant getByName(String name) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTenant::getName, name);
        return sysTenantMapper.selectOne(wrapper);
    }

    @Override
    public SysTenant create(SysTenant tenant) {
        sysTenantMapper.insert(tenant);
        return tenant;
    }
}
