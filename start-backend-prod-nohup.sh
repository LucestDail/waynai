#!/bin/bash
# ------------------------------------------------------------
# WaynAI 백엔드 운영 서버 백그라운드 실행
# - .env 또는 /var/www/key/* 레거시 파일에서 키 주입
# ------------------------------------------------------------
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
fi

fallback_from_file() {
    local var_name="$1"
    local path="$2"
    if [ -n "${!var_name:-}" ]; then
        return 0
    fi
    if [ -f "$path" ]; then
        local val
        val="$(tr -d '\n\r' < "$path")"
        [ -n "$val" ] && export "$var_name=$val"
    fi
}

fallback_from_file GEMINI_API_KEY       /var/www/key/key
fallback_from_file NAVER_CLIENT_ID      /var/www/key/naverId
fallback_from_file NAVER_CLIENT_SECRET  /var/www/key/naverSecret
fallback_from_file TOUR_API_SERVICE_KEY /var/www/key/tourApiKey

if [ -z "${GEMINI_API_KEY:-}" ]; then
    echo "[ERROR] GEMINI_API_KEY 가 설정되지 않았습니다."
    exit 1
fi

if [ -z "${TOUR_API_SERVICE_KEY:-}" ]; then
    echo "[WARN] TOUR_API_SERVICE_KEY 미설정 - 관광공사 API 호출이 실패할 수 있습니다."
fi

cd "$SCRIPT_DIR/waynai-backend"
JAR=target/waynai-backend-0.0.1-SNAPSHOT.jar
if [ ! -f "$JAR" ]; then
    echo "[INFO] JAR 빌드 중..."
    ./mvnw -q clean package -DskipTests
fi

nohup java -jar "$JAR" --spring.profiles.active=prod > "$SCRIPT_DIR/backend.log" 2>&1 &
echo $! > "$SCRIPT_DIR/backend.pid"
echo "[INFO] 백엔드 백그라운드 실행 (pid=$(cat "$SCRIPT_DIR/backend.pid"))"
echo "[INFO] tail -f $SCRIPT_DIR/backend.log 로 로그 확인"
