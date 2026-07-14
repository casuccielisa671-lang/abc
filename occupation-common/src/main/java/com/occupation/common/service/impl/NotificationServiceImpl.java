package com.occupation.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.entity.PushRecord;
import com.occupation.common.exception.BizException;
import com.occupation.common.mapper.PushRecordMapper;
import com.occupation.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 站内通知服务实现 — push_record 表（多租户自动隔离）。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final PushRecordMapper pushRecordMapper;

    @Override
    public void createPush(Long userId, String type, String title, String content) {
        createPush(userId, type, title, content, null, null);
    }

    @Override
    public void createPush(Long userId, String type, String title, String content, String refType, Long refId) {
        PushRecord record = new PushRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setTitle(title);
        record.setContent(content);
        record.setRefType(refType);
        record.setRefId(refId);
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
    public int markAllRead(Long userId) {
        return pushRecordMapper.update(null, new LambdaUpdateWrapper<PushRecord>()
                .eq(PushRecord::getUserId, userId)
                .eq(PushRecord::getIsRead, 0)
                .set(PushRecord::getIsRead, 1));
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
