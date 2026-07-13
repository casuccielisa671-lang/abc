package com.occupation.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.service.ClassService;
import com.occupation.common.exception.BizException;
import com.occupation.report.entity.ReportDelivery;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.mapper.ReportDeliveryMapper;
import com.occupation.report.mapper.ReportRecordMapper;
import com.occupation.report.service.ReportDeliveryService;
import com.occupation.report.vo.ReceivedReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报告下发实现。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDeliveryServiceImpl implements ReportDeliveryService {

    private final ReportDeliveryMapper deliveryMapper;
    private final ReportRecordMapper recordMapper;
    private final ClassService classService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deliver(Long reportId, String targetType, String targetValue) {
        ReportRecord report = recordMapper.selectById(reportId);
        if (report == null) {
            throw new BizException("报告不存在");
        }
        if (report.getUserId() != null) {
            throw new BizException("学生个人报告不可下发");
        }
        if (!"SUCCESS".equals(report.getStatus())) {
            throw new BizException("报告尚未生成成功，无法下发");
        }
        if ("MARKET".equals(report.getCategory())) {
            throw new BizException("市场行业报告已对全体学生可见，无需发送");
        }

        Set<Long> targets = resolveTargets(targetType, targetValue);
        if (targets.isEmpty()) {
            throw new BizException("该范围内没有可发送的学生");
        }

        // 已发过的不重复
        Set<Long> already = deliveryMapper.selectList(new LambdaQueryWrapper<ReportDelivery>()
                        .select(ReportDelivery::getUserId)
                        .eq(ReportDelivery::getReportId, reportId))
                .stream().map(ReportDelivery::getUserId).collect(Collectors.toSet());

        int inserted = 0;
        for (Long uid : targets) {
            if (already.contains(uid)) {
                continue;
            }
            ReportDelivery d = new ReportDelivery();
            d.setReportId(reportId);
            d.setUserId(uid);
            deliveryMapper.insert(d);
            inserted++;
        }
        log.info("报告下发: reportId={}, 范围={}:{}, 目标 {} 人, 新增 {} 人",
                reportId, targetType, targetValue, targets.size(), inserted);
        return inserted;
    }

    /** 把 范围类型+值 解析成学生 userId 集合（多租户自动隔离在当前管理员租户内） */
    private Set<Long> resolveTargets(String type, String value) {
        if (type == null) {
            throw new BizException("请选择发送范围");
        }
        switch (type) {
            case "ALL":
                return classService.allStudentIds();
            case "MAJOR":
                requireValue(value, "专业");
                return nullToEmpty(classService.studentIdsByMajorYear(value, null));
            case "GRADE":
                requireValue(value, "入学年级");
                return nullToEmpty(classService.studentIdsByMajorYear(null, parseInt(value)));
            case "CLASS":
                requireValue(value, "班级");
                return classService.studentIdsInClass(parseLong(value));
            default:
                throw new BizException("不支持的发送范围类型：" + type);
        }
    }

    @Override
    public long deliveredCount(Long reportId) {
        return deliveryMapper.selectCount(new LambdaQueryWrapper<ReportDelivery>()
                .eq(ReportDelivery::getReportId, reportId));
    }

    @Override
    public Page<ReceivedReportVO> receivedFor(Long userId, int pageNum, int pageSize) {
        // 1) 广播：市场行业报告，全体可见
        List<ReportRecord> broadcast = recordMapper.selectList(new LambdaQueryWrapper<ReportRecord>()
                .isNull(ReportRecord::getUserId)
                .eq(ReportRecord::getCategory, "MARKET")
                .eq(ReportRecord::getStatus, "SUCCESS"));

        // 2) 定向：下发给我的报告（含已读状态）
        List<ReportDelivery> mine = deliveryMapper.selectList(new LambdaQueryWrapper<ReportDelivery>()
                .eq(ReportDelivery::getUserId, userId));

        List<ReceivedReportVO> all = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ReportRecord r : broadcast) {
            all.add(ReceivedReportVO.of(r, "BROADCAST", true));
            seen.add(r.getId());
        }
        if (!mine.isEmpty()) {
            List<Long> ids = mine.stream().map(ReportDelivery::getReportId).collect(Collectors.toList());
            List<ReportRecord> records = recordMapper.selectBatchIds(ids).stream()
                    .filter(r -> "SUCCESS".equals(r.getStatus()))
                    .collect(Collectors.toList());
            java.util.Map<Long, Boolean> readMap = mine.stream()
                    .collect(Collectors.toMap(ReportDelivery::getReportId, d -> d.getReadTime() != null, (a, b) -> a || b));
            for (ReportRecord r : records) {
                if (seen.contains(r.getId())) {
                    continue; // 极端情况下市场报告也被定向发过，避免重复
                }
                all.add(ReceivedReportVO.of(r, "DELIVERED", Boolean.TRUE.equals(readMap.get(r.getId()))));
            }
        }

        // 按时间倒序，内存分页（报告量级小）
        all.sort((a, b) -> {
            LocalDateTime x = a.getCreateTime(), y = b.getCreateTime();
            if (x == null && y == null) return 0;
            if (x == null) return 1;
            if (y == null) return -1;
            return y.compareTo(x);
        });
        int total = all.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<ReceivedReportVO> pageList = from >= total ? Collections.emptyList() : all.subList(from, to);

        Page<ReceivedReportVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pageList);
        return page;
    }

    @Override
    public boolean canStudentAccess(Long reportId, Long userId) {
        ReportRecord r = recordMapper.selectById(reportId);
        if (r == null || r.getUserId() != null) {
            return false;
        }
        if ("MARKET".equals(r.getCategory())) {
            return true; // 广播
        }
        return deliveryMapper.selectCount(new LambdaQueryWrapper<ReportDelivery>()
                .eq(ReportDelivery::getReportId, reportId)
                .eq(ReportDelivery::getUserId, userId)) > 0;
    }

    @Override
    public void markRead(Long reportId, Long userId) {
        deliveryMapper.update(null, new LambdaUpdateWrapper<ReportDelivery>()
                .set(ReportDelivery::getReadTime, LocalDateTime.now())
                .eq(ReportDelivery::getReportId, reportId)
                .eq(ReportDelivery::getUserId, userId)
                .isNull(ReportDelivery::getReadTime));
    }

    // ---- helpers ----
    private static Set<Long> nullToEmpty(Set<Long> s) {
        return s == null ? Collections.emptySet() : s;
    }

    private static void requireValue(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException("请选择" + label);
        }
    }

    private static int parseInt(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException("入学年级格式错误：" + v);
        }
    }

    private static long parseLong(String v) {
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException("班级 id 格式错误：" + v);
        }
    }
}
