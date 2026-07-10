package com.occupation.common.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 一条对话消息（OpenAI 消息格式）
 *
 * @author occupation-team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** system / user / assistant */
    private String role;

    private String content;

    public static AiMessage system(String content) {
        return new AiMessage("system", content);
    }

    public static AiMessage user(String content) {
        return new AiMessage("user", content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content);
    }
}
