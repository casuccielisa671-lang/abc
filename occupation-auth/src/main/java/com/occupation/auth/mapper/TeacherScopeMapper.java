package com.occupation.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.auth.entity.TeacherScope;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教师可见范围 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface TeacherScopeMapper extends BaseMapper<TeacherScope> {
}
