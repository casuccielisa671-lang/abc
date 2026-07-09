package com.occupation.analysis.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 技能词库提取测试 —— 重点覆盖 ASCII 词边界，这是该词库存在的主要理由。
 */
class SkillDictionaryTest {

    @Test
    @DisplayName("Django 不应被识别为 Go")
    void djangoIsNotGo() {
        assertFalse(SkillDictionary.extract("熟悉 Django 框架").contains("Go"));
    }

    @Test
    @DisplayName("JavaScript 不应被识别为 Java")
    void javaScriptIsNotJava() {
        Set<String> hits = SkillDictionary.extract("精通 JavaScript 与前端工程化");
        assertTrue(hits.contains("JavaScript"));
        assertFalse(hits.contains("Java"));
    }

    @Test
    @DisplayName("Java 独立出现时应被识别，且不牵连 JavaScript")
    void javaStandalone() {
        Set<String> hits = SkillDictionary.extract("要求掌握 Java、MySQL");
        assertTrue(hits.contains("Java"));
        assertTrue(hits.contains("MySQL"));
        assertFalse(hits.contains("JavaScript"));
    }

    @Test
    @DisplayName("C++ 带符号结尾也能命中")
    void cPlusPlus() {
        assertTrue(SkillDictionary.extract("熟练使用 C++ 进行系统开发").contains("C++"));
    }

    @Test
    @DisplayName("别名归一到标准名：K8s → Kubernetes，Golang → Go")
    void aliasesNormalize() {
        assertTrue(SkillDictionary.extract("有 K8s 运维经验").contains("Kubernetes"));
        assertTrue(SkillDictionary.extract("使用 Golang 开发微服务").contains("Go"));
        assertTrue(SkillDictionary.extract("熟悉 SpringBoot").contains("Spring Boot"));
    }

    @Test
    @DisplayName("中文技能按子串匹配")
    void chineseSkills() {
        Set<String> hits = SkillDictionary.extract("负责机器学习模型训练，具备良好的团队协作能力");
        assertTrue(hits.contains("机器学习"));
        assertTrue(hits.contains("团队协作"));
    }

    @Test
    @DisplayName("大小写不敏感")
    void caseInsensitive() {
        assertTrue(SkillDictionary.extract("熟悉 java 和 mysql").contains("Java"));
        assertTrue(SkillDictionary.extract("熟悉 java 和 mysql").contains("MySQL"));
    }

    @Test
    @DisplayName("空文本返回空集合")
    void emptyText() {
        assertEquals(0, SkillDictionary.extract(null).size());
        assertEquals(0, SkillDictionary.extract("").size());
    }

    @Test
    @DisplayName("不含技能的文本不误报")
    void noFalsePositive() {
        assertEquals(0, SkillDictionary.extract("负责日常行政事务与会议纪要整理").size());
    }
}
