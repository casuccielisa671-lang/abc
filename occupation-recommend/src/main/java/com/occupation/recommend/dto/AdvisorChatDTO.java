package com.occupation.recommend.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 职业顾问对话入参
 * <p>
 * 前端把整段对话历史传回来（无状态服务端，不存会话）。
 * 服务端只接受 user / assistant 两种 role，并且会自己注入 system —— 见
 * {@code CareerAdvisorServiceImpl.sanitize}。
 *
 * @author occupation-team
 */
@Data
public class AdvisorChatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "对话内容不能为空")
    @Size(max = 40, message = "对话轮次过多，请开启新会话")
    private List<Message> messages;

    @Data
    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        /** user / assistant，其余取值会被服务端丢弃 */
        private String role;

        private String content;
    }
}
