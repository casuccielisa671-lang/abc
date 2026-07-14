package com.occupation.common.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.entity.PushRecord;

/**
 * 站内通知服务 — 全平台唯一的站内信出口
 * <p>
 * 放在 common 供任意模块复用：推荐（recommend）、投递状态变更（recommend）、
 * 报告下发（report）都通过它发消息，避免各模块各写一份、也避免跨业务模块循环依赖。
 * 当前实现为站内信；邮件/短信渠道为后续扩展点（接口不变，增加发送器即可）。
 *
 * @author occupation-team
 */
public interface NotificationService {

    /**
     * 发一条纯通知（不可点击跳转）
     *
     * @param userId  目标用户
     * @param type    RECOMMEND / SYSTEM / INTERVIEW / OFFER / REJECT / REPORT
     * @param title   标题
     * @param content 内容
     */
    void createPush(Long userId, String type, String title, String content);

    /**
     * 发一条可跳转通知
     *
     * @param refType 关联对象类型：APPLICATION / REPORT
     * @param refId   关联对象 ID，前端据此跳转
     */
    void createPush(Long userId, String type, String title, String content, String refType, Long refId);

    /** 我的消息列表（未读在前，时间倒序） */
    Page<PushRecord> pageMyPushes(Long userId, int pageNum, int pageSize);

    /** 标记单条已读（校验归属，不能读别人的消息） */
    void markAsRead(Long pushId, Long userId);

    /** 全部标记已读，返回受影响条数 */
    int markAllRead(Long userId);

    /** 未读数（前端导航栏红点） */
    long countUnread(Long userId);
}
