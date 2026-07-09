package com.occupation.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.occupation.report.dto.GenerateReportDTO;
import com.occupation.report.entity.ReportRecord;

/**
 * 报告生成引擎 — 报告模块核心
 * <p>
 * 生成流程（六步）：
 * <pre>
 * ① 读模板(report_template) → ② 调 AnalysisService 取分析数据
 * → ③ 调 AiSummaryService 生成智能摘要 → ④ Freemarker 渲染 HTML
 * → ⑤ 按 fileType 导出（HTML 直存 / PDF / WORD）→ ⑥ 落盘 + 写 report_record
 * </pre>
 *
 * @author occupation-team
 */
public interface ReportGeneratorService {

    /**
     * 生成一份报告
     *
     * @return 生成的报告记录（含 fileUrl）
     */
    ReportRecord generate(GenerateReportDTO dto);

    /** 报告记录分页列表（当前租户） */
    Page<ReportRecord> pageRecords(int pageNum, int pageSize);

    /**
     * 当前租户下最新一份生成成功的报告；没有则返回 null。
     * <p>
     * 供开放 API 的报告摘要接口使用。租户范围由多租户插件按
     * {@code TenantContextHolder} 注入，调用前必须已设置租户上下文。
     */
    ReportRecord latestSuccess();

    /** 按 ID 取报告记录（下载接口用它拼出带扩展名的文件名）；不存在返回 null */
    ReportRecord getRecord(Long recordId);

    /**
     * 读取报告文件内容（供下载接口返回）
     *
     * @return 文件字节流
     */
    byte[] loadReportFile(Long recordId);

    /** 删除报告记录及文件 */
    void deleteRecord(Long recordId);
}
