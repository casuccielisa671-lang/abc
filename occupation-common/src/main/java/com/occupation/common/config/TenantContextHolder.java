package com.occupation.common.config;

/**
 * 租户上下文持有者 — 基于 ThreadLocal 的租户 ID 传递工具
 * <p>
 * 在请求进入时由 JwtAuthenticationFilter 设置，
 * 在请求结束时自动清理（通过拦截器或过滤器的 finally 块）。
 *
 * @author occupation-team
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的租户 ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    /**
     * 获取当前线程的租户 ID
     *
     * @return 租户 ID，可能为 null（未认证或跨租户查询）
     */
    public static Long getTenantId() {
        return TENANT_HOLDER.get();
    }

    /**
     * 清除当前线程的租户 ID（防止内存泄漏，必须在请求结束时调用）
     */
    public static void clear() {
        TENANT_HOLDER.remove();
    }
}
