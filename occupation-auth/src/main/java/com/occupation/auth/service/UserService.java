package com.occupation.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.dto.UserSaveDTO;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.vo.UserVO;

/**
 * 用户服务接口
 * <p>
 * 除登录所需的查询外，提供管理员用户管理能力（UserController 使用），
 * 以及供 recommend 模块跨模块查询学生列表（教师端）。
 *
 * @author occupation-team
 */
public interface UserService {

    /**
     * 根据用户名和租户 ID 查询用户（联合唯一键）
     */
    SysUser getByUsername(String username, Long tenantId);

    /**
     * 根据用户 ID 查询
     */
    SysUser getById(Long userId);

    // ========== 管理员用户管理（P3 用户管理页面） ==========

    /**
     * 分页查询用户列表（当前租户内，多租户插件自动过滤）
     *
     * @param role     角色筛选，null 表示不筛选
     * @param keyword  用户名/姓名模糊搜索，null 表示不搜索
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Page<UserVO> pageUsers(String role, String keyword, int pageNum, int pageSize);

    /**
     * 新增或编辑用户
     * <p>
     * 新增：密码必填，BCrypt 加密后存储，tenant_id 自动注入。
     * 编辑：密码留空则不修改。
     */
    void saveUser(UserSaveDTO dto);

    /**
     * 启用/禁用用户（禁用后其 Token 在下次请求校验时拒绝）
     *
     * @param status 1=启用 0=禁用
     */
    void updateStatus(Long userId, Integer status);
}
