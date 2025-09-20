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
}
