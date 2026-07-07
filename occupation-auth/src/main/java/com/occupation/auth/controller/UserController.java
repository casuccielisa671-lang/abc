package com.occupation.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.dto.UserSaveDTO;
import com.occupation.auth.service.UserService;
import com.occupation.auth.vo.UserVO;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口（管理后台）
 * <p>
 * 仅 ADMIN 角色可访问；数据自动限定在当前租户内（多租户插件）。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    /**
     * 用户分页列表（可按角色筛选、按用户名/姓名搜索）
     */
    @GetMapping
    public Result<PageResult<UserVO>> pageUsers(@RequestParam(required = false) String role,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        Page<UserVO> page = userService.pageUsers(role, keyword, pageNum, pageSize);
        return Result.ok(PageResult.of(page));
    }

    /**
     * 新增用户
     */
    @PostMapping
    public Result<Void> createUser(@RequestBody @Validated UserSaveDTO dto) {
        dto.setId(null);
        userService.saveUser(dto);
        return Result.ok();
    }

    /**
     * 编辑用户（密码留空表示不修改）
     */
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody @Validated UserSaveDTO dto) {
        dto.setId(id);
        userService.saveUser(dto);
        return Result.ok();
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    // TODO(P3): POST /api/admin/users/batch-import — Excel 批量导入（EasyExcel 或 POI 解析）
}
