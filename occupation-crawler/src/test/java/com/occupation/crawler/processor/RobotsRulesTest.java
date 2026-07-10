package com.occupation.crawler.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * robots.txt 规则 → 正则 的转换测试。
 * <p>
 * 只测纯函数 {@link RobotsRules#toRegex(String)}，不发网络请求 —— 单测不该依赖外网。
 */
class RobotsRulesTest {

    /** 用 toRegex 编译规则再匹配路径，模拟 isAllowed 内部的判断 */
    private boolean blocked(String rule, String path) {
        return path.matches(RobotsRules.toRegex(rule));
    }

    @Test
    @DisplayName("robots 是前缀匹配：/user/ 挡住 /user/login")
    void prefixMatch() {
        assertTrue(blocked("/user/", "/user/login"));
        assertFalse(blocked("/user/", "/users"));
    }

    @Test
    @DisplayName("$ 结尾锚定：/*.js$ 只挡 .js 结尾")
    void anchored() {
        assertTrue(blocked("/*.js$", "/static/app.js"));
        assertFalse(blocked("/*.js$", "/static/app.js.map"));
    }

    @Test
    @DisplayName("Boss 直聘的 robots 挡住了本项目原来拼的那个 URL")
    void bossDisallowsOurOldUrl() {
        // 这条是 www.zhipin.com/robots.txt 里的原文，它正是挡住我们的那一条
        assertTrue(blocked("/*?query=*", "/web/geek/job?query=Java&city=101010100"));
    }

    @Test
    @DisplayName("robots 里的 ? 是普通字符，不是正则的「零或一次」")
    void questionMarkIsLiteral() {
        // Boss 还有一条 Disallow: *?city=*，它要求 city= 前面紧挨着字面量 '?'。
        // 我们的 URL 里 city= 前面是 '&'，所以这条并不命中 —— 起作用的是 /*?query=* 那条。
        // 一开始我把这两条都断言成命中，是想当然。
        assertTrue(blocked("*?city=*", "/web/geek/job?city=101010100"));
        assertFalse(blocked("*?city=*", "/web/geek/job?query=Java&city=101010100"));
    }

    @Test
    @DisplayName("智联禁止一切带查询串的 URL，但 301 之后的路径式地址被允许")
    void zhaopinPathFormAllowed() {
        // www.zhaopin.com/robots.txt: Disallow: /*?*
        assertTrue(blocked("/*?*", "/sou/jl653?kw=Java"));
        assertFalse(blocked("/*?*", "/sou/jl653/kw01500O80EO062"));
        assertFalse(blocked("/*?*", "/sou/jl653/kw01500O80EO062/p2"));
    }

    @Test
    @DisplayName("正则元字符要转义，不能当通配符用")
    void escapesRegexMetaChars() {
        assertTrue(blocked("/a.b", "/a.b"));
        // '.' 若没转义，"/axb" 会被误判为命中
        assertFalse(blocked("/a.b", "/axb"));
    }

    @Test
    @DisplayName("* 展开为任意字符")
    void wildcard() {
        assertTrue(blocked("/sem/*", "/sem/landing/page1"));
        assertTrue(blocked("*?utm_source=*", "/index?utm_source=baidu"));
        assertFalse(blocked("*?utm_source=*", "/index?ref=baidu"));
    }
}
