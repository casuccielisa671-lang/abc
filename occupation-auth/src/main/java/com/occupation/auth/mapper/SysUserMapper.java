package com.occupation.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * @author occupation-team
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
