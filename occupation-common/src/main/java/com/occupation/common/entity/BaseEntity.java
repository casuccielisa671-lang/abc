package com.occupation.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类 — 所有数据库实体必须继承此类
 * <p>
 * 提供统一的主键策略、多租户隔离字段、时间戳。
 * MyBatis-Plus 自动填充 createTime / updateTime。
 *
 * @author occupation-team
 */
@Data
public abstract class BaseEntity implements Serializable {

    /** 主键（雪花算法自动生成） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID（多租户行级隔离） */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0=正常 1=已删除） */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
