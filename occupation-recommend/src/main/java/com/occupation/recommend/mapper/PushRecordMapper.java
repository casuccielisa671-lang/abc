package com.occupation.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.recommend.entity.PushRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推送记录 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {
}
