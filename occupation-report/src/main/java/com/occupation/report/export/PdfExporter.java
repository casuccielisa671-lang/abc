package com.occupation.report.export;

import com.itextpdf.text.pdf.BaseFont;
import com.occupation.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PDF 导出器 — HTML → PDF（Flying Saucer）
 * <p>
 * <b>中文字体</b>：Flying Saucer 内置字体不含中文字形，不注册中文字体会导出满页「口口口」。
 * 字体按以下顺序查找，命中即用：
 * <ol>
 *   <li>配置项 {@code app.report.pdf.font-path} 指定的文件；</li>
 *   <li>类路径 {@code fonts/} 下的字体（团队自备字体时放这里）；</li>
 *   <li>操作系统自带的常见中文字体（Windows 宋体/雅黑、Linux 文泉驿/Noto、macOS 苹方）。</li>
 * </ol>
 * <b>为什么不把字体文件提交进仓库</b>：Windows 的 simsun.ttc 约 18MB 且受 Microsoft 授权限制，
 * 不适合随源码分发。因此改为运行时探测，找不到时打印明确告警而不是静默导出乱码。
 * <p>
 * 字体以 {@code EMBEDDED} 方式嵌入 PDF，保证在没装该字体的机器上也能正常显示。
 *
 * @author occupation-team
 */
@Slf4j
@Component
public class PdfExporter {

    /** HTML 模板里声明的 font-family 必须能落到这个已注册的字体上 */
    private static final List<String> OS_FONT_CANDIDATES = Arrays.asList(
            // Windows
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simhei.ttf",
            // Linux（Docker 镜像里常见）
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/arphic/uming.ttc",
            // macOS
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/Supplemental/Songti.ttc");

    private static final String CLASSPATH_FONT = "fonts/simsun.ttf";

    @Value("${app.report.pdf.font-path:}")
    private String configuredFontPath;

    /** 解析结果缓存：null 表示尚未解析，空列表表示解析过但没找到 */
    private volatile List<String> resolvedFonts;

    /**
     * 将 HTML 字符串渲染为 PDF 字节流
     *
     * @param html 完整的 XHTML 文档（标签必须闭合，Flying Saucer 是严格 XML 解析）
     */
    public byte[] export(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            registerChineseFonts(renderer);

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF 导出失败", e);
            throw new BizException("PDF 导出失败: " + e.getMessage());
        }
    }

    private void registerChineseFonts(ITextRenderer renderer) {
        for (String path : resolveFonts()) {
            try {
                renderer.getFontResolver().addFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                log.debug("PDF 已注册中文字体: {}", path);
            } catch (Exception e) {
                log.warn("PDF 中文字体注册失败（已跳过）: {} — {}", path, e.getMessage());
            }
        }
    }

    /** 首次导出时探测一次字体路径，之后复用 */
    private List<String> resolveFonts() {
        List<String> cached = resolvedFonts;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (resolvedFonts != null) {
                return resolvedFonts;
            }
            List<String> found = new ArrayList<>();

            if (configuredFontPath != null && !configuredFontPath.trim().isEmpty()) {
                File f = new File(configuredFontPath.trim());
                if (f.isFile()) {
                    found.add(f.getAbsolutePath());
                } else {
                    log.warn("app.report.pdf.font-path 指向的字体不存在: {}", configuredFontPath);
                }
            }

            if (found.isEmpty()) {
                try {
                    ClassPathResource res = new ClassPathResource(CLASSPATH_FONT);
                    if (res.exists()) {
                        found.add(res.getURL().toString());
                    }
                } catch (Exception e) {
                    log.debug("类路径字体不可用: {}", e.getMessage());
                }
            }

            if (found.isEmpty()) {
                for (String candidate : OS_FONT_CANDIDATES) {
                    if (new File(candidate).isFile()) {
                        found.add(candidate);
                        break;
                    }
                }
            }

            if (found.isEmpty()) {
                log.warn("未找到任何中文字体，导出的 PDF 中文将显示为方块。"
                        + "请设置 app.report.pdf.font-path，或将字体放到 classpath 的 fonts/ 目录下。");
            } else {
                log.info("PDF 中文字体: {}", found);
            }
            resolvedFonts = found;
            return found;
        }
    }
}
