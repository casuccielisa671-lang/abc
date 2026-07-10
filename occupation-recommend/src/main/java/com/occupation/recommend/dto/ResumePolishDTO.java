package com.occupation.recommend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 简历润色入参
 *
 * @author occupation-team
 */
@Data
public class ResumePolishDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 所属板块，如「自我评价」「项目经历」。作为改写风格的上下文 */
    @Size(max = 20)
    private String section;

    @NotBlank(message = "请先填写要润色的内容")
    @Size(max = 2000, message = "单次润色不能超过 2000 字")
    private String text;
}
