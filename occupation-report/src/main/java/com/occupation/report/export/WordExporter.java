package com.occupation.report.export;

import com.occupation.analysis.vo.DashboardVO;
import com.occupation.common.exception.BizException;
import com.occupation.recommend.vo.EmploymentReportData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Word 导出器 — Apache POI 生成 .docx
 * <p>
 * 结构化导出：标题 → 生成时间 → 智能摘要 → 各维度数据表格（XWPFTable）。
 * 表格数据直接取自 {@link DashboardVO}，与 PDF/HTML 走的是同一份分析结果，口径一致。
 *
 * @author occupation-team
 */
@Slf4j
@Component
public class WordExporter {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Word 里显式指定中文字体，避免默认西文字体渲染中文时字形缺失 */
    private static final String CJK_FONT = "SimSun";

    /**
     * 结构化导出报告
     *
     * @param title     报告标题
     * @param aiSummary 智能摘要正文
     * @param dashboard 分析数据（各维度排行）
     */
    public byte[] export(String title, String aiSummary, DashboardVO dashboard) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            writeTitle(doc, title);
            writeMeta(doc, "生成时间：" + LocalDateTime.now().format(TIME_FMT));

            writeHeading(doc, "一、智能摘要");
            writeBody(doc, aiSummary == null || aiSummary.isEmpty() ? "（无摘要）" : aiSummary);

            writeHeading(doc, "二、行业岗位需求 Top10");
            writeDimensionTable(doc, "行业", "岗位数", dashboard.getIndustryTop());

            writeHeading(doc, "三、热门技能 Top20");
            writeDimensionTable(doc, "技能", "热度", dashboard.getSkillHot());

            writeHeading(doc, "四、城市分布");
            writeDimensionTable(doc, "城市", "岗位数", dashboard.getCityDist());

            writeHeading(doc, "五、学历分布");
            writeDimensionTable(doc, "学历", "岗位数", dashboard.getEducationDist());

            writeHeading(doc, "六、趋势（按月）");
            writeTrendTable(doc, dashboard.getTrend());

            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Word 导出失败", e);
            throw new BizException("Word 导出失败: " + e.getMessage());
        }
    }

    /**
     * 结构化导出学生就业数据报告（EMPLOYMENT 类）。数据取自 {@link EmploymentReportData}，
     * 与 PDF/HTML 走同一份聚合结果，口径一致。
     */
    public byte[] exportEmployment(String title, String aiSummary, EmploymentReportData data) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            writeTitle(doc, title);
            writeMeta(doc, "范围：" + data.getScopeLabel()
                    + "    生成时间：" + LocalDateTime.now().format(TIME_FMT));

            writeHeading(doc, "一、智能摘要");
            writeBody(doc, aiSummary == null || aiSummary.isEmpty() ? "（无摘要）" : aiSummary);

            writeHeading(doc, "二、总览");
            writeBody(doc, String.format(
                    "学生 %d 人，已填画像 %d 人；累计投递 %d 次（%d 人投递），已录用 %d，OFFER 率 %.1f%%。",
                    data.getStudentCount(), data.getProfiledCount(), data.getApplicationCount(),
                    data.getAppliedStudentCount(), data.getOfferCount(), data.getOfferRate()));

            writeHeading(doc, "三、投递状态分布");
            writeDimItemTable(doc, "状态", "数量", data.getFunnel());

            writeHeading(doc, "四、意向城市");
            writeDimItemTable(doc, "城市", "人数", data.getIntentCity());

            writeHeading(doc, "五、意向行业");
            writeDimItemTable(doc, "行业", "人数", data.getIntentIndustry());

            writeHeading(doc, "六、期望薪资分布");
            writeDimItemTable(doc, "薪资区间", "人数", data.getSalaryBuckets());

            writeHeading(doc, "七、学生掌握技能 Top");
            writeDimItemTable(doc, "技能", "掌握人数", data.getTopSkills());

            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("就业报告 Word 导出失败", e);
            throw new BizException("Word 导出失败: " + e.getMessage());
        }
    }

    private void writeDimItemTable(XWPFDocument doc, String nameHeader, String valueHeader,
                                   List<EmploymentReportData.DimItem> items) {
        if (items == null || items.isEmpty()) {
            writeBody(doc, "（暂无数据）");
            return;
        }
        writeTable(doc, new String[]{nameHeader, valueHeader}, items,
                EmploymentReportData.DimItem::getName,
                i -> String.valueOf(i.getValue()));
    }

    private void writeTitle(XWPFDocument doc, String title) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = newRun(p);
        r.setText(title);
        r.setBold(true);
        r.setFontSize(18);
    }

    private void writeMeta(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = newRun(p);
        r.setText(text);
        r.setFontSize(10);
        r.setColor("808080");
    }

    private void writeHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = newRun(p);
        r.setText(text);
        r.setBold(true);
        r.setFontSize(14);
    }

    private void writeBody(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = newRun(p);
        r.setText(text);
        r.setFontSize(11);
    }

    /** 两列表格：维度值 + 指标 */
    private void writeDimensionTable(XWPFDocument doc, String nameHeader, String valueHeader,
                                     List<DashboardVO.DimensionItem> items) {
        if (items == null || items.isEmpty()) {
            writeBody(doc, "（暂无数据）");
            return;
        }
        writeTable(doc, new String[]{nameHeader, valueHeader}, items,
                DashboardVO.DimensionItem::getName,
                i -> i.getValue() == null ? "" : i.getValue().stripTrailingZeros().toPlainString());
    }

    /** 三列表格：周期 + 岗位数 + 平均薪资 */
    private void writeTrendTable(XWPFDocument doc, List<DashboardVO.TrendItem> items) {
        if (items == null || items.isEmpty()) {
            writeBody(doc, "（暂无数据）");
            return;
        }
        XWPFTable table = doc.createTable(items.size() + 1, 3);
        fillRow(table.getRow(0), true, "周期", "岗位数", "平均薪资(元)");
        for (int i = 0; i < items.size(); i++) {
            DashboardVO.TrendItem t = items.get(i);
            fillRow(table.getRow(i + 1), false,
                    t.getPeriod(),
                    String.valueOf(t.getJobCount()),
                    t.getAvgSalary() == null ? "" : t.getAvgSalary().stripTrailingZeros().toPlainString());
        }
    }

    private <T> void writeTable(XWPFDocument doc, String[] headers, List<T> items,
                                Function<T, String> col1, Function<T, String> col2) {
        XWPFTable table = doc.createTable(items.size() + 1, headers.length);
        fillRow(table.getRow(0), true, headers);
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            fillRow(table.getRow(i + 1), false, col1.apply(item), col2.apply(item));
        }
    }

    /**
     * 填一行。POI 新建表格时每个单元格已自带一个空段落，
     * 直接 setText 会追加出第二段，所以复用 getParagraphArray(0)。
     */
    private void fillRow(XWPFTableRow row, boolean bold, String... values) {
        for (int i = 0; i < values.length; i++) {
            XWPFParagraph p = row.getCell(i).getParagraphArray(0);
            XWPFRun r = newRun(p);
            r.setText(values[i] == null ? "" : values[i]);
            r.setBold(bold);
            r.setFontSize(10);
        }
    }

    private XWPFRun newRun(XWPFParagraph p) {
        XWPFRun r = p.createRun();
        r.setFontFamily(CJK_FONT);
        return r;
    }
}
