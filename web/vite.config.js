import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/SSE': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/chat': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/rag': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/internet': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
