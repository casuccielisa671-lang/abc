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
    port: 5177,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            console.log('[Vite Proxy]', req.method, req.url, '->', proxyReq.path)
          })
          proxy.on('error', (err, req) => {
            console.error('[Vite Proxy Error]', req.url, err.message)
          })
        }
      },
      '/amap-tile': {
        target: 'https://webst01.is.autonavi.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/amap-tile/, '')
      }
    }
  }
})
