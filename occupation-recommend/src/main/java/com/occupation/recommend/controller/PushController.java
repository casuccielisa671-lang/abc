package com.occupation.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.recommend.entity.PushRecord;
import com.occupation.recommend.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 推送消息接口 — 所有登录角色可用（学生收推荐，其他角色收系统通知）
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    /** 我的推送列表（未读置顶） */
    @GetMapping("/list")
    public Result<PageResult<PushRecord>> myPushes(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        Page<PushRecord> page = pushService.pageMyPushes(UserContextHolder.getUserId(), pageNum, pageSize);
        return Result.ok(PageResult.of(page));
    }

    /** 标记已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        pushService.markAsRead(id, UserContextHolder.getUserId());
        return Result.ok();
    }

    /** 未读数量（导航栏红点） */
    @GetMapping("/unread/count")
    public Result<Long> unreadCount() {
        return Result.ok(pushService.countUnread(UserContextHolder.getUserId()));
    }
}
