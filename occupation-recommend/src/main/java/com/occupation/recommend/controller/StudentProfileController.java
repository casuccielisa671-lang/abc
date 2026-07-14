package com.occupation.recommend.controller;

import com.occupation.common.config.UserContextHolder;
import com.occupation.common.result.Result;
import com.occupation.recommend.dto.ProfileSaveDTO;
import com.occupation.recommend.entity.SysStudentProfile;
import com.occupation.recommend.service.BehaviorService;
import com.occupation.recommend.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 学生画像接口（学生端）
 *
 * @author occupation-team
 */
@Slf4j
@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileController {

    private final StudentProfileService profileService;
    private final BehaviorService behaviorService;

    /** 证件照存储目录，默认 ./data/avatars */
    @Value("${app.avatar.storage-path:./data/avatars}")
    private String avatarStoragePath;

    /** 查看自己的画像（未填写返回 null，前端引导填写） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public Result<SysStudentProfile> getMyProfile() {
        return Result.ok(profileService.getByUserId(UserContextHolder.getUserId()));
    }

    /** 保存/更新画像 */
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping
    public Result<Void> saveProfile(@RequestBody @Validated ProfileSaveDTO dto) {
        profileService.saveProfile(UserContextHolder.getUserId(), dto);
        return Result.ok();
    }

    /** 上传证件照 */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅支持图片格式");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过 5MB");
        }

        try {
            // 按日期分目录存储
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Path uploadDir = Paths.get(avatarStoragePath, dateDir);
            Files.createDirectories(uploadDir);

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path targetPath = uploadDir.resolve(fileName);
            file.transferTo(targetPath.toFile());

            // 存储相对路径
            String relativePath = "/avatars/" + dateDir + "/" + fileName;
            String avatarUrl = "/api" + relativePath;

            // 更新画像中的证件照URL
            SysStudentProfile profile = profileService.getByUserId(UserContextHolder.getUserId());
            if (profile != null) {
                profile.setAvatarUrl(avatarUrl);
                ProfileSaveDTO dto = new ProfileSaveDTO();
                dto.setMajor(profile.getMajor());
                dto.setSkills(profile.getSkills());
                dto.setExpectedCity(profile.getExpectedCity());
                dto.setExpectedIndustry(profile.getExpectedIndustry());
                dto.setExpectedSalaryMin(profile.getExpectedSalaryMin());
                dto.setExpectedSalaryMax(profile.getExpectedSalaryMax());
                dto.setEducationLevel(profile.getEducationLevel());
                dto.setAvatarUrl(avatarUrl);
                profileService.saveProfile(UserContextHolder.getUserId(), dto);
            }

            log.info("证件照上传成功: userId={}, path={}", UserContextHolder.getUserId(), relativePath);
            return Result.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IOException e) {
            log.error("证件照上传失败: userId={}", UserContextHolder.getUserId(), e);
            return Result.error(500, "上传失败，请重试");
        }
    }

    /** 个人求职统计（浏览/收藏/投递计数） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/stats")
    public Result<Map<String, Long>> getMyStats() {
        return Result.ok(behaviorService.countByAction(UserContextHolder.getUserId()));
    }
}
