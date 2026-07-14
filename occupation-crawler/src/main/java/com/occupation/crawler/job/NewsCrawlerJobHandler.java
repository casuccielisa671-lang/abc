package com.occupation.crawler.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.crawler.processor.InfoQNewsProcessor;
import com.occupation.crawler.processor.NewsPageProcessor;
import com.occupation.crawler.processor.OsChinaNewsProcessor;
import com.occupation.recommend.entity.News;
import com.occupation.recommend.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;

import java.util.List;

/**
 * 资讯爬虫定时任务（Spring @Scheduled 轻量版）
 * <p>
 * 每天定时从 InfoQ、OSCHINA 等公开 RSS 源拉取计算机行业资讯，
 * 按标题去重后写入 news 表（type=EXTERNAL，status=1 上架）。
 * <p>
 * 通过配置项 {@code app.scheduler.enabled} 控制开关，
 * 关闭后本定时任务不生效。
 *
 * @author occupation-team
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class NewsCrawlerJobHandler {

    private final NewsMapper newsMapper;

    /**
     * 定时采集资讯（每天 8:00、12:00、18:00 执行）
     */
    @Scheduled(cron = "0 0 8,12,18 * * ?")
    public void crawlNews() {
        log.info("===== 资讯采集任务开始 =====");

        int totalAdded = 0;
        int totalSkipped = 0;

        // 依次采集各资讯源
        NewsPageProcessor[] processors = {
                new InfoQNewsProcessor(),
                new OsChinaNewsProcessor()
        };

        for (NewsPageProcessor processor : processors) {
            try {
                int[] result = crawlSource(processor);
                totalAdded += result[0];
                totalSkipped += result[1];
            } catch (Exception e) {
                log.error("资讯源 {} 采集失败: {}", processor.getSourceName(), e.getMessage(), e);
            }
        }

        String msg = String.format("资讯采集完成 — 新增=%d, 跳过(重复)=%d", totalAdded, totalSkipped);
        log.info(msg);
    }

    /**
     * 采集单个资讯源
     *
     * @return [新增数, 跳过数]
     */
    private int[] crawlSource(NewsPageProcessor processor) {
        String rssUrl = processor.getRssUrl();
        log.info("开始采集 [{}]: {}", processor.getSourceName(), rssUrl);

        Spider spider = Spider.create(processor)
                .thread(1)
                .addRequest(new Request(rssUrl));

        spider.run(); // 同步执行，等待完成
        spider.stop();

        List<News> newsList = processor.drainCollectedNews();
        if (newsList.isEmpty()) {
            log.warn("[{}] 未采集到任何资讯", processor.getSourceName());
            return new int[]{0, 0};
        }

        int added = 0;
        int skipped = 0;
        for (News news : newsList) {
            // 按标题去重
            Long exists = newsMapper.selectCount(
                    new LambdaQueryWrapper<News>().eq(News::getTitle, news.getTitle()));
            if (exists != null && exists > 0) {
                skipped++;
                continue;
            }
            try {
                newsMapper.insert(news);
                added++;
            } catch (Exception e) {
                log.error("资讯入库失败 — title={}, error={}", news.getTitle(), e.getMessage());
            }
        }

        log.info("[{}] 采集完成 — 新增={}, 跳过重复={}", processor.getSourceName(), added, skipped);
        return new int[]{added, skipped};
    }
}
