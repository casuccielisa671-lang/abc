package com.occupation.report.export;

import com.occupation.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * Word 导出器 — Apache POI 生成 .docx
 * <p>
 * 当前为基础实现：将报告的纯文本内容逐段写入 Word。
 * TODO(P3-B组): 结构化导出——解析报告章节，生成带标题层级、
 * 数据表格（XWPFTable）和图表截图的完整 Word 文档。
 *
 * @author occupation-team
 */
@Slf4j
@Component
public class WordExporter {

    /**
     * 将报告文本内容导出为 Word 字节流
     *
     * @param title      报告标题
     * @param paragraphs 正文段落（已去除 HTML 标签的纯文本）
     */
    public byte[] export(String title, java.util.List<String> paragraphs) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            // 正文
            for (String text : paragraphs) {
                XWPFParagraph para = doc.createParagraph();
                para.createRun().setText(text);
            }

            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Word 导出失败", e);
            throw new BizException("Word 导出失败: " + e.getMessage());
        }
    }
}
