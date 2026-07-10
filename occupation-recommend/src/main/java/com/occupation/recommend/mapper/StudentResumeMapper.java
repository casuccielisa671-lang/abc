package com.occupation.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.recommend.entity.StudentResume;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生简历 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface StudentResumeMapper extends BaseMapper<StudentResume> {
}
