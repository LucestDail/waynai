# TripAmigo Tour Course Service

LLM 기반 관광 코스 생성 백엔드 API 서비스입니다.

## 개요

이 서비스는 관광지 정보와 관광 관련 정보를 바탕으로 개인화된 관광 코스를 생성해주는 Spring Boot 기반 REST API입니다.

## 주요 기능

- **관광 코스 생성**: 목적지, 일정, 테마 등을 기반으로 맞춤형 관광 코스 생성
- **관광지 정보 관리**: 관광지 정보 조회, 검색, 필터링
- **LLM 통합**: Google Gemini API를 활용한 지능형 코스 생성 및 여행 팁 생성
- **RESTful API**: 표준화된 REST API 제공

## 기술 스택

- **Framework**: Spring Boot 4.0.0-SNAPSHOT
- **Language**: Java 24
- **Build Tool**: Maven
- **Lombok**: 코드 간소화
- **Validation**: Bean Validation
- **HTTP Client**: RestTemplate

## 프로젝트 구조

```
src/main/java/com/tripamigo/demo/
├── controller/          # REST 컨트롤러
│   ├── TourCourseController.java
│   └── TouristSpotController.java
├── service/            # 비즈니스 로직
│   ├── TourCourseService.java
│   ├── TouristSpotService.java
│   └── LLMService.java
├── dto/               # 데이터 전송 객체
│   ├── TourCourseRequestDto.java
│   ├── TourCourseResponseDto.java
│   ├── TouristSpotDto.java
│   ├── DayPlanDto.java
│   ├── SpotVisitDto.java
│   └── ApiResponseDto.java
├── interceptor/       # 인터셉터
│   ├── LoggingInterceptor.java
│   └── AuthInterceptor.java
├── config/           # 설정 클래스
│   ├── WebConfig.java
│   └── RestTemplateConfig.java
├── exception/        # 예외 처리
│   └── GlobalExceptionHandler.java
├── util/            # 유틸리티 클래스
│   ├── ValidationUtil.java
│   └── DateUtil.java
└── client/          # HTTP 클라이언트
    └── HttpClientService.java
```

## API 엔드포인트

### 관광 코스 API

- `POST /api/v1/tour-courses` - 관광 코스 생성 (Gemini API 활용)
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

## 실행 방법

### 1. 프로젝트 빌드

```bash
cd demo
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

## API 사용 예시

### 관광 코스 생성

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

## 설정

### application.properties 주요 설정

- `server.port`: 서버 포트 (기본값: 8080)
- `logging.level.com.tripamigo.demo`: 로깅 레벨 (기본값: DEBUG)
- `spring.jackson.time-zone`: 타임존 (기본값: Asia/Seoul)

## 개발 가이드

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

## 향후 개선 사항

- [x] **LLM API 연동 (Google Gemini)** ✅
- [ ] 데이터베이스 연동 (JPA/Hibernate)
- [ ] 사용자 인증/인가 시스템 (JWT)
- [ ] 캐싱 시스템 (Redis)
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] 단위 테스트 및 통합 테스트
- [ ] Docker 컨테이너화
- [ ] CI/CD 파이프라인 구축
- [ ] LLM 응답 JSON 파싱 개선
- [ ] 에러 처리 및 재시도 로직 강화

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 