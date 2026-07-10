package com.occupation.recommend.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 顾问回复
 * <p>
 * {@code aiGenerated=false} 表示大模型不可用、这是规则化兜底文字，
 * 前端据此提示用户，不要把模板文案冒充成 AI 输出。
 *
 * @author occupation-team
 */
@Data
public class AdvisorReplyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reply;

    private boolean aiGenerated;

    public static AdvisorReplyVO of(String reply, boolean aiGenerated) {
        AdvisorReplyVO vo = new AdvisorReplyVO();
        vo.reply = reply;
        vo.aiGenerated = aiGenerated;
        return vo;
    }
}
