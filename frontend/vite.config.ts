import {fileURLToPath, URL} from 'node:url'

import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Загружаем переменные окружения из .env
  const env = loadEnv(mode, process.cwd(), '');

  return {
    define: {
      global: 'window',
    },
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
        // Все запросы, начинающиеся с /api, будут перенаправлены на backend
        '/api': {
          target: `http://${env.VITE_API_IP}:8080`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '/api'),
          configure: (proxy, _options) => {
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              proxyReq.setHeader('X-Visitor-Id2', 'dev-fingerprint-bypass');
            });
          }
        },
        '/ws': {
          target: `ws://${env.VITE_API_IP}:8080`,
          ws: true,
          changeOrigin: true
        },
        '/media': {
          target: `http://${env.VITE_API_IP}:9000`,
          changeOrigin: true
        }
      }
    }
  };
})
