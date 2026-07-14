package com.occupation.recommend.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * JD-简历语义匹配入参。
 *
 * @author occupation-team
 */
@Data
@Builder
public class SemanticMatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JD 描述文本（岗位标题 + 技能要求 + 岗位描述） */
    private String jdText;

    /** 简历文本（学生画像 + 简历内容） */
    private String resumeText;
}
