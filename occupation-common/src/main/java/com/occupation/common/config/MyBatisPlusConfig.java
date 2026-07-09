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
         * ⚠️ 返回 null <b>不会</b>让插件跳过条件注入 —— 它会照样拼出
         * {@code AND tenant_id = null}，而 SQL 里 {@code = null} 恒不成立，
         * 查询会静默返回 0 行。想跳过隔离只能靠 {@link #ignoreTable(String)}。
         * <p>
         * 因此这里返回 0 作为「不可能匹配的租户」而非 null：真要跨租户查询的表
         * 必须显式列进 ignoreTable，而不是依赖 null 的语义。
         */
        @Override
        public Expression getTenantId() {
            Long tenantId = TenantContextHolder.getTenantId();
            return new LongValue(tenantId == null ? 0L : tenantId);
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
            //
            // api_client 含 tenant_id，但它是开放 API 的入口表：
            // 调用 /api/open/auth/token 时还没有任何租户上下文，必须按全局唯一的
            // api_key 查询。查到之后 ApiTokenInterceptor 才用它建立租户上下文，
            // 后续所有租户表的查询照常隔离。api_client 没有列表类接口，不存在越权读取。
            return "sys_tenant".equalsIgnoreCase(tableName)
                || "raw_job_data".equalsIgnoreCase(tableName)
                || "job_detail".equalsIgnoreCase(tableName)
                || "api_client".equalsIgnoreCase(tableName);
        }
    }

}
