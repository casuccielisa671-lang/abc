package com.occupation.recommend.controller;

import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.service.AdminToolService;
import com.occupation.recommend.vo.TenantHealthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端工具箱接口 — 数据导出 / 租户健康度
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tools")
@RequiredArgsConstructor
public class AdminToolController {

    private final AdminToolService adminToolService;

    /** 租户健康度监控：各租户活跃度、数据完整度 */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tenant-health")
    public Result<TenantHealthVO> tenantHealth() {
        log.info("租户健康度查询: adminId={}", UserContextHolder.getUserId());
        return Result.ok(adminToolService.getTenantHealth());
    }
}
