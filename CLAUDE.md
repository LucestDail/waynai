# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## WaynAI 개요

AI 여행 플래너. **OpenRouter(OpenAI 호환) LLM** + 공공 API 기반 풀스택 웹앱(+ Flutter 모바일).
> LLM 백엔드는 2026-07 Gemini → OpenRouter 로 전환됨. 라우터: `client/OpenRouterModelRouter.java`(핫스왑), 어댑터: `client/GeminiApiClient.java`(이름만 유지). 설정: `.env` 의 `OPENROUTER_API_KEY` / `OPENROUTER_MODEL_CHAIN`.
> - 모델 체인(저가): `deepseek/deepseek-v3.2 → deepseek/deepseek-chat → z-ai/glm-4.7-flash`. 재시도 0(느린 모델 즉시 핫스왑), 타임아웃 120초(전체 일정 JSON 생성이 ~60초 걸림).
> - **JSON 모드**: 구조화 플랜/의도분석은 `generateJson()`(response_format=json_object)으로 유효 JSON 강제. 채팅 등 자유 텍스트는 `generateText()`.
> - **국내/해외 분기**(2026-07): intent 분석이 `international`/`destination` 판정 → 해외면 한국관광공사 RAG 스킵하고 LLM 지식+블로그로 구성. `IntentAnalysisDto`, `intent_analysis.txt`, `TravelOrchestratorService.safeCollectTour/safeCollectFlights/buildStructuredPrompt` 참조.

- `waynai-backend/` — Java 17, Spring Boot 3.2, **WebFlux(SSE)**. Maven.
- `waynai-frontend/` — Vue 3 + TS + Vite + Pinia. axios, html2canvas/jspdf.
- `waynai-mobile/` — Flutter (Phase 후순위).

### 실행
```bash
./start-backend-dev.sh      # 백엔드 개발
./start-frontend-dev.sh     # 프론트 개발
# 백엔드 단독: cd waynai-backend && ./mvnw spring-boot:run
# 프론트 단독: cd waynai-frontend && npm run dev
```

### 현재 아키텍처 (요청 → 응답)
```
사용자 텍스트쿼리
 → IntentAnalysisService (지역/키워드 분석)
 → 병렬 RAG: TouristInfoService(관광공사 관광지명+좌표) + NaverSearchService(블로그 텍스트)
 → TravelOrchestratorService.buildStructuredPrompt() + prompt/travel_plan_structured.txt
 → GeminiApiClient(어댑터) / OpenRouterModelRouter (핫스왑)
 → JSON 파싱 → TravelPlanDto → SSE(TravelEvent) 스트림
```
- 엔드포인트: `GET /api/travel/plan/stream`(SSE), `POST /api/travel/plan/structured`(JSON), `GET /api/travel/plan`(text).
- SSE 이벤트 타입: `stage / intent / sources.tour / sources.naver / model / token / plan / done / error`.
- 프론트 소비: `services/streamService.ts` → `stores/stream.ts` → `components/StreamResult.vue`(일별 타임라인).
- **Stateless**: DB/JPA/인증 전혀 없음. 계획 저장 안 됨.

---

## 개선 플랜 — 실데이터 기반 여행 플래너 (2026-07)

### 목표
실제 정보 기반으로 **비용 + 이동 동선 + 국제 항공편 + 교통/경로 + 관광**을 산출.
여행 스타일 커버: 배낭여행 / 신혼여행 / 효도여행 / 국내여행 / 도보·산책.

### 결정된 방향 (사용자 확정)
- **착수 범위: 국제 항공편 먼저** grounding.
- **API 예산: 무료 우선** (유료 전환은 나중).
- **항공 API = Travelpayouts** (Aviasales). ⚠️ Amadeus 셀프서비스는 2026-07-17 폐지되어 무료 신규 발급 불가 → Travelpayouts로 확정. 무료 제휴 가입 + 항공권 가격 데이터 + **제휴 커미션 수익화**(상품화 플랜과 정합). https://www.travelpayouts.com 가입 → API Token.

### 핵심 문제
데이터 모델(`TravelPlanDto`: `estimatedBudgetKrw`, `latitude/longitude`, `transportation`, `durationMin`)은 이미 있으나 **채우는 실데이터 소스가 없어 Gemini가 값을 추측(hallucination)**함. 개선의 본질 = **실데이터 수집 후 LLM은 조합/설명만**(grounding), LLM을 "작가"에서 "편집자"로.

### 무료 우선 API 매핑
| 데이터 | 무료 소스 | 비고 |
|---|---|---|
| **국제 항공편/가격** ⭐우선 | **Travelpayouts / Aviasales**(무료 제휴) | 최저가·가격달력·인기노선. 제휴 커미션 수익화. (Amadeus 셀프서비스 2026-07-17 폐지) |
| 경로·이동시간 | **OpenRouteService**(무료 키) | 자동차/도보/자전거 leg별 거리·시간 |
| 지도 | **Leaflet + OSM 타일** | 프론트 마커 + day별 polyline (좌표 기존 존재) |
| 환율 | **exchangerate-api / open.er-api**(무료) | 국제여행 통화 표시 |
| 국내 관광 POI | 한국관광공사(기존 유지) | — |
| 해외 POI | OpenTripMap(무료) | 국제 확장 시 |
| 입장료·식비 | LLM 추정 + 블로그 RAG(현실적 타협) | 정확 무료 API 없음 → "범위+근거"로 표기 |

> 무료 티어 한계(레이트리밋·정확도)는 감수. 정확도 이슈 크면 유료(Google Directions/Places, Kakao Mobility)로 나중에 교체.

### 여행 스타일 프리셋 (프롬프트 프로파일 + 가중치)
- **배낭여행**: 최저가 항공/게스트하우스, 대중교통·도보, 예산 상한 타이트
- **신혼여행**: 고급 숙박·로맨틱 스팟, 이동 편의(택시/렌트), 여유 페이스
- **효도여행**: 저강도 동선(하루 2~3곳), 무장애/의료접근성, 휴식 반영
- **국내여행**: 관광공사 + 경로 API, KTX/렌트
- **도보/산책**: 좁은 반경, 도보 이동시간만, 반나절 코스

---

## 단계별 로드맵

### Phase 0 — 입력 구조화 (선행 필수)
지금은 `buildQuery()`가 문자열을 이어붙여 전송(`"부산 부산진구 1박2일 커플..."`). 실데이터 조회를 위해 **구조화 요청 객체**로 전환.
- 프론트: `views/TravelPlanView.vue` 폼 → JSON(출발지/도착지/날짜/인원/스타일/예산범위).
- 백엔드: `TravelController` 에 구조화 요청 DTO 수용 엔드포인트 추가.

### Phase 1 — 국제 항공편 grounding ⭐ (백엔드 완료, 2026-07-08)
- [x] `client/TravelpayoutsApiClient.java` — `x-access-token` 헤더 인증 + `/v1/prices/cheap` 최저가 조회. HttpURLConnection 방식(공유 RestTemplate은 data.go.kr 외 TLS 차단하므로).
- [x] `client/IataResolver.java` — 도시명→IATA (autocomplete places2 + 정적 오버라이드 + 캐시).
- [x] `dto/FlightOfferDto.java` — origin/destination/airline/transfers/price/currency/departureAt/returnAt/bookingUrl.
- [x] `service/FlightSearchService.java` — 지명해석+조회+Aviasales 제휴 딥링크(marker) 생성.
- [x] `controller/FlightController.java` — `GET /api/flights?origin=&destination=&departDate=&returnDate=` (단독 테스트용).
- [x] `TravelPlanDto.flights` 필드 추가.
- [x] `TravelOrchestratorService`: 항공권 조회 → SSE `sources.flight` 이벤트 + **실가격을 프롬프트에 주입(추측 금지)** → 파싱된 plan에 flights 부착. `/api/travel/plan/stream`에 `origin/departDate/returnDate` 옵션 파라미터 추가.
- [x] 설정: `application.properties` + `.env.example`에 `TRAVELPAYOUTS_*`. 토큰 미설정 시 항공 기능만 자동 비활성.
- [x] 프론트: `StreamResult.vue`에 항공권 카드(제휴 예약 링크) + `stores/stream.ts`의 `TravelPlan.flights`/`FlightOffer` + `sources.flight` 처리.

**테스트**: `.env`에 `TRAVELPAYOUTS_TOKEN` 넣고 `./start-backend-dev.sh` →
`curl 'http://localhost:8080/api/flights?origin=서울&destination=오사카&departDate=2026-08'`

### Phase 2 — 동선/경로 + 지도 UI (프론트 완료, 2026-07)
- [x] 프론트: `components/TravelMap.vue`(Leaflet) — day별 마커 + polyline(색상 구분), 좌표 있으면 표시. `leaflet`/`@types/leaflet` 추가.
- [x] 비용: `StreamResult.vue`에 총 예상 비용(계획 예산 + 최저 항공권) 요약.
- [ ] **남음**: `client/RoutingApiClient.java`(OpenRouteService)로 leg별 실제 이동시간/거리 계산 후 프롬프트 주입 (현재 좌표/동선은 LLM 지식 기반).

### Phase 2.5 — 응답 스트리밍 (완료, 2026-07)
- [x] `OpenRouterModelRouter.streamText()` — OpenRouter `stream:true` SSE 델타 실시간 방출 + 모델 체인 폴백.
- [x] `GeminiApiClient.generateJsonStream()`, `TravelOrchestratorService` 가 델타를 즉시 `token` 이벤트로 방출(누적 후 파싱).
- [x] 프론트 `StreamResult.vue` 라이브 생성 패널(글자수·스트리밍 텍스트). **첫 토큰 ~9초**(이전 60초 일괄 → 체감 대폭 개선).

### Phase 3 — 비용 현실화 + 스타일 프리셋
1. 항목별 비용 분해(숙박/교통/식비/입장료) + 환율 API(현지통화→원화).
2. 스타일별 프롬프트 프로파일 → `prompt/` 분리 + `TravelPlanView.vue` 스타일 칩(배낭/신혼/효도/도보) + 예산 범위 숫자 입력 + 출발지/날짜 폼(현재 origin 기본 SEL, dates 옵션).

### Phase 3 — 완료 (2026-07-09): 에이전틱 자연어 + 단순화
- [x] 관광지 검색/추천 화면 제거(라우트 redirect, nav 정리) → 홈/세부계획/소개만.
- [x] **에이전틱 자연어 입력**: 콤보/시군구 코드 제거. `IntentAnalysisDto`에 days/origin/departDate/returnDate/style/budgetLevel/companions 추가 → `intent_analysis.txt`가 문장에서 추출 → 오케스트레이터가 항공권(출발지/날짜)·플랜(일수/스타일/예산/동반)에 반영. 검증: "오사카로 3박4일 커플 넉넉하게" → international/오사카/4일/커플/고급 자동 추출.
- [x] 프론트: 홈=한 줄 바로시작, `/travel-plan`=자연어 textarea + 빠른추가 칩(기간/동행/스타일/예산). 개발 용어(Gemini/SSE/RAG/모델명) 전면 제거.
- [x] **JSON 미노출**: `SearchProgress.vue`를 친근한 "찾는 중" 단계 UI로 전면 재작성(관광공사/네이버/모델명/JSON/로그 제거). `StreamResult`는 완성 계획만 렌더, 파싱 실패 시 raw JSON 대신 안내.
- [x] 레이아웃: 진행/결과 폭 960 통일, 항공권/비용/지도 섹션 좌우 패딩 정렬.

### Phase 2.6 — 완료 (2026-07-09): 실제 이동시간 (ORS)
- [x] `client/RoutingApiClient.java`(OpenRouteService) — 좌표열 → 총 거리/시간. `TravelOrchestratorService.enrichRouteTimes()`가 계획 파싱 후 각 날짜 `transportation`에 "실제 이동 약 N분 · Xkm" 반영. 도보 스타일이면 foot-walking. **`ORS_API_KEY` 미설정 시 비활성**(https://openrouteservice.org 무료 키). 좌표는 LLM 제공분에 의존.

### Phase 4 — 완료 (로컬) / 잔여 (서버·인증)
- [x] 계획 저장/히스토리: `stores/history.ts`(localStorage) + `StreamResult` "내 여행에 저장" + `/travel-plan` "저장한 여행" 목록(불러오기/삭제). `streamStore.loadSaved()`.
- [ ] **잔여**: 서버 저장(교차기기) + 사용자 인증. 워크스페이스 패턴상 파일 기반(JSON) 백엔드가 자연스러움. 상품화 시 진행.

---

## 개선 시 주의
- **모든 신규 데이터는 프롬프트 컨텍스트로 주입**해 LLM이 추측하지 않게 할 것 (grounding이 이 개선의 핵심).
- 국내/국제 분기: 국내는 관광공사+국내 경로, 국제는 Amadeus+OpenTripMap. `TravelOrchestratorService`에서 분기.
- 무료 티어 레이트리밋 → 캐싱/폴백 고려. LLM 핫스왑(`OpenRouterModelRouter`) 패턴 재사용.
- 하드코딩된 API base URL / CORS 과다 허용은 배포 전 정리 필요(기존 이슈).
- 실행/재확인: 변경 후 `./start-backend-dev.sh` + `./start-frontend-dev.sh`로 SSE 흐름 확인.
