package com.occupation.common.config;

/**
 * 用户上下文持有者 — 基于 ThreadLocal 的当前登录用户传递工具
 * <p>
 * 与 {@link TenantContextHolder} 配套：请求进入时由 JwtAuthenticationFilter
 * 从 JWT 中解析 userId / role 并设置，请求结束时清理。
 * 业务层（如推荐模块记录学生行为）通过它获取"当前是谁在操作"。
 *
 * @author occupation-team
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的用户 ID 与角色
     */
    public static void set(Long userId, String role) {
        USER_ID_HOLDER.set(userId);
        ROLE_HOLDER.set(role);
    }

    /**
     * 获取当前登录用户 ID（未认证请求返回 null）
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取当前登录用户角色：STUDENT / TEACHER / ADMIN / HR（未认证返回 null）
     */
    public static String getRole() {
        return ROLE_HOLDER.get();
    }

    /**
     * 清除当前线程的用户上下文（必须在请求结束时调用，防止线程池复用导致数据串号）
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}
