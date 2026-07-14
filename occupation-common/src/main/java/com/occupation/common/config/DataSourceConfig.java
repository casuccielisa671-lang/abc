package com.occupation.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Druid 数据源配置（显式声明，便于后续扩展监控/防火墙）
 *
 * @author occupation-team
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.druid.initial-size:5}")
    private int initialSize;

    @Value("${spring.datasource.druid.min-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.druid.max-active:20}")
    private int maxActive;

    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setInitialSize(initialSize);
        ds.setMinIdle(minIdle);
        ds.setMaxActive(maxActive);

        // 连接验证与保活：防止 MySQL 8 小时超时断开后 Druid 拿到已关闭的连接
        ds.setValidationQuery("SELECT 1");
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(false);

        // 驱逐检查间隔（毫秒），必须小于 keepAliveBetweenTimeMillis
        ds.setTimeBetweenEvictionRunsMillis(30000);

        // 保活检测间隔（毫秒），必须大于 timeBetweenEvictionRunsMillis 且小于 MySQL wait_timeout（28800秒）
        ds.setKeepAlive(true);
        ds.setKeepAliveBetweenTimeMillis(120000);

        // 连接泄漏检测
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(1800);
        ds.setLogAbandoned(true);

        // 连接获取失败时不关闭整个数据源，持续重试（避免因 MySQL 短暂不可用导致整个应用不可用）
        ds.setBreakAfterAcquireFailure(false);
        ds.setConnectionErrorRetryAttempts(3);
        ds.setTimeBetweenConnectErrorMillis(3000);
        // 获取连接超时：等待 30 秒，给 MySQL 恢复留足时间
        ds.setMaxWait(30000);

        return ds;
    }
}
