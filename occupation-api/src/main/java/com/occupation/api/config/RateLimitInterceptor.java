package com.occupation.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 开放 API 限流拦截器 — Redis 固定窗口计数
 * <p>
 * 按 apiKey + 分钟 计数：key=open-api:rate:{apiKey}:{yyyyMMddHHmm}，
 * 超过 app.open-api.rate-limit-per-minute 返回 429。
 * <p>
 * TODO(P5-B组·进阶): 固定窗口在窗口边界可能突发 2 倍流量，
 * 可升级为滑动窗口（Redis ZSET）或令牌桶（Lua 脚本）。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.open-api.rate-limit-per-minute:60}")
    private long limitPerMinute;

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String apiKey = (String) request.getAttribute(ApiTokenInterceptor.ATTR_API_KEY);
        if (apiKey == null) {
            // Token 拦截器未放行的请求不会到这里；防御性放行
            return true;
        }

        String key = "open-api:rate:" + apiKey + ":" + LocalDateTime.now().format(MINUTE_FMT);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 70, TimeUnit.SECONDS);
        }

        if (count != null && count > limitPerMinute) {
            log.warn("开放 API 触发限流: apiKey={}, count={}", apiKey, count);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，每分钟最多 "
                    + limitPerMinute + " 次\",\"data\":null}");
            return false;
        }
        return true;
    }
}
