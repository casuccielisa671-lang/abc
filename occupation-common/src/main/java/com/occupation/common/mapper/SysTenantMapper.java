package com.occupation.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.common.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper 接口
 *
 * @author occupation-team
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
}
