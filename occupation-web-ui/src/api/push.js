import request from './request'

/**
 * 站内消息（通知中心）API
 * 后端 /api/push，任意登录角色可用。分页用 pageNum/pageSize。
 */

/** 我的消息分页列表（未读置顶、时间倒序） */
export function getMyMessages(params) {
  return request.get('/push/list', { params })
}

/** 未读数（导航栏铃铛红点） */
export function getUnreadCount() {
  return request.get('/push/unread/count')
}

/** 标记单条已读 */
export function markMessageRead(id) {
  return request.put(`/push/${id}/read`)
}

/** 全部标记已读，返回处理条数 */
export function markAllMessagesRead() {
  return request.put('/push/read-all')
}
