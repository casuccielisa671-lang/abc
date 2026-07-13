package com.occupation.report.vo;

import com.occupation.report.entity.ReportRecord;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生「收到的报告」出参 —— 管理员下发的市场/就业报告。
 * <p>
 * 在报告本体之外补两个学生视角的字段：{@code source}（广播 or 定向下发）、{@code read}（是否已读）。
 * 市场行业报告走广播口径（{@code source=BROADCAST}），不做未读追踪，恒为已读。
 *
 * @author occupation-team
 */
@Data
public class ReceivedReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    /** 报告大类：MARKET=市场行业 / EMPLOYMENT=学生就业 */
    private String category;
    private String fileType;
    private LocalDateTime createTime;

    /** 来源：BROADCAST=市场报告全体可见 / DELIVERED=管理员定向发给我 */
    private String source;
    /** 是否已读（广播报告恒为 true） */
    private boolean read;

    public static ReceivedReportVO of(ReportRecord r, String source, boolean read) {
        ReceivedReportVO vo = new ReceivedReportVO();
        vo.id = r.getId();
        vo.name = r.getName();
        vo.category = r.getCategory();
        vo.fileType = r.getFileType();
        vo.createTime = r.getCreateTime();
        vo.source = source;
        vo.read = read;
        return vo;
    }
}
