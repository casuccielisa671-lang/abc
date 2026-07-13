package com.occupation.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教师可见范围 — 一个教师可有多行
 * <p>
 * scope_type：CLASS=班主任（scope_value=班级id）/ MAJOR=专业老师（scope_value=专业名）
 * / GRADE=届老师（scope_value=入学年级）。含 tenant_id，多租户插件自动隔离。
 *
 * @author occupation-team
 */
@Data
@TableName("teacher_scope")
public class TeacherScope implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 范围类型常量 */
    public static final String CLASS = "CLASS";
    public static final String MAJOR = "MAJOR";
    public static final String GRADE = "GRADE";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID，插入时由多租户插件自动填充 */
    private Long tenantId;

    /** 教师用户ID（sys_user.id，role=TEACHER） */
    private Long teacherId;

    /** 范围类型：CLASS / MAJOR / GRADE */
    private String scopeType;

    /** 范围值：CLASS→班级id / MAJOR→专业名 / GRADE→入学年级 */
    private String scopeValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
