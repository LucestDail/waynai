package com.waynai.demo.controller;

import com.waynai.demo.dto.SearchRequestDto;
import com.waynai.demo.dto.SearchResponseDto;
import com.waynai.demo.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    /**
     * SSE를 통한 실시간 검색 처리
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<SearchResponseDto>> searchStream(@RequestBody SearchRequestDto request) {
        log.info("Starting search stream for query: {}", request.getQuery());
        
        Flux<SearchResponseDto> searchFlux = searchService.processSearch(request)
                .doOnNext(response -> log.debug("Search response: {}", response.getStatus()))
                .doOnComplete(() -> log.info("Search stream completed"))
                .doOnError(error -> log.error("Search stream error: {}", error.getMessage()));
        
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(searchFlux);
    }
    
    /**
     * 검색 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Search service is running");
    }
} 