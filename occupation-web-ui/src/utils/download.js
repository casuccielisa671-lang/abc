/**
 * 带鉴权的文件下载
 *
 * 不能用 `window.open('/api/xxx')`：那样发出的请求不带 Authorization 头，
 * 后端直接 401，浏览器还会静默跳走，表现为「点了下载没反应」。
 * 必须走 axios（拦截器会注入 Token），拿到 Blob 后再用 <a download> 触发保存。
 */

import { ElMessage } from 'element-plus'

/** 从 Content-Disposition 解析文件名，优先 RFC 5987 的 filename*（中文名走这个） */
function parseFilename(disposition, fallback) {
  if (!disposition) return fallback

  const star = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (star) {
    try {
      return decodeURIComponent(star[1])
    } catch {
      /* 解码失败则继续尝试普通 filename */
    }
  }
  const plain = disposition.match(/filename="?([^";]+)"?/i)
  return plain ? plain[1] : fallback
}

/**
 * @param {Promise} responsePromise responseType:'blob' 的 axios 请求
 * @param {string} fallbackName 后端没给文件名时使用
 */
export async function saveBlob(responsePromise, fallbackName) {
  const response = await responsePromise

  // 后端下载失败时返回的是 JSON 错误体（Content-Type: application/json），
  // 但 responseType:'blob' 让它也变成 Blob。若不识别就会把 `{"code":500,...}`
  // 当成文件存盘——用户打开「PDF」看到的却是一段 JSON。这里拦下来，改为抛错弹提示。
  const blob = response.data
  if (blob && typeof blob.type === 'string' && blob.type.includes('application/json')) {
    const text = await blob.text()
    let msg = '下载失败'
    try { msg = JSON.parse(text).message || msg } catch { /* 非标准 JSON，用默认文案 */ }
    const err = new Error(msg)
    err.handled = true            // 已在此处弹过提示，调用方不必重复
    ElMessage.error(msg)
    throw err
  }

  const filename = parseFilename(response.headers?.['content-disposition'], fallbackName)

  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  // 立刻 revoke 会让部分浏览器来不及开始下载
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
