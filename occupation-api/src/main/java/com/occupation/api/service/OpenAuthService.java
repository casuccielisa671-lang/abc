package com.occupation.api.service;

import com.occupation.api.vo.TokenVO;

/**
 * 开放 API 鉴权服务
 * <p>
 * 鉴权模型（轻量版 client_credentials）：
 * <pre>
 * 第三方系统 → POST /api/open/auth/token (apiKey + apiSecret)
 *           ← accessToken（UUID，Redis 存储，TTL = app.open-api.token-ttl）
 * 后续请求  → Authorization: Bearer {accessToken}
 *           → ApiTokenInterceptor 校验 Redis 中是否存在
 * </pre>
 *
 * @author occupation-team
 */
public interface OpenAuthService {

    /**
     * 校验 apiKey + apiSecret，签发访问令牌
     *
     * @throws com.occupation.common.exception.BizException 凭证无效或客户端被禁用
     */
    TokenVO issueToken(String apiKey, String apiSecret);

    /**
     * 校验访问令牌
     *
     * @return 令牌对应的 apiKey；无效返回 null
     */
    String validateToken(String accessToken);

    /**
     * 查询 apiKey 所属的租户 ID；apiKey 不存在时返回 null。
     * <p>
     * 开放 API 无 JWT，多租户插件拿不到租户上下文就<b>不会</b>注入 tenant_id 条件，
     * 涉及租户表（如 report_record）的查询会跨租户返回数据。
     * 因此鉴权通过后必须用它把租户上下文设起来。
     */
    Long tenantIdOf(String apiKey);
}
