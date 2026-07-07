package com.occupation.auth.controller;

import com.occupation.auth.dto.LoginDTO;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.service.TenantService;
import com.occupation.auth.service.UserService;
import com.occupation.auth.util.JwtUtil;
import com.occupation.auth.vo.LoginVO;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.entity.SysTenant;
import com.occupation.common.exception.BizException;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 — 登录/登出
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TenantService tenantService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录接口
     * <p>
     * 根据 tenantName + username + password 验证身份，返回 JWT Token。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        // 1. 根据租户名称查找租户（sys_tenant 不在多租户过滤范围内）
        SysTenant tenant = findTenantByName(dto.getTenantName());
        if (tenant == null || tenant.getStatus() != 1) {
            throw new BizException(401, "租户不存在或已禁用");
        }

        // 登录时设置租户上下文，确保多租户插件能正确注入 tenant_id 条件
        TenantContextHolder.setTenantId(tenant.getId());
        try {
            // 2. 查询用户
            SysUser user = userService.getByUsername(dto.getUsername(), tenant.getId());
            if (user == null) {
                throw new BizException(401, "用户名或密码错误");
            }
            if (user.getStatus() != 1) {
                throw new BizException(403, "账号已被禁用，请联系管理员");
            }

            // 3. 密码校验
            if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
                throw new BizException(401, "用户名或密码错误");
            }

            // 4. 生成 JWT
            String token = jwtUtil.generateToken(user.getId(), user.getTenantId(), user.getRole());
            long expiresAt = System.currentTimeMillis() + 86400000L; // 24h

            log.info("用户登录成功: username={}, role={}, tenant={}",
                    user.getUsername(), user.getRole(), tenant.getName());

            // 5. 封装响应
            LoginVO vo = LoginVO.builder()
                    .token(token)
                    .expiresAt(expiresAt)
                    .role(user.getRole())
                    .username(user.getUsername())
                    .realName(user.getRealName())
                    .tenantName(tenant.getName())
                    .build();

            return Result.ok(vo);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * 根据租户名称查找租户
     * <p>
     * 注意：sys_tenant 表不在多租户过滤范围内（已在 MyBatisPlusConfig.ignoreTable 中排除）。
     */
    private SysTenant findTenantByName(String name) {
        // iteration over the SysTenantMapper is in TenantService
        // We'll use the mapper directly for the name-based query
        // Since TenantService only has getById, we need to add a getByName
        // For now, we use a workaround: iterate tenants
        return tenantService.getByName(name);
    }
}
