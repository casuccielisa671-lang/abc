package com.occupation.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.UUID;

/**
 * 开放 API 限流拦截器 — Redis ZSET 滑动窗口
 * <p>
 * 每次请求以 {@code (时间戳, 唯一成员)} 写入 ZSET，窗口外的成员被剔除后再判断集合大小。
 * 相比固定窗口计数（key 按分钟分桶），滑动窗口不存在「上一分钟末尾 + 下一分钟开头」
 * 各打满一次配额、瞬时通过 2 倍流量的边界问题。
 * <p>
 * 剔除 → 计数 → 写入 三步必须原子完成，否则并发下会超发，因此放在 Lua 脚本里由 Redis 单线程执行。
 *
 * @author occupation-team
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.open-api.rate-limit-per-minute:60}")
    private long limitPerMinute;

    /** 滑动窗口长度：1 分钟 */
    private static final long WINDOW_MILLIS = 60_000L;

    private static final String KEY_PREFIX = "open-api:rate:";

    /**
     * KEYS[1]=限流 key；ARGV: 1=当前毫秒 2=窗口毫秒 3=配额 4=本次请求的唯一成员。
     * 返回 1 放行、0 拒绝。
     */
    private static final RedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1]\n"
            + "local now = tonumber(ARGV[1])\n"
            + "local window = tonumber(ARGV[2])\n"
            + "local limit = tonumber(ARGV[3])\n"
            + "local member = ARGV[4]\n"
            + "redis.call('ZREMRANGEBYSCORE', key, 0, now - window)\n"
            + "if redis.call('ZCARD', key) < limit then\n"
            + "  redis.call('ZADD', key, now, member)\n"
            + "  redis.call('PEXPIRE', key, window)\n"
            + "  return 1\n"
            + "end\n"
            + "return 0", Long.class);

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String apiKey = (String) request.getAttribute(ApiTokenInterceptor.ATTR_API_KEY);
        if (apiKey == null) {
            // Token 拦截器未放行的请求不会到这里；防御性放行
            return true;
        }

        long now = System.currentTimeMillis();
        // 成员必须唯一：同一毫秒内的两次请求若共用成员，ZADD 会覆盖而非新增，导致少计一次
        String member = now + "-" + UUID.randomUUID();

        Long allowed = redisTemplate.execute(SLIDING_WINDOW_SCRIPT,
                Collections.singletonList(KEY_PREFIX + apiKey),
                String.valueOf(now), String.valueOf(WINDOW_MILLIS),
                String.valueOf(limitPerMinute), member);

        if (allowed != null && allowed == 1L) {
            return true;
        }

        log.warn("开放 API 触发限流: apiKey={}", apiKey);
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，每分钟最多 "
                + limitPerMinute + " 次\",\"data\":null}");
        return false;
    }
}
