package com.occupation.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 开放 API Token 申请入参
 *
 * @author occupation-team
 */
@Data
public class TokenRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "apiKey 不能为空")
    private String apiKey;

    @NotBlank(message = "apiSecret 不能为空")
    private String apiSecret;
}
