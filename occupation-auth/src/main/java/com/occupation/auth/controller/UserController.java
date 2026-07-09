package com.occupation.auth.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.dto.UserSaveDTO;
import com.occupation.auth.service.UserService;
import com.occupation.auth.service.impl.UserServiceImpl;
import com.occupation.auth.vo.BatchImportVO;
import com.occupation.auth.vo.UserVO;
import com.occupation.common.exception.BizException;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    /**
     * Excel 批量导入用户
     * <p>
     * 全量校验通过才写库；任一行不合法则整体拒绝并返回逐行错误，
     * 便于管理员改好名单后重传，不会留下导入一半的状态。
     */
    @PostMapping("/batch-import")
    public Result<BatchImportVO> batchImport(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BizException("请选择要导入的 Excel 文件");
        }
        try (InputStream in = file.getInputStream()) {
            return Result.ok(userService.batchImport(in));
        }
    }

    /** 下载导入模板（含表头与一行示例） */
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        String filename = "用户批量导入模板.xlsx";
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));

        try (ExcelWriter writer = ExcelUtil.getWriter(true);
             OutputStream out = response.getOutputStream()) {
            writer.writeHeadRow(UserServiceImpl.IMPORT_COLUMNS);
            // 示例行：密码留空表示使用系统初始密码
            writer.writeRow(Arrays.asList("student99", "张三", "学生", "13800000000", "zhangsan@example.com", ""));
            writer.flush(out, true);
        }
    }
}
