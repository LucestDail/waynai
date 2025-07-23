package com.waynai.demo.controller;

import com.waynai.demo.dto.SearchRequestDto;
import com.waynai.demo.dto.SearchResponseDto;
import com.waynai.demo.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    /**
     * 단순 검색 처리
     */
    @PostMapping("/search")
    public ResponseEntity<SearchResponseDto> search(@RequestBody SearchRequestDto request) {
        log.info("Received search request: {}", request);
        log.info("Query: '{}', Destination: '{}', Theme: '{}', Days: {}", 
                request.getQuery(), request.getDestination(), request.getTheme(), request.getDays());
        
        try {
            SearchResponseDto response = searchService.processSearchSimple(request);
            log.info("Search completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            SearchResponseDto errorResponse = SearchResponseDto.builder()
                    .status("error")
                    .message("검색 중 오류가 발생했습니다")
                    .step("error")
                    .data(e.getMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .progress(java.util.Arrays.asList("오류 발생: " + e.getMessage()))
                    .build();
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * 검색 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Search service is running");
    }
} 