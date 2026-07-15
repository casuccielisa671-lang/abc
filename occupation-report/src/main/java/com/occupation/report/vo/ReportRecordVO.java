package com.occupation.report.vo;

import com.occupation.report.entity.ReportRecord;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报告记录出参 —— 报告名与大类直接来自记录本体（已无模板概念）。
 *
 * @author occupation-team
 */
@Data
public class ReportRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 报告名称 */
    private String name;
    /** 报告大类：MARKET=市场行业 / EMPLOYMENT=学生就业 */
    private String category;

    /** 文件类型：PDF / WORD / HTML */
    private String fileType;
    /** 状态：PENDING / GENERATING / SUCCESS / FAILED */
    private String status;
    private String errorMsg;
    private LocalDateTime createTime;

    /** 可见性：PUBLIC=全体可见 / SELF=仅自己可见。市场报告用它展示可见状态；就业报告看 deliveredCount */
    private String visibility;

    /**
     * 已下发的学生人数（仅 EMPLOYMENT 报告有意义）。
     * >0 表示当前对这些学生可见；0 表示仅自己可见（未发布或已撤回）。
     * MARKET 报告为广播，此字段为 null，前端按「全体可见」展示。
     */
    private Long deliveredCount;

    public static ReportRecordVO of(ReportRecord r) {
        ReportRecordVO vo = new ReportRecordVO();
        vo.id = r.getId();
        vo.name = r.getName();
        vo.category = r.getCategory();
        vo.fileType = r.getFileType();
        vo.status = r.getStatus();
        vo.errorMsg = r.getErrorMsg();
        vo.createTime = r.getCreateTime();
        vo.visibility = r.getVisibility();
        return vo;
    }
}
