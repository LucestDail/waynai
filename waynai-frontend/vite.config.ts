import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

/**
 * 🔴 운영 빌드에 `VITE_API_BASE_URL` 이 없으면 **빌드를 멈춘다**(2026-09-16).
 *
 * `src/config/api.ts` 에도 같은 검사가 있지만 그건 **런타임**이다 — 번들에 들어가
 * 브라우저에서 터지므로, 문제를 **배포한 사람이 아니라 사용자가** 먼저 만난다.
 * 여기서 막으면 `npm run build` 가 그 자리에서 실패한다.
 *
 * ⚠️ 런타임 검사를 지우지 않는 이유: 이 플러그인을 거치지 않는 경로(설정을 바꿔
 * 빌드하거나 다른 도구로 번들링)가 있을 수 있다. 두 겹이고, 값이 있으면 둘 다 조용하다.
 */
function requireApiBaseUrlOnProd(mode: string): Plugin {
  return {
    name: 'waynai:require-api-base-url',
    // 설정을 읽는 시점에 막는다 — 번들을 만들기도 전에 끝난다.
    config() {
      if (mode !== 'production') return
      const env = loadEnv(mode, process.cwd(), 'VITE_')
      if (env.VITE_API_BASE_URL) return
      throw new Error(
        '\n🔴 VITE_API_BASE_URL 이 없습니다 — 운영 빌드를 만들 수 없습니다.\n' +
          '   이대로 빌드하면 사용자의 브라우저가 **자기 PC 의 localhost:8080** 을 두드리고,\n' +
          '   화면에는 "연결할 수 없음" 만 떠서 원인이 안 보입니다.\n\n' +
          '   해결: VITE_API_BASE_URL=https://<공개 도메인> npm run build\n' +
          '        또는 .env.production 에 지정\n',
      )
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    vueDevTools(),
    requireApiBaseUrlOnProd(mode),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  // NOTE: VITE_API_BASE_URL 은 .env / .env.local / .env.production 로 관리합니다.
  // 과거 define 으로 운영 IP 를 baked-in 하던 방식은 로컬 개발 시 AWS 로 오요청이 가서 제거했습니다.
}))
