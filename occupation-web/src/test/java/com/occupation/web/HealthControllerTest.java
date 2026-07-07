package com.occupation.web;

import com.occupation.common.exception.GlobalExceptionHandler;
import com.occupation.common.mapper.SysTenantMapper;
import com.occupation.web.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step 1.2 验证：
 * - GET /api/health → {"code":200,"message":"success","data":"ok"}
 * - 抛出 BizException → 统一错误格式
 * <p>
 * 说明：内置 SliceConfig 替代主 Application 作为切片测试配置，
 * 避免 Application 上的 @MapperScan 把全部 Mapper 拉进 Web 切片
 * （切片中没有 SqlSessionFactory，会导致上下文加载失败）。
 */
@WebMvcTest(HealthController.class)
@Import({HealthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    /** 切片测试专用最小配置（不含 @MapperScan / 业务包扫描） */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class SliceConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    /** HealthController 构造器依赖 SysTenantMapper，切片中 Mock 掉 */
    @MockBean
    private SysTenantMapper sysTenantMapper;

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
