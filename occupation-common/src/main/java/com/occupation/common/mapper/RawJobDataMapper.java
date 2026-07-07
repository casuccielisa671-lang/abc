package com.occupation.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.common.entity.RawJobData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原始职位数据 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface RawJobDataMapper extends BaseMapper<RawJobData> {
}
