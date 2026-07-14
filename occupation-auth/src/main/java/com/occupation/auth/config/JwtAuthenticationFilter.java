package com.occupation.auth.config;

import com.occupation.auth.util.JwtUtil;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.config.UserContextHolder;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * <p>
 * 拦截所有 API 请求，从 Authorization Header 提取 Bearer Token，
 * 校验有效性后将 userId、tenantId、role 设置到上下文。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /** Bearer Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 白名单路径（无需认证）
     *  注意：这是独立于 SecurityConfig.permitAll() 的第二道白名单 ——
     *  本过滤器跑在 Spring Security 授权之前，只在 SecurityConfig 里放行是不够的，
     *  请求会先被这里拦下返回 401。新增无鉴权接口时两处都要加。
     *  /api/open/** 由 occupation-api 模块的 ApiTokenInterceptor 独立鉴权（apiKey → Token），
     *  /doc.html、/v3/api-docs、/webjars 为 Knife4j 接口文档静态资源。 */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/tenants",
            "/api/health",
            "/api/health/error",
            "/api/open/",
            "/api/news",
            "/doc.html",
            "/v3/api-docs",
            "/webjars/",
            "/favicon.ico"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 尝试从请求中提取并解析 Token（无论是否白名单）
        String token = extractToken(request);
        DecodedJWT jwt = (StringUtils.hasText(token)) ? jwtUtil.verifyAndParse(token) : null;

        // 白名单路径：有 Token 就设置租户上下文（保证 MetaObjectHandler 等组件可用），
        // 没 Token 也正常放行（匿名访问）。
        if (isWhiteListed(request.getServletPath())) {
            if (jwt != null) {
                try {
                    TenantContextHolder.setTenantId(jwtUtil.getTenantId(jwt));
                    UserContextHolder.set(jwtUtil.getUserId(jwt), jwtUtil.getRole(jwt));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    jwtUtil.getUserId(jwt), null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + jwtUtil.getRole(jwt))));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    log.debug("白名单路径 Token 解析失败，继续匿名访问: {}", e.getMessage());
                }
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContextHolder.clear();
                UserContextHolder.clear();
                SecurityContextHolder.clearContext();
            }
            return;
        }

        // 非白名单路径：必须提供有效 Token
        if (!StringUtils.hasText(token)) {
            log.warn("缺少 Token: {}", request.getServletPath());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 已过期\",\"data\":null}");
            return;
        }

        if (jwt == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return;
        }

        // 设置租户上下文 + Spring Security 认证对象
        try {
            Long tenantId = jwtUtil.getTenantId(jwt);
            TenantContextHolder.setTenantId(tenantId);

            // 设置 Spring Security 认证信息（否则 authorizeRequests 会返回 403）
            String role = jwtUtil.getRole(jwt);
            // 设置用户上下文（业务层通过 UserContextHolder 获取当前操作人）
            UserContextHolder.set(jwtUtil.getUserId(jwt), role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            jwtUtil.getUserId(jwt), null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("认证成功: userId={}, tenantId={}, role={}",
                    jwtUtil.getUserId(jwt), tenantId, role);
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清除上下文，防止内存泄漏
            TenantContextHolder.clear();
            UserContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 从请求头提取 Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 判断是否为白名单路径
     */
    private boolean isWhiteListed(String path) {
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                return true;
            }
        }
        return false;
    }
}
