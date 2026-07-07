package com.occupation.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.crawler.entity.CrawlerTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集任务 Mapper
 */
@Mapper
public interface CrawlerTaskMapper extends BaseMapper<CrawlerTask> {
}
