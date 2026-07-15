import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/amap-tile': {
        target: 'https://webst01.is.autonavi.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/amap-tile/, '')
      }
    }
  }
})
