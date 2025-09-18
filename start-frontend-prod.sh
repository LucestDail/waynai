#!/bin/bash

# WaynAI 프론트엔드 운영 서버 실행 스크립트
# 포트: 3000

echo "WaynAI 프론트엔드 운영 서버 시작..."

cd waynai-frontend

# dist 폴더가 존재하는지 확인
if [ ! -d "dist" ]; then
    echo "프론트엔드 빌드 중..."
    npm run build
fi

# 운영 환경으로 정적 파일 서빙 (serve 사용)
if ! command -v serve &> /dev/null; then
    echo "serve 패키지 설치 중..."
    npm install -g serve
fi

serve -s dist -l 3000
