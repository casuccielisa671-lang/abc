package com.occupation.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.auth.dto.UserSaveDTO;
import com.occupation.auth.entity.SysUser;
import com.occupation.auth.mapper.SysUserMapper;
import com.occupation.auth.service.UserService;
import com.occupation.auth.vo.UserVO;
import com.occupation.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author occupation-team
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

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
