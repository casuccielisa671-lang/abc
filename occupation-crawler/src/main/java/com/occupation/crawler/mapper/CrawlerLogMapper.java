package com.occupation.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.crawler.entity.CrawlerLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集日志 Mapper
 */
@Mapper
public interface CrawlerLogMapper extends BaseMapper<CrawlerLog> {
}
