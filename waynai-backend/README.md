# WaynAI Backend

LLM 기반 AI 여행 파트너 백엔드 API 서비스입니다.

## 🌟 WaynAI 소개

"당신의 길을 함께 찾는 여행 파트너, WaynAI"

WaynAI는 단순한 정보 검색 툴이 아닌, 여행자의 목적, 취향, 일정, 감성에 따라 여정을 설계해주는 AI 기반 여행 동반자입니다. 말 그대로 "어디로 갈까?"를 고민할 때, WaynAI가 방향을 제시해줍니다.

## 🚀 주요 기능

### 🤖 AI 여행 플래너
- **개인화된 여행 설계**: 목적지, 일정, 테마, 예산 등을 기반으로 맞춤형 여행 코스 생성
- **LLM 통합**: Google Gemini API를 활용한 지능형 코스 생성 및 여행 팁 생성
- **의도 분석**: 사용자 입력을 분석하여 키워드 기반 추천과 문장 기반 계획 생성으로 구분

### 💬 대화형 여행 가이드
- **자연어 처리**: GPT 기반으로 여행지 설명, 역사/문화 맥락, 음식 추천 등 제공
- **실시간 상호작용**: SSE(Server-Sent Events)를 통한 실시간 대화형 인터페이스

### 🎯 현지 맞춤 추천
- **RAG 시스템**: 한국관광공사 API를 활용한 최신 관광지 정보 기반 코스 생성
- **맥락 기반 추천**: 날씨, 시간대, 계절, 현재 트렌드를 고려한 현지 맞춤 추천
- **연관 관광지 정보**: 지역 및 키워드 기반 연관 관광지 정보 제공

### 🗺️ 루트 최적화
- **지도 기반 코스**: 추천 코스 안내 및 교통 정보 제공
- **실시간 진행**: 단계별 검색 진행 상황을 실시간으로 전송

## 🛠️ 기술 스택

> ⚠️ 이 절은 2026-09-16 까지 **Spring Boot 4.0.0-SNAPSHOT · Java 24** 라고 적혀 있었다.
> 실제 `pom.xml` 은 **3.2.0 / 17** 이다. 그대로 믿고 환경을 맞추면 빌드가 안 된다.

- **Framework**: Spring Boot **3.2.0**
- **Language**: Java **17**
- **Build Tool**: Maven
- **Lombok**: 코드 간소화
- **Validation**: Bean Validation
- **HTTP Client**: RestTemplate + **WebClient**(둘 다 쓴다)
- **AI Integration**: **osh-ai-gateway 경유**(OpenRouter). 직결 아님
- **External API**: 한국관광공사(`apis.data.go.kr`) · **Travelpayouts**(항공) ·
  **OpenRouteService**(경로) · **Naver 검색** · **Nominatim**(지오코딩) ·
  **daero**(자체 대중교통 엔진, `daero.duckdns.org`)

## 📁 프로젝트 구조

```
src/main/java/com/waynai/demo/
├── controller/          # REST 컨트롤러 (12개)
│   ├── TravelController.java            # 여행 계획 생성(SSE)
│   ├── PlanController.java              # 계획 저장/조회/삭제(익명 토큰)
│   ├── ChatController.java              # 대화형 질의
│   ├── LLMController.java               # LLM 프록시
│   ├── FlightController.java            # 항공편 검색(Travelpayouts)
│   ├── RouteController.java             # 경로(OpenRouteService·daero)
│   ├── GpsController.java               # 주변 검색
│   ├── NaverSearchController.java       # 네이버 검색
│   ├── TouristInfoController.java       # 관광지 정보(한국관광공사)
│   ├── RelatedTouristInfoController.java# 연관 관광지
│   ├── HealthController.java            # 헬스체크
│   └── TestController.java              # 개발용
├── service/            # 비즈니스 로직 (12개)
│   ├── TravelOrchestratorService.java   # 권역분할·계획 조립(핵심)
│   ├── TravelPlanService.java / TravelService.java
│   ├── PlanArchiveService.java          # 서버 보관(X-Owner-Token)
│   ├── FlightSearchService.java         # IATA 변환 + 최저가
│   ├── IntentAnalysisService.java       # 의도 분석
│   ├── ChatService.java · LLMService.java
│   ├── GpsNearbyService.java · NaverSearchService.java
│   └── TouristInfoService.java · RelatedTouristInfoService.java
├── client/             # 외부 API 클라이언트
├── repository/         # 계획 파일 저장소
├── dto/                # 데이터 전송 객체
├── config/             # WebConfig(CORS) 등
└── util/
```

## 🔧 API 엔드포인트

### 실시간 검색 API

- `POST /api/search/stream` - SSE 기반 실시간 검색
- `GET /api/search/health` - 서버 상태 확인

### 여행 코스 API

- `POST /api/v1/tour-courses` - 여행 코스 생성 (Gemini API 활용)
- `POST /api/v1/tour-courses/travel-tips` - 여행 팁 생성 (Gemini API 활용)
- `GET /api/v1/tour-courses/{courseId}` - 특정 코스 조회
- `GET /api/v1/tour-courses/destination/{destination}` - 목적지별 코스 조회
- `GET /api/v1/tour-courses/health` - 헬스 체크

### 관광지 API

- `GET /api/v1/tourist-spots` - 모든 관광지 조회
- `GET /api/v1/tourist-spots/{spotId}` - 특정 관광지 조회
- `GET /api/v1/tourist-spots/search?keyword={keyword}` - 관광지 검색
- `GET /api/v1/tourist-spots/destination/{destination}/theme/{theme}` - 목적지/테마별 관광지 조회
- `GET /api/v1/tourist-spots/health` - 헬스 체크

### 관광지 정보 API (한국관광공사 연동)

- `GET /api/tourist-info/area-based` - 지역기반 연관 관광지 정보 조회
- `GET /api/tourist-info/keyword-based` - 키워드 기반 연관 관광지 정보 조회
- `GET /api/tourist-info/rag-context` - RAG를 위한 관광지 정보 컨텍스트 조회
- `GET /api/tourist-info/area-rag-context` - 지역 기반 관광지 정보 컨텍스트 조회
- `GET /api/tourist-info/category-based` - 카테고리별 관광지 정보 조회

## 🚀 실행 방법

### 1. 프로젝트 빌드

```bash
cd waynai-backend
./mvnw clean compile
```

### 2. 애플리케이션 실행

```bash
./mvnw spring-boot:run
```

또는

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### 3. 서비스 접속

- 서비스 URL: http://localhost:8080
- Actuator: http://localhost:8080/actuator

## 📋 API 사용 예시

### 실시간 검색

```bash
curl -X POST http://localhost:8080/api/search/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "query": "서울에서 3일간 문화 여행하고 싶어요",
    "destination": "서울",
    "theme": "문화",
    "days": 3
  }'
```

### 여행 코스 생성

```bash
curl -X POST http://localhost:8080/api/v1/tour-courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer valid-token" \
  -d '{
    "destination": "서울",
    "days": 3,
    "theme": "문화",
    "budget": "30만원",
    "transportation": "지하철",
    "interests": ["역사", "전통문화"],
    "travelStyle": "느긋한"
  }'
```

### 관광지 검색

```bash
curl -X GET "http://localhost:8080/api/v1/tourist-spots/search?keyword=경복궁" \
  -H "Authorization: Bearer valid-token"
```

### 여행 팁 생성

```bash
curl -X POST http://localhost:8080/api/v1/tour-courses/travel-tips \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer valid-token" \
  -d '{
    "destination": "부산",
    "days": 3,
    "theme": "자연",
    "budget": "25만원",
    "transportation": "대중교통",
    "interests": ["바다", "산"],
    "travelStyle": "활동적인"
  }'
```

### 관광지 정보 조회 (한국관광공사 API)

```bash
# 지역기반 연관 관광지 정보 조회
curl -X GET "http://localhost:8080/api/tourist-info/area-based?areaCd=11&signguCd=11680&pageNo=1&numOfRows=10" \
  -H "Authorization: Bearer valid-token"

# 키워드 기반 연관 관광지 정보 조회
curl -X GET "http://localhost:8080/api/tourist-info/keyword-based?keyword=경복궁&areaCd=11&signguCd=11680&pageNo=1&numOfRows=10" \
  -H "Authorization: Bearer valid-token"

# RAG 컨텍스트 조회
curl -X GET "http://localhost:8080/api/tourist-info/rag-context?keyword=문화&areaCd=11&signguCd=11680" \
  -H "Authorization: Bearer valid-token"
```

## ⚙️ 설정

### application.properties 주요 설정

```properties
# Server Configuration
server.port=8080
spring.jackson.time-zone=Asia/Seoul

# Logging
logging.level.com.waynai.demo=DEBUG

# Gemini API Configuration
gemini.api.key=your-gemini-api-key
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent

# Tourist API Configuration
tourist.api.base-url=http://api.visitkorea.or.kr/openapi/service/rest/KorService
tourist.api.service-key=your-tourist-api-key

# CORS Configuration
spring.web.cors.allowed-origins=http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
```

## 🧭 타깃 사용자

- **외국인 관광객**: 맞춤 코스 & 문화 설명이 필요함
- **한국인 자유여행자**: 일정 설계와 정보 탐색을 편하게 하고 싶은 Z세대
- **콘텐츠 크리에이터**: 감성적 장소 추천, 동선 최적화 등 필요
- **디지털 노마드**: 장기 체류자를 위한 지역 기반 추천과 일정 최적화

## 📣 슬로건

- "AI가 이끄는 당신만의 여행 길"
- "모르는 길도 걱정 없이, WaynAI와 함께"
- "당신의 다음 목적지, AI가 알고 있어요"
- "길을 묻는 여행자에게, AI로 답하다"
- "Every Way, Your Way — with WaynAI"

## 🚀 개발 가이드

### 새로운 기능 추가

1. DTO 클래스 생성 (필요시)
2. Service 클래스에 비즈니스 로직 구현
3. Controller 클래스에 API 엔드포인트 추가
4. 테스트 코드 작성

### 코드 컨벤션

- 클래스명: PascalCase
- 메서드명: camelCase
- 상수: UPPER_SNAKE_CASE
- 패키지명: 소문자

### API 설계 원칙

- RESTful API 설계
- 표준 HTTP 상태 코드 사용
- 일관된 응답 형식
- 적절한 에러 핸들링

## 🔒 보안

- API 키는 환경 변수로 관리
- CORS 설정으로 프론트엔드 접근 허용
- 입력 검증 및 에러 핸들링
- 인증 인터셉터를 통한 보안 강화

## 📈 성능 최적화

- **리액티브 프로그래밍**: Spring WebFlux로 비동기 처리
- **SSE**: 실시간 데이터 스트리밍으로 대기 시간 최소화
- **RAG**: 외부 API를 통한 최신 정보 활용
- **캐싱**: 반복 요청에 대한 응답 시간 단축

## 🧪 테스트

### 단위 테스트 실행

```bash
./mvnw test
```

### 통합 테스트 실행

```bash
./mvnw verify
```

## 📄 라이선스

MIT License

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요. 