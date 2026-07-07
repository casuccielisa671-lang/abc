package com.occupation.recommend.service;

import com.occupation.recommend.dto.ProfileSaveDTO;
import com.occupation.recommend.entity.SysStudentProfile;

import java.util.List;

/**
 * 学生画像服务 — 推荐匹配的输入数据
 *
 * @author occupation-team
 */
public interface StudentProfileService {

    /**
     * 查询指定学生的画像，不存在返回 null
     */
    SysStudentProfile getByUserId(Long userId);

    /**
     * 保存/更新画像（一个学生一条，按 userId upsert）
     */
    void saveProfile(Long userId, ProfileSaveDTO dto);

    /**
     * 当前租户内全部学生画像（教师端统计 / 每日推送任务使用）
     */
    List<SysStudentProfile> listAll();
}
