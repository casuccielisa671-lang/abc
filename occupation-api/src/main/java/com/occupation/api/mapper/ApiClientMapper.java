package com.occupation.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.api.entity.ApiClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 客户端 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface ApiClientMapper extends BaseMapper<ApiClient> {
}
