package com.occupation.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.dto.UserSaveDTO;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.mapper.SysUserMapper;
import com.occupation.auth.service.UserService;
import com.occupation.auth.vo.BatchImportVO;
import com.occupation.auth.vo.UserVO;
import com.occupation.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author occupation-team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /** Excel 导入时未填写密码的账号使用的初始密码 */
    @Value("${app.user.default-password:Occupation@123}")
    private String defaultPassword;

    @Override
    public SysUser getByUsername(String username, Long tenantId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username)
               .eq(SysUser::getTenantId, tenantId);
        return sysUserMapper.selectOne(wrapper);
    }

    @Override
    public SysUser getById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    @Override
    public Map<Long, SysUser> mapByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    @Override
    public long countByRole(String role) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, role));
        return count == null ? 0L : count;
    }

    @Override
    public Page<UserVO> pageUsers(String role, String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(role), SysUser::getRole, role);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                              .or()
                              .like(SysUser::getRealName, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUser(UserSaveDTO dto) {
        if (dto.getId() == null) {
            // 新增：用户名在租户内唯一 + 密码必填
            if (StrUtil.isBlank(dto.getPassword())) {
                throw new BizException("新增用户时密码不能为空");
            }
            SysUser user = new SysUser();
            user.setUsername(dto.getUsername());
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            user.setRole(dto.getRole());
            user.setRealName(dto.getRealName());
            user.setPhone(dto.getPhone());
            user.setEmail(dto.getEmail());
            user.setStatus(1);
            sysUserMapper.insert(user);
        } else {
            // 编辑：密码留空不修改
            SysUser user = sysUserMapper.selectById(dto.getId());
            if (user == null) {
                throw new BizException("用户不存在");
            }
            user.setRole(dto.getRole());
            user.setRealName(dto.getRealName());
            user.setPhone(dto.getPhone());
            user.setEmail(dto.getEmail());
            if (StrUtil.isNotBlank(dto.getPassword())) {
                user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            }
            sysUserMapper.updateById(user);
        }
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    // ==================== Excel 批量导入 ====================

    /** Excel 表头 → 字段。角色列同时接受中文标签与英文枚举 */
    private static final String COL_USERNAME = "用户名";
    private static final String COL_REAL_NAME = "姓名";
    private static final String COL_ROLE = "角色";
    private static final String COL_PHONE = "手机号";
    private static final String COL_EMAIL = "邮箱";
    private static final String COL_PASSWORD = "密码";

    /** 导入模板的列顺序 */
    public static final List<String> IMPORT_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            COL_USERNAME, COL_REAL_NAME, COL_ROLE, COL_PHONE, COL_EMAIL, COL_PASSWORD));

    private static final Map<String, String> ROLE_ALIASES = new HashMap<>();
    static {
        ROLE_ALIASES.put("STUDENT", "STUDENT");
        ROLE_ALIASES.put("TEACHER", "TEACHER");
        ROLE_ALIASES.put("ADMIN", "ADMIN");
        ROLE_ALIASES.put("HR", "HR");
        ROLE_ALIASES.put("学生", "STUDENT");
        ROLE_ALIASES.put("教师", "TEACHER");
        ROLE_ALIASES.put("老师", "TEACHER");
        ROLE_ALIASES.put("管理员", "ADMIN");
        ROLE_ALIASES.put("企业HR", "HR");
        ROLE_ALIASES.put("企业 HR", "HR");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchImportVO batchImport(InputStream excel) {
        List<Map<String, Object>> rows;
        try (ExcelReader reader = ExcelUtil.getReader(excel)) {
            rows = reader.readAll();
        } catch (Exception e) {
            throw new BizException("Excel 解析失败，请确认文件为 .xlsx 格式：" + e.getMessage());
        }

        BatchImportVO report = new BatchImportVO();
        report.setTotal(rows.size());
        if (rows.isEmpty()) {
            throw new BizException("Excel 中没有数据行");
        }

        // 先把本租户已存在的用户名取出来，避免逐行查库
        Set<String> existing = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().select(SysUser::getUsername))
                .stream().map(SysUser::getUsername).collect(Collectors.toSet());

        Set<String> seenInFile = new HashSet<>();
        List<SysUser> pending = new ArrayList<>(rows.size());
        boolean usedDefaultPassword = false;

        for (int i = 0; i < rows.size(); i++) {
            // +2：Excel 第 1 行是表头，数据从第 2 行开始
            int rowNum = i + 2;
            Map<String, Object> row = rows.get(i);

            String username = str(row.get(COL_USERNAME));
            if (StrUtil.isBlank(username)) {
                report.addError(rowNum, username, "用户名为空");
                continue;
            }
            if (!seenInFile.add(username)) {
                report.addError(rowNum, username, "Excel 内用户名重复");
                continue;
            }
            if (existing.contains(username)) {
                report.addError(rowNum, username, "该用户名在本校已存在");
                continue;
            }

            String roleRaw = str(row.get(COL_ROLE));
            String role = ROLE_ALIASES.get(roleRaw == null ? "" : roleRaw.trim().toUpperCase());
            if (role == null) {
                role = ROLE_ALIASES.get(roleRaw == null ? "" : roleRaw.trim());
            }
            if (role == null) {
                report.addError(rowNum, username, "角色无效（应为 学生/教师/管理员/HR 或 STUDENT/TEACHER/ADMIN/HR）");
                continue;
            }

            String password = str(row.get(COL_PASSWORD));
            if (StrUtil.isBlank(password)) {
                password = defaultPassword;
                usedDefaultPassword = true;
            }

            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(role);
            user.setRealName(str(row.get(COL_REAL_NAME)));
            user.setPhone(str(row.get(COL_PHONE)));
            user.setEmail(str(row.get(COL_EMAIL)));
            user.setStatus(1);
            pending.add(user);
        }

        // 有任何一行不合法就整体拒绝：宁可让管理员改好再传，也不留下半份名单
        if (report.hasErrors()) {
            report.setImported(0);
            return report;
        }

        for (SysUser user : pending) {
            sysUserMapper.insert(user);
        }
        report.setImported(pending.size());
        report.setDefaultPassword(usedDefaultPassword ? defaultPassword : null);
        log.info("批量导入用户完成: {} 条", pending.size());
        return report;
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
