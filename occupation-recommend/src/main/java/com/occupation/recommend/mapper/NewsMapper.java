package com.occupation.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.occupation.recommend.entity.News;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资讯 Mapper
 *
 * @author occupation-team
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface NewsMapper extends BaseMapper<News> {
}
