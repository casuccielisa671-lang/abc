/**
 * Service 接口层 — 业务逻辑契约定义
 *
 * <p>职责：
 * <ul>
 *   <li>定义业务方法签名（接口）</li>
 *   <li>添加 @Transactional 注解（实现类上）</li>
 *   <li>编排调用 Mapper 和其他 Service</li>
 * </ul>
 *
 * <p>实现类统一放在 {@code com.occupation.web.service.impl} 包下。
 */
package com.occupation.web.service;
