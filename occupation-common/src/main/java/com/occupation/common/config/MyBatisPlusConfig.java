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
         * <p>
         * 返回 null 时 MyBatis-Plus 不会注入租户条件（TenantLineInnerInterceptor
         * 内部对 null 做了短路：见 3.5.5 源码 TenantLineInnerInterceptor#buildTableExpression）。
         * 这让白名单路径（如 /api/news 匿名访问）能自然工作，而
         * {@link #ignoreTable(String)} 负责声明哪些表永远不做隔离。
         * <p>
         * 历史注释里说"返回 null 会拼 AND tenant_id = null"——那是早期版本行为，
         * 实际 3.5.5 已修复为直接跳过。
         */
        @Override
        public Expression getTenantId() {
            Long tenantId = TenantContextHolder.getTenantId();
            return tenantId == null ? null : new LongValue(tenantId);
        }

        /**
         * 跳过不需要多租户隔离的表
         */
        @Override
        public boolean ignoreTable(String tableName) {
            // 以下表不含 tenant_id 字段或为全平台共享内容，跳过隔离：
            // - sys_tenant：租户表本身
            // - raw_job_data：原始数据，全平台共享
            // - job_detail：清洗后职位，全平台共享
            // - news：资讯为公共内容，所有租户共享（含 tenant_id 字段但应全局可见）
            //
            // api_client 含 tenant_id，但它是开放 API 的入口表：
            // 调用 /api/open/auth/token 时还没有任何租户上下文，必须按全局唯一的
            // api_key 查询。查到之后 ApiTokenInterceptor 才用它建立租户上下文，
            // 后续所有租户表的查询照常隔离。api_client 没有列表类接口，不存在越权读取。
            return "sys_tenant".equalsIgnoreCase(tableName)
                || "raw_job_data".equalsIgnoreCase(tableName)
                || "job_detail".equalsIgnoreCase(tableName)
                || "news".equalsIgnoreCase(tableName)
                || "api_client".equalsIgnoreCase(tableName);
        }
    }

}
