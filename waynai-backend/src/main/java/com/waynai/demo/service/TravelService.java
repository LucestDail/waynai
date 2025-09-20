package com.waynai.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 통합 여행 서비스
 * 의도 분석 + 여행 계획 생성을 통합하여 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelService {

    private final IntentAnalysisService intentAnalysisService;
    private final TravelPlanService travelPlanService;

    /**
     * 통합 여행 계획 생성
     * @param query 사용자 입력
     * @return 여행 계획 스트림
     */
    public Flux<String> generateTravelPlan(String query) {
        log.info("통합 여행 계획 생성 시작: {}", query);
        
        return intentAnalysisService.analyzeIntent(query)
                .flatMapMany(intentAnalysis -> {
                    log.info("의도 분석 완료, 여행 계획 생성 시작: {}", intentAnalysis);
                    return travelPlanService.generateTravelPlan(intentAnalysis);
                })
                .doOnComplete(() -> log.info("통합 여행 계획 생성 완료"))
                .doOnError(error -> log.error("통합 여행 계획 생성 오류", error));
    }

    /**
     * 통합 여행 계획 생성 (네이버 검색 포함)
     * @param query 사용자 입력
     * @return 여행 계획 스트림
     */
    public Flux<String> generateTravelPlanWithSearch(String query) {
        log.info("통합 여행 계획 생성 시작 (네이버 검색 포함): {}", query);
        
        return intentAnalysisService.analyzeIntentWithSearch(query)
                .flatMapMany(result -> {
                    log.info("의도 분석 및 네이버 검색 완료: {}", result);
                    
                    // 네이버 검색 결과가 있는 경우 해당 결과를 포함한 여행 계획 생성
                    if (result.isHasNaverSearch() && result.getNaverSearchResult() != null) {
                        return travelPlanService.generateTravelPlanWithSearch(result);
                    } else {
                        return travelPlanService.generateTravelPlan(result.getIntentAnalysis());
                    }
                })
                .doOnComplete(() -> log.info("통합 여행 계획 생성 완료 (네이버 검색 포함)"))
                .doOnError(error -> log.error("통합 여행 계획 생성 오류 (네이버 검색 포함)", error));
    }
}
