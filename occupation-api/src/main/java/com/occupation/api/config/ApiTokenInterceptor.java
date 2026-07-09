package com.occupation.api.config;

import com.occupation.api.service.OpenAuthService;
import com.occupation.common.config.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 开放 API Token 校验拦截器
 * <p>
 * 拦截 /api/open/**（除 /api/open/auth/token），校验 Bearer Token 是否有效，
 * 校验通过后将 apiKey 存入 request attribute 供限流拦截器使用。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTokenInterceptor implements HandlerInterceptor {

    private final OpenAuthService openAuthService;

    /** 校验通过后 apiKey 存放的 attribute 名 */
    public static final String ATTR_API_KEY = "openApiKey";

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        }

        String apiKey = openAuthService.validateToken(token);
        if (apiKey == null) {
            log.warn("开放 API 拒绝访问（Token 无效）: {}", request.getRequestURI());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"accessToken 无效或已过期，请先调用 /api/open/auth/token\",\"data\":null}");
            return false;
        }

        request.setAttribute(ATTR_API_KEY, apiKey);

        // 建立租户上下文：开放 API 没有 JWT，不设置的话多租户插件不会注入 tenant_id 条件，
        // 涉及租户表的查询（如报告摘要）会把别的学校的数据也返回给调用方。
        Long tenantId = openAuthService.tenantIdOf(apiKey);
        if (tenantId == null) {
            log.warn("开放 API 拒绝访问（apiKey 无对应客户端）: {}", apiKey);
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"客户端不存在\",\"data\":null}");
            return false;
        }
        TenantContextHolder.setTenantId(tenantId);
        return true;
    }

    /** 请求结束务必清理，否则 Tomcat 线程复用会把租户串到下一个请求上 */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}
