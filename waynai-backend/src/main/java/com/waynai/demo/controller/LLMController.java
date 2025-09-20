package com.waynai.demo.controller;

import com.waynai.demo.service.LLMService;
import com.waynai.demo.service.RelatedTouristInfoService;
import com.waynai.demo.service.TouristInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * LLM 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LLMController {

    private final LLMService llmService;
    private final TouristInfoService touristInfoService;
    private final RelatedTouristInfoService relatedTouristInfoService;

    /**
     * 일반 질의응답
     * @param query 사용자 질의
     * @return 스트림 응답
     */
    @GetMapping(value = "/ask", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> askGeneral(@RequestParam String query) {
        log.info("일반 질의응답 요청: {}", query);
        return llmService.askGeneral(query);
    }

    /**
     * 여행 가이드 질의응답
     * @param query 사용자 질의
     * @param areaCode 지역 코드 (선택사항)
     * @param sigunguCode 시군구 코드 (선택사항)
     * @return 스트림 응답
     */
    @GetMapping(value = "/travel-guide", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> askTravelGuide(
            @RequestParam String query,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode) {
        
        log.info("여행 가이드 질의응답 요청: {} (지역: {}, 시군구: {})", query, areaCode, sigunguCode);
        
        if (areaCode != null && sigunguCode != null) {
            // 특정 지역의 관광지 정보 조회
            return Mono.fromCallable(() -> touristInfoService.getTouristSpots(areaCode, sigunguCode, 1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askTravelGuide(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        } else {
            // 무작위 관광지 정보 조회
            return Mono.fromCallable(() -> touristInfoService.getRandomTouristSpots(1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askTravelGuide(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        }
    }

    /**
     * 관광지 정보 질의응답
     * @param query 사용자 질의
     * @param areaCode 지역 코드 (선택사항)
     * @param sigunguCode 시군구 코드 (선택사항)
     * @return 스트림 응답
     */
    @GetMapping(value = "/tourist-info", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> askTouristInfo(
            @RequestParam String query,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode) {
        
        log.info("관광지 정보 질의응답 요청: {} (지역: {}, 시군구: {})", query, areaCode, sigunguCode);
        
        if (areaCode != null && sigunguCode != null) {
            // 특정 지역의 관광지 정보 조회
            return Mono.fromCallable(() -> touristInfoService.getTouristSpots(areaCode, sigunguCode, 1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askTouristInfo(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        } else {
            // 무작위 관광지 정보 조회
            return Mono.fromCallable(() -> touristInfoService.getRandomTouristSpots(1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askTouristInfo(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        }
    }

    /**
     * 연관 관광지 질의응답
     * @param query 사용자 질의
     * @param areaCode 지역 코드 (선택사항)
     * @param sigunguCode 시군구 코드 (선택사항)
     * @return 스트림 응답
     */
    @GetMapping(value = "/related-spots", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> askRelatedSpots(
            @RequestParam String query,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode) {
        
        log.info("연관 관광지 질의응답 요청: {} (지역: {}, 시군구: {})", query, areaCode, sigunguCode);
        
        if (areaCode != null && sigunguCode != null) {
            // 특정 지역의 연관 관광지 정보 조회
            return Mono.fromCallable(() -> relatedTouristInfoService.getAreaBasedRelatedSpots(areaCode, sigunguCode, 1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askRelatedSpots(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 연관 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        } else {
            // 무작위 연관 관광지 정보 조회
            return Mono.fromCallable(() -> relatedTouristInfoService.getRandomRelatedTouristSpots(1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askRelatedSpots(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 연관 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        }
    }

    /**
     * 키워드 검색 연관 관광지 질의응답
     * @param query 사용자 질의
     * @param keyword 검색 키워드
     * @param areaCode 지역 코드 (선택사항)
     * @param sigunguCode 시군구 코드 (선택사항)
     * @return 스트림 응답
     */
    @GetMapping(value = "/related-spots/keyword", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> askRelatedSpotsByKeyword(
            @RequestParam String query,
            @RequestParam String keyword,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode) {
        
        log.info("키워드 검색 연관 관광지 질의응답 요청: {} (키워드: {}, 지역: {}, 시군구: {})", 
                query, keyword, areaCode, sigunguCode);
        
        if (areaCode != null && sigunguCode != null) {
            // 특정 지역의 키워드 검색 연관 관광지 정보 조회
            return Mono.fromCallable(() -> relatedTouristInfoService.searchKeywordRelatedSpots(areaCode, sigunguCode, keyword, 1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askRelatedSpots(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 키워드 검색 연관 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        } else {
            // 기본 지역으로 키워드 검색
            return Mono.fromCallable(() -> relatedTouristInfoService.searchKeywordRelatedSpots("11", "11530", keyword, 1, 10))
                    .flatMapMany(response -> {
                        if (response.isSuccess()) {
                            return llmService.askRelatedSpots(query, response.getItems());
                        } else {
                            return Flux.just("죄송합니다. 키워드 검색 연관 관광지 정보를 가져올 수 없습니다: " + response.getResultMsg());
                        }
                    });
        }
    }

    /**
     * 사용 가능한 프롬프트 목록 조회
     * @return 프롬프트 목록
     */
    @GetMapping("/prompts")
    public String[] getAvailablePrompts() {
        return llmService.getAvailablePrompts();
    }
}
