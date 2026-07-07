package com.occupation.auth.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类 — Token 签发、校验、解析
 *
 * @author occupation-team
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param role     角色
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId, Long tenantId, String role) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);

        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("tenantId", tenantId)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 校验并解析 Token
     *
     * @param token JWT Token
     * @return 解析后的 DecodedJWT，校验失败返回 null
     */
    public DecodedJWT verifyAndParse(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取用户 ID
     */
    public Long getUserId(DecodedJWT jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    /**
     * 从 Token 中提取租户 ID
     */
    public Long getTenantId(DecodedJWT jwt) {
        return jwt.getClaim("tenantId").asLong();
    }

    /**
     * 从 Token 中提取角色
     */
    public String getRole(DecodedJWT jwt) {
        return jwt.getClaim("role").asString();
    }

    /**
     * 获取 Token 过期时间
     */
    public Date getExpiresAt(DecodedJWT jwt) {
        return jwt.getExpiresAt();
    }
}
