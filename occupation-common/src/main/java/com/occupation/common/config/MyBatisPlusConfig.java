package com.occupation.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 * <p>
 * 包含：分页插件 + 多租户行级隔离插件。
 *
 * @author occupation-team
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 多租户插件 — 自动在 SQL 中注入 tenant_id 条件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler()));

        // 2. 分页插件 — 支持 MySQL 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    /**
     * 租户行级隔离处理器
     */
    static class TenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

        /** 多租户字段名 */
        @Override
        public String getTenantIdColumn() {
            return "tenant_id";
        }

        /**
         * 获取当前租户 ID
         * 返回 null 时不追加 tenant_id 条件（用于跨租户查询或未认证请求）
         */
        @Override
        public Expression getTenantId() {
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                return null;
            }
            return new LongValue(tenantId);
        }

        /**
         * 跳过不需要多租户隔离的表
         */
        @Override
        public boolean ignoreTable(String tableName) {
            // 以下表不含 tenant_id 字段，跳过隔离：
            // - sys_tenant：租户表本身
            // - raw_job_data：原始数据，全平台共享
            // - job_detail：清洗后职位，全平台共享
            return "sys_tenant".equalsIgnoreCase(tableName)
                || "raw_job_data".equalsIgnoreCase(tableName)
                || "job_detail".equalsIgnoreCase(tableName);
        }
    }

}
