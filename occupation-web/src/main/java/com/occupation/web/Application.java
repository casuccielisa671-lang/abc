package com.occupation.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 职业能力大数据服务平台 — 启动入口
 *
 * @author occupation-team
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.occupation")
@MapperScan({
    "com.occupation.common.mapper",
    "com.occupation.web.mapper",
    "com.occupation.auth.mapper",
    "com.occupation.crawler.mapper",
    "com.occupation.analysis.mapper",
    "com.occupation.report.mapper",
    "com.occupation.recommend.mapper",
    "com.occupation.api.mapper"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
