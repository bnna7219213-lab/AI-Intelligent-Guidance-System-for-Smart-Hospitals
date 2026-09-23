import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 添加请求时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }
    
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 检查响应格式
    if (!res || typeof res.code === 'undefined') {
      ElMessage({
        message: '响应格式错误',
        type: 'error',
        duration: 3000
      })
      return Promise.reject(new Error('Invalid response format'))
    }
    
    // 业务错误处理
    if (res.code !== 200) {
      // 401 未授权
      if (res.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error(res.message || 'Unauthorized'))
      }
      
      // 429 限流
      if (res.code === 429) {
        ElMessage({
          message: '请求过于频繁，请稍后再试',
          type: 'warning',
          duration: 3000
        })
        return Promise.reject(new Error('Rate limited'))
      }
      
      // 其他业务错误
      ElMessage({
        message: res.message || '请求失败',
        type: 'error',
        duration: 3000
      })
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    
    return res
  },
  (error) => {
    console.error('Response error:', error)
    
    // 网络错误
    if (!error.response) {
      ElMessage({
        message: '网络异常，请检查网络连接',
        type: 'error',
        duration: 3000
      })
      return Promise.reject(error)
    }
    
    const { status, data } = error.response
    
    // HTTP 状态码处理
    switch (status) {
      case 401:
        handleUnauthorized()
        break
        
      case 403:
        ElMessage({
          message: data?.message || '没有权限执行此操作',
          type: 'error',
          duration: 3000
        })
        break
        
      case 404:
        ElMessage({
          message: '请求的资源不存在',
          type: 'error',
          duration: 3000
        })
        break
        
      case 429:
        ElMessage({
          message: '请求过于频繁，请稍后再试',
          type: 'warning',
          duration: 3000
        })
        break
        
      case 500:
        ElMessage({
          message: data?.message || '服务器内部错误',
          type: 'error',
          duration: 3000
        })
        break
        
      default:
        ElMessage({
          message: data?.message || `请求错误 (${status})`,
          type: 'error',
          duration: 3000
        })
    }
    
    return Promise.reject(error)
  }
)

// 处理未授权
let isHandlingUnauthorized = false
function handleUnauthorized() {
  if (isHandlingUnauthorized) return
  
  isHandlingUnauthorized = true
  ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    window.location.href = '/login'
  }).catch(() => {
    // 用户取消，不跳转
  }).finally(() => {
    isHandlingUnauthorized = false
  })
}

// 导出请求方法
export const request = {
  get(url, params) {
    return service.get(url, { params })
  },
  
  post(url, data) {
    return service.post(url, data)
  },
  
  put(url, data) {
    return service.put(url, data)
  },
  
  delete(url, params) {
    return service.delete(url, { params })
  },
  
  // 上传文件
  upload(url, file, onProgress) {
    const formData = new FormData()
    formData.append('file', file)
    
    return service.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percentCompleted)
        }
      }
    })
  }
}

export default service
