package com.occupation.recommend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.recommend.entity.PushRecord;

/**
 * 推送服务 — 站内消息通知
 *
 * @author occupation-team
 */
public interface PushService {

    /**
     * 创建一条推送
     *
     * @param userId  目标用户
     * @param type    RECOMMEND（推荐类）/ SYSTEM（系统类）
     * @param title   标题
     * @param content 内容
     */
    void createPush(Long userId, String type, String title, String content);

    /** 我的推送列表（未读在前，时间倒序） */
    Page<PushRecord> pageMyPushes(Long userId, int pageNum, int pageSize);

    /** 标记已读（校验归属，不能读别人的消息） */
    void markAsRead(Long pushId, Long userId);

    /** 未读数（前端导航栏红点） */
    long countUnread(Long userId);
}
