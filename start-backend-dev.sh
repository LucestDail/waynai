#!/bin/bash
# ------------------------------------------------------------
# WaynAI 백엔드 개발 서버 실행 스크립트
# - 루트 .env 파일을 로드한 뒤 Spring Boot 실행
# - 포트: ${SERVER_PORT:-8080}
# ------------------------------------------------------------
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
    echo "[INFO] .env 로드: $ENV_FILE"
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
else
    echo "[WARN] .env 파일이 없습니다. .env.example 을 복사하여 생성하세요."
fi

if [ -z "${GEMINI_API_KEY:-}" ]; then
    echo "[ERROR] GEMINI_API_KEY 가 설정되지 않았습니다. (.env 확인)"
    exit 1
fi
if [ -z "${NAVER_CLIENT_ID:-}" ] || [ -z "${NAVER_CLIENT_SECRET:-}" ]; then
    echo "[WARN] NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 미설정. 검색 API 비활성화 상태로 구동됩니다."
fi

echo "[INFO] WaynAI 백엔드 개발 서버 시작... (port=${SERVER_PORT:-8080})"
cd "$SCRIPT_DIR/waynai-backend"
./mvnw spring-boot:run
