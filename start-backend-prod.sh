#!/bin/bash

# WaynAI 백엔드 운영 서버 실행 스크립트
# 포트: 8080

echo "WaynAI 백엔드 운영 서버 시작..."

cd waynai-backend

# JAR 파일이 존재하는지 확인
if [ ! -f "target/waynai-backend-0.0.1-SNAPSHOT.jar" ]; then
    echo "JAR 파일을 빌드 중..."
    ./mvnw clean package -DskipTests
fi

# 운영 환경으로 JAR 실행
java -jar target/waynai-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
