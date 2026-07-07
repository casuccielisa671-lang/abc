package com.occupation.web.controller;

import com.occupation.common.entity.SysTenant;
import com.occupation.common.exception.BizException;
import com.occupation.common.mapper.SysTenantMapper;
import com.occupation.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查 & 框架验证接口
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final SysTenantMapper sysTenantMapper;

    public HealthController(SysTenantMapper sysTenantMapper) {
        this.sysTenantMapper = sysTenantMapper;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("ok");
    }

    /**
     * 异常测试（验证全局异常处理器是否生效）
     */
    @GetMapping("/health/error")
    public Result<String> errorTest() {
        throw new BizException(400, "测试业务异常——全局异常处理器生效");
    }

    /**
     * 数据库连接测试 — 同时验证 SysTenant 和 SysUser 查询
     */
    @GetMapping("/health/db")
    public Result<Map<String, Object>> dbTest() {
        Map<String, Object> result = new HashMap<>();
        try {
            SysTenant tenant = sysTenantMapper.selectById(1L);
            result.put("tenantFound", tenant != null);
            if (tenant != null) {
                result.put("tenantName", tenant.getName());
                result.put("tenantStatus", tenant.getStatus());
            }
            long count = sysTenantMapper.selectCount(null);
            result.put("tenantCount", count);
        } catch (Exception e) {
            result.put("sysTenantError", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return Result.ok(result);
    }
}
