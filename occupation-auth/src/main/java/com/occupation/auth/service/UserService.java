package com.occupation.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.dto.UserSaveDTO;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.vo.BatchImportVO;
import com.occupation.auth.vo.UserVO;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

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

    /**
     * 批量按用户 ID 查询：userId → SysUser。
     * <p>
     * 供 recommend 模块一次性补齐学生姓名/学号，避免逐条查库的 N+1。
     * userIds 为空时返回空 Map。
     */
    Map<Long, SysUser> mapByIds(Collection<Long> userIds);

    /**
     * 统计当前租户内指定角色的用户数（教师端概览：学生总数）。
     * 多租户插件自动限定范围。
     */
    long countByRole(String role);

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

    /**
     * Excel 批量导入用户到当前租户。
     * <p>
     * 全量校验通过才写库；任何一行不合法都整体拒绝并返回逐行错误报告。
     * Excel 未填写密码的账号使用配置的初始密码。
     *
     * @param excel .xlsx 输入流，调用方负责关闭
     */
    BatchImportVO batchImport(InputStream excel);
}
