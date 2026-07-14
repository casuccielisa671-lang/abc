package com.occupation.recommend.controller;

import com.occupation.common.ai.AiMessage;
import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.dto.AdvisorChatDTO;
import com.occupation.recommend.service.CareerAdvisorService;
import com.occupation.recommend.vo.AdvisorReplyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 职业顾问接口（学生端）
 * <p>
 * 服务端无状态：不存会话，前端每次把完整对话历史传上来。
 * 好处是水平扩展无需粘性会话；代价是长对话的请求体会变大 —— 因此服务端只取最近若干轮。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/student/advisor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class AdvisorController {

    private final CareerAdvisorService advisorService;

    /** 与 AI 职业顾问对话一轮 */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/chat")
    public Result<AdvisorReplyVO> chat(@RequestBody @Validated AdvisorChatDTO dto) {
        List<AiMessage> history = dto.getMessages().stream()
                .map(m -> new AiMessage(m.getRole(), m.getContent()))
                .collect(Collectors.toList());
        return Result.ok(advisorService.chat(UserContextHolder.getUserId(), history));
    }

    /**
     * 用自然语言解读「为什么给我推荐这个职位」。
     * <p>
     * 按需单条生成：推荐列表一次 20 条，逐条调大模型既慢又贵。
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/explain/{jobId}")
    public Result<AdvisorReplyVO> explain(@PathVariable Long jobId) {
        return Result.ok(advisorService.explainMatch(UserContextHolder.getUserId(), jobId));
    }
}
