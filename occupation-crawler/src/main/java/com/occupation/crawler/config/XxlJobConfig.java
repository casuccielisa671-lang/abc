package com.occupation.crawler.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 调度器配置（可选组件，默认关闭）
 * <p>
 * 连接 XXL-Job Admin 管理端，注册为执行器。
 * 需要单独部署 XXL-Job Admin 服务（默认端口 8081）。
 * <p>
 * 实训环境默认使用 Spring @Scheduled 做定时调度（零部署成本），
 * 如需演示分布式调度，在 application.yml 中设置 {@code xxl.job.enabled: true}
 * 并部署 XXL-Job Admin 后启用。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses:http://localhost:8081/xxl-job-admin}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname:occupation-crawler-executor}")
    private String appname;

    @Value("${xxl.job.executor.port:9999}")
    private int port;

    @Value("${xxl.job.executor.logpath:./logs/xxl-job}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:7}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("XXL-Job 初始化 — admin={}, appname={}, port={}", adminAddresses, appname, port);

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }

}
