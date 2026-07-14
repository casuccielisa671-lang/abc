package com.occupation.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 * <p>
 * 禁用默认的 Session + Form Login，改用自定义 JWT 无状态认证。
 *
 * @author occupation-team
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（API 无状态，不需要）
            .csrf().disable()
            // 禁用 Session（JWT 无状态）
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 权限配置
            .authorizeRequests()
            // 登录入口 + 登录页的学校/企业下拉（只暴露启用中的租户名称）
            .antMatchers("/api/auth/login", "/api/auth/tenants", "/api/health/**").permitAll()
            // 对外开放 API：由 occupation-api 模块的 ApiTokenInterceptor 独立鉴权（apiKey 换 Token）
            .antMatchers("/api/open/**").permitAll()
            // 新闻资讯：公开内容，无需登录即可浏览
            .antMatchers("/api/news/**").permitAll()
            // Knife4j / OpenAPI 接口文档
            .antMatchers("/doc.html", "/v3/api-docs/**", "/webjars/**", "/favicon.ico").permitAll()
            .anyRequest().authenticated()
            .and()
            // 注册 JWT 过滤器（在 Spring Security 用户名密码过滤器之前）
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 禁用 JwtAuthenticationFilter 在 Servlet 容器层面的自动注册，
     * 仅通过 Spring Security 过滤器链管理。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
