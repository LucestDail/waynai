package com.waynai.demo.controller;

import com.waynai.demo.dto.IntentAnalysisWithSearchDto;
import com.waynai.demo.dto.NaverBlogSearchDto;
import com.waynai.demo.service.IntentAnalysisService;
import com.waynai.demo.service.NaverSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 네이버 검색 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/naver")
@RequiredArgsConstructor
public class NaverSearchController {

    private final NaverSearchService naverSearchService;
    private final IntentAnalysisService intentAnalysisService;

    /**
     * 네이버 블로그 검색
     * @param query 검색어
     * @param display 검색 결과 개수 (기본값: 10)
     * @param start 검색 시작 위치 (기본값: 1)
     * @param sort 정렬 방법 (sim: 정확도순, date: 날짜순)
     * @return 네이버 블로그 검색 결과
     */
    @GetMapping(value = "/search/blog", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<NaverBlogSearchDto> searchBlog(
            @RequestParam String query,
            @RequestParam(required = false) Integer display,
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) String sort) {
        
        log.info("네이버 블로그 검색 요청: query={}, display={}, start={}, sort={}", 
                query, display, start, sort);
        
        return naverSearchService.searchBlog(query, display, start, sort);
    }

    /**
     * 의도 분석 및 네이버 검색 (None인 경우)
     * @param query 사용자 입력
     * @return 의도 분석 결과와 네이버 검색 결과
     */
    @GetMapping(value = "/analyze-with-search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IntentAnalysisWithSearchDto> analyzeWithSearch(@RequestParam String query) {
        log.info("의도 분석 및 네이버 검색 요청: {}", query);
        return intentAnalysisService.analyzeIntentWithSearch(query);
    }

    /**
     * 간단한 네이버 블로그 검색 (기본 파라미터)
     * @param query 검색어
     * @return 네이버 블로그 검색 결과
     */
    @GetMapping(value = "/search/blog/simple", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<NaverBlogSearchDto> searchBlogSimple(@RequestParam String query) {
        log.info("간단한 네이버 블로그 검색 요청: {}", query);
        return naverSearchService.searchBlog(query);
    }
}
