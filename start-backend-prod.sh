#!/bin/bash
# ------------------------------------------------------------
# WaynAI 백엔드 운영 서버 실행 스크립트
# - 루트 .env 파일을 로드
# - 루트 .env 가 없으면 /var/www/key/ 의 파일 기반으로 자동 주입 (레거시 호환)
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
    echo "[INFO] .env 부재. 레거시 키 파일(/var/www/key/*)로 대체 주입합니다."
fi

# ------------------------------------------------------------
# 레거시 파일 폴백: .env 로드 이후에도 특정 값이 비어 있으면
# /var/www/key/* 를 마지막 수단으로 읽어 채워 넣는다.
# (운영 EC2 등 systemd 단위 배포 환경에서 점진 마이그레이션 지원)
# ------------------------------------------------------------
fallback_from_file() {
    local var_name="$1"
    local path="$2"
    # 이미 값이 있으면 skip
    if [ -n "${!var_name:-}" ]; then
        return 0
    fi
    if [ -f "$path" ]; then
        local val
        val="$(tr -d '\n\r' < "$path")"
        if [ -n "$val" ]; then
            export "$var_name=$val"
            echo "[INFO] $var_name 를 $path 에서 주입"
        fi
    fi
}

fallback_from_file GEMINI_API_KEY       /var/www/key/key
fallback_from_file NAVER_CLIENT_ID      /var/www/key/naverId
fallback_from_file NAVER_CLIENT_SECRET  /var/www/key/naverSecret
fallback_from_file TOUR_API_SERVICE_KEY /var/www/key/tourApiKey

if [ -z "${GEMINI_API_KEY:-}" ]; then
    echo "[ERROR] GEMINI_API_KEY 미설정 (env 또는 /var/www/key/key)"
    exit 1
fi

if [ -z "${TOUR_API_SERVICE_KEY:-}" ]; then
    echo "[WARN] TOUR_API_SERVICE_KEY 미설정 - 관광공사 API 호출이 실패할 수 있습니다."
    echo "       .env 또는 /var/www/key/tourApiKey 에 **디코딩된(원문)** 키를 배치하세요."
fi

cd "$SCRIPT_DIR/waynai-backend"
JAR=target/waynai-backend-0.0.1-SNAPSHOT.jar
if [ ! -f "$JAR" ]; then
    echo "[INFO] JAR 빌드 중..."
    ./mvnw -q clean package -DskipTests
fi

echo "[INFO] WaynAI 운영 서버 시작 (port=${SERVER_PORT:-8080})"
exec java -jar "$JAR" --spring.profiles.active=prod
