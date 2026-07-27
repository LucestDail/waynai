package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.FlightOfferDto;
import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.dto.NaverBlogSearchDto;
import com.waynai.demo.dto.SourceSummaryDto;
import com.waynai.demo.dto.TouristSpotDto;
import com.waynai.demo.dto.TouristSpotResponseDto;
import com.waynai.demo.dto.TravelEvent;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 여행 계획 생성 오케스트레이터.
 *
 * <p>프론트의 진행 상태 UI 가 실시간으로 "의도 분석 → 컨텍스트 수집 → 모델 선택 →
 * 토큰 스트림 → 구조화 계획" 을 볼 수 있도록, 각 단계를 {@link TravelEvent} 로 푸시합니다.
 *
 * <p>기존 {@link TravelPlanService} 가 제공하던 구조화 JSON 프롬프트를 재사용하되,
 * Gemini 호출이 완료된 시점에 구조화 파싱을 시도하고 성공하면 {@code plan} 이벤트,
 * 실패하면 누적된 {@code token} 텍스트가 그대로 결과로 남도록 설계했습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelOrchestratorService {

    private final IntentAnalysisService intentAnalysisService;
    private final NaverSearchService naverSearchService;
    private final TouristInfoService touristInfoService;
    private final GeminiApiClient geminiApiClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final FlightSearchService flightSearchService;
    private final com.waynai.demo.client.RoutingApiClient routingApiClient;
    private final com.waynai.demo.client.IataResolver iataResolver;
    private final com.waynai.demo.client.TavilyApiClient tavilyApiClient;
    private final com.waynai.demo.client.HotellookApiClient hotellookApiClient;
    private final com.waynai.demo.client.GeocodingClient geocodingClient;
    private final com.waynai.demo.client.DaeroClient daeroClient;

    /** 국내 공항 IATA (여기에 없는 코드로 해석되면 해외로 판정). */
    private static final java.util.Set<String> KOREAN_AIRPORTS = java.util.Set.of(
            "SEL", "ICN", "GMP", "PUS", "CJU", "TAE", "USN", "KWJ", "RSU",
            "KUV", "HIN", "WJU", "YNY", "KPO", "MWX", "CJJ", "YEC", "KAG");

    /**
     * 사용자 질의로부터 Flux<TravelEvent> 파이프라인을 생성한다.
     * 호출자(Controller)는 이 Flux 를 그대로 SSE 로 흘려보내면 된다.
     */
    public Flux<TravelEvent> generatePlanStream(String query) {
        return generatePlanStream(query, null, null, null);
    }

    /**
     * 항공편 정보를 함께 요청하는 변형.
     * @param origin     출발 지명 (null 이면 기본 출발지=서울)
     * @param departDate YYYY-MM 또는 YYYY-MM-DD (nullable)
     * @param returnDate YYYY-MM 또는 YYYY-MM-DD (nullable → 편도)
     */
    public Flux<TravelEvent> generatePlanStream(String query, String origin,
                                                String departDate, String returnDate) {
        return Flux.defer(() -> {
            Sinks.Many<TravelEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
            // 별도 스레드에서 파이프라인 실행. 각 단계에서 sink 로 이벤트 push.
            CompletableFuture.runAsync(() -> runPipeline(query, origin, departDate, returnDate, sink),
                    Schedulers.boundedElastic()::schedule);
            return sink.asFlux();
        });
    }

    private void runPipeline(String query, String origin, String departDate, String returnDate,
                             Sinks.Many<TravelEvent> sink) {
        try {
            emit(sink, TravelEvent.builder()
                    .type("stage").stage("analyzing")
                    .message("여행 의도를 분석하는 중입니다.")
                    .build());

            IntentAnalysisDto intent = intentAnalysisService.analyzeIntent(query)
                    .onErrorResume(e -> {
                        log.warn("[orchestrator] intent 분석 실패, 기본값 사용: {}", e.getMessage());
                        return reactor.core.publisher.Mono.just(IntentAnalysisDto.builder()
                                .intent("general")
                                .confidence(0.3)
                                .reason("intent 분석 실패: " + e.getMessage())
                                .build());
                    })
                    .block();

            // intent LLM 이 불안정(실패 시 general)해도 IATA 로 국내/해외를 결정적으로 보정.
            normalizeIntent(intent, query);

            emit(sink, TravelEvent.builder()
                    .type("intent").stage("analyzing")
                    .message(describeIntent(intent))
                    .payload(intent)
                    .build());

            emit(sink, TravelEvent.builder()
                    .type("stage").stage("searching")
                    .message("RAG 컨텍스트(관광공사 · 네이버 블로그)를 병렬로 수집하는 중입니다.")
                    .build());

            // 병렬 수집 (실패해도 파이프라인은 계속 진행).
            CompletableFuture<TouristSpotResponseDto> tourFut = CompletableFuture.supplyAsync(
                    () -> safeCollectTour(intent));
            CompletableFuture<NaverBlogSearchDto> naverFut = CompletableFuture.supplyAsync(
                    () -> safeCollectNaver(intent, query));
            CompletableFuture<List<com.waynai.demo.client.TavilyApiClient.WebResult>> webFut =
                    CompletableFuture.supplyAsync(() -> safeCollectWeb(intent, query));
            CompletableFuture.allOf(tourFut, naverFut, webFut).join();

            TouristSpotResponseDto tour = tourFut.get();
            NaverBlogSearchDto naver = naverFut.get();
            List<com.waynai.demo.client.TavilyApiClient.WebResult> web = webFut.get();

            emit(sink, TravelEvent.builder()
                    .type("sources.tour").stage("searching")
                    .message(tour != null && tour.isSuccess()
                            ? String.format("관광공사 API 에서 관광지 %d개를 찾았습니다.",
                                    nullSafe(tour.getItemCount()))
                            : "관광공사 API 호출 결과가 없습니다.")
                    .payload(toTourSummary(intent, tour))
                    .build());

            emit(sink, TravelEvent.builder()
                    .type("sources.naver").stage("searching")
                    .message(naver != null && naver.getItems() != null
                            ? String.format("네이버 블로그에서 참고 포스트 %d개를 찾았습니다.",
                                    naver.getItems().size())
                            : "네이버 블로그 검색 결과가 없습니다.")
                    .payload(toNaverSummary(query, naver))
                    .build());

            if (web != null && !web.isEmpty()) {
                List<SourceSummaryDto.SourceItem> webItems = web.stream().limit(5)
                        .map(w -> SourceSummaryDto.SourceItem.builder()
                                .title(w.title()).url(w.url()).build())
                        .collect(Collectors.toList());
                emit(sink, TravelEvent.builder()
                        .type("sources.web").stage("searching")
                        .message(String.format("웹에서 참고 자료 %d건을 찾았습니다.", web.size()))
                        .payload(SourceSummaryDto.builder()
                                .source("web").count(web.size()).items(webItems).context(query).build())
                        .build());
            }

            // === 숙소 조회 (Hotellook 실데이터) ===
            List<TravelPlanDto.Accommodation> hotels = safeCollectHotels(intent, query);
            if (hotels != null && !hotels.isEmpty()) {
                TravelPlanDto.Accommodation h = hotels.get(0);
                emit(sink, TravelEvent.builder()
                        .type("sources.hotel").stage("searching")
                        .message(String.format("숙소 %d곳 (예: %s%s)", hotels.size(), h.getName(),
                                h.getPricePerNightKrw() != null ? String.format(" · 1박 %,d원", h.getPricePerNightKrw()) : ""))
                        .payload(hotels)
                        .build());
            }

            // === 항공권 조회 (실데이터 grounding) ===
            List<FlightOfferDto> flights = safeCollectFlights(intent, query, origin, departDate, returnDate);
            if (flights != null && !flights.isEmpty()) {
                FlightOfferDto cheapest = flights.get(0);
                emit(sink, TravelEvent.builder()
                        .type("sources.flight").stage("searching")
                        .message(String.format("항공권 %d건 (최저 %s→%s %,d%s)",
                                flights.size(), cheapest.getOrigin(), cheapest.getDestination(),
                                cheapest.getPrice() != null ? cheapest.getPrice() : 0,
                                cheapest.getCurrency() != null ? cheapest.getCurrency().toUpperCase() : ""))
                        .payload(flights)
                        .build());
            }

            // === 일정 생성 ===
            emit(sink, TravelEvent.builder()
                    .type("stage").stage("generating")
                    .message("여행 일정을 구성하는 중입니다.")
                    .build());

            String ragCtx = buildRagContext(intent, tour, naver, flights, web);
            boolean segmented = intent != null && intent.getSegments() != null && intent.getSegments().size() >= 2;
            TravelPlanDto plan;
            String aiText = "";
            if (segmented) {
                // 권역별로 나눠 상세 생성 → 병합 (긴 다도시 일정에서 누락·일반화 방지).
                plan = generateSegmentedPlan(intent, ragCtx, sink);
            } else {
                String prompt = buildStructuredPrompt(intent, tour, naver, flights, web);
                StringBuilder full = new StringBuilder();
                geminiApiClient.generateJsonStream(prompt, model ->
                        emit(sink, TravelEvent.builder()
                                .type("model").stage("generating")
                                .message("AI 모델을 선택했습니다: " + model)
                                .payload(Map.of("model", model))
                                .build()))
                        .doOnNext(delta -> {
                            full.append(delta);
                            emit(sink, TravelEvent.builder()
                                    .type("token").stage("generating")
                                    .payload(delta)
                                    .build());
                        })
                        .blockLast();
                aiText = full.toString();
                plan = tryParsePlan(aiText);
                // 저가 모델이 긴 JSON 을 깨뜨리는 경우(스마트따옴표·구조붕괴) 비스트리밍으로 1회 재생성.
                if (plan == null) {
                    log.warn("[orchestrator] 스트리밍 JSON 파싱 실패 → 비스트리밍 재생성 1회 시도");
                    try {
                        String retry = geminiApiClient.generateJson(prompt).block();
                        plan = tryParsePlan(retry);
                        if (plan != null) log.info("[orchestrator] 재생성으로 파싱 성공");
                    } catch (Exception re) {
                        log.warn("[orchestrator] 재생성 실패: {}", re.getMessage());
                    }
                }
            }
            if (plan != null && flights != null && !flights.isEmpty()) {
                // LLM 이 지어낸 값 대신 실제 항공권 오퍼를 계획에 부착.
                plan.setFlights(flights);
            }
            if (plan != null && hotels != null && !hotels.isEmpty()) {
                // LLM 추상 숙소 대신 실제 호텔(가격·예약링크) 부착.
                plan.setAccommodation(hotels.get(0));
            }
            // LLM 이 채운 숙소에 예약 링크가 없으면 Hotellook 검색 딥링크로 보완(예약 버튼 활성).
            if (plan != null && plan.getAccommodation() != null
                    && (plan.getAccommodation().getBookingUrl() == null || plan.getAccommodation().getBookingUrl().isBlank())) {
                plan.getAccommodation().setBookingUrl(
                        hotellookApiClient.searchLink(resolveDestinationName(intent, query)));
            }
            // 좌표 누락 보정 + LLM 환각 좌표(한국 밖·지역서 과도히 먼) 교정 (지도/경로/요금/대중교통 정확도↑). 지연 제한 위해 상한.
            if (plan != null) {
                fillMissingCoords(plan, intent);
            }
            // 비용을 규칙 기반으로 현실화 (LLM 추측 대신 항공 실값 + 숙소×박수 + per-diem).
            if (plan != null) {
                computeCosts(plan, intent, flights);
            }
            // 좌표가 있으면 실제 이동시간을 계산해 각 날짜 교통 문구에 반영 (ORS 키 있을 때만).
            if (plan != null && routingApiClient.isEnabled()) {
                enrichRouteTimes(plan, intent);
            }
            // 국내 구간은 daero 대중교통(버스·지하철·기차 + 환승·실요금)으로 추가 보강 (daero 기동 시).
            if (plan != null && daeroClient.isEnabled()) {
                enrichTransit(plan, intent);
            }
            if (plan != null && plan.getItinerary() != null && !plan.getItinerary().isEmpty()) {
                emit(sink, TravelEvent.builder()
                        .type("plan").stage("completed")
                        .message(String.format("%s · %s 일정이 완성됐어요.",
                                plan.getDestination() != null ? plan.getDestination() : "여행지",
                                plan.getDuration() != null ? plan.getDuration() : "맞춤"))
                        .payload(plan)
                        .build());
            } else if (aiText != null && !aiText.isBlank()) {
                // 텍스트는 받았으나 구조화 파싱 실패(모델이 JSON 을 깨뜨림) → 원문 폴백.
                emit(sink, TravelEvent.builder()
                        .type("plan").stage("completed")
                        .message("구조화(JSON) 파싱 결과가 없어 텍스트 본문을 그대로 사용합니다.")
                        .payload(Map.of("fallback", true, "text", aiText))
                        .build());
            } else {
                // LLM 이 아무 응답도 내지 못함(사용량 한도·인증·타임아웃 등) → 빈 계획으로 묻지 말고 명확히 에러.
                log.error("[orchestrator] LLM 빈 응답 → 계획 생성 실패 (OpenRouter 키 한도/인증 확인)");
                emit(sink, TravelEvent.builder()
                        .type("error").stage("error")
                        .message("AI 응답을 받지 못했어요. 잠시 후 다시 시도하거나, 사용량 한도를 확인해 주세요.")
                        .build());
            }

            emit(sink, TravelEvent.builder()
                    .type("done").stage("completed")
                    .message("여행 계획 생성이 완료되었습니다.")
                    .build());

            sink.tryEmitComplete();
        } catch (Exception e) {
            log.error("[orchestrator] 파이프라인 실패", e);
            emit(sink, TravelEvent.builder()
                    .type("error").stage("error")
                    .message("여행 계획 생성 중 오류가 발생했습니다: " + e.getMessage())
                    .build());
            sink.tryEmitComplete();
        }
    }

    private void emit(Sinks.Many<TravelEvent> sink, TravelEvent event) {
        Sinks.EmitResult r = sink.tryEmitNext(event);
        if (r.isFailure()) {
            log.debug("[orchestrator] emit 실패({}): {}", r, event.getType());
        }
    }

    /**
     * intent 결과를 IATA 기반으로 보정한다. intent LLM 이 목적지/국내외를 못 잡아도(파싱 실패 등)
     * 질의에서 도시를 해석해 해외 여부를 결정적으로 판정하고, 목적지명을 채운다.
     */
    private void normalizeIntent(IntentAnalysisDto intent, String query) {
        if (intent == null) return;
        try {
            // 1) 목적지명 후보
            String destName = intent.getDestination();
            if ((destName == null || destName.isBlank()) && intent.getArea() != null) {
                destName = intent.getArea().getName();
            }
            // 2) IATA 해석 (목적지명 우선, 없으면 질의 토큰 스캔)
            String iata = null;
            String matchedToken = null;
            if (destName != null && !destName.isBlank()) {
                iata = iataResolver.resolve(destName);
            }
            if (iata == null && query != null) {
                for (String tok : query.trim().split("\\s+")) {
                    String code = iataResolver.resolve(tok);
                    if (code != null) { iata = code; matchedToken = tok; break; }
                }
            }
            if (iata == null) return; // 해석 불가 → intent 원본 유지

            boolean korean = KOREAN_AIRPORTS.contains(iata);
            // intent 가 명확히 판정하지 못했거나 IATA 와 상충하면 IATA 기준으로 보정
            if (intent.getInternational() == null || intent.getInternational() != !korean) {
                intent.setInternational(!korean);
            }
            // 목적지명이 비어 있으면 매칭 토큰/기존 값으로 채움
            if ((intent.getDestination() == null || intent.getDestination().isBlank())) {
                intent.setDestination(destName != null && !destName.isBlank() ? destName : matchedToken);
            }
            // 해외로 판정되면 국내 지역코드(area)는 오염원이므로 제거
            if (!korean) {
                intent.setArea(null);
            }
            log.info("[orchestrator] intent 보정: dest={}, iata={}, international={}",
                    intent.getDestination(), iata, intent.getInternational());
        } catch (Exception e) {
            log.warn("[orchestrator] intent 보정 실패 (무시): {}", e.getMessage());
        }
    }

    private String describeIntent(IntentAnalysisDto intent) {
        if (intent == null) return "의도 분석 결과 없음";
        String dest = resolveDestinationName(intent, null);
        String keyword = intent.getKeyword() != null ? intent.getKeyword() : "없음";
        String scope = Boolean.TRUE.equals(intent.getInternational()) ? "해외" : "국내";
        return String.format("의도 분석 완료 — 분류: %s · 목적지: %s(%s) · 키워드: %s",
                intent.getIntent(), dest != null ? dest : "미지정", scope, keyword);
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    /**
     * 각 날짜 방문지 좌표열로 실제 총 이동시간(ORS)을 계산해 day.transportation 문구에 덧붙인다.
     * 도보 스타일이면 도보 프로필 사용. 좌표 2개 미만인 날은 건너뜀.
     */
    private void enrichRouteTimes(TravelPlanDto plan, IntentAnalysisDto intent) {
        try {
            if (plan.getItinerary() == null) return;
            String style = intent != null ? intent.getStyle() : null;
            boolean walking = style != null && (style.contains("도보") || style.contains("산책"));
            String profile = walking ? "foot-walking" : "driving-car";
            for (TravelPlanDto.DayPlan day : plan.getItinerary()) {
                if (day.getSpots() == null) continue;
                List<double[]> pts = new ArrayList<>();
                for (TravelPlanDto.Spot s : day.getSpots()) {
                    if (s.getLatitude() != null && s.getLongitude() != null) {
                        pts.add(new double[]{s.getLatitude(), s.getLongitude()});
                    }
                }
                // routeDetail(캐시됨) 사용 → 프론트 지도의 /api/route 와 캐시 공유(중복 ORS 호출 방지).
                var detail = routingApiClient.routeDetail(pts, profile);
                if (detail == null) continue;
                long min = Math.round(detail.durationSeconds() / 60.0);
                double km = detail.distanceMeters() / 1000.0;
                String note = String.format("실제 이동 약 %d분 · %.1fkm (%s)",
                        min, km, walking ? "도보" : "차량");
                day.setTransportation(day.getTransportation() == null || day.getTransportation().isBlank()
                        ? note
                        : day.getTransportation() + " · " + note);
            }
            log.info("[orchestrator] ORS 이동시간 반영 완료");
        } catch (Exception e) {
            log.warn("[orchestrator] 이동시간 계산 실패 (무시): {}", e.getMessage());
        }
    }

    /**
     * 국내 구간 대중교통 실경로를 daero 엔진으로 조회해 각 날짜 교통 문구에 보강.
     * ORS(차량/도보)와 달리 버스·지하철·기차 + 환승·실요금 포함. 방문지 좌표 연속쌍의 대중교통을
     * 합산해 하루 요약을 만든다. 해외 목적지·daero 미기동·무경로 시 조용히 건너뜀(best-effort).
     */
    private void enrichTransit(TravelPlanDto plan, IntentAnalysisDto intent) {
        try {
            if (plan.getItinerary() == null) return;
            // daero 는 국내 GTFS 전용 → 해외 목적지엔 적용하지 않음.
            if (intent != null && Boolean.TRUE.equals(intent.getInternational())) return;
            int enrichedDays = 0;
            for (TravelPlanDto.DayPlan day : plan.getItinerary()) {
                if (day.getSpots() == null || day.getSpots().size() < 2) continue;
                List<double[]> pts = new ArrayList<>();
                for (TravelPlanDto.Spot s : day.getSpots()) {
                    if (s.getLatitude() != null && s.getLongitude() != null) {
                        pts.add(new double[]{s.getLatitude(), s.getLongitude()});
                    }
                }
                if (pts.size() < 2) continue;
                int totalMin = 0, totalTransfers = 0, totalFare = 0, legs = 0;
                java.util.LinkedHashSet<String> dayModes = new java.util.LinkedHashSet<>();
                for (int i = 0; i + 1 < pts.size(); i++) {
                    var t = daeroClient.transit(pts.get(i)[0], pts.get(i)[1],
                            pts.get(i + 1)[0], pts.get(i + 1)[1], "09:00");
                    if (t == null) continue;
                    totalMin += t.durationMin();
                    totalTransfers += t.transfers();
                    totalFare += t.fareKrw();
                    legs++;
                    if (t.modeSummary() != null && !t.modeSummary().isBlank()) {
                        for (String m : t.modeSummary().split("·")) if (!m.isBlank()) dayModes.add(m);
                    }
                }
                if (legs == 0) continue;
                String modeStr = dayModes.isEmpty() ? "" : " (" + String.join("·", dayModes) + ")";
                String note = String.format("대중교통 약 %d분·환승 %d회·~%,d원", totalMin, totalTransfers, totalFare) + modeStr;
                day.setTransportation(day.getTransportation() == null || day.getTransportation().isBlank()
                        ? note
                        : day.getTransportation() + " · " + note);
                enrichedDays++;
            }
            if (enrichedDays > 0) log.info("[orchestrator] daero 대중교통 반영 완료 ({}일)", enrichedDays);
        } catch (Exception e) {
            log.warn("[orchestrator] daero 대중교통 보강 실패 (무시): {}", e.getMessage());
        }
    }

    /** intent 에서 도착지명 결정: destination → area.name → 원 질의 순. */
    private String resolveDestinationName(IntentAnalysisDto intent, String fallbackQuery) {
        if (intent != null && intent.getDestination() != null && !intent.getDestination().isBlank()) {
            return intent.getDestination();
        }
        if (intent != null && intent.getArea() != null && intent.getArea().getName() != null
                && !intent.getArea().getName().isBlank()) {
            return intent.getArea().getName();
        }
        return fallbackQuery;
    }

    private TouristSpotResponseDto safeCollectTour(IntentAnalysisDto intent) {
        try {
            // 해외 목적지는 한국관광공사 API 에 데이터가 없으므로 스킵 (엉뚱한 국내 지역 오염 방지).
            if (intent != null && Boolean.TRUE.equals(intent.getInternational())) {
                log.info("[orchestrator] 해외 목적지({}) → 한국관광공사 RAG 스킵", intent.getDestination());
                return null;
            }
            // 지역을 특정하지 못하면 랜덤 국내 관광지를 먹이지 않는다(엉뚱한 지역 계획 방지).
            // 블로그 RAG + LLM 지식으로 대체.
            if (intent == null || intent.getArea() == null) {
                log.info("[orchestrator] 국내 지역 미특정 → 관광공사 RAG 스킵(랜덤 방지)");
                return null;
            }
            String areaCode = intent.getArea().getCode();
            String sigunguCode = intent.getArea().getSigungu() != null
                    ? intent.getArea().getSigungu().getCode()
                    : null;
            if (sigunguCode == null || "UNKNOWN".equals(sigunguCode)) {
                return touristInfoService.getRandomSigunguTouristSpots(areaCode, 1, 20);
            }
            return touristInfoService.getTouristSpots(areaCode, sigunguCode, 1, 20);
        } catch (Exception e) {
            log.warn("[orchestrator] 관광공사 API 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    /** 숙소 조회(Hotellook). 미설정/실패 시 빈 리스트. */
    private List<TravelPlanDto.Accommodation> safeCollectHotels(IntentAnalysisDto intent, String query) {
        try {
            if (!hotellookApiClient.isEnabled()) return List.of();
            String dest = resolveDestinationName(intent, query);
            if (dest == null || dest.isBlank()) return List.of();
            return hotellookApiClient.search(dest, 4);
        } catch (Exception e) {
            log.warn("[orchestrator] 숙소 조회 실패 (무시): {}", e.getMessage());
            return List.of();
        }
    }

    /** 웹 검색(Tavily). 해외/국내 모두 보강용. 미설정/실패 시 빈 리스트. */
    private List<com.waynai.demo.client.TavilyApiClient.WebResult> safeCollectWeb(IntentAnalysisDto intent, String query) {
        try {
            if (!tavilyApiClient.isEnabled()) return List.of();
            String dest = resolveDestinationName(intent, null);
            String kw = intent != null && intent.getKeyword() != null ? intent.getKeyword() : "";
            // 목적지 중심 질의 구성 (해외일수록 웹 검색이 유용).
            String q = (dest != null && !dest.isBlank())
                    ? (dest + " 여행 추천 명소 맛집 " + kw).trim()
                    : query;
            return tavilyApiClient.search(q);
        } catch (Exception e) {
            log.warn("[orchestrator] 웹 검색 실패 (무시): {}", e.getMessage());
            return List.of();
        }
    }

    private NaverBlogSearchDto safeCollectNaver(IntentAnalysisDto intent, String query) {
        try {
            // 긴 원문을 그대로 넘기면 네이버가 414(URI too large)를 낸다 → 목적지+키워드로 간결화.
            String dest = resolveDestinationName(intent, null);
            String kw = intent != null && intent.getKeyword() != null ? intent.getKeyword() : "";
            String q = (dest != null && !dest.isBlank()) ? (dest + " 여행 " + kw).trim() : query;
            if (q.length() > 80) q = q.substring(0, 80);
            return naverSearchService.searchBlog(q).block();
        } catch (Exception e) {
            log.warn("[orchestrator] 네이버 검색 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    /**
     * 항공권 조회 (실패해도 파이프라인 계속). 도착지는 intent 지역명을 우선 사용한다.
     * Travelpayouts 미설정이면 서비스가 빈 리스트를 반환한다.
     */
    private List<FlightOfferDto> safeCollectFlights(IntentAnalysisDto intent, String query,
                                                    String origin, String departDate, String returnDate) {
        try {
            if (!flightSearchService.isEnabled()) return List.of();
            String destination = resolveDestinationName(intent, query);
            // 국내는 항공이 부적절(서울→부산=KTX)하나, 오판 방지를 위해 "intent 가 한국 지역(area)을
            // 확실히 식별한 경우"에만 스킵한다. area 불명이면(해외일 가능성) 항공권을 조회한다.
            boolean confirmedKoreanArea = intent != null
                    && !Boolean.TRUE.equals(intent.getInternational())
                    && intent.getArea() != null
                    && intent.getArea().getName() != null && !intent.getArea().getName().isBlank();
            if (confirmedKoreanArea && !needsDomesticFlight(destination)) {
                log.info("[orchestrator] 국내 지역({}) → 항공권 스킵(KTX/버스 위주)", intent.getArea().getName());
                return List.of();
            }
            // 명시 파라미터 우선, 없으면 자연어에서 추출한 intent 값 사용.
            String effOrigin = firstNonBlank(origin, intent != null ? intent.getOrigin() : null);
            String effDepart = firstNonBlank(departDate, intent != null ? intent.getDepartDate() : null);
            String effReturn = firstNonBlank(returnDate, intent != null ? intent.getReturnDate() : null);
            // 귀국일 미지정 + 출발일이 '전체 날짜(YYYY-MM-DD)'일 때만 출발+여행일수로 계산.
            // (월 단위 YYYY-MM 이면 형식 불일치를 피하려 그대로 두고 Travelpayouts 왕복 최저가에 맡김)
            if ((effReturn == null || effReturn.isBlank()) && effDepart != null && effDepart.length() == 10
                    && intent != null && intent.getDays() != null) {
                effReturn = addDays(effDepart, intent.getDays());
            }
            // 출발이 '월 단위(YYYY-MM)'뿐이면 Travelpayouts 가 같은날 왕복 캐시를 주므로, 날짜를 비워
            // 현실적인 왕복 최저가(출발~귀국 며칠 간격)를 받도록 한다.
            String qDepart = (effDepart != null && effDepart.length() == 10) ? effDepart : null;
            String qReturn = (qDepart != null) ? effReturn : null;
            return flightSearchService.search(effOrigin, destination, qDepart, qReturn, 5);
        } catch (Exception e) {
            log.warn("[orchestrator] 항공권 조회 실패 (무시): {}", e.getMessage());
            return List.of();
        }
    }

    /** 국내에서도 항공이 필요한 도서/원거리(화이트리스트). */
    private boolean needsDomesticFlight(String dest) {
        if (dest == null) return false;
        String d = dest.replaceAll("\\s", "");
        return d.contains("제주") || d.contains("울릉") || d.contains("흑산");
    }

    /** "YYYY-MM-DD" 또는 "YYYY-MM"(→01일)에 days-1 을 더한 날짜 문자열. 실패 시 원본. */
    private String addDays(String date, int days) {
        try {
            String ymd = date.length() == 7 ? date + "-01" : date;
            java.time.LocalDate d = java.time.LocalDate.parse(ymd).plusDays(Math.max(1, days - 1));
            return d.toString();
        } catch (Exception e) {
            return date;
        }
    }

    private SourceSummaryDto toTourSummary(IntentAnalysisDto intent, TouristSpotResponseDto tour) {
        if (tour == null || tour.getItems() == null) {
            return SourceSummaryDto.builder()
                    .source("tour").count(0).items(new ArrayList<>())
                    .context(intent != null && intent.getArea() != null ? intent.getArea().getName() : null)
                    .build();
        }
        List<SourceSummaryDto.SourceItem> items = tour.getItems().stream()
                .limit(8)
                .map(this::toTourItem)
                .collect(Collectors.toList());
        String context = null;
        if (!tour.getItems().isEmpty()) {
            TouristSpotDto first = tour.getItems().get(0);
            context = String.format("%s %s",
                    nullSafe(first.getAreaNm()),
                    nullSafe(first.getSignguNm())).trim();
        }
        return SourceSummaryDto.builder()
                .source("tour")
                .count(nullSafe(tour.getItemCount()))
                .items(items)
                .context(context)
                .build();
    }

    private SourceSummaryDto.SourceItem toTourItem(TouristSpotDto spot) {
        return SourceSummaryDto.SourceItem.builder()
                .title(nullSafe(spot.getHubTatsNm()))
                .subtitle(String.format("%s · %s",
                        nullSafe(spot.getSignguNm()),
                        nullSafe(spot.getHubCtgryMclsNm())))
                .build();
    }

    private SourceSummaryDto toNaverSummary(String query, NaverBlogSearchDto naver) {
        if (naver == null || naver.getItems() == null) {
            return SourceSummaryDto.builder()
                    .source("naver").count(0).items(new ArrayList<>())
                    .context(query)
                    .build();
        }
        List<SourceSummaryDto.SourceItem> items = naver.getItems().stream()
                .limit(5)
                .map(item -> SourceSummaryDto.SourceItem.builder()
                        .title(stripHtml(item.getTitle()))
                        .subtitle(nullSafe(item.getBloggerName()))
                        .url(item.getLink())
                        .build())
                .collect(Collectors.toList());
        return SourceSummaryDto.builder()
                .source("naver")
                .count(naver.getTotal() != null ? naver.getTotal() : naver.getItems().size())
                .items(items)
                .context(query)
                .build();
    }

    /** RAG(웹검색·항공·관광공사·블로그) + 해외 안내를 하나의 컨텍스트 문자열로. 단일/권역 생성 공용. */
    private String buildRagContext(IntentAnalysisDto intent, TouristSpotResponseDto tour,
                                   NaverBlogSearchDto naver, List<FlightOfferDto> flights,
                                   List<com.waynai.demo.client.TavilyApiClient.WebResult> web) {
        StringBuilder ctx = new StringBuilder();
        if (web != null && !web.isEmpty()) {
            ctx.append("=== 웹 검색 결과 (최신 현지 정보 — 실제 명소·식당·팁 근거로 활용) ===\n");
            web.stream().limit(6).forEach(w -> ctx.append(String.format(
                    "- %s\n  %s\n", nullSafe(w.title()),
                    w.content() != null && w.content().length() > 240
                            ? w.content().substring(0, 240) : nullSafe(w.content()))));
            ctx.append("\n");
        }
        if (flights != null && !flights.isEmpty()) {
            ctx.append("=== 실제 항공권 최저가 (Travelpayouts) ===\n");
            flights.stream().limit(3).forEach(f -> ctx.append(String.format(
                    "- %s→%s %s · %,d%s\n", nullSafe(f.getOrigin()), nullSafe(f.getDestination()),
                    nullSafe(f.getAirline()), f.getPrice() != null ? f.getPrice() : 0,
                    f.getCurrency() != null ? f.getCurrency().toUpperCase() : "")));
            ctx.append("\n");
        }
        if (tour != null && tour.getItems() != null && !tour.getItems().isEmpty()) {
            ctx.append("=== 관광공사 관광지 ===\n");
            tour.getItems().stream().limit(12).forEach(s -> ctx.append(String.format(
                    "- %s (%s %s)\n", nullSafe(s.getHubTatsNm()), nullSafe(s.getAreaNm()), nullSafe(s.getSignguNm()))));
            ctx.append("\n");
        }
        if (naver != null && naver.getItems() != null && !naver.getItems().isEmpty()) {
            ctx.append("=== 네이버 블로그 참고 ===\n");
            naver.getItems().stream().limit(5).forEach(b -> ctx.append(String.format(
                    "- %s — %s\n", stripHtml(b.getTitle()), stripHtml(b.getDescription()))));
            ctx.append("\n");
        }
        if (ctx.length() == 0) ctx.append("참고 정보가 없습니다. 일반적인 여행 상식으로 생성해 주세요.\n");
        return ctx.toString();
    }

    private String buildStructuredPrompt(IntentAnalysisDto intent, TouristSpotResponseDto tour,
                                         NaverBlogSearchDto naver, List<FlightOfferDto> flights,
                                         List<com.waynai.demo.client.TavilyApiClient.WebResult> web) {
        boolean intl = intent != null && Boolean.TRUE.equals(intent.getInternational());
        String dest = resolveDestinationName(intent, null);
        StringBuilder ctx = new StringBuilder(buildRagContext(intent, tour, naver, flights, web));
        if (intl) {
            ctx.append(String.format(
                    "\n=== 해외 목적지 안내 ===\n"
                  + "목적지는 해외 '%s'. 위 웹검색/블로그의 **실제 명소·식당명**을 그대로 활용하고, destination 은 '%s'.\n",
                    dest, dest));
        }

        String keyword = intent != null && intent.getKeyword() != null ? intent.getKeyword() : "없음";
        String intentLine = String.format(
                "의도=%s, 목적지=%s(%s), 키워드=%s, 일수=%s, 스타일=%s, 예산=%s, 동반=%s, 블로그 후보=%d건",
                intent != null ? intent.getIntent() : "general",
                dest != null ? dest : "미지정",
                intl ? "해외" : "국내",
                keyword,
                intent != null && intent.getDays() != null ? intent.getDays() + "일" : "미지정",
                intent != null && intent.getStyle() != null ? intent.getStyle() : "미지정",
                intent != null && intent.getBudgetLevel() != null ? intent.getBudgetLevel() : "미지정",
                intent != null && intent.getCompanions() != null ? intent.getCompanions() : "미지정",
                naver != null && naver.getItems() != null ? naver.getItems().size() : 0);

        Map<String, String> variables = new HashMap<>();
        variables.put("intent", intentLine);
        variables.put("context", ctx.toString());

        String prompt = promptLoader.getPromptWithVariables("travel_plan_structured", variables);
        if (prompt == null) {
            // 프롬프트 템플릿이 없으면 간단 프롬프트 폴백
            prompt = "아래 참고 정보를 바탕으로 여행 계획을 JSON 으로 작성해줘.\n\n"
                    + "의도: " + intentLine + "\n\n"
                    + "참고 정보:\n" + ctx;
        }
        return prompt;
    }

    /**
     * 권역별로 나눠 상세 일정을 생성하고 하나의 계획으로 병합한다.
     * 각 권역이 완성될 때마다 partial 이벤트로 실시간 빌드업을 보낸다.
     */
    private TravelPlanDto generateSegmentedPlan(IntentAnalysisDto intent, String ragCtx,
                                                Sinks.Many<TravelEvent> sink) {
        List<TravelPlanDto.DayPlan> merged = new ArrayList<>();
        TravelPlanDto plan = TravelPlanDto.builder()
                .type("travel_plan")
                .destination(resolveDestinationName(intent, null))
                .days(intent.getDays())
                .duration(intent.getDays() != null ? intent.getDays() + "일" : null)
                .theme(intent.getKeyword())
                .build();
        List<IntentAnalysisDto.Segment> segs = intent.getSegments();
        int totalDays = intent.getDays() != null && intent.getDays() > 0 ? intent.getDays() : segs.size() * 3;
        // 권역별 일수를 총일수에 맞춰 결정적으로 분배(합계=totalDays 보장). intent 권역일수는 가중치로만.
        int[] segDaysArr = distributeDays(totalDays, segs);
        int dayCounter = 1;
        for (int si = 0; si < segs.size(); si++) {
            IntentAnalysisDto.Segment seg = segs.get(si);
            int segDays = segDaysArr[si];
            String segTitle = seg.getTitle() != null ? seg.getTitle() : (si + 1) + "권역";
            emit(sink, TravelEvent.builder()
                    .type("stage").stage("generating")
                    .message(String.format("%s 일정을 구성하는 중… (%d/%d)", segTitle, si + 1, segs.size()))
                    .build());
            try {
                Map<String, String> vars = new HashMap<>();
                vars.put("segment", describeSegment(seg, segDays));
                vars.put("startDay", String.valueOf(dayCounter));
                vars.put("days", String.valueOf(segDays));
                vars.put("context", ragCtx);
                String prompt = promptLoader.getPromptWithVariables("travel_plan_segment", vars);
                String text = geminiApiClient.generateJson(prompt).block();
                for (TravelPlanDto.DayPlan d : parseSegmentDays(text)) {
                    d.setDay(dayCounter++);
                    merged.add(d);
                }
            } catch (Exception e) {
                log.warn("[orchestrator] 권역 생성 실패({}) 무시: {}", segTitle, e.getMessage());
            }
            plan.setItinerary(new ArrayList<>(merged));
            emit(sink, TravelEvent.builder()
                    .type("partial").stage("generating")
                    .message(String.format("%s 완료 · 누적 %d일", segTitle, merged.size()))
                    .payload(plan)
                    .build());
        }
        // 목표 일수 초과 시 트림 + day 재번호 (권역 균등분배로 1~2일 초과 방지).
        if (intent.getDays() != null && intent.getDays() > 0 && merged.size() > intent.getDays()) {
            merged = new ArrayList<>(merged.subList(0, intent.getDays()));
        }
        for (int i = 0; i < merged.size(); i++) merged.get(i).setDay(i + 1);
        plan.setItinerary(merged);
        plan.setDays(merged.size());
        plan.setDuration(merged.size() + "일");
        enrichMeta(plan, intent);
        return plan;
    }

    private String describeSegment(IntentAnalysisDto.Segment seg, int segDays) {
        StringBuilder b = new StringBuilder();
        b.append("권역: ").append(nullSafe(seg.getTitle()));
        if (seg.getDates() != null && !seg.getDates().isBlank()) b.append(" (").append(seg.getDates()).append(")");
        b.append("\n대표 지역/도시: ").append(nullSafe(seg.getArea()));
        b.append("\n일수: ").append(segDays).append("일");
        b.append("\n키워드: ").append(nullSafe(seg.getKeywords()));
        if (seg.getMustInclude() != null && !seg.getMustInclude().isEmpty()) {
            b.append("\n반드시 포함(생략 금지): ").append(String.join(", ", seg.getMustInclude()));
        }
        return b.toString();
    }

    private List<TravelPlanDto.DayPlan> parseSegmentDays(String text) {
        List<TravelPlanDto.DayPlan> out = new ArrayList<>();
        try {
            String json = extractJson(text);
            if (json == null) return out;
            var root = objectMapper.readTree(json);
            var it = root.path("itinerary");
            if (it.isArray()) {
                for (var node : it) out.add(objectMapper.treeToValue(node, TravelPlanDto.DayPlan.class));
            }
        } catch (Exception e) {
            log.warn("[orchestrator] 권역 일정 파싱 실패: {}", e.getMessage());
        }
        return out;
    }

    /** 병합 후 상단 메타(요약·날씨·현지·준비물·팁)를 짧게 채운다. */
    private void enrichMeta(TravelPlanDto plan, IntentAnalysisDto intent) {
        try {
            String dest = plan.getDestination();
            String metaPrompt = "다음 여행의 요약 정보만 순수 JSON 으로 반환(설명·코드펜스 금지):\n"
                    + "{\"summary\":\"한 줄 요약\",\"theme\":\"테마\",\"weatherInfo\":\"기간 날씨·복장\","
                    + "\"localInfo\":\"현지 치안·교통·통화·팁\",\"packingList\":[\"준비물 6~8개\"],\"tips\":[\"핵심 팁 5개 이내\"]}\n"
                    + "목적지: " + dest + ", 기간: " + (plan.getDays() != null ? plan.getDays() : "") + "일"
                    + (intent != null && intent.getStyle() != null ? ", 스타일: " + intent.getStyle() : "");
            String t = geminiApiClient.generateJson(metaPrompt).block();
            String json = extractJson(t);
            if (json == null) return;
            var m = objectMapper.readTree(json);
            if (plan.getSummary() == null) plan.setSummary(m.path("summary").asText(null));
            if (plan.getTheme() == null || plan.getTheme().isBlank()) plan.setTheme(m.path("theme").asText(plan.getTheme()));
            plan.setWeatherInfo(m.path("weatherInfo").asText(null));
            plan.setLocalInfo(m.path("localInfo").asText(null));
            List<String> packing = new ArrayList<>();
            for (var p : m.path("packingList")) packing.add(p.asText());
            if (!packing.isEmpty()) plan.setPackingList(packing);
            List<String> tips = new ArrayList<>();
            for (var p : m.path("tips")) tips.add(p.asText());
            if (!tips.isEmpty()) plan.setTips(tips);
        } catch (Exception e) {
            log.warn("[orchestrator] 메타 생성 실패 (무시): {}", e.getMessage());
        }
    }

    /** 총일수를 권역에 결정적으로 분배(합계=total 보장). 권역별 intent 일수를 가중치로, 없으면 균등. */
    private int[] distributeDays(int total, List<IntentAnalysisDto.Segment> segs) {
        int n = segs.size();
        int[] out = new int[n];
        int[] w = new int[n];
        int wsum = 0;
        boolean allValid = true;
        for (int i = 0; i < n; i++) {
            Integer d = segs.get(i).getDays();
            if (d == null || d <= 0) allValid = false;
        }
        for (int i = 0; i < n; i++) {
            Integer d = segs.get(i).getDays();
            w[i] = allValid ? d : 1;
            wsum += w[i];
        }
        int assigned = 0;
        for (int i = 0; i < n; i++) {
            out[i] = Math.max(1, (int) Math.floor((double) total * w[i] / wsum));
            assigned += out[i];
        }
        int diff = total - assigned;
        for (int i = 0; diff != 0 && n > 0; i = (i + 1) % n) {
            if (diff > 0) { out[i]++; diff--; }
            else if (out[i] > 1) { out[i]--; diff++; }
        }
        return out;
    }

    /** 좌표 없는 방문지를 이름+목적지로 지오코딩해 채운다(상한 있음, best-effort). */
    private void fillMissingCoords(TravelPlanDto plan, IntentAnalysisDto intent) {
        try {
            if (plan.getItinerary() == null) return;
            String region = plan.getDestination();
            boolean domestic = intent == null || !Boolean.TRUE.equals(intent.getInternational());
            // 국내 여행: 지역 중심 좌표(1회 지오코딩)를 스팟 타당성 기준으로 사용.
            double[] center = (domestic && region != null && !region.isBlank())
                    ? geocodingClient.geocode(region, "대한민국") : null;
            int budget = 12; // Nominatim 초당 1회 → 지연 제한 위해 최대 12곳만
            int fixed = 0;
            for (TravelPlanDto.DayPlan day : plan.getItinerary()) {
                if (day.getSpots() == null) continue;
                for (TravelPlanDto.Spot s : day.getSpots()) {
                    boolean missing = s.getLatitude() == null || s.getLongitude() == null;
                    boolean implausible = false;
                    if (!missing && domestic) {
                        double lat = s.getLatitude(), lon = s.getLongitude();
                        boolean inKorea = lat >= 33 && lat <= 39.5 && lon >= 124 && lon <= 132;
                        boolean farFromRegion = center != null && haversineKm(lat, lon, center[0], center[1]) > 100;
                        implausible = !inKorea || farFromRegion; // 한국 밖 or 지역서 100km↑ → LLM 환각 의심
                    }
                    if (!(missing || implausible)) continue;
                    if (s.getName() == null || s.getName().isBlank()) continue;
                    if (budget <= 0) { if (implausible) { s.setLatitude(null); s.setLongitude(null); } continue; }
                    double[] p = geocodingClient.geocode(s.getName(), region);
                    budget--;
                    boolean okKorea = p != null && (!domestic || (p[0] >= 33 && p[0] <= 39.5 && p[1] >= 124 && p[1] <= 132));
                    if (okKorea) { s.setLatitude(p[0]); s.setLongitude(p[1]); if (implausible) fixed++; }
                    else if (implausible) { s.setLatitude(null); s.setLongitude(null); fixed++; } // 교정 실패 → 오염 좌표 제거
                }
            }
            if (fixed > 0) log.info("[orchestrator] 좌표 교정(환각/오류) {}건", fixed);
        } catch (Exception e) {
            log.warn("[orchestrator] 지오코딩 보정 실패 (무시)", e);
        }
    }

    /** 두 좌표 간 대략 거리(km, Haversine). */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0, dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 동반 유형/스타일로 인원 추정. */
    private int detectParty(IntentAnalysisDto intent) {
        String c = intent != null && intent.getCompanions() != null ? intent.getCompanions() : "";
        String s = intent != null && intent.getStyle() != null ? intent.getStyle() : "";
        if (c.contains("혼자")) return 1;
        if (c.contains("가족")) return 4;
        if (c.contains("단체")) return 6;
        if (s.contains("신혼") || c.contains("커플") || c.contains("신혼")) return 2;
        if (c.contains("친구")) return 2;
        return 2; // 기본 2인
    }

    /**
     * 비용을 규칙 기반으로 현실화. 항공(실값)×인원 + 숙소(1박가×박수×객실) + 현지 per-diem(식비/교통/입장).
     * LLM 이 채운 costBreakdown(0/비현실)을 덮어쓰고 estimatedBudget=합계로 정합.
     */
    private void computeCosts(TravelPlanDto plan, IntentAnalysisDto intent, List<FlightOfferDto> flights) {
        try {
            int days = plan.getDays() != null && plan.getDays() > 0 ? plan.getDays()
                    : (plan.getItinerary() != null ? Math.max(1, plan.getItinerary().size()) : 1);
            int nights = Math.max(1, days - 1);
            int party = detectParty(intent);
            int rooms = (int) Math.ceil(party / 2.0);
            int intercity = intent != null && intent.getSegments() != null
                    ? Math.max(0, intent.getSegments().size() - 1) : 0;

            int cheapestFlight = 0;
            if (flights != null) {
                cheapestFlight = flights.stream().map(FlightOfferDto::getPrice)
                        .filter(java.util.Objects::nonNull).min(Integer::compareTo).orElse(0);
            }
            boolean estimatedFlight = false;
            // 해외인데 실항공권을 못 구했으면 노선 기반 왕복 추정(예산 과소평가 방지).
            if (cheapestFlight <= 0 && intent != null && Boolean.TRUE.equals(intent.getInternational())) {
                cheapestFlight = estimateIntlRoundTripKrw(intent);
                estimatedFlight = cheapestFlight > 0;
            }
            // 목적지 물가 계수(한국=1.0 기준): 동남아 0.6, 유럽/일본 1.35, 초고물가 1.7 등.
            double coef = costCoef(intent);
            int defaultPerNight = (int) Math.round(120000 * coef);
            Integer llmPerNight = plan.getAccommodation() != null ? plan.getAccommodation().getPricePerNightKrw() : null;
            // LLM 이 넣은 1박 요금이 비현실적으로 낮으면(통화혼동·자리표시자) 계수 기반 기본값으로 대체.
            int perNight = (llmPerNight != null && llmPerNight >= 15000) ? llmPerNight : defaultPerNight;
            if ((llmPerNight == null || llmPerNight < 15000) && plan.getAccommodation() != null) {
                plan.getAccommodation().setPricePerNightKrw(perNight); // UI 표시 일관성
            }
            int foodPerDay = (int) Math.round(40000 * coef);      // 1인 1일 식비
            int activPerDay = (int) Math.round(20000 * coef);     // 1인 1일 입장/액티비티
            int localTransDay = (int) Math.round(15000 * coef);   // 1일 현지 교통(공용)
            int intercityFare = (int) Math.round(60000 * coef);   // 도시 간 이동 1회(1인)

            int flightsKrw = cheapestFlight * party;
            int accommodationKrw = perNight * nights * rooms;
            int foodKrw = foodPerDay * party * days;
            int transportKrw = localTransDay * days + intercityFare * intercity * party;
            int activitiesKrw = activPerDay * party * days;
            int etcKrw = (int) Math.round(0.1 * (flightsKrw + accommodationKrw + foodKrw + transportKrw + activitiesKrw));

            plan.setCostBreakdown(TravelPlanDto.CostBreakdown.builder()
                    .flightsKrw(flightsKrw).accommodationKrw(accommodationKrw).foodKrw(foodKrw)
                    .transportKrw(transportKrw).activitiesKrw(activitiesKrw).etcKrw(etcKrw).build());
            int total = flightsKrw + accommodationKrw + foodKrw + transportKrw + activitiesKrw + etcKrw;
            plan.setEstimatedBudgetKrw(total);
            plan.setBudget(String.format("약 %,d원 (%d인 기준%s)", total, party,
                    estimatedFlight ? ", 항공권 추정치 포함" : ""));
            log.info("[orchestrator] 비용 재계산: 총 {}원 ({}인, {}박, 물가계수 {})", total, party, nights, coef);
        } catch (Exception e) {
            log.warn("[orchestrator] 비용 계산 실패 (무시): {}", e.getMessage());
        }
    }

    /** 목적지 물가 계수(한국=1.0). per-diem(식비·교통·입장·숙박폴백)에 곱해 예산 현실화. */
    private double costCoef(IntentAnalysisDto intent) {
        if (intent == null) return 1.0;
        String d = ((intent.getDestination() != null ? intent.getDestination() : "")
                + " " + (intent.getKeyword() != null ? intent.getKeyword() : "")
                + " " + (intent.getArea() != null && intent.getArea().getName() != null ? intent.getArea().getName() : "")).toLowerCase();
        // 초고물가(1.7): 스위스·북유럽·뉴욕·하와이·싱가포르·런던·두바이·아이슬란드
        String[] veryHigh = {"스위스", "취리히", "제네바", "인터라켄", "노르웨이", "덴마크", "핀란드", "아이슬란드", "뉴욕", "맨해튼", "하와이", "싱가포르", "런던", "두바이", "switzerland", "norway", "iceland", "new york", "singapore", "london", "dubai", "hawaii"};
        // 고물가(1.35): 서유럽·미국일반·호주·일본·캐나다
        String[] high = {"유럽", "이탈리아", "로마", "밀라노", "베네치아", "피렌체", "프랑스", "파리", "스페인", "바르셀로나", "독일", "네덜란드", "암스테르담", "오스트리아", "벨기에", "미국", "la", "로스앤젤레스", "캐나다", "호주", "시드니", "멜버른", "뉴질랜드", "일본", "도쿄", "오사카", "교토", "후쿠오카", "삿포로", "italy", "rome", "france", "paris", "spain", "germany", "usa", "australia", "japan", "tokyo", "osaka"};
        // 저물가(0.6): 동남아·동유럽
        String[] low = {"베트남", "다낭", "하노이", "호치민", "나트랑", "태국", "방콕", "치앙마이", "푸켓", "필리핀", "세부", "보라카이", "말레이시아", "쿠알라룸푸르", "인도네시아", "발리", "캄보디아", "앙코르", "라오스", "체코", "프라하", "헝가리", "부다페스트", "폴란드", "vietnam", "thailand", "bangkok", "philippines", "indonesia", "bali", "cambodia", "laos", "prague"};
        // 초저물가(0.45): 인도·네팔·이집트 등
        String[] veryLow = {"인도", "네팔", "이집트", "스리랑카", "india", "nepal", "egypt"};
        for (String s : veryHigh) if (d.contains(s)) return 1.7;
        for (String s : veryLow) if (d.contains(s)) return 0.45;
        for (String s : low) if (d.contains(s)) return 0.6;
        for (String s : high) if (d.contains(s)) return 1.35;
        // 국내(한국) 또는 미분류 = 기준 1.0
        return 1.0;
    }

    /** 실항공권 미검색 시 목적지 지역으로 왕복 1인 요금 추정(원). 대략치. */
    private int estimateIntlRoundTripKrw(IntentAnalysisDto intent) {
        String d = ((intent.getDestination() != null ? intent.getDestination() : "")
                + " " + (intent.getKeyword() != null ? intent.getKeyword() : "")).toLowerCase();
        // 근거리 아시아(일본/중국/대만/홍콩)
        String[] near = {"일본", "도쿄", "오사카", "후쿠오카", "삿포로", "교토", "중국", "상하이", "베이징", "칭다오", "대만", "타이베이", "홍콩", "japan", "china", "taiwan", "hong kong"};
        // 동남아
        String[] sea = {"베트남", "다낭", "하노이", "호치민", "태국", "방콕", "치앙마이", "필리핀", "세부", "싱가포르", "말레이시아", "쿠알라룸푸르", "발리", "인도네시아", "캄보디아", "라오스", "vietnam", "thailand", "singapore", "bali"};
        // 유럽
        String[] eu = {"유럽", "이탈리아", "로마", "프랑스", "파리", "영국", "런던", "스페인", "독일", "스위스", "네덜란드", "체코", "프라하", "오스트리아", "그리스", "포르투갈", "italy", "france", "spain", "europe", "london", "paris"};
        // 미주/오세아니아/중동
        String[] far = {"미국", "뉴욕", "la", "로스앤젤레스", "하와이", "캐나다", "호주", "시드니", "뉴질랜드", "브라질", "두바이", "usa", "america", "australia", "hawaii", "dubai"};
        for (String s : eu) if (d.contains(s)) return 1300000;
        for (String s : far) if (d.contains(s)) return 1500000;
        for (String s : sea) if (d.contains(s)) return 500000;
        for (String s : near) if (d.contains(s)) return 350000;
        return 900000; // 미분류 해외 기본값
    }

    private TravelPlanDto tryParsePlan(String text) {
        try {
            String json = extractJson(text);
            if (json == null || json.isBlank() || !json.startsWith("{")) return null;
            return objectMapper.readValue(json, TravelPlanDto.class);
        } catch (Exception e) {
            log.warn("[orchestrator] 구조화 플랜 파싱 실패 (텍스트로 대체): {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String text) {
        if (text == null) return null;
        // 저가 모델이 종종 스마트따옴표(“ ” ‘ ’)를 섞어 JSON 을 깨뜨림 → 표준 따옴표로 정규화.
        String t = text.replace('“', '"').replace('”', '"')
                .replace('‘', '\'').replace('’', '\'').trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            if (first > 0) t = t.substring(first + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return null;
    }

    private List<String> chunk(String s, int size) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        int n = s.length();
        for (int i = 0; i < n; i += size) {
            out.add(s.substring(i, Math.min(n, i + size)));
        }
        return out;
    }

    private String stripHtml(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "").replace("&quot;", "\"").replace("&amp;", "&");
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private int nullSafe(Integer i) {
        return i == null ? 0 : i;
    }
}
