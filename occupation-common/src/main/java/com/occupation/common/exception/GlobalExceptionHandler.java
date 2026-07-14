package com.occupation.common.exception;

import com.occupation.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;

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
     * 参数校验失败（@Valid / @Validated）
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
     * JSON 解析失败 / 请求体不可读
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleJsonParse(HttpMessageNotReadableException e) {
        log.warn("JSON 解析失败: {}", e.getMessage());
        String hint = e.getMessage() != null && e.getMessage().contains("out of START_ARRAY")
                ? "请求体格式错误：期望 JSON 对象，实际收到 JSON 数组"
                : "请求体格式错误：请确保 Content-Type=application/json 且 JSON 格式正确";
        return Result.error(400, hint);
    }

    /**
     * HTTP 方法用错（如对只支持 GET 的地址发 POST）
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
     * 非法参数（Service 层抛出的参数校验异常）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 非法状态（如数据不存在、前置条件未满足）
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException e) {
        log.warn("状态异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 权限拒绝（@PreAuthorize 校验失败）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限拒绝: {}", e.getMessage());
        return Result.error(403, "无权访问：当前角色没有操作权限");
    }

    /**
     * 数据库访问异常（表不存在、连接失败等）
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccess(DataAccessException e) {
        log.error("数据库访问异常: {}", e.getMessage(), e);
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("doesn't exist") || msg.contains("Table") && msg.contains("exist")) {
                return Result.error(503, "数据库表未初始化，请执行 init.sql 后重试");
            }
            if (msg.contains("Connection") || msg.contains("Communications")) {
                return Result.error(503, "数据库连接失败，请检查 MySQL 是否已启动");
            }
        }
        return Result.error(503, "数据库访问异常，请稍后重试");
    }

    /**
     * SQL 异常（兜底）
     */
    @ExceptionHandler(SQLException.class)
    public Result<Void> handleSQL(SQLException e) {
        log.error("SQL 异常: {}", e.getMessage(), e);
        return Result.error(503, "数据库异常：" + e.getMessage());
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
