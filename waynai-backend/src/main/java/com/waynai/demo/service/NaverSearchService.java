package com.waynai.demo.service;

import com.waynai.demo.client.NaverApiClient;
import com.waynai.demo.dto.NaverBlogSearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 네이버 검색 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverSearchService {

    private final NaverApiClient naverApiClient;

    /**
     * 네이버 블로그 검색
     * @param query 검색어
     * @return 네이버 블로그 검색 결과
     */
    public Mono<NaverBlogSearchDto> searchBlog(String query) {
        return Mono.fromCallable(() -> {
            log.info("네이버 블로그 검색 시작: {}", query);
            return naverApiClient.searchBlog(query);
        })
        .doOnSuccess(result -> log.info("네이버 블로그 검색 완료: {}개 결과", result.getTotal()))
        .doOnError(error -> log.error("네이버 블로그 검색 실패", error));
    }

    /**
     * 네이버 블로그 검색 (상세 파라미터)
     * @param query 검색어
     * @param display 검색 결과 개수
     * @param start 검색 시작 위치
     * @param sort 정렬 방법
     * @return 네이버 블로그 검색 결과
     */
    public Mono<NaverBlogSearchDto> searchBlog(String query, Integer display, Integer start, String sort) {
        return Mono.fromCallable(() -> {
            log.info("네이버 블로그 검색 시작: query={}, display={}, start={}, sort={}", 
                    query, display, start, sort);
            return naverApiClient.searchBlog(query, display, start, sort);
        })
        .doOnSuccess(result -> log.info("네이버 블로그 검색 완료: {}개 결과", result.getTotal()))
        .doOnError(error -> log.error("네이버 블로그 검색 실패", error));
    }
}
