package com.occupation.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.report.dto.StudentReportDTO;
import com.occupation.report.entity.ReportRecord;
import com.occupation.report.vo.StudentAiReportVO;

/**
 * 学生个人 AI 分析报告服务。
 * <p>
 * 综合本人 画像 + 简历 + 推荐匹配 + 技能缺口 + 市场热点，由 AI 写一段个性化求职分析
 * （AI 关闭则规则化兜底）。预览可多轮改（前端持对话历史），定稿才落库为个人报告。
 *
 * @author occupation-team
 */
public interface StudentAiReportService {

    /** 预览/多轮改：生成正文但不落库 */
    StudentAiReportVO preview(Long userId, StudentReportDTO dto);

    /** 定稿保存：落库为该学生的个人报告 + 导出文件 */
    ReportRecord save(Long userId, StudentReportDTO dto);

    /** 我的报告列表（仅当前学生自己的） */
    Page<ReportRecord> pageMy(Long userId, int pageNum, int pageSize);
}
