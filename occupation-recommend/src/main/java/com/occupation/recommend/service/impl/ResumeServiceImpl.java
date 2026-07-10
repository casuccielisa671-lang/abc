package com.occupation.recommend.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.occupation.recommend.dto.ResumeSaveDTO;
import com.occupation.recommend.entity.StudentResume;
import com.occupation.recommend.mapper.StudentResumeMapper;
import com.occupation.recommend.model.ResumeSection;
import com.occupation.recommend.service.ResumeService;
import com.occupation.recommend.vo.ResumeReviewVO;
import com.occupation.recommend.vo.ResumeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生简历服务实现 — student_resume 表（多租户自动隔离）
 * <p>
 * JSON 序列化只在本层发生：库里存 JSON 字符串，HTTP 层收发结构化数组。
 * 解析失败一律降级为空列表 —— 简历是展示数据，一个脏字段不该让整页 500。
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final StudentResumeMapper resumeMapper;

    @Override
    public StudentResume findByUserId(Long userId) {
        return resumeMapper.selectOne(
                new LambdaQueryWrapper<StudentResume>().eq(StudentResume::getUserId, userId));
    }

    @Override
    public ResumeVO getByUserId(Long userId) {
        return toVO(findByUserId(userId));
    }

    @Override
    public void save(Long userId, ResumeSaveDTO dto) {
        StudentResume resume = findByUserId(userId);
        boolean isNew = resume == null;
        if (isNew) {
            resume = new StudentResume();
            resume.setUserId(userId);
        }
        resume.setContactPhone(blankToNull(dto.getContactPhone()));
        resume.setContactEmail(blankToNull(dto.getContactEmail()));
        resume.setJobIntention(blankToNull(dto.getJobIntention()));
        resume.setSelfIntro(blankToNull(dto.getSelfIntro()));
        resume.setEducations(toJson(dto.getEducations()));
        resume.setProjects(toJson(dto.getProjects()));
        resume.setInternships(toJson(dto.getInternships()));
        resume.setHonors(toJson(dto.getHonors()));

        if (isNew) {
            resumeMapper.insert(resume);
        } else {
            resumeMapper.updateById(resume);
        }
        log.info("学生简历已保存: userId={}, isNew={}", userId, isNew);
    }

    @Override
    public void saveAiReview(Long userId, String reviewJson) {
        StudentResume resume = findByUserId(userId);
        if (resume == null) {
            return;
        }
        resume.setAiReview(reviewJson);
        resume.setAiReviewTime(LocalDateTime.now());
        resumeMapper.updateById(resume);
    }

    @Override
    public Set<Long> filterUsersWithResume(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 只取 user_id 一列，别把八个 TEXT 字段捞回来
        return resumeMapper.selectList(new LambdaQueryWrapper<StudentResume>()
                        .select(StudentResume::getUserId)
                        .in(StudentResume::getUserId, userIds))
                .stream()
                .map(StudentResume::getUserId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<Long, StudentResume> mapByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return resumeMapper.selectList(new LambdaQueryWrapper<StudentResume>()
                        .in(StudentResume::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(StudentResume::getUserId, Function.identity(), (a, b) -> a));
    }

    /** 实体 → VO，把 JSON 列解析成数组 */
    private ResumeVO toVO(StudentResume r) {
        ResumeVO vo = new ResumeVO();
        if (r == null) {
            vo.setExists(false);
            vo.setEducations(Collections.emptyList());
            vo.setProjects(Collections.emptyList());
            vo.setInternships(Collections.emptyList());
            vo.setHonors(Collections.emptyList());
            return vo;
        }
        vo.setExists(true);
        vo.setContactPhone(r.getContactPhone());
        vo.setContactEmail(r.getContactEmail());
        vo.setJobIntention(r.getJobIntention());
        vo.setSelfIntro(r.getSelfIntro());
        vo.setEducations(parseList(r.getEducations(), ResumeSection.Education.class));
        vo.setProjects(parseList(r.getProjects(), ResumeSection.Project.class));
        vo.setInternships(parseList(r.getInternships(), ResumeSection.Internship.class));
        vo.setHonors(parseList(r.getHonors(), String.class));
        vo.setAiReview(parseReview(r.getAiReview()));
        vo.setAiReviewTime(r.getAiReviewTime());
        vo.setUpdateTime(r.getUpdateTime());
        return vo;
    }

    private ResumeReviewVO parseReview(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, ResumeReviewVO.class);
        } catch (Exception e) {
            log.warn("AI 诊断结果解析失败，忽略: {}", e.getMessage());
            return null;
        }
    }

    private <T> List<T> parseList(String json, Class<T> type) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<T> list = JSON.parseArray(json, type);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            log.warn("简历字段 JSON 解析失败({})，按空列表处理: {}", type.getSimpleName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 空列表也序列化成 "[]"，读的时候就不用区分 null 和空 */
    private String toJson(List<?> list) {
        return JSON.toJSONString(list == null ? Collections.emptyList() : list);
    }

    private String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
