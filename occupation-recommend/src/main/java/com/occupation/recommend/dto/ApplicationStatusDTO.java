package com.occupation.recommend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * HR 变更投递状态的入参
 * <p>
 * 当 {@code status=INTERVIEW} 时，面试字段（时间/地点/联系人/内容）会随通知模板发给学生；
 * Service 层强制要求填写面试时间与地点。其余状态忽略这些字段。
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

    /** 面试时间（status=INTERVIEW 时必填） */
    private LocalDateTime interviewTime;

    /** 面试地点/方式：线下地址或线上会议链接（status=INTERVIEW 时必填） */
    @Size(max = 300, message = "面试地点不能超过 300 字")
    private String interviewPlace;

    /** 面试官/联系人 */
    @Size(max = 100, message = "联系人不能超过 100 字")
    private String interviewContact;

    /** 面试内容/环节说明 */
    @Size(max = 500, message = "面试内容不能超过 500 字")
    private String interviewContent;
}
