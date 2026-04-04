import axios from 'axios'
import { useErmAuthStore } from '../stores/ermAuth'

const http = axios.create({
  baseURL: '/api',
  timeout: 120000
})

http.interceptors.request.use((config) => {
  try {
    const auth = useErmAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
    if (auth.tenantCode) {
      config.headers['X-Tenant-Id'] = auth.tenantCode
    }
  } catch {
    // Pinia 未就绪时跳过
  }
  return config
})

export default http
