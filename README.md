# WaynAI - AI 기반 여행 파트너

WaynAI는 AI를 활용한 여행 계획 및 관광지 추천 서비스입니다.

## 프로젝트 구조

- `waynai-backend/`: Spring Boot 기반 백엔드 서버
- `waynai-frontend/`: Vue.js 기반 프론트엔드 애플리케이션

## 개발 환경 실행

### 백엔드 서버 실행
```bash
./start-backend-dev.sh
```
- 포트: 8080
- 개발 모드로 실행 (핫 리로드 지원)

### 프론트엔드 서버 실행
```bash
./start-frontend-dev.sh
```
- 포트: 5173
- 개발 모드로 실행 (핫 리로드 지원)

## 운영 환경 실행

### 일반 실행
```bash
# 백엔드
./start-backend-prod.sh

# 프론트엔드
./start-frontend-prod.sh
```

### 백그라운드 실행 (서버 배포용)
```bash
# 백엔드 (백그라운드)
./start-backend-prod-nohup.sh

# 프론트엔드 (백그라운드)
./start-frontend-prod-nohup.sh
```

### 서버 중지
```bash
./stop-servers.sh
```

## 로그 확인

### 개발 환경
- 백엔드: 터미널에서 직접 확인
- 프론트엔드: 터미널에서 직접 확인

### 운영 환경 (백그라운드 실행)
- 백엔드: `tail -f waynai-backend/backend.log`
- 프론트엔드: `tail -f waynai-frontend/frontend.log`

## 포트 정보

- 백엔드: 8080
- 프론트엔드 (개발): 5173
- 프론트엔드 (운영): 3000

## 사전 요구사항

- Java 17+
- Node.js 18+
- Maven 3.6+
- npm 또는 yarn