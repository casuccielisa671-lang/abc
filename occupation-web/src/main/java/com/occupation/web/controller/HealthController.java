package com.occupation.web.controller;

import com.occupation.common.exception.BizException;
import com.occupation.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查 & 框架验证接口
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("ok");
    }

    /**
     * 异常测试（验证全局异常处理器是否生效）
     */
    @GetMapping("/health/error")
    public Result<String> errorTest() {
        throw new BizException(400, "测试业务异常——全局异常处理器生效");
    }
}
