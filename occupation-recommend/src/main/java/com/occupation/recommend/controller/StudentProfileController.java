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
import java.nio.file.StandardCopyOption;
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
            // 不用 file.transferTo(File)：它对相对路径按 servlet 临时目录解析，与上面 createDirectories
            // 建的目录对不上会 FileNotFound。改用 Files.copy，路径解析与 createDirectories 一致。
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 存储相对路径
            String relativePath = "/avatars/" + dateDir + "/" + fileName;
            String avatarUrl = "/api" + relativePath;

            // 更新画像中的证件照URL；记录旧 URL，保存后删掉旧文件（每人最多留 1 张，避免磁盘越堆越多）
            SysStudentProfile profile = profileService.getByUserId(UserContextHolder.getUserId());
            String oldAvatarUrl = profile != null ? profile.getAvatarUrl() : null;
            if (profile != null) {
                ProfileSaveDTO dto = toSaveDto(profile);
                dto.setAvatarUrl(avatarUrl);
                profileService.saveProfile(UserContextHolder.getUserId(), dto);
            }
            deleteAvatarFile(oldAvatarUrl);

            log.info("证件照上传成功: userId={}, path={}", UserContextHolder.getUserId(), relativePath);
            return Result.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IOException e) {
            log.error("证件照上传失败: userId={}", UserContextHolder.getUserId(), e);
            return Result.error(500, "上传失败，请重试");
        }
    }

    /** 删除证件照：清空画像 avatar_url 并删除磁盘文件（真正删，不只是清前端字段） */
    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/avatar")
    public Result<Void> deleteAvatar() {
        SysStudentProfile profile = profileService.getByUserId(UserContextHolder.getUserId());
        if (profile != null && profile.getAvatarUrl() != null) {
            String old = profile.getAvatarUrl();
            profileService.clearAvatar(UserContextHolder.getUserId());
            deleteAvatarFile(old);
        }
        return Result.ok();
    }

    /** 复制画像字段到 DTO（保存时不能漏字段，否则会把其它字段清空） */
    private ProfileSaveDTO toSaveDto(SysStudentProfile p) {
        ProfileSaveDTO dto = new ProfileSaveDTO();
        dto.setMajor(p.getMajor());
        dto.setSkills(p.getSkills());
        dto.setExpectedCity(p.getExpectedCity());
        dto.setExpectedIndustry(p.getExpectedIndustry());
        dto.setExpectedSalaryMin(p.getExpectedSalaryMin());
        dto.setExpectedSalaryMax(p.getExpectedSalaryMax());
        dto.setEducationLevel(p.getEducationLevel());
        dto.setAvatarUrl(p.getAvatarUrl());
        return dto;
    }

    /** 把 /api/avatars/xxx 映射回磁盘文件并删除；非 avatars 路径或不存在则忽略 */
    private void deleteAvatarFile(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/api/avatars/")) {
            return;
        }
        try {
            String rel = avatarUrl.substring("/api/avatars/".length());
            Files.deleteIfExists(Paths.get(avatarStoragePath, rel));
        } catch (Exception e) {
            log.warn("删除旧证件照文件失败(忽略): {}", avatarUrl, e);
        }
    }

    /** 个人求职统计（浏览/收藏/投递计数） */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/stats")
    public Result<Map<String, Long>> getMyStats() {
        return Result.ok(behaviorService.countByAction(UserContextHolder.getUserId()));
    }
}
