package com.waynai.demo.controller;

import com.waynai.demo.dto.TravelEvent;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.service.TravelOrchestratorService;
import com.waynai.demo.service.TravelPlanService;
import com.waynai.demo.service.TravelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 여행 컨트롤러
 * 의도 분석 + 여행 계획 생성을 통합하여 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;
    private final TravelPlanService travelPlanService;
    private final TravelOrchestratorService travelOrchestratorService;

    /**
     * 구조화된 여행 계획 생성 (JSON). 프론트 타임라인 UI 전용.
     */
    @GetMapping(value = "/plan/structured", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TravelPlanDto> generateStructuredPlan(@RequestParam String query) {
        log.info("구조화 여행 계획 생성 요청: {}", query);
        return travelPlanService.generateStructuredPlan(query);
    }

    @PostMapping(value = "/plan/structured", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TravelPlanDto> generateStructuredPlanPost(@RequestBody TravelRequest request) {
        log.info("구조화 여행 계획 생성 요청 (POST): {}", request);
        return travelPlanService.generateStructuredPlan(request.getQuery());
    }

    /**
     * 타입드 SSE 스트림. 단계/소스/모델/토큰/플랜을 실시간으로 푸시한다.
     * 프론트 SearchProgress 가 이 스트림을 구독해 진행 UI 를 렌더링한다.
     *
     * 이벤트 포맷:
     *   event: &lt;type&gt;       (stage | intent | sources.tour | sources.naver | model | token | plan | done | error)
     *   data:  &lt;JSON&gt;         (TravelEvent payload 포함)
     */
    @GetMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TravelEvent>> generateTravelPlanStream(
            @RequestParam String query,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String departDate,
            @RequestParam(required = false) String returnDate) {
        log.info("[sse] 여행 계획 스트림 요청: {} (origin={}, depart={}, return={})",
                query, origin, departDate, returnDate);
        AtomicLong seq = new AtomicLong(0);
        return travelOrchestratorService.generatePlanStream(query, origin, departDate, returnDate)
                .map(evt -> ServerSentEvent.<TravelEvent>builder()
                        .id(String.valueOf(seq.incrementAndGet()))
                        .event(evt.getType() != null ? evt.getType() : "message")
                        .data(evt)
                        .build())
                .doOnComplete(() -> log.info("[sse] 여행 계획 스트림 완료: {}", query))
                .doOnError(err -> log.error("[sse] 여행 계획 스트림 오류: {}", err.getMessage()));
    }

    /**
     * 타입드 SSE 스트림 (POST). 긴 자연어 질의를 바디로 받아 GET URL 길이 제한(414/헤더 초과)을 회피한다.
     */
    @PostMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TravelEvent>> generateTravelPlanStreamPost(@RequestBody StreamRequest req) {
        String query = req.getQuery() != null ? req.getQuery() : "";
        log.info("[sse] 여행 계획 스트림 요청(POST): len={} (origin={}, depart={}, return={})",
                query.length(), req.getOrigin(), req.getDepartDate(), req.getReturnDate());
        AtomicLong seq = new AtomicLong(0);
        return travelOrchestratorService.generatePlanStream(query, req.getOrigin(), req.getDepartDate(), req.getReturnDate())
                .map(evt -> ServerSentEvent.<TravelEvent>builder()
                        .id(String.valueOf(seq.incrementAndGet()))
                        .event(evt.getType() != null ? evt.getType() : "message")
                        .data(evt)
                        .build())
                .doOnComplete(() -> log.info("[sse] 여행 계획 스트림 완료(POST)"))
                .doOnError(err -> log.error("[sse] 여행 계획 스트림 오류(POST): {}", err.getMessage()));
    }

    /** 스트림 POST 요청 바디. */
    public static class StreamRequest {
        private String query;
        private String origin;
        private String departDate;
        private String returnDate;
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getDepartDate() { return departDate; }
        public void setDepartDate(String departDate) { this.departDate = departDate; }
        public String getReturnDate() { return returnDate; }
        public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
    }

    /**
     * 여행 계획 생성 (스트림) - 네이버 검색 포함
     * @param query 사용자 입력 (지역, 키워드, 또는 둘 다)
     * @return 여행 계획 스트림 (네이버 검색 결과 포함)
     */
    @GetMapping(value = "/plan", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateTravelPlan(@RequestParam String query) {
        log.info("여행 계획 생성 요청 (네이버 검색 포함): {}", query);
        return travelService.generateTravelPlanWithSearch(query)
                .map(data -> "data: " + data + "\n\n")
                .doOnComplete(() -> log.info("여행 계획 스트림 완료 (네이버 검색 포함)"));
    }

    /**
     * 여행 계획 생성 (POST 방식) - 네이버 검색 포함
     * @param request 여행 요청
     * @return 여행 계획 스트림 (네이버 검색 결과 포함)
     */
    @PostMapping(value = "/plan", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generateTravelPlanPost(@RequestBody TravelRequest request) {
        log.info("여행 계획 생성 요청 (POST, 네이버 검색 포함): {}", request);
        return travelService.generateTravelPlanWithSearch(request.getQuery());
    }

    /**
     * 여행 계획 생성 (네이버 검색 포함, 스트림)
     * @param query 사용자 입력 (지역, 키워드, 또는 둘 다)
     * @return 여행 계획 스트림 (네이버 검색 결과 포함)
     */
    @GetMapping(value = "/plan-with-search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateTravelPlanWithSearch(@RequestParam String query) {
        log.info("여행 계획 생성 요청 (네이버 검색 포함): {}", query);
        return travelService.generateTravelPlanWithSearch(query)
                .map(data -> "data: " + data + "\n\n")
                .doOnComplete(() -> log.info("여행 계획 스트림 완료 (네이버 검색 포함)"));
    }

    /**
     * 여행 계획 생성 (네이버 검색 포함, POST 방식)
     * @param request 여행 요청
     * @return 여행 계획 스트림 (네이버 검색 결과 포함)
     */
    @PostMapping(value = "/plan-with-search", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generateTravelPlanWithSearchPost(@RequestBody TravelRequest request) {
        log.info("여행 계획 생성 요청 (네이버 검색 포함, POST): {}", request);
        return travelService.generateTravelPlanWithSearch(request.getQuery());
    }

    /**
     * 여행 요청 DTO
     */
    public static class TravelRequest {
        private String query;
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
    }
}
