/**
 * API 베이스 주소 — **한 곳에서만 정한다.**
 *
 * 🔴 왜 만들었나 (2026-09-16)
 *
 * 종전에는 네 파일이 각자 이렇게 적고 있었다:
 *
 * ```ts
 * const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
 * ```
 *
 * 개발에서는 편하지만 **운영 빌드에서 값을 안 주면 조용히 `localhost` 로 떨어진다.**
 * 그 번들을 배포하면 사용자의 브라우저가 **자기 PC 의 8080** 을 두드리므로
 * 전부 실패하는데, **에러 메시지는 "연결할 수 없음" 뿐이라 원인이 안 보인다.**
 *
 * ⚠️ 이 워크스페이스는 이미 같은 상처가 있다 — `chominjungum-web` 의
 * `.env.production` 에 LAN 절대주소가 박혀 있어 **서버에서 빌드하면 외부 도메인에서
 * API 가 깨지는** 상태였다. 조용한 기본값은 그때도 원인을 가렸다.
 *
 * ⇒ **개발에서는 폴백을 남기고, 운영 빌드에서는 없으면 즉시 실패**시킨다.
 *   "빌드는 됐는데 런타임에 아무도 모르게 틀린" 상태를 만들지 않는다.
 */

const DEV_FALLBACK = 'http://localhost:8080'

function resolveApiBase(): string {
  const configured = import.meta.env.VITE_API_BASE_URL

  if (configured) {
    // 끝의 `/` 는 떼어 둔다 — 호출부가 `${base}/api/...` 로 쓰기 때문에
    // 안 떼면 `//api/...` 가 되어 일부 프록시에서 404 가 난다.
    return String(configured).replace(/\/+$/, '')
  }

  // 🔴 운영 빌드에서 값이 없으면 **조용히 넘어가지 않는다.**
  //    여기서 던지면 앱이 첫 화면에서 죽지만, 그게 "모든 요청이 원인 불명으로
  //    실패하는" 것보다 낫다 — 후자는 배포한 사람이 몇 시간 뒤에야 안다.
  if (import.meta.env.PROD) {
    throw new Error(
      '[waynai] VITE_API_BASE_URL 이 설정되지 않은 채 운영 빌드가 만들어졌습니다. ' +
        '빌드 시 `VITE_API_BASE_URL=https://<공개 도메인>` 을 주세요. ' +
        '(설정하지 않으면 사용자의 브라우저가 자기 PC 의 8080 을 두드립니다.)',
    )
  }

  return DEV_FALLBACK
}

/** 모든 API 호출이 쓰는 베이스 주소. 끝에 `/` 없음. */
export const API_BASE_URL = resolveApiBase()
