#!/bin/bash

# 서버 IP 주소 업데이트 스크립트
# 서버 IP가 변경되었을 때 실행

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

# 서버 IP 주소 감지
SERVER_IP=$(curl -s ifconfig.me || curl -s ipinfo.io/ip || hostname -I | awk '{print $1}')

if [ -z "$SERVER_IP" ]; then
    log_error "서버 IP 주소를 감지할 수 없습니다."
    exit 1
fi

log_info "감지된 서버 IP: $SERVER_IP"

# 1. Nginx 설정 업데이트
log_info "Nginx 설정 업데이트 중..."
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

# 2. Nginx 설정 테스트 및 재시작
sudo nginx -t
sudo systemctl reload nginx

# 3. 프론트엔드 환경변수 업데이트
log_info "프론트엔드 환경변수 업데이트 중..."
cd /var/www/waynai/waynai-frontend
echo "VITE_API_BASE_URL=http://$SERVER_IP:8080" > .env.production

# 4. 프론트엔드 재빌드
log_info "프론트엔드 재빌드 중..."
npm run build

# 5. 정적 파일 재배포
log_info "정적 파일 재배포 중..."
sudo rm -rf /var/www/html/*
sudo cp -r dist/* /var/www/html/
sudo chown -R www-data:www-data /var/www/html

log_success "서버 IP 업데이트 완료!"
log_info "🌐 접속 정보:"
log_info "   프론트엔드: http://$SERVER_IP"
log_info "   백엔드 API: http://$SERVER_IP/api"
log_info "   헬스체크: http://$SERVER_IP/api/health"
