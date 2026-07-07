/**
 * 配置层 — 本模块特有的 Spring 配置类
 *
 * <p>职责：
 * <ul>
 *   <li>WebMvc 配置</li>
 *   <li>拦截器注册</li>
 *   <li>定时任务配置</li>
 *   <li>跨模块 Bean 注入</li>
 * </ul>
 *
 * <p>跨模块复用配置放 {@code com.occupation.common.config}。
 */
package com.occupation.web.config;
