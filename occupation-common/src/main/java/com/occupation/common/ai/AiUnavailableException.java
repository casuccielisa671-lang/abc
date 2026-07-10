package com.occupation.common.ai;

/**
 * 大模型不可用（未配置密钥、超时、限流、上游报错）。
 * <p>
 * 调用方<b>必须</b>捕获它并降级为规则化输出 —— AI 是增强能力，不能成为主流程的单点故障。
 *
 * @author occupation-team
 */
public class AiUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
