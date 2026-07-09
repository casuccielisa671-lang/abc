package com.occupation.crawler.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 智联薪资字符串解析测试 —— 智联的写法比 BOSS 多样，单位混用是主要坑点。
 */
class ZhaopinSalaryParseTest {

    private final ZhaopinJobPageProcessor p = new ZhaopinJobPageProcessor("Java", "530", 1);

    @Test
    @DisplayName("万-万")
    void wanToWan() {
        assertArrayEquals(new int[]{15000, 20000}, p.parseSalary("1.5万-2万"));
    }

    @Test
    @DisplayName("千-万 混用单位")
    void qianToWan() {
        assertArrayEquals(new int[]{8000, 12000}, p.parseSalary("8千-1.2万"));
    }

    @Test
    @DisplayName("K-K")
    void kToK() {
        assertArrayEquals(new int[]{15000, 25000}, p.parseSalary("15K-25K"));
        assertArrayEquals(new int[]{15000, 25000}, p.parseSalary("15k-25k"));
    }

    @Test
    @DisplayName("前段省略单位时沿用后段单位")
    void unitInheritedFromMax() {
        assertArrayEquals(new int[]{15000, 20000}, p.parseSalary("1.5-2万"));
        assertArrayEquals(new int[]{8000, 12000}, p.parseSalary("8-12千"));
    }

    @Test
    @DisplayName("波浪号分隔")
    void tildeSeparator() {
        assertArrayEquals(new int[]{10000, 15000}, p.parseSalary("1万~1.5万"));
    }

    @Test
    @DisplayName("带后缀的整段文本也能提取")
    void withSuffix() {
        assertArrayEquals(new int[]{15000, 25000}, p.parseSalary("15K-25K·13薪"));
    }

    @Test
    @DisplayName("面议 / 空值返回 0")
    void unparseable() {
        assertArrayEquals(new int[]{0, 0}, p.parseSalary("面议"));
        assertArrayEquals(new int[]{0, 0}, p.parseSalary(""));
        assertArrayEquals(new int[]{0, 0}, p.parseSalary(null));
    }
}
