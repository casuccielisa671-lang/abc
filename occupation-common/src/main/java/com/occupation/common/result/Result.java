package com.occupation.common.result;

import lombok.Data;

/**
 * 统一响应类
 *
 * @param <T> 响应数据类型
 * @author occupation-team
 */
@Data
public class Result<T> {

    /** 状态码 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    private Result() {}

    // ========== 成功响应 ==========

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    // ========== 失败响应 ==========

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = null;
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
