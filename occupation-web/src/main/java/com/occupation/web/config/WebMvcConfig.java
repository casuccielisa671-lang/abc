package com.occupation.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — 静态资源映射（证件照等上传文件访问）
 *
 * @author occupation-team
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.avatar.storage-path:./data/avatars}")
    private String avatarStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 证件照访问映射：/api/avatars/** → 本地存储目录
        String location = "file:" + avatarStoragePath.replace("\\", "/");
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/api/avatars/**")
                .addResourceLocations(location);
    }
}
