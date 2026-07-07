package com.occupation.api.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.api.entity.ApiClient;
import com.occupation.api.mapper.ApiClientMapper;
import com.occupation.api.service.OpenAuthService;
import com.occupation.api.vo.TokenVO;
import com.occupation.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 开放 API 鉴权实现 — api_client 表 + Redis Token
 * <p>
 * Token 为不透明令牌（UUID），存 Redis：key=open-api:token:{token}，value=apiKey。
 * 撤销即删 key，天然支持过期，无需维护 JWT 黑名单。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAuthServiceImpl implements OpenAuthService {

    private final ApiClientMapper apiClientMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.open-api.token-ttl:7200}")
    private long tokenTtl;

    private static final String TOKEN_KEY_PREFIX = "open-api:token:";

    @Override
    public TokenVO issueToken(String apiKey, String apiSecret) {
        // api_client 含 tenant_id，但开放 API 是平台级入口（无租户上下文），
        // 多租户插件此时不注入条件，按 apiKey 全局唯一查询
        ApiClient client = apiClientMapper.selectOne(
                new LambdaQueryWrapper<ApiClient>().eq(ApiClient::getApiKey, apiKey));

        // TODO(P5-B组): apiSecret 应加密存储（BCrypt），此处改为 passwordEncoder.matches 比对
        if (client == null || !client.getApiSecret().equals(apiSecret)) {
            throw new BizException(401, "apiKey 或 apiSecret 无效");
        }
        if (client.getStatus() == null || client.getStatus() != 1) {
            throw new BizException(403, "客户端已被禁用");
        }

        String token = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, apiKey, tokenTtl, TimeUnit.SECONDS);
        log.info("开放 API 签发 Token: client={}", client.getClientName());
        return new TokenVO(token, tokenTtl, client.getScopes());
    }

    @Override
    public String validateToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return null;
        }
        return redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + accessToken);
    }
}
