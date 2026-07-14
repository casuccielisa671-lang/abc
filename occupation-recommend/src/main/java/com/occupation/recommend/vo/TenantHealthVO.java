package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 租户健康度监控 VO
 *
 * @author occupation-team
 */
@Data
public class TenantHealthVO {

    /** 租户列表 */
    private List<TenantItem> tenants;

    /** 汇总统计 */
    private Summary summary;

    @Data
    public static class TenantItem {
        private Long id;
        private String name;
        /** 健康度 0-100 */
        private Integer health;
        private Integer activeUsers;
        private Integer totalUsers;
        /** 数据完整度 0-100 */
        private Integer dataCompleteness;
        private Integer apiCalls;
        /** normal / warning / error */
        private String status;
    }

    @Data
    public static class Summary {
        private int tenantCount;
        private int warningCount;
        private int avgHealth;
    }
}
