package com.occupation.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.occupation.analysis.service.JobDetailService;
import com.occupation.analysis.vo.JobDetailVO;
import com.occupation.common.exception.BizException;
import com.occupation.common.service.NotificationService;
import com.occupation.recommend.dto.ApplicationStatusDTO;
import com.occupation.recommend.entity.ApplicationStatus;
import com.occupation.recommend.entity.JobApplication;
import com.occupation.recommend.mapper.JobApplicationMapper;
import com.occupation.recommend.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 投递记录服务实现 — job_application 表（多租户自动隔离）
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    /** 通知里的面试时间格式 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JobApplicationMapper applicationMapper;
    private final JobDetailService jobDetailService;
    private final NotificationService notificationService;

    @Override
    public JobApplication apply(Long userId, Long jobId, Long publisherId) {
        JobApplication exists = findByUserAndJob(userId, jobId);
        if (exists != null) {
            return exists;
        }
        JobApplication app = new JobApplication();
        app.setUserId(userId);
        app.setJobId(jobId);
        app.setPublisherId(publisherId);
        app.setStatus(ApplicationStatus.SUBMITTED.name());
        app.setAppliedAt(LocalDateTime.now());
        try {
            applicationMapper.insert(app);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时过了上面的 exists 检查，唯一索引兜住。重查一次返回既有记录
            log.debug("重复投递被唯一索引拦下: userId={}, jobId={}", userId, jobId);
            return findByUserAndJob(userId, jobId);
        }
        log.info("投递已创建: userId={}, jobId={}, publisherId={}", userId, jobId, publisherId);
        return app;
    }

    @Override
    public List<JobApplication> listByPublisher(Long publisherId) {
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getPublisherId, publisherId)
                .orderByDesc(JobApplication::getAppliedAt));
    }

    @Override
    public List<JobApplication> listByUser(Long userId) {
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .orderByDesc(JobApplication::getAppliedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long applicationId, Long operatorId, ApplicationStatusDTO dto) {
        String newStatus = dto.getStatus();
        if (!ApplicationStatus.isValid(newStatus)) {
            throw new BizException("非法的投递状态: " + newStatus);
        }
        JobApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BizException("投递记录不存在");
        }
        // 归属校验：只能改自己发布的职位上的投递
        if (!operatorId.equals(app.getPublisherId())) {
            log.warn("拒绝越权变更投递状态: applicationId={}, publisherId={}, operatorId={}",
                    applicationId, app.getPublisherId(), operatorId);
            throw new BizException(403, "无权操作该投递：它不属于你发布的职位");
        }

        ApplicationStatus from = ApplicationStatus.valueOf(app.getStatus());
        ApplicationStatus to = ApplicationStatus.valueOf(newStatus);
        if (!ApplicationStatus.canTransit(from, to)) {
            throw new BizException(String.format("不能从「%s」变更为「%s」%s",
                    from.getLabel(), to.getLabel(),
                    from.isTerminal() ? "：该投递已处于终态" : ""));
        }

        // 录用前拦一道：学生已在别处入职（有 ACCEPTED）就不能再被录用
        if (to == ApplicationStatus.OFFER && isEmployed(app.getUserId())) {
            throw new BizException("该学生已入职他处，无法录用");
        }

        // 邀请面试：时间、地点必填，并把结构化面试信息落库（学生详情页展示成面试卡）
        if (to == ApplicationStatus.INTERVIEW) {
            if (dto.getInterviewTime() == null) {
                throw new BizException("请填写面试时间");
            }
            if (trimToNull(dto.getInterviewPlace()) == null) {
                throw new BizException("请填写面试地点或线上会议方式");
            }
            app.setInterviewTime(dto.getInterviewTime());
            app.setInterviewPlace(dto.getInterviewPlace().trim());
            app.setInterviewContact(trimToNull(dto.getInterviewContact()));
            app.setInterviewContent(trimToNull(dto.getInterviewContent()));
        }

        app.setStatus(to.name());
        app.setStatusChangedAt(LocalDateTime.now());
        String hrNote = dto.getHrNote();
        if (hrNote != null) {
            app.setHrNote(hrNote.trim().isEmpty() ? null : hrNote.trim());
        }
        applicationMapper.updateById(app);
        log.info("投递状态变更: id={}, {} -> {}", applicationId, from, to);

        // 给学生发站内通知（仅 INTERVIEW/OFFER/REJECTED 三态；VIEWED 是自动流转，不打扰）
        notifyStudent(app, to);
    }

    /**
     * 按状态给投递学生发一条站内通知，点击可跳到「我的投递」详情。
     * <p>学生看得到通知内容（含面试卡信息），但拿不到 HR 的内部备注 hrNote。
     */
    private void notifyStudent(JobApplication app, ApplicationStatus to) {
        String type;
        String title;
        switch (to) {
            case INTERVIEW: type = "INTERVIEW"; title = "面试邀请"; break;
            case OFFER:     type = "OFFER";     title = "录用通知"; break;
            case REJECTED:  type = "REJECT";    title = "投递结果"; break;
            default:        return;   // VIEWED 等不通知
        }
        JobDetailVO job = jobDetailService.getJobById(app.getJobId());
        String company = job != null && job.getCompany() != null ? job.getCompany() : "招聘方";
        String jobTitle = job != null && job.getTitle() != null ? job.getTitle() : "你投递的职位";
        String content = buildContent(to, company, jobTitle, app);
        notificationService.createPush(app.getUserId(), type, title, content, "APPLICATION", app.getId());
    }

    private String buildContent(ApplicationStatus to, String company, String jobTitle, JobApplication app) {
        switch (to) {
            case INTERVIEW:
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("【面试邀请】%s 邀请你参加「%s」面试。\n", company, jobTitle));
                sb.append("面试时间：").append(app.getInterviewTime().format(TIME_FMT)).append("\n");
                sb.append("面试地点：").append(app.getInterviewPlace());
                if (app.getInterviewContact() != null) {
                    sb.append("\n联系人：").append(app.getInterviewContact());
                }
                if (app.getInterviewContent() != null) {
                    sb.append("\n面试内容：").append(app.getInterviewContent());
                }
                sb.append("\n请提前做好准备，如有变动请及时与招聘方联系。");
                return sb.toString();
            case OFFER:
                return String.format("【录用通知】恭喜！%s「%s」决定录用你。"
                        + "请留意招聘方后续联系，商谈入职事宜。", company, jobTitle);
            case REJECTED:
                return String.format("【投递结果】感谢你投递 %s「%s」。"
                        + "很遗憾，你与本岗位的匹配未达预期，祝你早日找到心仪的工作！", company, jobTitle);
            default:
                return "";
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public int markViewed(Long publisherId, Long applicantUserId) {
        // 只推进 SUBMITTED；已经 INTERVIEW/OFFER/REJECTED 的不能被拉回 VIEWED
        LambdaUpdateWrapper<JobApplication> wrapper = new LambdaUpdateWrapper<JobApplication>()
                .eq(JobApplication::getPublisherId, publisherId)
                .eq(JobApplication::getUserId, applicantUserId)
                .eq(JobApplication::getStatus, ApplicationStatus.SUBMITTED.name())
                .set(JobApplication::getStatus, ApplicationStatus.VIEWED.name())
                .set(JobApplication::getStatusChangedAt, LocalDateTime.now());
        return applicationMapper.update(null, wrapper);
    }

    @Override
    public Map<Long, List<JobApplication>> groupByApplicant(Long publisherId, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getPublisherId, publisherId)
                        .in(JobApplication::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(JobApplication::getUserId));
    }

    private JobApplication findByUserAndJob(Long userId, Long jobId) {
        return applicationMapper.selectOne(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .eq(JobApplication::getJobId, jobId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptOffer(Long applicationId, Long userId) {
        JobApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BizException("投递记录不存在");
        }
        if (!userId.equals(app.getUserId())) {
            throw new BizException(403, "无权操作该投递");
        }
        if (!ApplicationStatus.OFFER.name().equals(app.getStatus())) {
            throw new BizException("只有「已录用」的投递才能接收");
        }
        if (isEmployed(userId)) {
            throw new BizException("你已接收过一份录用，一个人只能入职一处");
        }
        app.setStatus(ApplicationStatus.ACCEPTED.name());
        app.setStatusChangedAt(LocalDateTime.now());
        applicationMapper.updateById(app);
        log.info("学生接收录用: applicationId={}, userId={}", applicationId, userId);

        // 通知发出该录用的 HR：学生已接受
        JobDetailVO job = jobDetailService.getJobById(app.getJobId());
        String jobTitle = job != null && job.getTitle() != null ? job.getTitle() : "你发布的职位";
        notificationService.createPush(app.getPublisherId(), "SYSTEM", "学生已接受录用",
                String.format("有学生接受了「%s」的录用，即将入职。", jobTitle),
                "APPLICATION", app.getId());
    }

    @Override
    public boolean isEmployed(Long userId) {
        Long n = applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .eq(JobApplication::getStatus, ApplicationStatus.ACCEPTED.name()));
        return n != null && n > 0;
    }

    @Override
    public String employmentStatus(Long userId) {
        return deriveStatus(listByUser(userId));
    }

    @Override
    public Set<Long> employedUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptySet();
        }
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                        .select(JobApplication::getUserId)
                        .in(JobApplication::getUserId, userIds)
                        .eq(JobApplication::getStatus, ApplicationStatus.ACCEPTED.name()))
                .stream().map(JobApplication::getUserId).collect(Collectors.toSet());
    }

    @Override
    public Map<Long, String> employmentStatusByUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<JobApplication>> byUser = applicationMapper.selectList(
                        new LambdaQueryWrapper<JobApplication>().in(JobApplication::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(JobApplication::getUserId));
        Map<Long, String> result = new HashMap<>();
        for (Long uid : userIds) {
            result.put(uid, deriveStatus(byUser.get(uid)));
        }
        return result;
    }

    @Override
    public long countEmployedInTenant() {
        return applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                        .select(JobApplication::getUserId)
                        .eq(JobApplication::getStatus, ApplicationStatus.ACCEPTED.name()))
                .stream().map(JobApplication::getUserId).distinct().count();
    }

    /** 从一名学生的全部投递派生就业状态 */
    private static String deriveStatus(List<JobApplication> apps) {
        if (apps == null || apps.isEmpty()) {
            return "IDLE";
        }
        if (apps.stream().anyMatch(a -> ApplicationStatus.ACCEPTED.name().equals(a.getStatus()))) {
            return "EMPLOYED";
        }
        if (apps.stream().anyMatch(a -> ApplicationStatus.OFFER.name().equals(a.getStatus()))) {
            return "OFFERED";
        }
        return "SEEKING";
    }
}
