#!/bin/bash
set -e

# WaynAI 프론트엔드 운영 서버 실행 스크립트 (백그라운드 실행)
# 포트: 3000
#
# 🔴 2026-09-14: `set -e` 가 없어서 **빌드가 실패해도 옛 dist 를 띄우고
#    "시작되었습니다" 를 찍었다.** 형제 스크립트들엔 다 있는데 이 파일만 빠져 있었다
#    — "형제 파일 중 하나만 빠졌다" 가 이 워크스페이스의 전형적 모양이다.

echo "WaynAI 프론트엔드 운영 서버 시작 (백그라운드 실행)..."

cd waynai-frontend

# dist 폴더가 존재하는지 확인
if [ ! -d "dist" ]; then
    echo "프론트엔드 빌드 중..."
    if ! npm run build; then
        echo "❌ 프론트엔드 빌드 실패 — 기동하지 않는다(옛 dist 를 띄우면 조용한 회귀다)"
        exit 1
    fi
fi

# serve 패키지 설치 확인
if ! command -v serve &> /dev/null; then
    echo "serve 패키지 설치 중..."
    npm install -g serve
fi

# 백그라운드에서 실행하고 로그를 파일로 저장
nohup serve -s dist -l 3000 > frontend.log 2>&1 &

# 프로세스 ID 저장
echo $! > frontend.pid

echo "프론트엔드 서버가 백그라운드에서 시작되었습니다."
echo "프로세스 ID: $(cat frontend.pid)"
echo "로그 확인: tail -f frontend.log"
