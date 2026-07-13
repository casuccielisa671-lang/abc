package com.occupation.recommend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.recommend.dto.ProfileSaveDTO;
import com.occupation.recommend.entity.SysStudentProfile;

import java.util.Collection;
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

    /**
     * 分页查询当前租户内的学生画像（教师端学生列表 / HR 端人才浏览）
     *
     * @param keyword        专业或技能模糊匹配，空表示不筛选
     * @param educationLevel 学历筛选，空表示不筛选
     */
    Page<SysStudentProfile> pageProfiles(String keyword, String educationLevel, int pageNum, int pageSize);

    /**
     * 分页查询画像，并按可见学生集合 {@code restrictUserIds} 限定（教师端范围过滤）。
     *
     * @param restrictUserIds 可见学生 userId 集合：{@code null}=不限制（管理员）；
     *                        空集=返回 0 条；非空=仅返回 userId 在集合内的画像
     */
    Page<SysStudentProfile> pageProfiles(String keyword, String educationLevel,
                                         Collection<Long> restrictUserIds, int pageNum, int pageSize);

    /**
     * 指定学生集合的画像（教师端范围内的概览统计 / 导出）。
     *
     * @param userIds {@code null}=当前租户全部（等价 {@link #listAll()}）；空集=空列表；
     *                非空=仅这些 userId 的画像
     */
    List<SysStudentProfile> listByUserIds(Collection<Long> userIds);
}
