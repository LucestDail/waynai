#!/bin/bash

# WaynAI 서버 중지 스크립트

echo "WaynAI 서버 중지 중..."

# 백엔드 서버 중지 (PID 파일 방식)
if [ -f "waynai-backend/backend.pid" ]; then
    BACKEND_PID=$(cat waynai-backend/backend.pid)
    if ps -p $BACKEND_PID > /dev/null; then
        echo "백엔드 서버 중지 중... (PID: $BACKEND_PID)"
        kill $BACKEND_PID
        rm waynai-backend/backend.pid
        echo "백엔드 서버가 중지되었습니다."
    else
        echo "백엔드 서버가 실행 중이 아닙니다."
        rm waynai-backend/backend.pid
    fi
else
    # PID 파일이 없는 경우 프로세스 이름으로 찾기
    BACKEND_PID=$(ps aux | grep "com.waynai.demo.DemoApplication" | grep -v grep | awk '{print $2}')
    if [ ! -z "$BACKEND_PID" ]; then
        echo "백엔드 서버 중지 중... (PID: $BACKEND_PID)"
        kill $BACKEND_PID
        echo "백엔드 서버가 중지되었습니다."
    else
        echo "백엔드 서버가 실행 중이 아닙니다."
    fi
fi

# 프론트엔드 서버 중지 (PID 파일 방식)
if [ -f "waynai-frontend/frontend.pid" ]; then
    FRONTEND_PID=$(cat waynai-frontend/frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null; then
        echo "프론트엔드 서버 중지 중... (PID: $FRONTEND_PID)"
        kill $FRONTEND_PID
        rm waynai-frontend/frontend.pid
        echo "프론트엔드 서버가 중지되었습니다."
    else
        echo "프론트엔드 서버가 실행 중이 아닙니다."
        rm waynai-frontend/frontend.pid
    fi
else
    # PID 파일이 없는 경우 프로세스 이름으로 찾기
    FRONTEND_PID=$(ps aux | grep "vite" | grep -v grep | awk '{print $2}')
    if [ ! -z "$FRONTEND_PID" ]; then
        echo "프론트엔드 서버 중지 중... (PID: $FRONTEND_PID)"
        kill $FRONTEND_PID
        echo "프론트엔드 서버가 중지되었습니다."
    else
        echo "프론트엔드 서버가 실행 중이 아닙니다."
    fi
fi

# Maven 프로세스도 중지 (백엔드 개발 서버)
MAVEN_PID=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')
if [ ! -z "$MAVEN_PID" ]; then
    echo "Maven 프로세스 중지 중... (PID: $MAVEN_PID)"
    kill $MAVEN_PID
    echo "Maven 프로세스가 중지되었습니다."
fi

echo "모든 서버 중지 완료."
