#!/bin/bash

# PATH 환경변수 수정 스크립트
# Ubuntu 서버에서 실행

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

log_info "🔧 PATH 환경변수 수정 시작..."

# 1. 현재 PATH 확인
log_info "현재 PATH: $PATH"

# 2. 기본 PATH 설정
DEFAULT_PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

# 3. /etc/environment 파일 수정
log_info "/etc/environment 파일 수정 중..."
sudo tee /etc/environment << EOF
PATH="$DEFAULT_PATH"
JAVA_HOME="/usr/lib/jvm/java-17-amazon-corretto"
MAVEN_HOME="/opt/maven"
EOF

# 4. /etc/profile 파일에 PATH 추가
log_info "/etc/profile 파일 수정 중..."
if ! grep -q "export PATH=" /etc/profile; then
    echo "export PATH=\"$DEFAULT_PATH\"" | sudo tee -a /etc/profile
fi

# 5. 사용자별 설정 파일 수정
log_info "사용자별 설정 파일 수정 중..."

# .bashrc 수정
if ! grep -q "export PATH=" ~/.bashrc; then
    echo "export PATH=\"$DEFAULT_PATH\"" >> ~/.bashrc
fi

# .profile 수정
if ! grep -q "export PATH=" ~/.profile; then
    echo "export PATH=\"$DEFAULT_PATH\"" >> ~/.profile
fi

# 6. 현재 세션에 PATH 적용
log_info "현재 세션에 PATH 적용 중..."
export PATH="$DEFAULT_PATH"

# 7. 명령어 테스트
log_info "명령어 테스트 중..."
if command -v lesspipe >/dev/null 2>&1; then
    log_success "lesspipe 명령어 사용 가능"
else
    log_warning "lesspipe 명령어를 찾을 수 없습니다. 직접 경로로 실행합니다."
    /usr/bin/lesspipe --help >/dev/null 2>&1 && log_success "lesspipe (/usr/bin/lesspipe) 정상 작동"
fi

if command -v dircolors >/dev/null 2>&1; then
    log_success "dircolors 명령어 사용 가능"
else
    log_warning "dircolors 명령어를 찾을 수 없습니다. 직접 경로로 실행합니다."
    /bin/dircolors --help >/dev/null 2>&1 && log_success "dircolors (/bin/dircolors) 정상 작동"
fi

# 8. 심볼릭 링크 생성 (대안)
log_info "심볼릭 링크 생성 중..."
sudo ln -sf /usr/bin/lesspipe /usr/local/bin/lesspipe 2>/dev/null || true
sudo ln -sf /bin/dircolors /usr/local/bin/dircolors 2>/dev/null || true

# 9. 최종 PATH 확인
log_info "수정된 PATH: $PATH"

log_success "PATH 환경변수 수정 완료!"
log_info "다음 중 하나를 실행하세요:"
log_info "1. 새 터미널 세션 시작"
log_info "2. source ~/.bashrc"
log_info "3. 서버 재부팅"
