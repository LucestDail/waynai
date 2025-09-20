#!/bin/bash

# WaynAI 지속적 배포 스크립트
# 사용법: ./deploy.sh [backend|frontend|all]

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
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
PROJECT_ROOT="/var/www/waynai"
BACKEND_DIR="$PROJECT_ROOT/waynai-backend"
FRONTEND_DIR="$PROJECT_ROOT/waynai-frontend"
BACKEND_JAR="waynai-backend-0.0.1-SNAPSHOT.jar"
BACKEND_PID_FILE="/var/run/waynai-backend.pid"
LOG_DIR="/var/log/waynai"
NGINX_WEB_ROOT="/var/www/html"

# Git 저장소 URL (실제 저장소 URL로 변경 필요)
GIT_REPO_URL="https://github.com/LucestDail/waynai.git"

# 자동 fix 함수들
fix_environment() {
    log_info "🔧 환경 설정 자동 수정 중..."
    
    # 1. PATH 환경변수 수정
    export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"
    export PATH="/opt/maven/bin:$PATH"
    export MAVEN_HOME="/opt/maven"
    
    # 2. Maven 명령어 확인 및 설정
    if ! command -v mvn >/dev/null 2>&1; then
        if [ -f "/opt/maven/bin/mvn" ]; then
            export PATH="/opt/maven/bin:$PATH"
            log_info "Maven PATH 설정: /opt/maven/bin"
        elif [ -f "/usr/bin/mvn" ]; then
            log_info "시스템 Maven 사용: /usr/bin/mvn"
        else
            log_warning "Maven을 찾을 수 없습니다. 시스템 Maven 설치를 시도합니다."
            sudo apt update && sudo apt install -y maven
        fi
    fi
    
    # 3. Java 환경변수 설정
    if [ -z "$JAVA_HOME" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-amazon-corretto"
        log_info "JAVA_HOME 설정: $JAVA_HOME"
    fi
    
    # 4. 환경변수 영구 설정
    echo 'export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"' >> ~/.bashrc
    echo 'export PATH="/opt/maven/bin:$PATH"' >> ~/.bashrc
    echo 'export MAVEN_HOME="/opt/maven"' >> ~/.bashrc
    echo 'export JAVA_HOME="/usr/lib/jvm/java-17-amazon-corretto"' >> ~/.bashrc
    
    log_success "환경 설정 수정 완료"
}

# 백엔드 배포 함수
deploy_backend() {
    log_info "🚀 백엔드 배포 시작..."
    
    # 환경 설정 자동 수정
    fix_environment
    
    # 1. 기존 백엔드 프로세스 중지
    log_info "기존 백엔드 프로세스 중지 중..."
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            log_success "백엔드 프로세스 중지됨 (PID: $PID)"
        fi
        rm -f "$BACKEND_PID_FILE"
    fi
    
    # 2. 소스 코드 최신화
    log_info "소스 코드 최신화 중..."
    cd "$PROJECT_ROOT"
    if [ -d ".git" ]; then
        git pull origin main
    else
        log_warning "Git 저장소가 아닙니다. 수동으로 코드를 업데이트해주세요."
    fi
    
    # 3. API 키 파일 처리
    log_info "API 키 파일 처리 중..."
    PROPERTIES_FILE="$BACKEND_DIR/src/main/resources/application.properties"
    
    # Gemini API 키 처리
    GEMINI_KEY_FILE="/var/www/key/key"
    if [ -f "$GEMINI_KEY_FILE" ]; then
        GEMINI_API_KEY=$(cat "$GEMINI_KEY_FILE" | tr -d '\n\r')
        if [ -n "$GEMINI_API_KEY" ]; then
            log_info "Gemini API 키 파일에서 키를 읽었습니다: ${GEMINI_API_KEY:0:10}..."
            
            if [ -f "$PROPERTIES_FILE" ]; then
                if grep -q "gemini.api.key" "$PROPERTIES_FILE"; then
                    sed -i "s/gemini.api.key=.*/gemini.api.key=$GEMINI_API_KEY/" "$PROPERTIES_FILE"
                    log_success "Gemini API 키가 업데이트되었습니다."
                else
                    echo "gemini.api.key=$GEMINI_API_KEY" >> "$PROPERTIES_FILE"
                    log_success "Gemini API 키가 추가되었습니다."
                fi
            fi
        else
            log_warning "Gemini API 키 파일이 비어있습니다: $GEMINI_KEY_FILE"
        fi
    else
        log_warning "Gemini API 키 파일을 찾을 수 없습니다: $GEMINI_KEY_FILE"
    fi
    
    # Naver API ID 처리
    NAVER_ID_FILE="/var/www/key/naverId"
    if [ -f "$NAVER_ID_FILE" ]; then
        NAVER_API_ID=$(cat "$NAVER_ID_FILE" | tr -d '\n\r')
        if [ -n "$NAVER_API_ID" ]; then
            log_info "Naver API ID 파일에서 ID를 읽었습니다: ${NAVER_API_ID:0:10}..."
            
            if [ -f "$PROPERTIES_FILE" ]; then
                if grep -q "naver.api.client.id" "$PROPERTIES_FILE"; then
                    sed -i "s/naver.api.client.id=.*/naver.api.client.id=$NAVER_API_ID/" "$PROPERTIES_FILE"
                    log_success "Naver API ID가 업데이트되었습니다."
                else
                    echo "naver.api.client.id=$NAVER_API_ID" >> "$PROPERTIES_FILE"
                    log_success "Naver API ID가 추가되었습니다."
                fi
            fi
        else
            log_warning "Naver API ID 파일이 비어있습니다: $NAVER_ID_FILE"
        fi
    else
        log_warning "Naver API ID 파일을 찾을 수 없습니다: $NAVER_ID_FILE"
    fi
    
    # Naver API Secret 처리
    NAVER_SECRET_FILE="/var/www/key/naverSecret"
    if [ -f "$NAVER_SECRET_FILE" ]; then
        NAVER_API_SECRET=$(cat "$NAVER_SECRET_FILE" | tr -d '\n\r')
        if [ -n "$NAVER_API_SECRET" ]; then
            log_info "Naver API Secret 파일에서 Secret을 읽었습니다: ${NAVER_API_SECRET:0:10}..."
            
            if [ -f "$PROPERTIES_FILE" ]; then
                if grep -q "naver.api.client.secret" "$PROPERTIES_FILE"; then
                    sed -i "s/naver.api.client.secret=.*/naver.api.client.secret=$NAVER_API_SECRET/" "$PROPERTIES_FILE"
                    log_success "Naver API Secret이 업데이트되었습니다."
                else
                    echo "naver.api.client.secret=$NAVER_API_SECRET" >> "$PROPERTIES_FILE"
                    log_success "Naver API Secret이 추가되었습니다."
                fi
            fi
        else
            log_warning "Naver API Secret 파일이 비어있습니다: $NAVER_SECRET_FILE"
        fi
    else
        log_warning "Naver API Secret 파일을 찾을 수 없습니다: $NAVER_SECRET_FILE"
    fi
    
    # 4. 백엔드 빌드
    log_info "백엔드 빌드 중..."
    cd "$BACKEND_DIR"
    
    # Maven 명령어 확인 및 실행
    MAVEN_CMD=""
    if command -v mvn >/dev/null 2>&1; then
        MAVEN_CMD="mvn"
        log_success "Maven 명령어 사용 가능: $(which mvn)"
    elif [ -f "/opt/maven/bin/mvn" ]; then
        MAVEN_CMD="/opt/maven/bin/mvn"
        log_info "Maven 직접 경로 사용: $MAVEN_CMD"
    elif [ -f "/usr/bin/mvn" ]; then
        MAVEN_CMD="/usr/bin/mvn"
        log_info "Maven 시스템 경로 사용: $MAVEN_CMD"
    else
        log_error "Maven을 찾을 수 없습니다. PATH: $PATH"
        log_info "사용 가능한 Maven 경로를 찾는 중..."
        find /opt /usr -name "mvn" -type f 2>/dev/null | head -1 | while read mvn_path; do
            log_info "발견된 Maven: $mvn_path"
            MAVEN_CMD="$mvn_path"
        done
        
        if [ -z "$MAVEN_CMD" ]; then
            log_error "Maven을 찾을 수 없습니다. Maven 설치가 필요합니다."
            log_info "Maven 설치를 위해 다음 명령어를 실행하세요:"
            log_info "sudo apt install maven"
            exit 1
        fi
    fi
    
    log_info "Maven 빌드 실행 중: $MAVEN_CMD"
    $MAVEN_CMD clean package -DskipTests
    
    if [ ! -f "target/$BACKEND_JAR" ]; then
        log_error "빌드 실패: JAR 파일이 생성되지 않았습니다."
        exit 1
    fi
    
    log_success "백엔드 빌드 완료"
    
    # 4. 백엔드 재시작
    log_info "백엔드 서비스 시작 중..."
    nohup java -jar "target/$BACKEND_JAR" > "$LOG_DIR/backend.log" 2>&1 &
    echo $! > "$BACKEND_PID_FILE"
    
    # 5. 서비스 상태 확인
    sleep 5
    if ps -p $(cat "$BACKEND_PID_FILE") > /dev/null 2>&1; then
        log_success "백엔드 서비스 시작 완료 (PID: $(cat $BACKEND_PID_FILE))"
    else
        log_error "백엔드 서비스 시작 실패"
        exit 1
    fi
}

# 프론트엔드 배포 함수
deploy_frontend() {
    log_info "🎨 프론트엔드 배포 시작..."
    
    # 환경 설정 자동 수정
    fix_environment
    
    # 1. 소스 코드 최신화
    log_info "소스 코드 최신화 중..."
    cd "$PROJECT_ROOT"
    if [ -d ".git" ]; then
        git pull origin main
    else
        log_warning "Git 저장소가 아닙니다. 수동으로 코드를 업데이트해주세요."
    fi
    
    # 2. 프론트엔드 빌드
    log_info "프론트엔드 빌드 중..."
    cd "$FRONTEND_DIR"
    
    # 프로덕션 환경변수 설정
    echo "VITE_API_BASE_URL=http://3.35.206.187:8080" > .env.production
    
    npm install
    npm run build
    
    if [ ! -d "dist" ]; then
        log_error "빌드 실패: dist 디렉토리가 생성되지 않았습니다."
        exit 1
    fi
    
    log_success "프론트엔드 빌드 완료"
    
    # 3. 정적 파일 배포
    log_info "정적 파일 배포 중..."
    sudo rm -rf "$NGINX_WEB_ROOT"/*
    sudo cp -r dist/* "$NGINX_WEB_ROOT/"
    sudo chown -R www-data:www-data "$NGINX_WEB_ROOT"
    
    # 4. Nginx 재시작
    log_info "Nginx 재시작 중..."
    sudo systemctl reload nginx
    
    log_success "프론트엔드 배포 완료"
}

# 전체 배포 함수
deploy_all() {
    log_info "🚀 전체 배포 시작..."
    
    # 환경 설정 자동 수정
    fix_environment
    
    deploy_backend
    deploy_frontend
    log_success "전체 배포 완료!"
}

# 서비스 상태 확인 함수
check_status() {
    log_info "📊 서비스 상태 확인..."
    
    # 백엔드 상태
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            log_success "백엔드: 실행 중 (PID: $PID)"
        else
            log_error "백엔드: 중지됨"
        fi
    else
        log_warning "백엔드: PID 파일 없음"
    fi
    
    # Nginx 상태
    if systemctl is-active --quiet nginx; then
        log_success "Nginx: 실행 중"
    else
        log_error "Nginx: 중지됨"
    fi
    
    # 포트 확인
    if netstat -tlnp | grep -q ":8080"; then
        log_success "포트 8080: 사용 중"
    else
        log_warning "포트 8080: 사용되지 않음"
    fi
    
    if netstat -tlnp | grep -q ":80"; then
        log_success "포트 80: 사용 중"
    else
        log_warning "포트 80: 사용되지 않음"
    fi
}

# 롤백 함수
rollback() {
    log_info "🔄 롤백 시작..."
    
    # 백엔드 롤백
    if [ -f "$BACKEND_PID_FILE" ]; then
        PID=$(cat "$BACKEND_PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            rm -f "$BACKEND_PID_FILE"
        fi
    fi
    
    # 이전 버전으로 복원 (Git 사용)
    cd "$PROJECT_ROOT"
    git reset --hard HEAD~1
    
    # 재배포
    deploy_all
    log_success "롤백 완료"
}

# 메인 실행 부분
case "${1:-all}" in
    "backend")
        deploy_backend
        ;;
    "frontend")
        deploy_frontend
        ;;
    "all")
        deploy_all
        ;;
    "status")
        check_status
        ;;
    "rollback")
        rollback
        ;;
    "fix")
        fix_environment
        log_success "환경 설정 수정 완료!"
        ;;
    "set-key")
        log_info "API 키 설정 중..."
        KEY_FILE="/var/www/key/key"
        sudo mkdir -p "$(dirname "$KEY_FILE")"
        
        if [ -n "$2" ]; then
            echo "$2" | sudo tee "$KEY_FILE" > /dev/null
            sudo chmod 600 "$KEY_FILE"
            sudo chown $USER:$USER "$KEY_FILE"
            log_success "Gemini API 키가 설정되었습니다: ${2:0:10}..."
        else
            read -p "Gemini API 키를 입력하세요: " api_key
            if [ -n "$api_key" ]; then
                echo "$api_key" | sudo tee "$KEY_FILE" > /dev/null
                sudo chmod 600 "$KEY_FILE"
                sudo chown $USER:$USER "$KEY_FILE"
                log_success "Gemini API 키가 설정되었습니다: ${api_key:0:10}..."
            else
                log_error "API 키가 입력되지 않았습니다."
                exit 1
            fi
        fi
        ;;
    "set-naver-id")
        log_info "Naver API ID 설정 중..."
        NAVER_ID_FILE="/var/www/key/naverId"
        sudo mkdir -p "$(dirname "$NAVER_ID_FILE")"
        
        if [ -n "$2" ]; then
            echo "$2" | sudo tee "$NAVER_ID_FILE" > /dev/null
            sudo chmod 600 "$NAVER_ID_FILE"
            sudo chown $USER:$USER "$NAVER_ID_FILE"
            log_success "Naver API ID가 설정되었습니다: ${2:0:10}..."
        else
            read -p "Naver API ID를 입력하세요: " naver_id
            if [ -n "$naver_id" ]; then
                echo "$naver_id" | sudo tee "$NAVER_ID_FILE" > /dev/null
                sudo chmod 600 "$NAVER_ID_FILE"
                sudo chown $USER:$USER "$NAVER_ID_FILE"
                log_success "Naver API ID가 설정되었습니다: ${naver_id:0:10}..."
            else
                log_error "Naver API ID가 입력되지 않았습니다."
                exit 1
            fi
        fi
        ;;
    "set-naver-secret")
        log_info "Naver API Secret 설정 중..."
        NAVER_SECRET_FILE="/var/www/key/naverSecret"
        sudo mkdir -p "$(dirname "$NAVER_SECRET_FILE")"
        
        if [ -n "$2" ]; then
            echo "$2" | sudo tee "$NAVER_SECRET_FILE" > /dev/null
            sudo chmod 600 "$NAVER_SECRET_FILE"
            sudo chown $USER:$USER "$NAVER_SECRET_FILE"
            log_success "Naver API Secret이 설정되었습니다: ${2:0:10}..."
        else
            read -p "Naver API Secret을 입력하세요: " naver_secret
            if [ -n "$naver_secret" ]; then
                echo "$naver_secret" | sudo tee "$NAVER_SECRET_FILE" > /dev/null
                sudo chmod 600 "$NAVER_SECRET_FILE"
                sudo chown $USER:$USER "$NAVER_SECRET_FILE"
                log_success "Naver API Secret이 설정되었습니다: ${naver_secret:0:10}..."
            else
                log_error "Naver API Secret이 입력되지 않았습니다."
                exit 1
            fi
        fi
        ;;
    "set-all-keys")
        log_info "모든 API 키 설정 중..."
        
        # Gemini API 키
        read -p "Gemini API 키를 입력하세요: " gemini_key
        if [ -n "$gemini_key" ]; then
            echo "$gemini_key" | sudo tee "/var/www/key/key" > /dev/null
            sudo chmod 600 "/var/www/key/key"
            sudo chown $USER:$USER "/var/www/key/key"
            log_success "Gemini API 키 설정 완료"
        fi
        
        # Naver API ID
        read -p "Naver API ID를 입력하세요: " naver_id
        if [ -n "$naver_id" ]; then
            echo "$naver_id" | sudo tee "/var/www/key/naverId" > /dev/null
            sudo chmod 600 "/var/www/key/naverId"
            sudo chown $USER:$USER "/var/www/key/naverId"
            log_success "Naver API ID 설정 완료"
        fi
        
        # Naver API Secret
        read -p "Naver API Secret을 입력하세요: " naver_secret
        if [ -n "$naver_secret" ]; then
            echo "$naver_secret" | sudo tee "/var/www/key/naverSecret" > /dev/null
            sudo chmod 600 "/var/www/key/naverSecret"
            sudo chown $USER:$USER "/var/www/key/naverSecret"
            log_success "Naver API Secret 설정 완료"
        fi
        
        log_success "모든 API 키 설정이 완료되었습니다!"
        ;;
    *)
        echo "사용법: $0 [backend|frontend|all|status|rollback|fix|set-key|set-naver-id|set-naver-secret|set-all-keys]"
        echo ""
        echo "옵션:"
        echo "  backend         - 백엔드만 배포"
        echo "  frontend        - 프론트엔드만 배포"
        echo "  all             - 전체 배포 (기본값)"
        echo "  status          - 서비스 상태 확인"
        echo "  rollback        - 이전 버전으로 롤백"
        echo "  fix             - 환경 설정 수정만 실행"
        echo "  set-key         - Gemini API 키 설정"
        echo "  set-naver-id    - Naver API ID 설정"
        echo "  set-naver-secret- Naver API Secret 설정"
        echo "  set-all-keys    - 모든 API 키 설정 (대화형)"
        echo ""
        echo "예시:"
        echo "  ./deploy.sh set-key YOUR_GEMINI_KEY"
        echo "  ./deploy.sh set-naver-id YOUR_NAVER_ID"
        echo "  ./deploy.sh set-naver-secret YOUR_NAVER_SECRET"
        echo "  ./deploy.sh set-all-keys"
        exit 1
        ;;
esac

log_success "배포 스크립트 실행 완료!"
