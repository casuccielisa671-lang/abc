package com.occupation.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户实体 — 多租户核心表
 * <p>
 * 注意：此表不继承 BaseEntity，因为 sys_tenant 表自身不含 tenant_id 字段。
 *
 * @author occupation-team
 */
@Data
@TableName("sys_tenant")
public class SysTenant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户名称 */
    private String name;

    /** 状态：1=启用 0=禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
