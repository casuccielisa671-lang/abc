package com.occupation.analysis.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 推荐职业（首页左侧卡片）
 */
@Data
public class RecommendJobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobName;
    /** 平台内该职业名称对应的岗位数量 */
    private Long jobCount;
}
