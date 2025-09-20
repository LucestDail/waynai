package com.waynai.demo.controller;

import com.waynai.demo.service.TravelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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

    /**
     * 여행 계획 생성 (스트림)
     * @param query 사용자 입력 (지역, 키워드, 또는 둘 다)
     * @return 여행 계획 스트림
     */
    @GetMapping(value = "/plan", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateTravelPlan(@RequestParam String query) {
        log.info("여행 계획 생성 요청: {}", query);
        return travelService.generateTravelPlan(query)
                .map(data -> "data: " + data + "\n\n")
                .doOnComplete(() -> log.info("여행 계획 스트림 완료"));
    }

    /**
     * 여행 계획 생성 (POST 방식)
     * @param request 여행 요청
     * @return 여행 계획 스트림
     */
    @PostMapping(value = "/plan", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generateTravelPlanPost(@RequestBody TravelRequest request) {
        log.info("여행 계획 생성 요청 (POST): {}", request);
        return travelService.generateTravelPlan(request.getQuery());
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
