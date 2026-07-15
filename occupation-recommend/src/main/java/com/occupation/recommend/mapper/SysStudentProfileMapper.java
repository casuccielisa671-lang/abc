package com.occupation.recommend.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.occupation.recommend.entity.SysStudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生画像 Mapper
 *
 * @author occupation-team
 */
@Mapper
public interface SysStudentProfileMapper extends BaseMapper<SysStudentProfile> {

    /**
     * 跨租户取所有非空 avatar_url（启动时对账清理孤儿证件照文件用）。
     * <p>方法级 {@code @InterceptorIgnore(tenantLine)} 只对本方法关租户过滤，
     * 不影响其它画像查询的租户隔离；启动时无租户上下文，普通查询会被过滤成 0 行。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT avatar_url FROM sys_student_profile WHERE avatar_url IS NOT NULL")
    List<String> selectAllAvatarUrls();
}
