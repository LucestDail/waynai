# WaynAI — AI 여행 플래너

> "당신의 길을 함께 찾는 여행 파트너". Google Gemini AI와 공공 API를 활용하여 맞춤형 여행 계획, 관광 정보, 연관 명소 추천을 제공하는 풀스택 웹 애플리케이션입니다.

## 주요 기능

- **AI 여행 계획** — Gemini 기반 SSE 스트리밍으로 실시간 여행 일정 생성
- **관광 정보 조회** — 한국관광공사 공공 API 연동, 지역별/키워드별 관광지 탐색
- **연관 명소 추천** — 선택한 관광지와 연관된 주변 명소 자동 추천
- **채팅형 가이드** — AI와 대화하며 여행 정보 탐색
- **네이버 블로그 연동** — 관광지 관련 블로그 리뷰 검색 및 분석
- **PDF/이미지 저장** — 여행 계획을 PDF 또는 이미지로 내보내기

## 기술 스택

| 구분 | 기술 |
|------|------|
| 백엔드 | Java 17, Spring Boot 3.2.0, Spring WebFlux (SSE) |
| AI | Google Gemini API (gemini-2.0-flash) |
| 외부 API | 한국관광공사 공공 API, 네이버 검색 API |
| 프론트엔드 | Vue 3, TypeScript, Vite 6, Pinia, Vue Router |
| 유틸리티 | html2canvas, jspdf |
| 빌드 | Maven (백엔드), npm (프론트엔드) |

## 프로젝트 구조

```
waynai/
├── waynai-backend/               # Spring Boot 백엔드
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/waynai/demo/
│       │   ├── controller/       # Travel, LLM, Chat, Naver, Tourist, Health
│       │   ├── service/          # AI, 관광, 검색 서비스
│       │   ├── client/           # 외부 API 클라이언트
│       │   └── config/           # WebConfig, CORS
│       └── resources/
│           ├── application.properties
│           └── prompt/           # LLM 프롬프트 템플릿
├── waynai-frontend/              # Vue 3 SPA
│   ├── package.json
│   └── src/
│       ├── views/                # Home, TravelPlan, TouristInfo, Recommendations
│       ├── components/           # 검색, 스트리밍, 내비게이션
│       ├── services/             # API 호출 (streamService, searchService)
│       └── stores/               # Pinia (stream, language)
├── start-backend-dev.sh          # 백엔드 개발 실행
├── start-frontend-dev.sh         # 프론트엔드 개발 실행
├── start-*-prod*.sh              # 운영 실행 스크립트
├── stop-servers.sh               # 서버 중지
└── setup-env.sh                  # 환경 변수 설정
```

## 실행

### 사전 요구사항
- JDK 17+, Maven (또는 `mvnw`)
- Node.js 18+, npm

### 개발 모드
```bash
# 백엔드 (포트 8080)
./start-backend-dev.sh
# 또는
cd waynai-backend && ./mvnw spring-boot:run

# 프론트엔드 (포트 5173)
./start-frontend-dev.sh
# 또는
cd waynai-frontend && npm install && npm run dev
```

### 운영 모드
```bash
./start-backend-prod.sh
./start-frontend-prod.sh    # npm run build → serve -s dist -l 3000
```

## 설정

### 백엔드 (`waynai-backend/src/main/resources/application.properties`)

| 항목 | 환경 변수 | 설명 |
|------|-----------|------|
| Gemini API 키 | `GEMINI_API_KEY` | AI 기능 필수 |
| 네이버 API | `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` | 블로그 검색 연동 |
| 공공 API | properties 내 `serviceKey` | 한국관광공사 API |

### 프론트엔드

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

## 라이선스

MIT
