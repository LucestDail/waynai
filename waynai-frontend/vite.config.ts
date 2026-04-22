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
  // NOTE: VITE_API_BASE_URL 은 .env / .env.local / .env.production 로 관리합니다.
  // 과거 define 으로 운영 IP 를 baked-in 하던 방식은 로컬 개발 시 AWS 로 오요청이 가서 제거했습니다.
})
