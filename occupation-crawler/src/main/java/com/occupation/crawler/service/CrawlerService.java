package com.occupation.crawler.service;

import com.occupation.crawler.entity.CrawlerTask;
import com.occupation.crawler.entity.CrawlerLog;

import java.util.List;

/**
 * 爬虫调度服务接口
 */
public interface CrawlerService {

    /**
     * 根据采集任务配置启动爬虫
     * @param task 采集任务
     * @return 采集日志（含 RUNNING 状态）
     */
    CrawlerLog startCrawl(CrawlerTask task);

    /**
     * 停止指定任务的爬虫
     * @param taskId 任务ID
     * @return 是否停止成功
     */
    boolean stopCrawl(Long taskId);

    /**
     * 查询任务是否正在运行
     * @param taskId 任务ID
     * @return true=运行中
     */
    boolean isRunning(Long taskId);

    /**
     * 启动模拟爬虫（开发测试用）
     * @param task 采集任务
     * @return 采集日志
     */
    CrawlerLog startMockCrawl(CrawlerTask task);

    /**
     * 查询所有运行中的任务
     */
    List<CrawlerTask> listRunningTasks();

    /**
     * 根据任务ID获取最近的采集日志
     */
    List<CrawlerLog> getLogsByTaskId(Long taskId, int page, int size);

}
