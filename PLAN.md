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

### Phase 3 — 모바일 앱 (6주) · 진행률 35%

> **업데이트(2026-04-22)**: `waynai-mobile/` Flutter 앱 스캐폴드 + 백엔드 `/api/gps/nearby`
> 엔드포인트 완료. `flutter analyze` 0 에러, `flutter test` 통과, `flutter build web` 성공.
> iOS/Android 네이티브 빌드는 Xcode / Android Studio 설치 후 `flutter run -d ios|android` 로 실행 가능
> (Flutter 단일 코드베이스이므로 추가 코드 수정 불필요).
>
> 1. ✅ **백엔드 Location API 구현** — `POST /api/gps/nearby`
>    (KorService2 → KorService1 → Naver Local API 3단 폴백)
> 2. ✅ **Flutter 앱 스캐폴드** (`waynai/waynai-mobile/`)
> 3. ✅ **기본 화면** (Home / Plan SSE / Nearby GPS)
> 4. ⏳ **여행 중 기능** (오프라인 캐시, 지출 기록)
> 5. ⏳ **동행자 공유** (초대 링크, 위치 공유)

**3.0 [완료] 백엔드 Location API**

구현된 엔드포인트:

```
POST /api/gps/nearby
Content-Type: application/json

{
  "lat": 37.5665,         // 위도 (required)
  "lng": 126.9780,        // 경도 (required)
  "radiusMeters": 1000,   // 검색 반경(m), 20~20000 (default 1000)
  "contentTypeId": "12",  // (optional) 12=관광지, 39=음식점, 14=문화시설, 38=쇼핑, 32=숙박
  "limit": 15,            // (optional) default 15, max 50
  "context": "경복궁 근처 맛집"  // (optional) 네이버 Local/블로그 결합 키워드
}
```

응답:

```
{
  "lat": 37.5665, "lng": 126.978, "radiusMeters": 1500,
  "spotCount": 5, "blogCount": 5,
  "spots": [
    { "title": "솔솥 광화문 경복궁 케이트윈점",
      "address": "서울특별시 종로구 종로1길 50 지하 1층",
      "contentTypeLabel": "음식점",
      "lat": 37.572, "lng": 126.982,
      "distanceMeters": 934, "tel": "..." }
  ],
  "blogs": [ { "title": "...", "url": "...", "description": "..." } ]
}
```

폴백 체인 (`LocationBasedApiClient` + `GpsNearbyService`):
1. **KorService2.locationBasedList2** (신버전 공공데이터, 서비스키 승인 필요)
2. 실패 시 **KorService1.locationBasedList1** (`listYN=Y`, arrange 생략)
3. 그래도 0건이면 **네이버 Local API** (mapx/mapy 1e7·경위도) → 하버사인 반경 필터 + 거리순 정렬
4. 네이버 블로그(Blog) 는 `context` 를 키워드로 항상 병렬 호출

검증: `curl` 로 서울 좌표(37.5665,126.9780) 호출 시 `spotCount=5, blogCount=5`, 거리순 정렬 확인 완료.

**3.1 Flutter 모바일 앱 `waynai-mobile/`** (100%)
- [x] 프로젝트 초기화 — Flutter 3.41.7 · Dart 3.11.5 · Riverpod + GoRouter + Dio
- [x] WaynAI 테마 적용 — ocean/cream/terra/amber/sage 팔레트(웹 디자인 시스템 동일)
- [x] 백엔드 REST 연동 — `ApiClient` (Dio) + `GpsService` + `.env` 기반 `API_BASE_URL`
- [x] SSE 스트리밍 연동 — `StreamService.planStream()` 으로 `/api/travel/plan/stream` 구독
- [x] `flutter analyze` 0 에러, `flutter test` 통과, `flutter build web` 성공
- [ ] 로그인/회원가입 (백엔드 인증 모듈 Phase 2 선행 후 착수)

**3.2 GPS 기반 실시간 기능** (부분 완료)
- [x] 현재 위치 추적 (`geolocator` 14.x, 300m 이동 스트림)
- [x] `POST /api/gps/nearby` 호출 + 반경 슬라이더(200m~5km) + 키워드 입력
- [x] 장소/블로그 카드 + 네이버 지도 딥링크
- [ ] 주기 폴링(60~120초) 자동 트리거 (수동 새로고침만 구현)
- [ ] 지도 위 POI 마커 (Google Maps SDK)
- [ ] 다음 목적지까지 최적 교통 수단 (Directions API)
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
