package com.occupation.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.recommend.entity.News;
import com.occupation.recommend.service.NewsService;
import com.occupation.recommend.vo.NewsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资讯接口 — 首页资讯板块与资讯页（任意已登录角色可读）。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    /** 资讯分页列表（按技术方向 / 类型筛选） */
    @GetMapping
    public Result<PageResult<NewsVO>> page(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<News> p = newsService.pageNews(category, type, pageNum, pageSize);
        List<NewsVO> list = p.getRecords().stream().map(NewsVO::of).collect(Collectors.toList());
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), list));
    }

    /** 首页资讯格子：最新若干条（置顶优先），可按技术方向筛选 */
    @GetMapping("/latest")
    public Result<List<NewsVO>> latest(@RequestParam(defaultValue = "6") int limit,
                                       @RequestParam(required = false) String category) {
        return Result.ok(newsService.latest(limit, category).stream().map(NewsVO::of).collect(Collectors.toList()));
    }

    /** 资讯详情（含正文，浏览数 +1） */
    @GetMapping("/{id}")
    public Result<News> detail(@PathVariable Long id) {
        return Result.ok(newsService.getDetail(id));
    }
}
