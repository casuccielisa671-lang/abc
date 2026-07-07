/**
 * Controller 层 — HTTP 请求入口
 *
 * <p>职责边界：
 * <ul>
 *   <li>接收 HTTP 请求参数</li>
 *   <li>@Valid 参数校验</li>
 *   <li>调用 Service 接口</li>
 *   <li>封装 Result&lt;T&gt; 响应</li>
 * </ul>
 *
 * <p>🚫 严禁：
 * <ul>
 *   <li>编写业务逻辑</li>
 *   <li>直接调用 Mapper</li>
 *   <li>操作 HttpSession</li>
 * </ul>
 */
package com.occupation.web.controller;
