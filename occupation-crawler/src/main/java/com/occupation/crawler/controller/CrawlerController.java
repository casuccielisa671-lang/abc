package com.occupation.crawler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.common.result.Result;
import com.occupation.crawler.dto.CrawlerTaskCreateDTO;
import com.occupation.crawler.entity.CrawlerLog;
import com.occupation.crawler.entity.CrawlerTask;
import com.occupation.crawler.mapper.CrawlerLogMapper;
import com.occupation.crawler.mapper.CrawlerTaskMapper;
import com.occupation.crawler.service.CrawlerService;
import com.occupation.crawler.vo.CrawlerLogVO;
import com.occupation.crawler.vo.CrawlerTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采集任务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerTaskMapper crawlerTaskMapper;
    private final CrawlerLogMapper crawlerLogMapper;
    private final CrawlerService crawlerService;

    // ==================== 采集任务 CRUD ====================

    /**
     * 创建采集任务
     */
    @PostMapping("/task")
    public Result<Long> createTask(@Valid @RequestBody CrawlerTaskCreateDTO dto) {
        CrawlerTask task = new CrawlerTask();
        task.setSourceType(dto.getSourceType());
        task.setSourceName(dto.getSourceName());
        task.setUrlPattern(dto.getUrlPattern());
        task.setCronExpr(dto.getCronExpr());
        task.setStatus(0);
        crawlerTaskMapper.insert(task);
        log.info("采集任务创建成功 — id={}, name={}", task.getId(), task.getSourceName());
        return Result.ok(task.getId());
    }

    /**
     * 任务列表（分页）
     */
    @GetMapping("/task")
    public Result<Page<CrawlerTaskVO>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        markStaleRunningLogs();
        Page<CrawlerTask> pageParam = new Page<>(page, size);
        Page<CrawlerTask> result = crawlerTaskMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CrawlerTask>().orderByDesc(CrawlerTask::getCreateTime));

        Page<CrawlerTaskVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(CrawlerTaskVO::from).collect(Collectors.toList()));
        return Result.ok(voPage);
    }

    /**
     * 任务详情
     */
    @GetMapping("/task/{id}")
    public Result<CrawlerTaskVO> getTask(@PathVariable Long id) {
        CrawlerTask task = crawlerTaskMapper.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.ok(CrawlerTaskVO.from(task));
    }

    /**
     * 更新任务
     */
    @PutMapping("/task/{id}")
    public Result<Void> updateTask(@PathVariable Long id, @Valid @RequestBody CrawlerTaskCreateDTO dto) {
        CrawlerTask task = crawlerTaskMapper.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        task.setSourceType(dto.getSourceType());
        task.setSourceName(dto.getSourceName());
        task.setUrlPattern(dto.getUrlPattern());
        task.setCronExpr(dto.getCronExpr());
        crawlerTaskMapper.updateById(task);
        return Result.ok();
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/task/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        if (crawlerService.isRunning(id)) {
            crawlerService.stopCrawl(id);
        }
        crawlerTaskMapper.deleteById(id);
        return Result.ok();
    }

    // ==================== 任务启停 ====================

    /**
     * 手动启动任务
     */
    @PutMapping("/task/{id}/start")
    public Result<Long> startTask(@PathVariable Long id) {
        CrawlerTask task = crawlerTaskMapper.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        if (crawlerService.isRunning(id)) {
            return Result.error(400, "任务已在运行中");
        }
        crawlerService.startCrawl(task);
        return Result.ok(id);
    }

    /**
     * 停止任务
     */
    @PutMapping("/task/{id}/stop")
    public Result<Void> stopTask(@PathVariable Long id) {
        CrawlerTask task = crawlerTaskMapper.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        crawlerService.stopCrawl(id);
        return Result.ok();
    }

    // 原先这里还有一个 POST /task/mock「便捷测试接口」：它每次调用都用
    // System.currentTimeMillis() 当主键新插一条一次性 crawler_task，跑完不清理，
    // 采集任务列表越点越脏（线上真留下过两条这样的垃圾任务）。
    // 而它的行为与「对一条 source_type=MOCK 的任务点启动」完全等价 —— createProcessor
    // 见到 MOCK 一样走 MockJobPageProcessor。既然等价，就只保留一个入口。

    // ==================== 采集日志查询 ====================

    /**
     * 采集日志列表（分页，按任务筛选）
     */
    @GetMapping("/log")
    public Result<Page<CrawlerLogVO>> listLogs(
            @RequestParam(required = false) Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        markStaleRunningLogs();
        LambdaQueryWrapper<CrawlerLog> wrapper = new LambdaQueryWrapper<CrawlerLog>()
                .orderByDesc(CrawlerLog::getCreateTime);
        if (taskId != null) {
            wrapper.eq(CrawlerLog::getTaskId, taskId);
        }

        Page<CrawlerLog> pageParam = new Page<>(page, size);
        Page<CrawlerLog> result = crawlerLogMapper.selectPage(pageParam, wrapper);

        Page<CrawlerLogVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(CrawlerLogVO::from).collect(Collectors.toList()));
        return Result.ok(voPage);
    }

    private void markStaleRunningLogs() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(3);
        List<CrawlerLog> staleLogs = crawlerLogMapper.selectList(new LambdaQueryWrapper<CrawlerLog>()
                .select(CrawlerLog::getTaskId)
                .eq(CrawlerLog::getStatus, "RUNNING")
                .isNull(CrawlerLog::getEndTime)
                .lt(CrawlerLog::getStartTime, threshold));
        if (staleLogs.isEmpty()) {
            return;
        }

        CrawlerLog stale = new CrawlerLog();
        stale.setStatus("FAILED");
        stale.setEndTime(LocalDateTime.now());
        stale.setErrorMsg("采集任务超过 3 分钟仍未回写完成状态，已自动标记失败。请重新点击“立即采集一次”测试。");
        crawlerLogMapper.update(stale, new LambdaUpdateWrapper<CrawlerLog>()
                .eq(CrawlerLog::getStatus, "RUNNING")
                .isNull(CrawlerLog::getEndTime)
                .lt(CrawlerLog::getStartTime, threshold));

        Set<Long> taskIds = staleLogs.stream().map(CrawlerLog::getTaskId).collect(Collectors.toSet());
        for (Long staleTaskId : taskIds) {
            CrawlerTask task = new CrawlerTask();
            task.setId(staleTaskId);
            task.setStatus(0);
            crawlerTaskMapper.updateById(task);
        }
    }

    /**
     * 日志详情
     */
    @GetMapping("/log/{id}")
    public Result<CrawlerLogVO> getLog(@PathVariable Long id) {
        CrawlerLog log = crawlerLogMapper.selectById(id);
        if (log == null) {
            return Result.error(404, "日志不存在");
        }
        return Result.ok(CrawlerLogVO.from(log));
    }

}
