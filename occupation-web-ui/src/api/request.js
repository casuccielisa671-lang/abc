import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器 — 注入 JWT Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 — 统一错误处理
request.interceptors.response.use(
  response => {
    // 文件下载（Excel / PDF）返回的是二进制，没有 code/data 包装，
    // 直接把整个 response 交给调用方去读 headers 里的文件名
    if (response.config.responseType === 'blob') {
      return response
    }
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    // 下载接口出错时后端返回的仍是 JSON，但因为 responseType=blob 被包成了 Blob，
    // 需要读出来才能拿到真正的错误信息，否则只会显示 "Request failed with status code 500"
    const data = error.response?.data
    if (data instanceof Blob && data.type?.includes('json')) {
      try {
        const parsed = JSON.parse(await data.text())
        ElMessage.error(parsed.message || '下载失败')
        return Promise.reject(new Error(parsed.message))
      } catch {
        // 解析失败则退回通用提示
      }
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
