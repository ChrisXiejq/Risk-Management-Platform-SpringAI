import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    open: true,//启动项目自动弹出浏览器
    port: 8192,//启动端口
    proxy: {
      // 后端 server.servlet.context-path=/api，此处不再 strip 前缀，保证 /api/** 直达网关上下文
      '/api': {
        target: 'http://localhost:8190',
        changeOrigin: true
      },
    },
    // client: {
    //   overlay: false   //关闭Uncaught runtime errors
    // }

  },
  define: {
    'process.env': {},
  },
})