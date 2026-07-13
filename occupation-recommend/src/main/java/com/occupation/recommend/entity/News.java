package com.occupation.recommend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.occupation.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资讯实体 — 首页资讯板块
 * <p>
 * type：DATA_CAST=数据播报（平台数据自动生成，点击去对应图表）/ ARTICLE=精选文章（有正文）
 * / EXTERNAL=外部资讯（跳原文）。多租户自动隔离。
 *
 * @author occupation-team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("news")
public class News extends BaseEntity {

    /** 技术方向：backend/frontend/test/devops/bigdata；null=通用 */
    private String category;

    /** 类型：DATA_CAST / ARTICLE / EXTERNAL */
    private String type;

    private String title;
    private String summary;

    /** 正文（仅 ARTICLE） */
    private String content;

    /** 封面色块样式：blue/green/purple/amber */
    private String coverStyle;

    /** 来源：平台数据播报 / RSS源名 / 作者 */
    private String source;

    /** 外部原文链接（EXTERNAL 点击跳出） */
    private String sourceUrl;

    /** 站内跳转路由（DATA_CAST 点击去对应图表） */
    private String linkTarget;

    private Integer viewCount;

    /** 置顶/精选：1=是 0=否 */
    private Integer featured;

    /** 状态：1=上架 0=下架 */
    private Integer status;

    private LocalDateTime publishTime;
}
