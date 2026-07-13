package com.occupation.recommend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private final NewsMapper newsMapper;
    private final AnalysisService analysisService;

    @Override
    public Page<News> pageNews(String category, String type, int pageNum, int pageSize) {
        return newsMapper.selectPage(new Page<>(pageNum, pageSize), listWrapper(category, type));
    }

    @Override
    public List<News> latest(int limit) {
        return newsMapper.selectList(listWrapper(null, null)
                .last("LIMIT " + Math.max(1, limit)));
    }

    @Override
    public News getDetail(Long id) {
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
    }

    /** 公共查询条件：仅上架，置顶优先、发布时间倒序；按方向/类型可选筛选 */
    private LambdaQueryWrapper<News> listWrapper(String category, String type) {
        LambdaQueryWrapper<News> w = new LambdaQueryWrapper<>();
        w.eq(News::getStatus, STATUS_ON);
        if (StrUtil.isNotBlank(category) && !"all".equalsIgnoreCase(category)) {
            w.eq(News::getCategory, category);
        }
        if (StrUtil.isNotBlank(type)) {
            w.eq(News::getType, type);
        }
        w.orderByDesc(News::getFeatured).orderByDesc(News::getPublishTime);
        return w;
    }

    // ==================== 管理端 ====================

    @Override
    public Page<News> pageAll(String category, String type, Integer status, int pageNum, int pageSize) {
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
    }

    @Override
    public News saveNews(News news) {
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
    }

    @Override
    public void deleteNews(Long id) {
        newsMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        newsMapper.update(null, new LambdaUpdateWrapper<News>()
                .eq(News::getId, id)
                .set(News::getStatus, status));
    }

    @Override
    public int generateDataCast() {
        DashboardQueryDTO q = new DashboardQueryDTO();
        q.setTenantId(TenantContextHolder.getTenantId());
        DashboardVO d = analysisService.getDashboard(q);

        // 先清掉旧的数据播报（逻辑删除），再生成一批新的
        newsMapper.delete(new LambdaQueryWrapper<News>().eq(News::getType, "DATA_CAST"));

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
        for (News n : casts) {
            newsMapper.insert(n);
        }
        log.info("数据播报生成完成: {} 条", casts.size());
        return casts.size();
    }

    @Override
    public int pullExternalNews(String query, int maxItems) {
        try {
            String url = "https://news.google.com/rss/search?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                    + "&hl=zh-CN&gl=CN&ceid=CN:zh-Hans";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
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
            for (int i = 0; i < items.getLength() && added < maxItems; i++) {
                Element it = (Element) items.item(i);
                String title = text(it, "title");
                String link = text(it, "link");
                if (StrUtil.isBlank(title)) {
                    continue;
                }
                Long exists = newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getTitle, title));
                if (exists != null && exists > 0) {
                    continue;
                }
                News n = new News();
                n.setType("EXTERNAL");
                n.setTitle(title.length() > 300 ? title.substring(0, 300) : title);
                n.setSummary("来自 Google News，点击「阅读原文」查看详情。");
                n.setSourceUrl(link);
                n.setSource("Google News");
                n.setCoverStyle(COVERS[added % COVERS.length]);
                n.setViewCount(0);
                n.setFeatured(0);
                n.setStatus(STATUS_ON);
                n.setPublishTime(LocalDateTime.now());
                newsMapper.insert(n);
                added++;
            }
            log.info("外部资讯拉取: query={}, 新增 {} 条", query, added);
            return added;
        } catch (Exception e) {
            log.warn("拉取外部资讯失败（服务器可能无法访问 Google）: {}", e.getMessage());
            return 0;
        }
    }

    // ---------- 工具 ----------

    private News cast(String title, String summary, String category, String cover, String link) {
        News n = new News();
        n.setType("DATA_CAST");
        n.setCategory(category);
        n.setTitle(title);
        n.setSummary(summary);
        n.setCoverStyle(cover);
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
