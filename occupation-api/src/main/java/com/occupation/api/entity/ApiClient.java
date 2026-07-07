package com.occupation.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 客户端实体 — 映射 api_client 表
 * <p>
 * 用于对外 API 的 OAuth2 客户端注册管理。
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_client")
public class ApiClient extends BaseEntity {

    /** 客户端名称 */
    private String clientName;

    /** API Key（客户端标识） */
    private String apiKey;

    /** API Secret（加密存储） */
    private String apiSecret;

    /** 授权范围（逗号分隔，如 "jobs:read,reports:read"） */
    private String scopes;

    /** 状态：1=启用 0=禁用 */
    private Integer status;
}
