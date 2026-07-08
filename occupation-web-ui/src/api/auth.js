import request from './request'

/**
 * 认证 API
 */
export function login(data) {
  return request.post('/auth/login', data)
}
