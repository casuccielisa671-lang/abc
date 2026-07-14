package com.occupation.common.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型接入配置 — 对应 {@code app.ai.*}
 * <p>
 * 兼容任何 OpenAI 风格的 {@code /chat/completions} 接口（DeepSeek / 通义千问 / 智谱 / Moonshot）。
 * <b>apiKey 必须由环境变量或 gitignored 的 application-local.yml 注入，严禁写进仓库。</b>
 *
 * @author occupation-team
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 总开关。关闭时所有 AI 能力降级为规则化输出，不影响主流程 */
    private boolean enabled = false;

    /** 接口根地址，需含版本号，如 https://api.deepseek.com/v1 */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** 密钥。留空视同 enabled=false */
    private String apiKey = "";

    /** 模型名。deepseek-chat 为通用对话模型，deepseek-reasoner 为推理模型（更慢更贵） */
    private String model = "deepseek-chat";

    /** 采样温度。0 最确定，1 最发散。报告/诊断类走低温，对话类可高一些 */
    private double temperature = 0.3;

    /** 单次回复上限（token）。中文约 1 token ≈ 1.5 字。升级到 3000 以支持更长的分析输出 */
    private int maxTokens = 3000;

    /** 建立连接超时（毫秒） */
    private int connectTimeoutMs = 5_000;

    /**
     * 读取响应超时（毫秒）。
     * 大模型首字延迟常在数秒，长回复可达数十秒 —— 设短了会把正常请求误判为失败。
     */
    private int readTimeoutMs = 60_000;

    /** 失败重试次数（仅对超时与 5xx 重试，4xx 是入参错误，重试无意义） */
    private int maxRetries = 1;

    /** 密钥是否就绪 */
    public boolean usable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
}
