package com.occupation.recommend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.recommend.entity.News;

import java.util.List;

/**
 * 资讯服务 — 首页资讯板块与资讯页。
 *
 * @author occupation-team
 */
public interface NewsService {

    /**
     * 分页查询上架资讯（按 置顶 + 发布时间 倒序）。
     *
     * @param category 技术方向，null/空/all 表示不筛选
     * @param type     类型（DATA_CAST/ARTICLE/EXTERNAL），null/空表示不筛选
     */
    Page<News> pageNews(String category, String type, int pageNum, int pageSize);

    /** 首页资讯格子：最新上架资讯若干条（置顶优先），可按技术方向筛选 */
    List<News> latest(int limit, String category);

    /** 资讯详情：仅上架可见，浏览数 +1 */
    News getDetail(Long id);

    // ==================== 管理端 ====================

    /** 管理端分页（含下架；category/type/status 均可空筛选） */
    Page<News> pageAll(String category, String type, Integer status, int pageNum, int pageSize);

    /** 新增/编辑资讯（管理端录入精选文章等） */
    News saveNews(News news);

    /** 逻辑删除 */
    void deleteNews(Long id);

    /** 上架/下架（status：1=上架 0=下架） */
    void updateStatus(Long id, Integer status);

    /** 从站内分析结果重新生成数据播报（DATA_CAST），返回生成条数 */
    int generateDataCast();

    /**
     * 从默认 RSS 源拉取外部资讯（best-effort）。
     * 源站暂不可访问时静默返回 0，不抛异常。
     *
     * @param query    关键词（如「IT就业」「程序员招聘」）
     * @param maxItems 最多入库条数
     * @return 新增条数
     */
    int pullExternalNews(String query, int maxItems);
}
