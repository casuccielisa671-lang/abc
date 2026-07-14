package com.occupation.recommend.service;

import com.occupation.recommend.vo.TenantHealthVO;

/**
 * 管理员端工具箱服务 — 数据导出 / 租户健康度
 *
 * @author occupation-team
 */
public interface AdminToolService {

    /**
     * 租户健康度监控：各租户活跃度、数据完整度、API 调用量
     */
    TenantHealthVO getTenantHealth();
}
