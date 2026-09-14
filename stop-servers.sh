#!/bin/bash

# WaynAI 서버 중지 스크립트
#
# 🔴 2026-09-14: 종전에는 `kill` 을 보내고 **죽었는지 확인 없이** "중지되었습니다" 를
#    찍었다. `kill` 은 시그널 전달만 성공해도 0 을 내므로, 프로세스가 안 죽어도
#    성공으로 보였다. 안 죽은 채 start 를 돌리면 포트 충돌이 난다.
#    (같은 날 osh/stop.sh 에서 같은 병을 고쳤다 — 짝을 이루는 스크립트는 함께 본다.)
STOP_FAIL=0

# PID 에 TERM 을 보내고 **실제로 죽었는지** 최대 10초 확인한다. 안 죽으면 KILL.
stop_pid() {
    local PID="$1" NAME="$2"
    kill "$PID" 2>/dev/null
    for _ in $(seq 1 10); do
        ps -p "$PID" > /dev/null 2>&1 || { echo "$NAME 가 중지되었습니다."; return 0; }
        sleep 1
    done
    echo "$NAME 가 TERM 에 안 죽어 강제 종료합니다 (PID: $PID)"
    kill -9 "$PID" 2>/dev/null
    sleep 1
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "✗ $NAME 를 멈추지 못했습니다 (PID: $PID)"
        return 1
    fi
    echo "$NAME 가 강제 중지되었습니다."
    return 0
}

echo "WaynAI 서버 중지 중..."

# 백엔드 서버 중지 (PID 파일 방식)
if [ -f "waynai-backend/backend.pid" ]; then
    BACKEND_PID=$(cat waynai-backend/backend.pid)
    if ps -p $BACKEND_PID > /dev/null; then
        echo "백엔드 서버 중지 중... (PID: $BACKEND_PID)"
        stop_pid $BACKEND_PID "백엔드 서버" || STOP_FAIL=$((STOP_FAIL + 1))
        rm waynai-backend/backend.pid

    else
        echo "백엔드 서버가 실행 중이 아닙니다."
        rm waynai-backend/backend.pid
    fi
else
    # PID 파일이 없는 경우 프로세스 이름으로 찾기
    BACKEND_PID=$(ps aux | grep "com.waynai.demo.DemoApplication" | grep -v grep | awk '{print $2}')
    if [ ! -z "$BACKEND_PID" ]; then
        echo "백엔드 서버 중지 중... (PID: $BACKEND_PID)"
        stop_pid $BACKEND_PID "백엔드 서버" || STOP_FAIL=$((STOP_FAIL + 1))
        
    else
        echo "백엔드 서버가 실행 중이 아닙니다."
    fi
fi

# 프론트엔드 서버 중지 (PID 파일 방식)
if [ -f "waynai-frontend/frontend.pid" ]; then
    FRONTEND_PID=$(cat waynai-frontend/frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null; then
        echo "프론트엔드 서버 중지 중... (PID: $FRONTEND_PID)"
        stop_pid $FRONTEND_PID "프론트엔드 서버" || STOP_FAIL=$((STOP_FAIL + 1))
        rm waynai-frontend/frontend.pid

    else
        echo "프론트엔드 서버가 실행 중이 아닙니다."
        rm waynai-frontend/frontend.pid
    fi
else
    # PID 파일이 없는 경우 프로세스 이름으로 찾기
    FRONTEND_PID=$(ps aux | grep "vite" | grep -v grep | awk '{print $2}')
    if [ ! -z "$FRONTEND_PID" ]; then
        echo "프론트엔드 서버 중지 중... (PID: $FRONTEND_PID)"
        stop_pid $FRONTEND_PID "프론트엔드 서버" || STOP_FAIL=$((STOP_FAIL + 1))
        
    else
        echo "프론트엔드 서버가 실행 중이 아닙니다."
    fi
fi

# Maven 프로세스도 중지 (백엔드 개발 서버)
MAVEN_PID=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')
if [ ! -z "$MAVEN_PID" ]; then
    echo "Maven 프로세스 중지 중... (PID: $MAVEN_PID)"
    stop_pid $MAVEN_PID "Maven 프로세스" || STOP_FAIL=$((STOP_FAIL + 1))
    
fi

echo "모든 서버 중지 완료."

# 🔴 마지막 줄이 그 실행의 전부를 말한다.
echo ""
if [ "$STOP_FAIL" -gt 0 ]; then
    echo "🔴 중지 실패 ${STOP_FAIL}건 — 프로세스가 남아 있습니다."
    echo "   이 상태로 시작하면 포트 충돌이 납니다."
    exit 1
fi
echo "✓ WaynAI 서버가 모두 중지되었습니다."
