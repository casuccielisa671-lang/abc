package com.occupation.report.export;

import com.occupation.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * PDF 导出器 — HTML → PDF（Flying Saucer）
 * <p>
 * ⚠️ 中文支持：Flying Saucer 默认字体不含中文，需要两步：
 * 1. 将 simsun.ttf（宋体）放入 resources/fonts/；
 * 2. 渲染前注册字体，且 HTML 的 CSS 中声明 font-family: SimSun。
 * TODO(P3-B组): 放置字体文件并打开下方注册代码。
 *
 * @author occupation-team
 */
@Slf4j
@Component
public class PdfExporter {

    /**
     * 将 HTML 字符串渲染为 PDF 字节流
     *
     * @param html 完整的 XHTML 文档（标签必须闭合，Flying Saucer 是严格 XML 解析）
     */
    public byte[] export(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // TODO(P3-B组): 注册中文字体（fonts/simsun.ttf 放入 resources 后取消注释）
            // renderer.getFontResolver().addFont(
            //         new ClassPathResource("fonts/simsun.ttf").getURL().toString(),
            //         BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF 导出失败", e);
            throw new BizException("PDF 导出失败: " + e.getMessage());
        }
    }
}
