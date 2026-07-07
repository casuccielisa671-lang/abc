package com.occupation.api.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 开放 API Token 出参
 *
 * @author occupation-team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌（后续请求放在 Authorization: Bearer 头） */
    private String accessToken;

    /** 有效期（秒） */
    private long expiresIn;

    /** 授权范围 */
    private String scopes;
}
