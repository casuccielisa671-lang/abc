package com.occupation.api.controller;

import com.occupation.api.dto.TokenRequestDTO;
import com.occupation.api.service.OpenAuthService;
import com.occupation.api.vo.TokenVO;
import com.occupation.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放 API 鉴权入口
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/open/auth")
@RequiredArgsConstructor
public class OpenAuthController {

    private final OpenAuthService openAuthService;

    /**
     * apiKey + apiSecret 换取访问令牌
     * <pre>
     * POST /api/open/auth/token
     * { "apiKey": "xxx", "apiSecret": "xxx" }
     * → { "code":200, "data": { "accessToken":"...", "expiresIn":7200, "scopes":"jobs:read,..." } }
     * </pre>
     */
    @PostMapping("/token")
    public Result<TokenVO> issueToken(@RequestBody @Validated TokenRequestDTO dto) {
        return Result.ok(openAuthService.issueToken(dto.getApiKey(), dto.getApiSecret()));
    }
}
