package com.occupation.report.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 报告下发入参 —— 管理员把就业报告发给某范围内的学生。
 *
 * @author occupation-team
 */
@Data
public class DeliverReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 范围类型：ALL=全体 / MAJOR=专业 / GRADE=入学年级 / CLASS=班级 */
    @NotBlank(message = "请选择发送范围")
    private String targetType;

    /** 范围值：MAJOR=专业名、GRADE=年级数字、CLASS=班级 id；ALL 可为空 */
    private String targetValue;
}
