package com.occupation.recommend.vo;

import lombok.Data;

import java.util.List;

/**
 * 学生预警看板 VO
 *
 * @author occupation-team
 */
@Data
public class StudentAlertVO {

    /** 预警列表 */
    private List<AlertItem> alerts;

    /** 统计汇总 */
    private AlertSummary summary;

    @Data
    public static class AlertItem {
        private Long id;
        private Long userId;
        private String studentName;
        private String className;
        /** 预警类型: profile / inactive / skill_gap */
        private String type;
        /** 严重程度: high / medium / low */
        private String severity;
        private String reason;
        private String detail;
        private String suggestion;
        private String date;
    }

    @Data
    public static class AlertSummary {
        private int highCount;
        private int mediumCount;
        private int lowCount;
        private int totalStudents;
    }
}
