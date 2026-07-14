package com.occupation.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.common.entity.PushRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内通知 Mapper（多租户自动隔离）
 *
 * @author occupation-team
 */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {
}
