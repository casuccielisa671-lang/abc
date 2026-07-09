/**
 * 列表响应解包
 *
 * 后端有两种列表形态，前端过去只处理了其中一种，导致页面空白：
 *   · 分页接口 → PageResult：{ total, pageNum, pageSize, list }
 *   · 非分页接口 → 直接一个数组
 *
 * 历史代码写的是 `data.records || data.list || []`：
 *   1. `records` 是 MyBatis Page 的字段名，PageResult 序列化后<b>根本不存在</b>，是死代码；
 *   2. 后端直接返回数组时，数组上没有 .records/.list，取值为 undefined → 列表被渲染成空；
 *   3. data 为 null 时（Result.ok() 无 data），`data.records` 直接抛 TypeError。
 *
 * 统一收敛到这里，三种情况都兜住。
 */

/** 从响应中取出列表，永远返回数组 */
export function toList(data) {
  if (Array.isArray(data)) return data
  return data?.list ?? data?.records ?? []
}

/**
 * 从响应中取出总数。
 * 后端返回纯数组时没有 total，用数组长度兜底，
 * 否则分页组件会一直显示 0 条、页码不可点。
 */
export function toTotal(data, fallbackList) {
  if (Array.isArray(data)) return data.length
  const total = data?.total
  if (typeof total === 'number') return total
  return (fallbackList ?? toList(data)).length
}
