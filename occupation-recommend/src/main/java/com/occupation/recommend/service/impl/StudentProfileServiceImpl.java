package com.occupation.recommend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.recommend.dto.ProfileSaveDTO;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.mapper.SysStudentProfileMapper;
import com.occupation.recommend.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 学生画像服务实现 — sys_student_profile 表（多租户自动隔离）
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private final SysStudentProfileMapper profileMapper;

    @Override
    public SysStudentProfile getByUserId(Long userId) {
        return profileMapper.selectOne(
                new LambdaQueryWrapper<SysStudentProfile>().eq(SysStudentProfile::getUserId, userId));
    }

    @Override
    public void saveProfile(Long userId, ProfileSaveDTO dto) {
        SysStudentProfile profile = getByUserId(userId);
        boolean isNew = profile == null;
        if (isNew) {
            profile = new SysStudentProfile();
            profile.setUserId(userId);
        }
        profile.setMajor(dto.getMajor());
        profile.setSkills(dto.getSkills() == null ? "[]" : dto.getSkills());
        profile.setExpectedCity(dto.getExpectedCity());
        profile.setExpectedIndustry(dto.getExpectedIndustry());
        profile.setExpectedSalaryMin(dto.getExpectedSalaryMin());
        profile.setExpectedSalaryMax(dto.getExpectedSalaryMax());
        profile.setEducationLevel(dto.getEducationLevel());
        if (dto.getAvatarUrl() != null) {
            profile.setAvatarUrl(dto.getAvatarUrl());
        }

        if (isNew) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
        log.info("学生画像已保存: userId={}, isNew={}", userId, isNew);
    }

    @Override
    public List<SysStudentProfile> listAll() {
        return profileMapper.selectList(null);
    }

    @Override
    public Page<SysStudentProfile> pageProfiles(String keyword, String educationLevel,
                                                int pageNum, int pageSize) {
        return pageProfiles(keyword, educationLevel, null, pageNum, pageSize);
    }

    @Override
    public Page<SysStudentProfile> pageProfiles(String keyword, String educationLevel,
                                                Collection<Long> restrictUserIds, int pageNum, int pageSize) {
        // 有限定集合但为空 → 直接返回空页，避免拼出 IN () 或退化成全查
        if (restrictUserIds != null && restrictUserIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }
        LambdaQueryWrapper<SysStudentProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(educationLevel), SysStudentProfile::getEducationLevel, educationLevel);
        if (restrictUserIds != null) {
            wrapper.in(SysStudentProfile::getUserId, restrictUserIds);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysStudentProfile::getMajor, keyword)
                              .or()
                              .like(SysStudentProfile::getSkills, keyword));
        }
        wrapper.orderByAsc(SysStudentProfile::getUserId);
        return profileMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SysStudentProfile> listByUserIds(Collection<Long> userIds) {
        if (userIds == null) {
            return listAll();
        }
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return profileMapper.selectList(new LambdaQueryWrapper<SysStudentProfile>()
                .in(SysStudentProfile::getUserId, userIds));
    }
}
