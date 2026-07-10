package com.occupation.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.recommend.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投递记录 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
