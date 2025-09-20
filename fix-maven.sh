#!/bin/bash

# Maven PATH 문제 해결 스크립트
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

log_info "🔧 Maven PATH 문제 해결 시작..."

# 1. Maven 설치 확인
log_info "Maven 설치 상태 확인 중..."
if [ -f "/opt/maven/bin/mvn" ]; then
    log_success "Maven이 /opt/maven/bin/mvn에 설치되어 있습니다."
    MAVEN_PATH="/opt/maven/bin/mvn"
elif [ -f "/usr/bin/mvn" ]; then
    log_success "Maven이 /usr/bin/mvn에 설치되어 있습니다."
    MAVEN_PATH="/usr/bin/mvn"
elif command -v mvn >/dev/null 2>&1; then
    MAVEN_PATH=$(which mvn)
    log_success "Maven이 $MAVEN_PATH에 설치되어 있습니다."
else
    log_error "Maven을 찾을 수 없습니다. 설치가 필요합니다."
    log_info "Maven 설치 중..."
    
    # Maven 3.9.3 설치
    cd /opt
    sudo wget https://archive.apache.org/dist/maven/maven-3/3.9.3/binaries/apache-maven-3.9.3-bin.tar.gz
    sudo tar -xzf apache-maven-3.9.3-bin.tar.gz
    sudo mv apache-maven-3.9.3 maven
    sudo rm apache-maven-3.9.3-bin.tar.gz
    
    MAVEN_PATH="/opt/maven/bin/mvn"
    log_success "Maven 설치 완료: $MAVEN_PATH"
fi

# 2. Maven 버전 확인
log_info "Maven 버전 확인 중..."
if [ -f "$MAVEN_PATH" ]; then
    MAVEN_VERSION=$($MAVEN_PATH --version | head -1)
    log_success "Maven 버전: $MAVEN_VERSION"
else
    log_error "Maven 실행 파일을 찾을 수 없습니다: $MAVEN_PATH"
    exit 1
fi

# 3. PATH 환경변수 설정
log_info "PATH 환경변수 설정 중..."

# Maven 디렉토리 추출
MAVEN_DIR=$(dirname "$MAVEN_PATH")
MAVEN_HOME_DIR=$(dirname "$MAVEN_DIR")

# 현재 세션에 PATH 설정
export PATH="$MAVEN_DIR:$PATH"
export MAVEN_HOME="$MAVEN_HOME_DIR"

# 영구 설정 파일에 추가
echo "export PATH=\"$MAVEN_DIR:\$PATH\"" >> ~/.bashrc
echo "export MAVEN_HOME=\"$MAVEN_HOME_DIR\"" >> ~/.bashrc

# 4. /etc/environment 파일 수정
log_info "/etc/environment 파일 수정 중..."
sudo tee /etc/environment << EOF
PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$MAVEN_DIR"
JAVA_HOME="/usr/lib/jvm/java-17-amazon-corretto"
MAVEN_HOME="$MAVEN_HOME_DIR"
EOF

# 5. systemd 서비스에 환경변수 추가
log_info "systemd 서비스 환경변수 설정 중..."
sudo mkdir -p /etc/systemd/system/waynai-backend.service.d
sudo tee /etc/systemd/system/waynai-backend.service.d/override.conf << EOF
[Service]
Environment=PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$MAVEN_DIR
Environment=JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
Environment=MAVEN_HOME=$MAVEN_HOME_DIR
EOF

# 6. 환경변수 적용
log_info "환경변수 적용 중..."
source ~/.bashrc
sudo systemctl daemon-reload

# 7. Maven 명령어 테스트
log_info "Maven 명령어 테스트 중..."
if command -v mvn >/dev/null 2>&1; then
    log_success "Maven 명령어 사용 가능: $(which mvn)"
    mvn --version | head -1
else
    log_warning "Maven 명령어를 찾을 수 없습니다. 직접 경로로 실행합니다."
    log_info "직접 실행: $MAVEN_PATH --version"
    $MAVEN_PATH --version | head -1
fi

log_success "Maven PATH 설정 완료!"
log_info "다음 단계:"
log_info "1. 새 터미널 세션을 시작하거나 source ~/.bashrc 실행"
log_info "2. 배포를 실행하세요: ./deploy.sh all"
