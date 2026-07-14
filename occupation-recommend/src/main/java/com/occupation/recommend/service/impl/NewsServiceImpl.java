package com.occupation.recommend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.analysis.dto.DashboardQueryDTO;
import com.occupation.analysis.service.AnalysisService;
import com.occupation.analysis.vo.DashboardVO;
import com.occupation.common.config.TenantContextHolder;
import com.occupation.common.exception.BizException;
import com.occupation.recommend.entity.News;
import com.occupation.recommend.mapper.NewsMapper;
import com.occupation.recommend.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 资讯服务实现（多租户自动隔离）。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private static final int STATUS_ON = 1;
    private static final String[] COVERS = {"blue", "green", "purple", "amber"};
    private static final String DEFAULT_RSS_URL = "https://www.oschina.net/news/rss";
    private static final String DEFAULT_RSS_SOURCE = "开源中国";

    private final NewsMapper newsMapper;
    private final AnalysisService analysisService;

    @Override
    public Page<News> pageNews(String category, String type, int pageNum, int pageSize) {
        return withoutTenant(() -> {
            ensureBaselineNews();
            Page<News> page = newsMapper.selectPage(new Page<>(pageNum, pageSize), listWrapper(category, type));
            if (page.getRecords().isEmpty() && hasCategoryFilter(category)) {
                return newsMapper.selectPage(new Page<>(pageNum, pageSize), listWrapper(null, type));
            }
            return page;
        });
    }

    @Override
    public List<News> latest(int limit, String category) {
        return withoutTenant(() -> {
            ensureBaselineNews();
            int max = Math.max(1, limit);
            List<News> rows = newsMapper.selectList(listWrapper(category, null).last("LIMIT " + max));
            if (rows.isEmpty() && hasCategoryFilter(category)) {
                return newsMapper.selectList(listWrapper(null, null).last("LIMIT " + max));
            }
            return rows;
        });
    }

    @Override
    public News getDetail(Long id) {
        return withoutTenant(() -> {
        News news = newsMapper.selectById(id);
        if (news == null || news.getStatus() == null || news.getStatus() != STATUS_ON) {
            throw new BizException("资讯不存在或已下架");
        }
        // 浏览数 +1（用 setSql 原子自增，避免并发覆盖）
        newsMapper.update(null, new LambdaUpdateWrapper<News>()
                .eq(News::getId, id)
                .setSql("view_count = view_count + 1"));
        news.setViewCount((news.getViewCount() == null ? 0 : news.getViewCount()) + 1);
        return news;
        });
    }

    /** 公共查询条件：仅上架，置顶优先、发布时间倒序；按方向/类型可选筛选 */
    private LambdaQueryWrapper<News> listWrapper(String category, String type) {
        LambdaQueryWrapper<News> w = new LambdaQueryWrapper<>();
        w.eq(News::getStatus, STATUS_ON);
        if (hasCategoryFilter(category)) {
            w.eq(News::getCategory, category);
        }
        if (StrUtil.isNotBlank(type)) {
            w.eq(News::getType, type);
        }
        w.orderByDesc(News::getFeatured).orderByDesc(News::getPublishTime);
        return w;
    }

    private boolean hasCategoryFilter(String category) {
        return StrUtil.isNotBlank(category) && !"all".equalsIgnoreCase(category);
    }

    // ==================== 管理端 ====================

    @Override
    public Page<News> pageAll(String category, String type, Integer status, int pageNum, int pageSize) {
        return withoutTenant(() -> {
        LambdaQueryWrapper<News> w = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(category) && !"all".equalsIgnoreCase(category)) {
            w.eq(News::getCategory, category);
        }
        if (StrUtil.isNotBlank(type)) {
            w.eq(News::getType, type);
        }
        if (status != null) {
            w.eq(News::getStatus, status);
        }
        w.orderByDesc(News::getFeatured).orderByDesc(News::getPublishTime);
        return newsMapper.selectPage(new Page<>(pageNum, pageSize), w);
        });
    }

    @Override
    public News saveNews(News news) {
        return withoutTenant(() -> {
        if (StrUtil.isBlank(news.getTitle())) {
            throw new BizException("标题不能为空");
        }
        if (news.getType() == null) {
            news.setType("ARTICLE");
        }
        if (news.getStatus() == null) {
            news.setStatus(STATUS_ON);
        }
        if (news.getCoverStyle() == null) {
            news.setCoverStyle("blue");
        }
        if (news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        if (news.getId() == null) {
            news.setViewCount(0);
            newsMapper.insert(news);
        } else {
            newsMapper.updateById(news);
        }
        return news;
        });
    }

    @Override
    public void deleteNews(Long id) {
        withoutTenant(() -> newsMapper.deleteById(id));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        withoutTenant(() -> {
        newsMapper.update(null, new LambdaUpdateWrapper<News>()
                .eq(News::getId, id)
                .set(News::getStatus, status));
        });
    }

    @Override
    public int generateDataCast() {
        DashboardQueryDTO q = new DashboardQueryDTO();
        q.setTenantId(TenantContextHolder.getTenantId());
        DashboardVO d = analysisService.getDashboard(q);

        // 先清掉旧的数据播报（逻辑删除），再生成一批新的
        withoutTenant(() -> newsMapper.delete(new LambdaQueryWrapper<News>().eq(News::getType, "DATA_CAST")));

        List<News> casts = new ArrayList<>();
        long totalJobs = sumCount(d.getCityDist());
        if (totalJobs > 0) {
            casts.add(cast("平台在库岗位达 " + totalJobs + " 个，数据分析已就绪",
                    "覆盖多座城市与技术方向，看板、推荐与报告均基于此。", null, "blue", "/admin/dashboard"));
        }
        DashboardVO.DimensionItem topSkill = first(d.getSkillHot());
        if (topSkill != null) {
            casts.add(cast(topSkill.getName() + " 稳居技能热度榜首",
                    "在全部岗位中，要求 " + topSkill.getName() + " 的职位数量最多，需求持续旺盛。",
                    null, "blue", "/admin/dashboard"));
        }
        DashboardVO.DimensionItem topCity = first(d.getCityDist());
        if (topCity != null) {
            casts.add(cast(topCity.getName() + " 岗位最集中",
                    "按城市分布，" + topCity.getName() + " 岗位数领先，是热门就业城市。",
                    null, "green", "/admin/dashboard"));
        }
        DashboardVO.DimensionItem topInd = first(d.getIndustryTop());
        if (topInd != null) {
            casts.add(cast(topInd.getName() + " 行业岗位需求最高",
                    "在各行业中，" + topInd.getName() + " 提供的岗位数量最多。",
                    null, "purple", "/admin/employment"));
        }
        withoutTenant(() -> {
            for (News n : casts) {
                newsMapper.insert(n);
            }
        });
        log.info("数据播报生成完成: {} 条", casts.size());
        return casts.size();
    }

    @Override
    public int pullExternalNews(String query, int maxItems) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(DEFAULT_RSS_URL).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (conn.getResponseCode() != 200) {
                return 0;
            }
            Document doc;
            try (InputStream in = conn.getInputStream()) {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            }
            NodeList items = doc.getElementsByTagName("item");
            int added = 0;
            added = withoutTenant(() -> importRssItems(items, query, maxItems, true));
            if (added == 0) {
                added = withoutTenant(() -> importRssItems(items, query, maxItems, false));
            }
            log.info("外部资讯拉取: source={}, query={}, 新增 {} 条", DEFAULT_RSS_SOURCE, query, added);
            return added;
        } catch (Exception e) {
            log.warn("拉取外部资讯失败（默认源 {} 暂不可访问）: {}", DEFAULT_RSS_SOURCE, e.getMessage());
            return 0;
        }
    }

    private int importRssItems(NodeList items, String query, int maxItems, boolean filterByQuery) {
        int added = 0;
        String q = StrUtil.blankToDefault(query, "").trim().toLowerCase();
        for (int i = 0; i < items.getLength() && added < maxItems; i++) {
            Element it = (Element) items.item(i);
            String title = text(it, "title");
            String link = text(it, "link");
            String desc = cleanSummary(text(it, "description"));
            if (StrUtil.isBlank(title)) {
                continue;
            }
            if (filterByQuery && StrUtil.isNotBlank(q)) {
                String haystack = (title + " " + desc).toLowerCase();
                if (!haystack.contains(q)) {
                    continue;
                }
            }
            Long exists = newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getTitle, title));
            if (exists != null && exists > 0) {
                continue;
            }
            News n = new News();
            n.setTenantId(1L);
            n.setType("EXTERNAL");
            n.setCategory(detectCategory(title + " " + desc));
            n.setTitle(title.length() > 300 ? title.substring(0, 300) : title);
            n.setSummary(StrUtil.blankToDefault(desc, "来自开源中国，点击「阅读原文」查看详情。"));
            n.setSourceUrl(link);
            n.setSource(DEFAULT_RSS_SOURCE);
            n.setCoverStyle(COVERS[added % COVERS.length]);
            // 给拉取的外部资讯附一张 picsum 随机图（按 added+时间戳 seed 锁图，保持稳定）
            n.setCoverImage("https://picsum.photos/seed/occ-rss-" + System.currentTimeMillis() + "-" + added + "/640/360");
            n.setViewCount(0);
            n.setFeatured(0);
            n.setStatus(STATUS_ON);
            n.setPublishTime(LocalDateTime.now());
            newsMapper.insert(n);
            added++;
        }
        return added;
    }

    private String detectCategory(String text) {
        String value = StrUtil.blankToDefault(text, "").toLowerCase();
        if (containsAny(value, "前端", "frontend", "vue", "react", "javascript", "typescript", "css", "html")) {
            return "frontend";
        }
        if (containsAny(value, "测试", "test", "qa", "selenium", "自动化测试", "质量")) {
            return "test";
        }
        if (containsAny(value, "运维", "devops", "docker", "kubernetes", "k8s", "linux", "云原生", "sre")) {
            return "devops";
        }
        if (containsAny(value, "大数据", "big data", "spark", "flink", "hadoop", "hive", "数据仓库")) {
            return "bigdata";
        }
        if (containsAny(value, "后端", "backend", "java", "spring", "go", "python", "微服务", "数据库", "api")) {
            return "backend";
        }
        return null;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Industry news is public platform content: admin maintains it, all roles read the same pool.
     * The explicit ignore avoids anonymous or role-scoped requests being filtered by tenant_id.
     */
    private <T> T withoutTenant(Supplier<T> action) {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
        try {
            return action.get();
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    private void withoutTenant(Runnable action) {
        withoutTenant(() -> {
            action.run();
            return null;
        });
    }

    private void ensureBaselineNews() {
        Long count = newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getStatus, STATUS_ON));
        if (count != null && count > 0) {
            return;
        }
        List<News> rows = new ArrayList<>();
        rows.add(article("后端岗位需求回暖：Spring Boot、Redis 与消息队列仍是高频组合",
                "近期岗位样本显示，Java 后端岗位对工程化、缓存与异步处理能力的要求更集中，建议学生优先补齐项目闭环经验。",
                "backend", "就业指导中心"));
        rows.add(article("前端岗位从页面开发转向体验工程，TypeScript 与组件设计权重上升",
                "企业更关注可维护组件、状态管理和性能优化能力，能把交互体验讲清楚的作品集更容易脱颖而出。",
                "frontend", "行业研究中心"));
        rows.add(article("测试岗位正在向质量工程转型，自动化与接口测试成为基础门槛",
                "测试方向不再只看用例执行，脚本能力、接口验证、持续集成和问题定位能力正在成为核心竞争点。",
                "test", "教学中心"));
        rows.add(article("云原生运维继续升温，Docker、K8s 与可观测性能力值得提前布局",
                "从部署到监控，企业希望候选人理解完整上线链路。建议以个人项目搭建一套可演示的发布流程。",
                "devops", "职业规划组"));
        rows.add(article("大数据岗位更看重业务理解，SQL、Flink 与数据建模仍是主线",
                "数据方向的竞争点从工具使用走向指标体系和业务解释能力，项目中应突出数据口径与分析结论。",
                "bigdata", "数据播报中心"));
        for (News row : rows) {
            newsMapper.insert(row);
        }
        log.info("行业资讯为空，已写入基础资讯 {} 条", rows.size());
    }

    private News article(String title, String summary, String category, String source) {
        News n = new News();
        n.setTenantId(1L);
        n.setType("ARTICLE");
        n.setCategory(category);
        n.setTitle(title);
        n.setSummary(summary);
        n.setContent("<p>" + summary + "</p>");
        n.setCoverStyle("blue");
        n.setSource(source);
        n.setViewCount(0);
        n.setFeatured(0);
        n.setStatus(STATUS_ON);
        n.setPublishTime(LocalDateTime.now());
        return n;
    }

    private String cleanSummary(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String cleaned = value.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .trim();
        return cleaned.length() > 180 ? cleaned.substring(0, 180) + "..." : cleaned;
    }

    // ---------- 工具 ----------

    private News cast(String title, String summary, String category, String cover, String link) {
        News n = new News();
        n.setTenantId(1L);
        n.setType("DATA_CAST");
        n.setCategory(category);
        n.setTitle(title);
        n.setSummary(summary);
        n.setCoverStyle(cover);
        // 数据播报也附带 picsum 锁图（seed 用标题 hash，避免重复抓同一张）
        n.setCoverImage("https://picsum.photos/seed/occ-cast-" + Math.abs(title.hashCode()) + "/640/360");
        n.setSource("平台数据播报");
        n.setLinkTarget(link);
        n.setViewCount(0);
        n.setFeatured(0);
        n.setStatus(STATUS_ON);
        n.setPublishTime(LocalDateTime.now());
        return n;
    }

    private long sumCount(List<DashboardVO.DimensionItem> items) {
        if (items == null) {
            return 0;
        }
        long s = 0;
        for (DashboardVO.DimensionItem i : items) {
            s += i.getCount() == null ? 0 : i.getCount();
        }
        return s;
    }

    private DashboardVO.DimensionItem first(List<DashboardVO.DimensionItem> items) {
        return (items == null || items.isEmpty()) ? null : items.get(0);
    }

    private String text(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        return nl.getLength() == 0 ? null : nl.item(0).getTextContent();
    }
}
