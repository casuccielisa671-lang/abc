package com.occupation.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知记录 — 映射 push_record 表
 * <p>
 * 全平台唯一的站内信实体，落在 common 供任意模块发送（推荐、投递状态、报告下发…）。
 * 注意：此表无 update_time 和 deleted 字段，不继承 {@link BaseEntity}。
 * 多租户 tenant_id 由 MyBatis-Plus 租户插件在 INSERT 时自动注入、查询时自动隔离。
 *
 * @author occupation-team
 */
@Data
@TableName("push_record")
public class PushRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID（租户插件自动填充） */
    private Long tenantId;

    /** 目标用户 ID */
    private Long userId;

    /** 推送类型：RECOMMEND / SYSTEM / INTERVIEW / OFFER / REJECT / REPORT */
    private String type;

    /** 推送标题 */
    private String title;

    /** 推送内容 */
    private String content;

    /** 关联对象类型：APPLICATION（投递）/ REPORT（报告）/ null（纯通知，点击不跳转） */
    private String refType;

    /** 关联对象 ID，前端据 refType + refId 决定点击跳转到投递详情 / 报告页 */
    private Long refId;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
