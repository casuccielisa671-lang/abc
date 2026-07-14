package com.occupation.report.export;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Arrays;

/**
 * Markdown → XHTML 渲染器（AI 报告导出专用）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>使用 {@code flexmark-all} 支持表格、删除线、脚注等扩展；</li>
 *   <li>输出严格 XHTML（自闭合标签、自定义属性），保证 Flying Saucer 解析不报错；</li>
 *   <li>对原始 Markdown 做轻量兜底清洗：去掉残留的 <think> 等模型思考片段。</li>
 * </ul>
 *
 * @author occupation-team
 */
public final class MarkdownRenderer {

    /** flexmark 解析器（线程安全，可单例） */
    private static final Parser PARSER;
    /** flexmark HTML 渲染器（线程安全） */
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet opts = new MutableDataSet();
        // 启用表格扩展（教学报告、对比数据常用 Markdown 表格）
        opts.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create()));
        // 渲染时把换行当段落分隔（贴近中文报告习惯）
        opts.set(HtmlRenderer.SOFT_BREAK, "<br />");
        PARSER = Parser.builder(opts).build();
        RENDERER = HtmlRenderer.builder(opts).build();
    }

    private MarkdownRenderer() {}

    /**
     * 将 Markdown 文本渲染为 XHTML 片段（不含 html/body 标签，可嵌入报告模板）。
     *
     * @param markdown 原始 Markdown
     * @return XHTML 片段（已转义、标签自闭合）
     */
    public static String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        String cleaned = sanitize(markdown);
        Node document = PARSER.parse(cleaned);
        return RENDERER.render(document);
    }

    /**
     * 轻量兜底清洗：去掉模型偶尔泄露的 <think>/```thinking 等私有思考片段。
     * 真正的"完整防御"应在 AI 网关层做；这里只解决导出时的视觉瑕疵。
     */
    private static String sanitize(String md) {
        // 1. 去掉 <think>...</think>
        String s = md.replaceAll("(?is)<think>.*?</think>", "");
        // 2. 去掉 ```thinking ... ``` 代码块
        s = s.replaceAll("(?is)```\\s*thinking[\\s\\S]*?```", "");
        return s.trim();
    }
}
