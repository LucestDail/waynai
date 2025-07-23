#!/bin/bash

# WaynAI 개발 서버 시작 스크립트
# 스프링 백엔드와 Vue 프론트엔드를 순차적으로 실행합니다.

echo "🚀 WaynAI 개발 서버를 시작합니다..."

# 현재 디렉토리를 프로젝트 루트로 설정
cd "$(dirname "$0")"

# 백그라운드에서 실행할 프로세스들을 저장할 배열
declare -a pids=()

# 함수: 프로세스 종료 시 모든 백그라운드 프로세스를 정리
cleanup() {
    echo "🛑 서버를 종료합니다..."
    for pid in "${pids[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            echo "프로세스 $pid 종료됨"
        fi
    done
    exit 0
}

# 시그널 핸들러 설정
trap cleanup SIGINT SIGTERM

# 함수: 백엔드 서버가 준비될 때까지 대기
wait_for_backend() {
    echo "⏳ 백엔드 서버가 시작될 때까지 대기 중..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo "✅ 백엔드 서버가 정상적으로 시작되었습니다!"
            return 0
        fi
        
        echo "🔄 백엔드 서버 준비 중... ($attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done
    
    echo "❌ 백엔드 서버 시작 실패 (최대 대기 시간 초과)"
    return 1
}

# 스프링 백엔드 시작
echo "📦 스프링 백엔드를 시작합니다..."
cd waynai-backend
./mvnw spring-boot:run > /dev/null 2>&1 &
backend_pid=$!
pids+=($backend_pid)
cd ..

# 백엔드 서버가 준비될 때까지 대기
if wait_for_backend; then
    # Vue 프론트엔드 시작
    echo "🎨 Vue 프론트엔드를 시작합니다..."
    cd waynai-frontend
    npm run dev > /dev/null 2>&1 &
    frontend_pid=$!
    pids+=($frontend_pid)
    cd ..
    
    # 프론트엔드 서버가 준비될 때까지 대기
    echo "⏳ 프론트엔드 서버가 시작될 때까지 대기 중..."
    sleep 5
    
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        echo ""
        echo "🎉 모든 서버가 정상적으로 시작되었습니다!"
        echo "📱 프론트엔드: http://localhost:5173"
        echo "🔧 백엔드: http://localhost:8080"
        echo "📊 백엔드 상태: http://localhost:8080/actuator/health"
        echo ""
        echo "서버를 종료하려면 Ctrl+C를 누르세요."
    else
        echo "⚠️  프론트엔드 서버 시작에 문제가 있을 수 있습니다."
        echo "📱 프론트엔드: http://localhost:5173 (수동으로 확인해주세요)"
        echo "🔧 백엔드: http://localhost:8080"
    fi
else
    echo "❌ 백엔드 서버 시작 실패로 프론트엔드를 시작하지 않습니다."
    cleanup
fi

# 모든 프로세스가 종료될 때까지 대기
wait 