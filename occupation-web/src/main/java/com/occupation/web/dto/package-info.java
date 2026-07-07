/**
 * DTO 数据传输对象 — 前端入参
 *
 * <p>职责：
 * <ul>
 *   <li>接收前端请求参数</li>
 *   <li>添加 JSR-303 校验注解（@NotNull / @NotBlank / @Size 等）</li>
 *   <li>命名规范：{@code XxxCreateDTO}、{@code XxxQueryDTO}、{@code XxxUpdateDTO}</li>
 * </ul>
 *
 * <p>不包含业务逻辑，仅为数据载体。
 */
package com.occupation.web.dto;
