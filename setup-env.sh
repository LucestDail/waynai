#!/bin/bash

# WaynAI 서버 환경변수 설정 스크립트
# 서버에서 실행하여 환경변수를 설정합니다

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

log_info "🔧 WaynAI 서버 환경변수 설정 시작..."

# 1. Gemini API 키 설정
if [ -z "$GEMINI_API_KEY" ]; then
    log_warning "GEMINI_API_KEY 환경변수가 설정되지 않았습니다."
    read -p "Gemini API 키를 입력하세요: " GEMINI_API_KEY
    
    if [ -z "$GEMINI_API_KEY" ]; then
        log_error "API 키가 입력되지 않았습니다."
        exit 1
    fi
fi

# 2. 환경변수 파일 생성
log_info "환경변수 파일 생성 중..."
sudo tee /etc/environment.d/waynai << EOF
# WaynAI 환경변수
GEMINI_API_KEY=$GEMINI_API_KEY
EOF

# 3. 현재 사용자 환경변수 설정
log_info "사용자 환경변수 설정 중..."
echo "export GEMINI_API_KEY=\"$GEMINI_API_KEY\"" >> ~/.bashrc
echo "export GEMINI_API_KEY=\"$GEMINI_API_KEY\"" >> ~/.profile

# 4. systemd 서비스에 환경변수 추가
log_info "systemd 서비스 환경변수 설정 중..."
sudo mkdir -p /etc/systemd/system/waynai-backend.service.d
sudo tee /etc/systemd/system/waynai-backend.service.d/override.conf << EOF
[Service]
Environment=GEMINI_API_KEY=$GEMINI_API_KEY
EOF

# 5. 환경변수 적용
log_info "환경변수 적용 중..."
source ~/.bashrc
sudo systemctl daemon-reload

# 6. 환경변수 확인
log_info "환경변수 확인 중..."
if [ -n "$GEMINI_API_KEY" ]; then
    log_success "GEMINI_API_KEY 설정 완료: ${GEMINI_API_KEY:0:10}..."
else
    log_error "GEMINI_API_KEY 설정 실패"
    exit 1
fi

log_success "환경변수 설정 완료!"
log_info "다음 단계:"
log_info "1. 서버를 재부팅하거나 새 터미널 세션을 시작하세요"
log_info "2. 배포를 실행하세요: ./deploy.sh all"
log_info "3. 서비스 상태를 확인하세요: ./deploy.sh status"
