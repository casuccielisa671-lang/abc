import request from './request'

/**
 * 认证 API
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 学校 / 企业列表（无鉴权，供登录页下拉联想）
 * 只返回启用中的租户，已禁用的学校不会出现。
 */
export function getTenants() {
  return request.get('/auth/tenants')
}
