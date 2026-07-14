package com.occupation.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.result.PageResult;
import com.occupation.common.result.Result;
import com.occupation.recommend.entity.News;
import com.occupation.recommend.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 资讯管理接口（管理后台）
 * <p>
 * 仅 ADMIN 可访问。管理精选文章（CRUD）、审核上下架、触发数据播报生成与外部资讯拉取。
 * 数据自动限定在当前租户内（多租户插件）。
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsController {

    private final NewsService newsService;

    /** 全部资讯分页（含下架；按 方向/类型/状态 筛选） */
    @GetMapping
    public Result<PageResult<News>> page(@RequestParam(required = false) String category,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        Page<News> p = newsService.pageAll(category, type, status, pageNum, pageSize);
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 新增/编辑资讯（id 为空=新增） */
    @PostMapping
    public Result<News> save(@RequestBody News news) {
        // 租户由多租户插件在插入时填充，不信任客户端传入
        news.setTenantId(null);
        return Result.ok(newsService.saveNews(news));
    }

    /** 删除资讯 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        newsService.deleteNews(id);
        return Result.ok();
    }

    /** 上架/下架 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        newsService.updateStatus(id, status);
        return Result.ok();
    }

    /** 从站内分析结果重新生成数据播报 */
    @PostMapping("/generate-datacast")
    public Result<Integer> generateDataCast() {
        return Result.ok(newsService.generateDataCast());
    }

    /** 从默认 RSS 源拉取外部资讯（best-effort，源站暂不可访问时返回 0） */
    @PostMapping("/pull-rss")
    public Result<Integer> pullRss(@RequestParam(defaultValue = "IT就业") String query,
                                   @RequestParam(defaultValue = "8") int maxItems) {
        return Result.ok(newsService.pullExternalNews(query, maxItems));
    }
}
