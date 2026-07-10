package com.occupation.common.exception;

import com.occupation.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 *
 * @author occupation-team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * JSON 解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleJsonParse(HttpMessageNotReadableException e) {
        log.warn("JSON 解析失败: {}", e.getMessage());
        return Result.error(400, "请求体格式错误");
    }

    /**
     * HTTP 方法用错（如对只支持 GET 的地址发 POST）
     * <p>
     * 不加这个处理器的话会被下面的兜底吞成 500「系统内部错误」，
     * 调用方完全看不出是自己用错了方法。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return Result.error(405, "该地址不支持 " + e.getMethod() + " 方法");
    }

    /**
     * 路径变量/查询参数类型不匹配（如 /task/abc 而 id 是 Long）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return Result.error(400, "参数 " + e.getName() + " 的取值不合法");
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "系统内部错误，请稍后重试");
    }
}
