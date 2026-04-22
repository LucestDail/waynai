# WaynAI — 상세 구현 계획

## 1. 프로젝트 비전

풀스택 풀패키지 엔드 투 엔드 여행 어시스턴트. 일정 자동화, 여행 정보 자동 산출, 여행 제안, 최적의 여행 경비, 날씨, 항공편, 교통편, 현지 사항까지 국제적 이동까지 원스톱으로 제공. 모바일 GPS를 통한 이동 경로, 교통편, 주변 장소 추천 실시간 통신 지원.

## 2. 현재 상태

- 백엔드: Spring Boot 3.2.0 + Gemini + 한국관광공사 API + 네이버 검색
- 프론트: Vue 3 + TypeScript + Vite 6 + Pinia
- SSE 스트리밍 여행 계획 생성
- 페이지: 홈, 여행 계획, 관광 정보, 추천, 소개
- 하드코딩 이슈: 프론트 API URL에 AWS IP 고정
- README 버전 불일치 (문서 Spring Boot 4 vs 실제 3.2)
- CORS `allowedOrigins("*")`

## 3. 디자인 시스템

**Google Material Design 3 (M3) 준용**

### M3 적용 방침
Vue 3 프로젝트이므로 Vuetify 3 (M3 기반) 또는 커스텀 CSS로 적용:

### 컬러 시스템
| 토큰 | Light | Dark | 용도 |
|------|-------|------|------|
| Primary | `#006D3B` | `#6EDB9E` | 여행/자연 테마 |
| On Primary | `#FFFFFF` | `#003919` | Primary 위 텍스트 |
| Secondary | `#4E6355` | `#B6CCB9` | 보조, 교통 정보 |
| Tertiary | `#3B6472` | `#A3CDDC` | 날씨, 항공 정보 |
| Surface | `#F6FBF3` | `#1A1C19` | 카드/배경 |
| Error | `#BA1A1A` | `#FFB4AB` | 경고/취소 |

> 여행 테마: 자연·탐험을 연상시키는 그린 계열 Primary

### 타이포그래피
- M3 Type Scale 적용
- 한글: Pretendard
- 금액/거리: Roboto (tabular figures)

### 핵심 컴포넌트
- 여행 일정 카드: M3 Elevated Card + 타임라인 레이아웃
- 검색: M3 Search Bar (상단 고정)
- 지도: M3 Surface 위 Full-bleed 지도
- 하단 시트: 모바일에서 장소 상세 정보
- FAB: 새 여행 계획 생성
- `border-radius: 16px` (M3 Large)

---

## 4. 단계별 구현 계획

### Phase 1 — 기존 안정화 + 문서 정합성 (2주)

**1.1 코드 정리**
- [ ] 백엔드 README 버전 수정 (Spring Boot 4 → 3.2, Java 24 → 17)
- [ ] 프론트 API URL 하드코딩 제거 → `VITE_API_BASE_URL` 환경 변수 필수화
- [ ] CORS: `allowedOrigins("*")` → 명시적 도메인 제한
- [ ] `application.properties` 내 공공 API 서비스키 → 환경 변수 분리

**1.2 기존 코드와 문서 불일치 수정**
- [ ] README에 없는 실제 컨트롤러 문서화 (TravelController, ChatController 등)
- [ ] README에 있으나 코드에 없는 컨트롤러 제거 (SearchController 등)

### Phase 2 — 여행 계획 고도화 (5주)

**2.1 구조화된 여행 일정**
- [ ] 현재: AI 텍스트 → 사용자에게 텍스트로 표시
- [ ] 개선: AI → 구조화 JSON (날짜, 시간, 장소, 교통, 비용, 좌표)
- [ ] Gemini 구조화 출력 (JSON Schema) 활용
- [ ] 타임라인 UI — 날짜별 카드, 시간축에 일정 배치

**2.2 항공편 검색**
- [ ] Amadeus API 연동 (항공편 검색, 가격 비교)
- [ ] 출발지/도착지/날짜 → 최저가 항공편 목록
- [ ] 여행 계획에 항공편 자동 연결

**2.3 숙소 검색**
- [ ] Booking.com Affiliate API 또는 Google Hotels 연동
- [ ] 위치/가격/평점 기반 추천
- [ ] 여행 일정과 숙소 매핑

**2.4 교통편 통합**
- [ ] Google Directions API — 관광지 간 이동 경로/시간
- [ ] 현지 대중교통 정보 (Google Transit)
- [ ] 택시/우버 예상 요금
- [ ] 최적 동선 자동 계산 (TSP 근사 알고리즘 + AI)

**2.5 여행 경비 계산기**
- [ ] 항공 + 숙소 + 교통 + 식비 + 관광 총 예상 경비
- [ ] 실시간 환율 자동 적용
- [ ] 예산 대비 경비 비교
- [ ] 경비 절감 제안 ("이 호텔 대신 에어비앤비를 쓰면 20% 절감")

### Phase 3 — 모바일 앱 (6주) · 진행률 0%

> 현재 저장소에는 모바일 앱 프로젝트(Flutter/React Native)가 존재하지 않습니다.
> 기능은 오로지 **백엔드(Spring Boot) + 웹 프론트(Vue)** 만 구현되어 있으며,
> 이 단계는 착수 전입니다. 본 Phase 는 다음 순서로 선행 작업이 필요합니다.
>
> 1. **백엔드 Location API 선행 구현** (GPS 의존 기능의 서버 계약 먼저 확정)
> 2. **Flutter 앱 스캐폴드** (별도 리포 또는 `waynai/waynai-mobile/`)
> 3. **GPS 실시간 루프** (근처 관광지/식당 추천, 경로 안내)
> 4. **여행 중 기능** (오프라인 캐시, 지출 기록)
> 5. **동행자 공유** (초대 링크, 위치 공유)

**3.0 [선행] 백엔드 Location API 스펙 제안 (완료 전 필수)**

모바일 GPS 기능은 서버에서 **좌표 기반 근처 관광지·식당·POI 조회**를 제공해야 동작합니다.
관광공사 `locationBasedList1` 엔드포인트를 래핑하는 신규 API를 다음과 같이 추가합니다.

```
POST /api/gps/nearby
Content-Type: application/json

{
  "lat": 35.1587,        // 위도 (required)
  "lon": 129.1604,       // 경도 (required)
  "radiusM": 1500,       // 검색 반경(m), 최대 20000 (default 1000)
  "contentTypeId": 12,   // TourAPI: 12=관광지, 39=음식점, 14=문화시설, 38=쇼핑, 32=숙박 (optional)
  "limit": 20            // default 20, max 50
}
```

응답(예시):

```
{
  "source": "tourapi.locationBasedList1",
  "center": { "lat": 35.1587, "lon": 129.1604 },
  "radiusM": 1500,
  "count": 20,
  "items": [
    {
      "contentId": "126508",
      "title": "해운대해수욕장",
      "category": "관광지",
      "contentTypeId": 12,
      "address": "부산광역시 해운대구 해운대해변로 264",
      "lat": 35.1587,
      "lon": 129.1604,
      "distanceM": 120,
      "imageUrl": "...",
      "tel": "051-749-7620"
    }
  ]
}
```

구현 요건:
- `TouristInfoService` 에 `locationBasedList1` 메서드 추가(X/Y 좌표 + 반경).
- 응답은 거리순 정렬, 캐시 60초.
- 429/5xx 폴백은 상위 3개 인기 스팟으로 대체.
- SSE 가 아닌 단건 REST (`Flux` 대신 `Mono<NearbyResponse>`).

**3.1 Flutter 모바일 앱** (0%)
- [ ] 프로젝트 초기화 (Flutter 3.x + Riverpod)
- [ ] WaynAI 테마 적용 (ocean/terra/sage 팔레트, Cormorant/DM Sans 패밀리)
- [ ] 백엔드 API 연동 (REST + `/api/travel/plan/stream` SSE)
- [ ] 로그인/회원가입 (백엔드 인증 모듈 필요 - Phase 2 선행)

**3.2 GPS 기반 실시간 기능** (0%, 3.0 선행 필요)
- [ ] 현재 위치 추적 (`geolocator`)
- [ ] `POST /api/gps/nearby` 주기 폴링(60-120초) + 이동 트리거(300m 이상 이동 시)
- [ ] 지도 위 근처 POI 마커 (Google Maps SDK)
- [ ] 다음 목적지까지 최적 교통 수단 제안 (Directions API)
- [ ] 주변 맛집/관광지/편의점/약국 실시간 추천
- [ ] "근처에 추천 맛집이 있어요" 푸시 알림

**3.3 여행 중 기능** (0%)
- [ ] 오프라인 일정 조회 (로컬 캐시)
- [ ] 지출 기록 — 카테고리별, 현지 통화 자동 환산
- [ ] 예산 대비 사용 현황 차트
- [ ] 긴급 정보 — 대사관, 응급번호, 병원 위치

**3.4 동행자 기능** (0%)
- [ ] 여행 일정 공유 (초대 링크)
- [ ] 동행자 실시간 위치 공유
- [ ] 공동 지출 기록 (더치페이 계산)

### Phase 4 — 국제 여행 + 안정화 (4주)

**4.1 국제 여행 정보**
- [ ] 비자/입국 요건 DB (국적별)
- [ ] 여행 보험 안내
- [ ] 현지 안전 정보 (외교부 해외안전여행 API)
- [ ] 시차 계산기

**4.2 해외 데이터소스 확장**
- [ ] Google Places API — 해외 관광지, 맛집
- [ ] TripAdvisor API — 리뷰, 평점
- [ ] 날씨: OpenWeatherMap (전세계)

**4.3 웹 프론트엔드 M3 리뉴얼**
- [ ] Vuetify 3 도입 또는 커스텀 M3 컴포넌트
- [ ] 반응형 레이아웃 (compact/medium/expanded)
- [ ] 여행 일정 타임라인 UI
- [ ] 지도 기반 인터페이스

---

## 5. 기술 스택

### 백엔드
| 구분 | 기술 |
|------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.2.0 |
| AI | Gemini API (Streaming) |
| 외부 API | 한국관광공사, 네이버, Amadeus, Google Directions/Places |
| 실시간 | SSE (WebFlux Flux) |

### 웹 프론트엔드
| 구분 | 기술 |
|------|------|
| 프레임워크 | Vue 3 + TypeScript |
| 빌드 | Vite 6 |
| 상태 | Pinia |
| UI | Vuetify 3 (M3) 또는 커스텀 |
| 지도 | Google Maps JavaScript API |

### 모바일 (신규)
| 구분 | 기술 |
|------|------|
| 프레임워크 | Flutter |
| 상태 | Riverpod |
| 지도 | Google Maps SDK |
| GPS | geolocator |
| 디자인 | Material 3 |
