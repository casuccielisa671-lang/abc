package com.occupation.web;

import com.occupation.common.exception.BizException;
import com.occupation.common.exception.GlobalExceptionHandler;
import com.occupation.common.result.Result;
import com.occupation.web.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step 1.2 验证：
 * - GET /api/health → {"code":200,"message":"success","data":"ok"}
 * - 抛出 BizException → 统一错误格式
 */
@WebMvcTest(HealthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void bizExceptionShouldReturnErrorFormat() throws Exception {
        mockMvc.perform(get("/api/health/error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("测试业务异常——全局异常处理器生效"));
    }
}
