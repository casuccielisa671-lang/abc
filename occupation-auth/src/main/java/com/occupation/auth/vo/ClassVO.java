package com.occupation.auth.vo;

import com.occupation.auth.entity.SysClass;
import lombok.Data;

import java.io.Serializable;

/**
 * 班级视图 — 班级信息 + 在册学生数（管理端班级列表）。
 *
 * @author occupation-team
 */
@Data
public class ClassVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String major;
    private Integer enrollYear;
    private String className;
    /** 统一命名：专业-入学年级-班级 */
    private String code;
    private Integer status;
    /** 在册学生数 */
    private long studentCount;

    public static ClassVO of(SysClass c, long studentCount) {
        ClassVO vo = new ClassVO();
        vo.id = c.getId();
        vo.major = c.getMajor();
        vo.enrollYear = c.getEnrollYear();
        vo.className = c.getClassName();
        vo.code = c.getCode();
        vo.status = c.getStatus();
        vo.studentCount = studentCount;
        return vo;
    }
}
