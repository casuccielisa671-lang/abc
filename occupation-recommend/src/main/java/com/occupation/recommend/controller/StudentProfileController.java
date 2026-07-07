package com.occupation.recommend.controller;

import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.dto.ProfileSaveDTO;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学生画像接口（学生端）
 *
 * @author occupation-team
 */
@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileController {

    private final StudentProfileService profileService;
    private final BehaviorService behaviorService;

    /** 查看自己的画像（未填写返回 null，前端引导填写） */
    @GetMapping
    public Result<SysStudentProfile> getMyProfile() {
        return Result.ok(profileService.getByUserId(UserContextHolder.getUserId()));
    }

    /** 保存/更新画像 */
    @PutMapping
    public Result<Void> saveProfile(@RequestBody @Validated ProfileSaveDTO dto) {
        profileService.saveProfile(UserContextHolder.getUserId(), dto);
        return Result.ok();
    }

    /** 个人求职统计（浏览/收藏/投递计数） */
    @GetMapping("/stats")
    public Result<Map<String, Long>> getMyStats() {
        return Result.ok(behaviorService.countByAction(UserContextHolder.getUserId()));
    }
}
