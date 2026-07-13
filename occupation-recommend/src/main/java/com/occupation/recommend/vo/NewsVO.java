package com.occupation.recommend.vo;

import com.occupation.recommend.entity.News;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资讯列表视图 — 不含正文，供首页资讯格子与资讯列表页使用。
 *
 * @author occupation-team
 */
@Data
public class NewsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String category;
    private String type;
    private String title;
    private String summary;
    private String coverStyle;
    private String source;
    /** 外部原文链接（EXTERNAL） */
    private String sourceUrl;
    /** 站内跳转（DATA_CAST） */
    private String linkTarget;
    private Integer viewCount;
    private Integer featured;
    private LocalDateTime publishTime;

    public static NewsVO of(News n) {
        NewsVO vo = new NewsVO();
        vo.id = n.getId();
        vo.category = n.getCategory();
        vo.type = n.getType();
        vo.title = n.getTitle();
        vo.summary = n.getSummary();
        vo.coverStyle = n.getCoverStyle();
        vo.source = n.getSource();
        vo.sourceUrl = n.getSourceUrl();
        vo.linkTarget = n.getLinkTarget();
        vo.viewCount = n.getViewCount();
        vo.featured = n.getFeatured();
        vo.publishTime = n.getPublishTime();
        return vo;
    }
}
