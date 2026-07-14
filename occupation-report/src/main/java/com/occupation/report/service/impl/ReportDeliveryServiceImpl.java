package com.occupation.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.service.ClassService;
import com.occupation.common.exception.BizException;
import com.occupation.common.service.NotificationService;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final NotificationService notificationService;

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
        // 市场报告没有「范围」概念，只有二态：全体可见(ALL) / 仅自己可见(SELF)，用 visibility 列开关广播
        if ("MARKET".equals(report.getCategory())) {
            return setMarketVisibility(report, targetType);
        }

        // 以下为学生就业报告：按范围定向下发（delivery 行）
        // SELF = 仅自己可见：撤回全部下发，报告回到私有状态；其余类型按范围解析
        boolean makePrivate = "SELF".equals(targetType);
        Set<Long> targets = makePrivate ? Collections.emptySet() : resolveTargets(targetType, targetValue);
        if (!makePrivate && targets.isEmpty()) {
            throw new BizException("该范围内没有可发送的学生");
        }

        // 覆盖语义：以本次范围为准。先取当前活跃接收者（保留已读状态），再物理清空重建。
        List<ReportDelivery> activeRows = deliveryMapper.selectList(new LambdaQueryWrapper<ReportDelivery>()
                .eq(ReportDelivery::getReportId, reportId));
        Set<Long> existing = activeRows.stream().map(ReportDelivery::getUserId).collect(Collectors.toSet());
        Map<Long, LocalDateTime> readByUser = activeRows.stream()
                .filter(d -> d.getReadTime() != null)
                .collect(Collectors.toMap(ReportDelivery::getUserId, ReportDelivery::getReadTime, (a, b) -> a));

        // 必须物理删除：本表有唯一键 uk_report_user，逻辑删除的残留行会卡住重新下发
        deliveryMapper.hardDeleteByReport(reportId);

        Set<Long> toAdd = targets.stream().filter(u -> !existing.contains(u)).collect(Collectors.toSet());
        for (Long uid : targets) {
            ReportDelivery d = new ReportDelivery();
            d.setReportId(reportId);
            d.setUserId(uid);
            d.setReadTime(readByUser.get(uid));   // 仍在范围内的学生保留已读状态
            deliveryMapper.insert(d);
        }
        // 只通知新增的学生，避免重复打扰仍在范围内的
        for (Long uid : toAdd) {
            notificationService.createPush(uid, "REPORT", "收到一份新报告",
                    String.format("你收到一份新报告：%s，点击查看。", report.getName()),
                    "REPORT", reportId);
        }
        int revoked = existing.size() - (targets.size() - toAdd.size());
        log.info("报告下发(覆盖): reportId={}, 范围={}:{}, 当前可见 {} 人, 新增 {}, 撤销 {}",
                reportId, targetType, targetValue, targets.size(), toAdd.size(), revoked);
        return targets.size();   // 返回覆盖后当前可见的总人数
    }

    /** 市场报告：切换「全体可见 / 仅自己可见」。返回 -1=全体（前端按类别显示），0=仅自己可见 */
    private int setMarketVisibility(ReportRecord report, String targetType) {
        String visibility;
        if ("ALL".equals(targetType)) {
            visibility = "PUBLIC";
        } else if ("SELF".equals(targetType)) {
            visibility = "SELF";
        } else {
            throw new BizException("市场行业报告只能设为「全体可见」或「仅自己可见」");
        }
        report.setVisibility(visibility);
        recordMapper.updateById(report);
        log.info("市场报告可见性变更: reportId={}, visibility={}", report.getId(), visibility);
        return "SELF".equals(visibility) ? 0 : -1;
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
    public Map<Long, Long> deliveredCountByReports(Collection<Long> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return deliveryMapper.selectList(new LambdaQueryWrapper<ReportDelivery>()
                        .select(ReportDelivery::getReportId)
                        .in(ReportDelivery::getReportId, reportIds))
                .stream()
                .collect(Collectors.groupingBy(ReportDelivery::getReportId, Collectors.counting()));
    }

    @Override
    public Page<ReceivedReportVO> receivedFor(Long userId, int pageNum, int pageSize) {
        // 1) 广播：市场行业报告，全体可见（「仅自己可见」的市场报告不广播）
        List<ReportRecord> broadcast = recordMapper.selectList(new LambdaQueryWrapper<ReportRecord>()
                .isNull(ReportRecord::getUserId)
                .eq(ReportRecord::getCategory, "MARKET")
                .eq(ReportRecord::getStatus, "SUCCESS")
                .ne(ReportRecord::getVisibility, "SELF"));

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
            return !"SELF".equals(r.getVisibility()); // 广播；仅自己可见的市场报告学生无权访问
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
