package com.occupation.web.controller;

import com.occupation.common.entity.SysTenant;
import com.occupation.common.exception.BizException;
import com.occupation.common.mapper.SysTenantMapper;
import com.occupation.common.result.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final JdbcTemplate jdbcTemplate;

    public HealthController(SysTenantMapper sysTenantMapper, JdbcTemplate jdbcTemplate) {
        this.sysTenantMapper = sysTenantMapper;
        this.jdbcTemplate = jdbcTemplate;
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
     * 数据库连接测试 — 同时验证 SysTenant 和 SysUser 查询 + 关键业务表存在性
     */
    @GetMapping("/health/db")
    public Result<Map<String, Object>> dbTest() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 租户表检查
        try {
            SysTenant tenant = sysTenantMapper.selectById(1L);
            result.put("sysTenant", tenant != null);
            if (tenant != null) {
                result.put("tenantName", tenant.getName());
                result.put("tenantStatus", tenant.getStatus());
            }
            long tenantCount = sysTenantMapper.selectCount(null);
            result.put("tenantCount", tenantCount);
        } catch (Exception e) {
            result.put("sysTenant", "ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // 2. 关键业务表存在性检查
        String[] tables = {
            "sys_user", "sys_class", "job_detail", "sys_student_profile",
            "student_behavior", "analysis_result", "crawler_task", "news"
        };
        Map<String, Object> tableStatus = new LinkedHashMap<>();
        for (String table : tables) {
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table, Long.class);
                tableStatus.put(table, count != null ? count + " rows" : "ERROR");
            } catch (Exception e) {
                tableStatus.put(table, "MISSING or ERROR: " + e.getMessage());
            }
        }
        result.put("tables", tableStatus);

        // 3. 数据连接概要
        try {
            String dbUrl = jdbcTemplate.queryForObject(
                "SELECT CONCAT(@@hostname, ':', @@port, '/', DATABASE())", String.class);
            result.put("database", dbUrl);
        } catch (Exception e) {
            result.put("database", "无法获取: " + e.getMessage());
        }

        return Result.ok(result);
    }

}
