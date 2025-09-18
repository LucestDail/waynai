#!/bin/bash

# WaynAI 백엔드 개발 서버 실행 스크립트
# 포트: 8080

echo "WaynAI 백엔드 개발 서버 시작..."

cd waynai-backend

# Maven을 사용하여 Spring Boot 애플리케이션 실행
./mvnw spring-boot:run
