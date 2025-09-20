#!/bin/bash

# WaynAI 로컬 개발 환경 배포 스크립트
# 개발 중 빠른 배포를 위한 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 설정 변수
BACKEND_DIR="./waynai-backend"
FRONTEND_DIR="./waynai-frontend"
BACKEND_JAR="waynai-backend-0.0.1-SNAPSHOT.jar"
BACKEND_PID_FILE="./backend.pid"

# 백엔드 로컬 배포
deploy_backend_local() {
    log_info "🚀 백엔드 로컬 배포 시작..."
    
    # 1. 기존 프로세스 중지
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            log_success "기존 백엔드 프로세스 중지됨 (PID: $PID)"
        fi
        rm -f "$BACKEND_PID_FILE"
    fi
    
    # 2. 백엔드 빌드
    log_info "백엔드 빌드 중..."
    cd "$BACKEND_DIR"
    mvn clean package -DskipTests
    
    if [ ! -f "target/$BACKEND_JAR" ]; then
        log_error "빌드 실패: JAR 파일이 생성되지 않았습니다."
        exit 1
    fi
    
    # 3. 백엔드 실행
    log_info "백엔드 실행 중..."
    nohup java -jar "target/$BACKEND_JAR" > ../backend.log 2>&1 &
    echo $! > "../$BACKEND_PID_FILE"
    
    cd ..
    log_success "백엔드 실행 완료 (PID: $(cat $BACKEND_PID_FILE))"
    log_info "백엔드 로그: tail -f backend.log"
}

# 프론트엔드 로컬 배포
deploy_frontend_local() {
    log_info "🎨 프론트엔드 로컬 배포 시작..."
    
    # 1. 프론트엔드 빌드
    log_info "프론트엔드 빌드 중..."
    cd "$FRONTEND_DIR"
    npm install
    npm run build
    
    if [ ! -d "dist" ]; then
        log_error "빌드 실패: dist 디렉토리가 생성되지 않았습니다."
        exit 1
    fi
    
    # 2. 프론트엔드 실행 (Vite preview)
    log_info "프론트엔드 실행 중..."
    nohup npm run preview > ../frontend.log 2>&1 &
    echo $! > "../frontend.pid"
    
    cd ..
    log_success "프론트엔드 실행 완료 (PID: $(cat frontend.pid))"
    log_info "프론트엔드 로그: tail -f frontend.log"
}

# 전체 로컬 배포
deploy_all_local() {
    log_info "🚀 전체 로컬 배포 시작..."
    deploy_backend_local
    deploy_frontend_local
    log_success "전체 로컬 배포 완료!"
    log_info "백엔드: http://localhost:8080"
    log_info "프론트엔드: http://localhost:4173"
}

# 로컬 서비스 중지
stop_local() {
    log_info "🛑 로컬 서비스 중지 중..."
    
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            log_success "백엔드 중지됨 (PID: $PID)"
        fi
        rm -f "$BACKEND_PID_FILE"
    fi
    
    if [ -f "frontend.pid" ]; then
        PID=$(cat "frontend.pid")
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            log_success "프론트엔드 중지됨 (PID: $PID)"
        fi
        rm -f "frontend.pid"
    fi
}

# 로컬 서비스 상태 확인
status_local() {
    log_info "📊 로컬 서비스 상태 확인..."
    
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            log_success "백엔드: 실행 중 (PID: $PID) - http://localhost:8080"
        else
            log_error "백엔드: 중지됨"
        fi
    else
        log_warning "백엔드: PID 파일 없음"
    fi
    
    if [ -f "frontend.pid" ]; then
        PID=$(cat "frontend.pid")
        if ps -p $PID > /dev/null 2>&1; then
            log_success "프론트엔드: 실행 중 (PID: $PID) - http://localhost:4173"
        else
            log_error "프론트엔드: 중지됨"
        fi
    else
        log_warning "프론트엔드: PID 파일 없음"
    fi
}

# 메인 실행 부분
case "${1:-all}" in
    "backend")
        deploy_backend_local
        ;;
    "frontend")
        deploy_frontend_local
        ;;
    "all")
        deploy_all_local
        ;;
    "stop")
        stop_local
        ;;
    "status")
        status_local
        ;;
    *)
        echo "사용법: $0 [backend|frontend|all|stop|status]"
        echo ""
        echo "옵션:"
        echo "  backend   - 백엔드만 로컬 실행"
        echo "  frontend  - 프론트엔드만 로컬 실행"
        echo "  all       - 전체 로컬 실행 (기본값)"
        echo "  stop      - 모든 로컬 서비스 중지"
        echo "  status    - 로컬 서비스 상태 확인"
        exit 1
        ;;
esac
