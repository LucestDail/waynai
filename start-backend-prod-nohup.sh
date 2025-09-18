#!/bin/bash

# WaynAI 백엔드 운영 서버 실행 스크립트 (백그라운드 실행)
# 포트: 8080

echo "WaynAI 백엔드 운영 서버 시작 (백그라운드 실행)..."

cd waynai-backend

# JAR 파일이 존재하는지 확인
if [ ! -f "target/waynai-backend-0.0.1-SNAPSHOT.jar" ]; then
    echo "JAR 파일을 빌드 중..."
    ./mvnw clean package -DskipTests
fi

# 백그라운드에서 실행하고 로그를 파일로 저장
nohup java -jar target/waynai-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > backend.log 2>&1 &

# 프로세스 ID 저장
echo $! > backend.pid

echo "백엔드 서버가 백그라운드에서 시작되었습니다."
echo "프로세스 ID: $(cat backend.pid)"
echo "로그 확인: tail -f backend.log"
