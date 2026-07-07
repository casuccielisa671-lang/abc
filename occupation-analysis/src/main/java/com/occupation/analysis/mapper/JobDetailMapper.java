package com.occupation.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.analysis.entity.JobDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位详情 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface JobDetailMapper extends BaseMapper<JobDetail> {
}
