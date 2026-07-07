package com.occupation.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生行为记录实体 — 映射 student_behavior 表
 * <p>
 * 注意：此表无 deleted 字段，不继承 BaseEntity。
 *
 * @author occupation-team
 */
@Data
@TableName("student_behavior")
public class StudentBehavior implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 学生用户 ID */
    private Long userId;

    /** 职位 ID */
    private Long jobId;

    /** 行为类型：VIEW / FAVORITE / APPLY / IGNORE */
    private String action;

    /** 创建时间 */
    private LocalDateTime createTime;
}
