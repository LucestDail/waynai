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

## 설정 (환경 변수)

모든 비밀키는 **리포지토리 루트의 `.env`** 또는 **쉘 환경 변수**로 주입합니다.
`.env` 파일은 `.gitignore` 에 포함되어 커밋되지 않습니다. 템플릿은 `./.env.example` 를 참고하세요.

```bash
cp .env.example .env
# .env 내부 값 편집
./start-backend-dev.sh
```

### 필수/선택 환경 변수

| 범주 | 변수 | 필수 | 설명 |
|------|------|:---:|------|
| Gemini | `GEMINI_API_KEY` | ✅ | Google Gemini API 키 |
| Gemini | `GEMINI_MODEL_CHAIN` |  | 핫스왑 모델 체인(콤마구분). 기본: `gemini-2.5-flash-lite,gemini-2.0-flash-lite,gemini-2.0-flash,gemini-2.5-flash,gemini-2.5-pro` |
| Gemini | `GEMINI_TIMEOUT_MS` / `GEMINI_RETRY_PER_MODEL` |  | 타임아웃(ms) / 모델당 재시도 횟수 |
| Naver | `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | ✅ | 네이버 검색 API 자격 증명 |
| 공공데이터 | `TOUR_API_SERVICE_KEY` | ✅ | 한국관광공사 API 서비스 키. **반드시 Decoding(원문) 값**을 넣을 것. 코드 내부에서 `URLEncoder.encode()` 로 자동 인코딩되므로 `%2B`/`%2F`/`%3D` 가 포함된 Encoded 값을 넣으면 이중 인코딩되어 `SERVICE_KEY_NOT_REGISTERED_ERROR` 가 발생합니다. 값에 `+`, `/`, `=` 가 포함되므로 `.env` 에서는 큰따옴표로 감싸 저장하세요. |
| 서버 | `SERVER_PORT` |  | 백엔드 포트 (기본 8080) |
| CORS | `CORS_ALLOWED_ORIGINS` |  | 콤마 구분 오리진 목록 (기본 `http://localhost:5173,http://127.0.0.1:5173`) |
| 프론트 | `VITE_API_BASE_URL` |  | 프론트가 호출할 백엔드 URL. 개발 모드에서는 Vite proxy가 `/api` 를 포워딩하므로 공란이어도 무방. |

### Gemini 핫스왑 (Hot-swap)

`GeminiModelRouter` 가 `GEMINI_MODEL_CHAIN` 의 **가장 저렴한 모델부터** 순차적으로 호출하며,
쿼터/레이트리밋 오류 시 해당 모델을 짧게 쿨다운시키고 다음 모델로 자동 전환합니다.
이 동작을 변경하려면 체인의 순서를 편집하기만 하면 됩니다.

### 레거시 서버 키 배치 (운영 서버에서 가져와야 하는 정보)

기존 EC2 배포 스크립트(`deploy.sh`)와의 호환을 위해 다음 경로를 레거시 폴백으로 지원합니다.
새로 구축하는 환경은 `.env` 사용을 권장합니다.

| 파일 경로 | 매핑 환경변수 | 비고 |
|-----------|---------------|------|
| `/var/www/key/key`         | `GEMINI_API_KEY`         | Gemini API 키 (줄바꿈 없이 원문) |
| `/var/www/key/naverId`     | `NAVER_CLIENT_ID`        | 네이버 검색 Client ID |
| `/var/www/key/naverSecret` | `NAVER_CLIENT_SECRET`    | 네이버 검색 Client Secret |
| `/var/www/key/tourApiKey`  | `TOUR_API_SERVICE_KEY`   | 한국관광공사 서비스 키 (**디코딩 원문**) |

스크립트(`start-backend-prod.sh`, `start-backend-prod-nohup.sh`)는 `.env` 로드 후
위 파일들을 차례로 검사하여 **비어 있는 값만** 주입합니다. 신규 배포에서는
`.env`/systemd 환경변수 사용을 권장하며, 레거시 경로는 점진 마이그레이션 용도입니다.

`start-backend-prod.sh` / `start-backend-prod-nohup.sh` 는 `.env` 부재 시 위 파일들을 자동으로 읽어 환경 변수로 주입합니다.
**파일 권한은 반드시 `chmod 600` 으로 유지하십시오.**

### 프론트엔드 설정 예시

```bash
cd waynai-frontend
echo "VITE_API_BASE_URL=http://localhost:8080" > .env.local
npm install && npm run dev
```

Vite 개발 서버는 `/api`, `/actuator` 경로를 `VITE_API_BASE_URL` 로 프록시합니다.
프로덕션 빌드 시 `VITE_API_BASE_URL` 값이 정적으로 주입됩니다.

## 디자인 시스템 (Material Design 3)

그린 테마 M3 토큰(`--m3-primary` 등)을 `src/assets/base.css` 에 선언하고, 공통 클래스
(`m3-card`, `m3-btn`, `m3-chip`)를 제공합니다. 다크 모드는 `.dark` 클래스 스코프로 토큰이 치환됩니다.

## 라이선스

MIT
