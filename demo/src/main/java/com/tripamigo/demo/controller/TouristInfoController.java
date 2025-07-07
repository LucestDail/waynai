package com.tripamigo.demo.controller;

import com.tripamigo.demo.dto.ApiResponseDto;
import com.tripamigo.demo.dto.tourist.TouristApiResponseDto;
import com.tripamigo.demo.service.TouristInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tourist-info")
@RequiredArgsConstructor
public class TouristInfoController {
    
    private final TouristInfoService touristInfoService;
    
    /**
     * 지역기반 연관 관광지 정보 조회
     */
    @GetMapping("/area-based")
    public ResponseEntity<ApiResponseDto<List<TouristApiResponseDto.TouristItem>>> getAreaBasedRelatedSpots(
            @RequestParam String areaCd,
            @RequestParam String signguCd,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        log.info("Requesting area-based related tourist spots - areaCd: {}, signguCd: {}, pageNo: {}, numOfRows: {}", 
                areaCd, signguCd, pageNo, numOfRows);
        
        try {
            List<TouristApiResponseDto.TouristItem> spots = touristInfoService
                    .getAreaBasedRelatedSpots(areaCd, signguCd, pageNo, numOfRows)
                    .block();
            
            return ResponseEntity.ok(ApiResponseDto.<List<TouristApiResponseDto.TouristItem>>builder()
                    .success(true)
                    .message("지역기반 연관 관광지 정보 조회 성공")
                    .data(spots)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get area-based related tourist spots: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponseDto.<List<TouristApiResponseDto.TouristItem>>builder()
                    .success(false)
                    .message("지역기반 연관 관광지 정보 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 키워드 기반 연관 관광지 정보 조회
     */
    @GetMapping("/keyword-based")
    public ResponseEntity<ApiResponseDto<List<TouristApiResponseDto.TouristItem>>> getKeywordBasedRelatedSpots(
            @RequestParam String keyword,
            @RequestParam String areaCd,
            @RequestParam String signguCd,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        log.info("Requesting keyword-based related tourist spots - keyword: {}, areaCd: {}, signguCd: {}, pageNo: {}, numOfRows: {}", 
                keyword, areaCd, signguCd, pageNo, numOfRows);
        
        try {
            List<TouristApiResponseDto.TouristItem> spots = touristInfoService
                    .getKeywordBasedRelatedSpots(keyword, areaCd, signguCd, pageNo, numOfRows)
                    .block();
            
            return ResponseEntity.ok(ApiResponseDto.<List<TouristApiResponseDto.TouristItem>>builder()
                    .success(true)
                    .message("키워드 기반 연관 관광지 정보 조회 성공")
                    .data(spots)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get keyword-based related tourist spots: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponseDto.<List<TouristApiResponseDto.TouristItem>>builder()
                    .success(false)
                    .message("키워드 기반 연관 관광지 정보 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * RAG를 위한 관광지 정보 컨텍스트 조회
     */
    @GetMapping("/rag-context")
    public ResponseEntity<ApiResponseDto<String>> getRagContext(
            @RequestParam String keyword,
            @RequestParam String areaCd,
            @RequestParam String signguCd) {
        
        log.info("Requesting RAG context - keyword: {}, areaCd: {}, signguCd: {}", keyword, areaCd, signguCd);
        
        try {
            String context = touristInfoService
                    .buildTouristContextForRAG(keyword, areaCd, signguCd)
                    .block();
            
            return ResponseEntity.ok(ApiResponseDto.<String>builder()
                    .success(true)
                    .message("RAG 컨텍스트 조회 성공")
                    .data(context)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get RAG context: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponseDto.<String>builder()
                    .success(false)
                    .message("RAG 컨텍스트 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 지역 기반 관광지 정보 컨텍스트 조회
     */
    @GetMapping("/area-rag-context")
    public ResponseEntity<ApiResponseDto<String>> getAreaBasedRagContext(
            @RequestParam String areaCd,
            @RequestParam String signguCd) {
        
        log.info("Requesting area-based RAG context - areaCd: {}, signguCd: {}", areaCd, signguCd);
        
        try {
            String context = touristInfoService
                    .buildAreaBasedContextForRAG(areaCd, signguCd)
                    .block();
            
            return ResponseEntity.ok(ApiResponseDto.<String>builder()
                    .success(true)
                    .message("지역 기반 RAG 컨텍스트 조회 성공")
                    .data(context)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get area-based RAG context: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponseDto.<String>builder()
                    .success(false)
                    .message("지역 기반 RAG 컨텍스트 조회 실패: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 카테고리별 관광지 정보 조회
     */
    @GetMapping("/category-based")
    public ResponseEntity<ApiResponseDto<String>> getCategoryBasedTouristInfo(
            @RequestParam String areaCd,
            @RequestParam String signguCd,
            @RequestParam String category) {
        
        log.info("Requesting category-based tourist info - areaCd: {}, signguCd: {}, category: {}", 
                areaCd, signguCd, category);
        
        try {
            String info = touristInfoService
                    .getCategoryBasedTouristInfo(areaCd, signguCd, category)
                    .block();
            
            return ResponseEntity.ok(ApiResponseDto.<String>builder()
                    .success(true)
                    .message("카테고리별 관광지 정보 조회 성공")
                    .data(info)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get category-based tourist info: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponseDto.<String>builder()
                    .success(false)
                    .message("카테고리별 관광지 정보 조회 실패: " + e.getMessage())
                    .build());
        }
    }
} 