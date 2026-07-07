package com.occupation.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 开放 API 拦截器注册
 * <p>
 * 顺序：Token 校验 → 限流（限流依赖 Token 校验放入的 apiKey）。
 * /api/open/auth/token 是获取令牌的入口，不拦截。
 *
 * @author occupation-team
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiWebConfig implements WebMvcConfigurer {

    private final ApiTokenInterceptor apiTokenInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiTokenInterceptor)
                .addPathPatterns("/api/open/**")
                .excludePathPatterns("/api/open/auth/token")
                .order(1);

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/open/**")
                .excludePathPatterns("/api/open/auth/token")
                .order(2);
    }
}
