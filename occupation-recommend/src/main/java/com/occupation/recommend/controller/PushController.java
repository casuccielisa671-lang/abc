package com.occupation.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.entity.PushRecord;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 站内消息接口 — 所有登录角色可用
 * <p>
 * 学生收推荐 / 投递进度 / 面试通知 / 报告下发，其他角色收系统通知。
 * 底层站内信服务 {@link NotificationService} 在 common，供各模块共用。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final NotificationService notificationService;

    /** 我的消息列表（未读置顶，时间倒序） */
    @GetMapping("/list")
    public Result<PageResult<PushRecord>> myPushes(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        Page<PushRecord> page = notificationService.pageMyPushes(UserContextHolder.getUserId(), pageNum, pageSize);
        return Result.ok(PageResult.of(page));
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id, UserContextHolder.getUserId());
        return Result.ok();
    }

    /** 全部标记已读，返回处理条数 */
    @PutMapping("/read-all")
    public Result<Integer> markAllRead() {
        return Result.ok(notificationService.markAllRead(UserContextHolder.getUserId()));
    }

    /** 未读数量（导航栏红点） */
    @GetMapping("/unread/count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.countUnread(UserContextHolder.getUserId()));
    }
}
