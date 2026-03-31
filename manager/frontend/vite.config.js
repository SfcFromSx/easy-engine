import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './test/setup.js',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      reportsDirectory: './coverage',
      include: [
        'src/utils/formatters.js',
        'src/views/Dashboard.vue',
        'src/views/Traces.vue',
        'src/views/Patterns.vue',
        'src/views/QueryDatasources.vue',
        'src/views/Acceleration.vue'
      ]
    }
  },
  server: {
    port: 5173,
    host: "0.0.0.0",
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8090',
        changeOrigin: true
      }
    }
  }
})
