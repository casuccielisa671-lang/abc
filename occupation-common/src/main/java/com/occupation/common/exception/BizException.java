package com.occupation.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * @author occupation-team
 */
@Getter
public class BizException extends RuntimeException {

    /** 错误码 */
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(500, message);
    }
}
