#!/bin/bash

# WaynAI 서버 초기 설정 스크립트
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

# 설정 변수
PROJECT_ROOT="/var/www/waynai"
LOG_DIR="/var/log/waynai"
NGINX_WEB_ROOT="/var/www/html"

log_info "🚀 WaynAI 서버 초기 설정 시작..."

# 1. 디렉토리 생성
log_info "필요한 디렉토리 생성 중..."
sudo mkdir -p "$PROJECT_ROOT"
sudo mkdir -p "$LOG_DIR"
sudo chown -R $USER:$USER "$PROJECT_ROOT"
sudo chown -R $USER:$USER "$LOG_DIR"

# 2. Git 저장소 클론 (실제 URL로 변경 필요)
log_info "Git 저장소 클론 중..."
cd "$PROJECT_ROOT"
if [ ! -d ".git" ]; then
    # 실제 Git 저장소 URL로 변경하세요
    # git clone https://github.com/your-username/waynai.git .
    log_warning "Git 저장소를 수동으로 클론해주세요:"
    log_warning "cd $PROJECT_ROOT"
    log_warning "git clone https://github.com/your-username/waynai.git ."
fi

# 3. 배포 스크립트 권한 설정
log_info "배포 스크립트 권한 설정 중..."
chmod +x deploy.sh

# 4. Nginx 설정 파일 생성
log_info "Nginx 설정 파일 생성 중..."

# 서버 IP 주소 자동 감지
SERVER_IP=$(curl -s ifconfig.me || curl -s ipinfo.io/ip || hostname -I | awk '{print $1}')
log_info "감지된 서버 IP: $SERVER_IP"

sudo tee /etc/nginx/sites-available/waynai << EOF
server {
    listen 80;
    server_name _ $SERVER_IP;

    # 프론트엔드 정적 파일
    location / {
        root /var/www/html;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }

    # 백엔드 API 프록시 (내부 localhost 사용)
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # CORS 헤더 추가
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
        add_header 'Access-Control-Expose-Headers' 'Content-Length,Content-Range' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;
    }

    # OPTIONS 요청 처리
    location ~* \.(OPTIONS)$ {
        add_header 'Access-Control-Allow-Origin' '*';
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
        add_header 'Access-Control-Max-Age' 1728000;
        add_header 'Content-Type' 'text/plain; charset=utf-8';
        add_header 'Content-Length' 0;
        return 204;
    }
}
EOF

# 5. Nginx 사이트 활성화
log_info "Nginx 사이트 활성화 중..."
sudo ln -sf /etc/nginx/sites-available/waynai /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx

# 6. systemd 서비스 파일 생성 (백엔드 자동 시작용)
log_info "systemd 서비스 파일 생성 중..."
sudo tee /etc/systemd/system/waynai-backend.service << 'EOF'
[Unit]
Description=WaynAI Backend Service
After=network.target

[Service]
Type=simple
User=waynai
Group=waynai
WorkingDirectory=/var/www/waynai/waynai-backend
ExecStart=/usr/bin/java -jar /var/www/waynai/waynai-backend/target/waynai-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/waynai/backend.log
StandardError=append:/var/log/waynai/backend.log

[Install]
WantedBy=multi-user.target
EOF

# 7. waynai 사용자 생성
log_info "waynai 사용자 생성 중..."
sudo adduser --system --group --home /var/www/waynai waynai || true
sudo usermod -aG sudo waynai || true
sudo chown -R waynai:waynai "$PROJECT_ROOT"
sudo chown -R waynai:waynai "$LOG_DIR"

# 8. 방화벽 설정
log_info "방화벽 설정 중..."
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw --force enable

# 9. 로그 로테이션 설정
log_info "로그 로테이션 설정 중..."
sudo tee /etc/logrotate.d/waynai << 'EOF'
/var/log/waynai/*.log {
    daily
    missingok
    rotate 7
    compress
    delaycompress
    notifempty
    create 644 waynai waynai
    postrotate
        systemctl reload waynai-backend
    endscript
}
EOF

log_success "서버 초기 설정 완료!"
log_info "다음 단계:"
log_info "1. Git 저장소를 클론하세요: cd $PROJECT_ROOT && git clone https://github.com/LucestDail/waynai.git ."
log_info "2. 배포를 실행하세요: ./deploy.sh all"
log_info "3. 서비스 상태를 확인하세요: ./deploy.sh status"
log_info ""
log_info "🌐 접속 정보:"
log_info "   프론트엔드: http://$SERVER_IP"
log_info "   백엔드 API: http://$SERVER_IP/api"
log_info "   헬스체크: http://$SERVER_IP/api/health"
