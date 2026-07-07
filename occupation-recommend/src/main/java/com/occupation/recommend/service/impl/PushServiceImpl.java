package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.exception.BizException;
import com.occupation.recommend.entity.PushRecord;
import com.occupation.recommend.mapper.PushRecordMapper;
import com.occupation.recommend.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 推送服务实现 — push_record 表（多租户自动隔离）
 * <p>
 * 当前实现为站内信；邮件/短信渠道为 P5 扩展点（接口不变，增加发送器即可）。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private final PushRecordMapper pushRecordMapper;

    @Override
    public void createPush(Long userId, String type, String title, String content) {
        PushRecord record = new PushRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setTitle(title);
        record.setContent(content);
        record.setIsRead(0);
        record.setCreateTime(LocalDateTime.now());
        pushRecordMapper.insert(record);
    }

    @Override
    public Page<PushRecord> pageMyPushes(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<PushRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushRecord::getUserId, userId)
               .orderByAsc(PushRecord::getIsRead)
               .orderByDesc(PushRecord::getCreateTime);
        return pushRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void markAsRead(Long pushId, Long userId) {
        PushRecord record = pushRecordMapper.selectById(pushId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BizException("消息不存在");
        }
        record.setIsRead(1);
        pushRecordMapper.updateById(record);
    }

    @Override
    public long countUnread(Long userId) {
        Long count = pushRecordMapper.selectCount(
                new LambdaQueryWrapper<PushRecord>()
                        .eq(PushRecord::getUserId, userId)
                        .eq(PushRecord::getIsRead, 0));
        return count == null ? 0 : count;
    }
}
