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
}
