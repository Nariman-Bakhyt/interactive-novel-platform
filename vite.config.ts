import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      // Все запросы, начинающиеся с /api, будут перенаправлены на 8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true, // Изменяет заголовок Origin на адрес целевого сервера
        rewrite: (path) => path.replace(/^\/api/, '/api'), // Убедитесь, что /api остается, если он нужен бэкенду
      }
    }
  }
})
