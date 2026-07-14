package com.occupation.recommend.dto;

import com.occupation.common.ai.AiMessage;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 多轮润色聊天入参 — 前端把完整对话历史传过来，后端追加 system prompt 后发给大模型。
 *
 * @author occupation-team
 */
@Data
public class ResumePolishChatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 所属板块，如「自我评价」「项目经历」 */
    @Size(max = 20)
    private String section;

    /** 原文（首次润色时必传，后续轮次可为空） */
    @Size(max = 2000)
    private String originalText;

    /** 对话历史（不含 system prompt），前端维护 */
    @NotEmpty(message = "对话消息不能为空")
    private List<AiMessage> messages;

    /** 用户刚输入的新消息（本轮指令，如"再精简一点"） */
    @NotBlank(message = "请输入润色要求")
    @Size(max = 500)
    private String userMessage;
}
