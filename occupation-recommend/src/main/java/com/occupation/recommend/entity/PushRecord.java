package com.occupation.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推送记录实体 — 映射 push_record 表
 * <p>
 * 注意：此表无 update_time 和 deleted 字段，不继承 BaseEntity。
 *
 * @author occupation-team
 */
@Data
@TableName("push_record")
public class PushRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 目标用户 ID */
    private Long userId;

    /** 推送类型：RECOMMEND / SYSTEM */
    private String type;

    /** 推送标题 */
    private String title;

    /** 推送内容 */
    private String content;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
