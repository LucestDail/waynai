#!/bin/bash

# WaynAI 프론트엔드 운영 서버 실행 스크립트 (백그라운드 실행)
# 포트: 3000

echo "WaynAI 프론트엔드 운영 서버 시작 (백그라운드 실행)..."

cd waynai-frontend

# dist 폴더가 존재하는지 확인
if [ ! -d "dist" ]; then
    echo "프론트엔드 빌드 중..."
    npm run build
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
