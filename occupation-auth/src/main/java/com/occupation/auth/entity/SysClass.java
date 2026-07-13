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
 * 班级实体 — 学院内组织结构：专业-入学年级-班级
 * <p>
 * 不继承 {@link com.occupation.common.entity.BaseEntity}：sys_class 无 deleted 列，
 * 用 status 做启用/停用。含 tenant_id，多租户插件自动隔离（不在 ignoreTable）。
 *
 * @author occupation-team
 */
@Data
@TableName("sys_class")
public class SysClass implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户（学院）ID，插入时由多租户插件自动填充 */
    private Long tenantId;

    /** 专业 */
    private String major;

    /** 入学年级（如 2022） */
    private Integer enrollYear;

    /** 班级名（如 "1班"） */
    private String className;

    /** 统一命名：专业-入学年级-班级（如 软件工程-2022-1班），租户内唯一 */
    private String code;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
