package com.occupation.report.vo;

import com.occupation.report.entity.ReportRecord;
import com.occupation.report.entity.ReportTemplate;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报告记录出参 — 记录本体 + 关联模板的名称与类型
 * <p>
 * report_record 只存 template_id，直接返回实体的话前端列表里
 * 「模板名称 / 报告类型」两列永远是空的。
 *
 * @author occupation-team
 */
@Data
public class ReportRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    /** 模板名称；模板已删除时为 null */
    private String templateName;
    /** 报告类型：MONTHLY / QUARTERLY / YEARLY */
    private String type;

    /** 文件类型：PDF / WORD / HTML */
    private String fileType;
    /** 状态：PENDING / GENERATING / SUCCESS / FAILED */
    private String status;
    private String errorMsg;
    private LocalDateTime createTime;

    public static ReportRecordVO of(ReportRecord r, ReportTemplate template) {
        ReportRecordVO vo = new ReportRecordVO();
        vo.id = r.getId();
        vo.templateId = r.getTemplateId();
        vo.fileType = r.getFileType();
        vo.status = r.getStatus();
        vo.errorMsg = r.getErrorMsg();
        vo.createTime = r.getCreateTime();
        if (template != null) {
            vo.templateName = template.getName();
            vo.type = template.getType();
        }
        return vo;
    }
}
