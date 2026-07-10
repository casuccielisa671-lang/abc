package com.occupation.recommend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * HR 变更投递状态的入参
 *
 * @author occupation-team
 */
@Data
public class ApplicationStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标状态。用 @Pattern 挡掉拼写错误，Service 里还会再校验流转是否合法。
     * SUBMITTED 不在允许列表里 —— 那是学生投递时的初始状态，HR 无法把记录改回「未处理」。
     */
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "VIEWED|INTERVIEW|OFFER|REJECTED",
             message = "状态只能是 VIEWED / INTERVIEW / OFFER / REJECTED")
    private String status;

    @Size(max = 500, message = "备注不能超过 500 字")
    private String hrNote;
}
