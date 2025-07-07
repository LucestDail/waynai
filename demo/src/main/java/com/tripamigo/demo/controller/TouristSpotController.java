package com.tripamigo.demo.controller;

import com.tripamigo.demo.dto.ApiResponseDto;
import com.tripamigo.demo.dto.TouristSpotDto;
import com.tripamigo.demo.service.TouristSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tourist-spots")
@RequiredArgsConstructor
public class TouristSpotController {
    
    private final TouristSpotService touristSpotService;
    
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<TouristSpotDto>>> getAllTouristSpots() {
        log.info("Retrieving all tourist spots");
        
        try {
            List<TouristSpotDto> spots = touristSpotService.getAllTouristSpots();
            return ResponseEntity.ok(ApiResponseDto.success(spots, "관광지 목록을 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to retrieve tourist spots: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("관광지 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{spotId}")
    public ResponseEntity<ApiResponseDto<TouristSpotDto>> getTouristSpotById(@PathVariable String spotId) {
        log.info("Retrieving tourist spot with ID: {}", spotId);
        
        try {
            TouristSpotDto spot = touristSpotService.getTouristSpotById(spotId);
            
            if (spot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDto.error("관광지를 찾을 수 없습니다.", "SPOT_NOT_FOUND"));
            }
            
            return ResponseEntity.ok(ApiResponseDto.success(spot, "관광지를 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to retrieve tourist spot: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("관광지 조회에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<List<TouristSpotDto>>> searchTouristSpots(@RequestParam String keyword) {
        log.info("Searching tourist spots with keyword: {}", keyword);
        
        try {
            List<TouristSpotDto> spots = touristSpotService.searchTouristSpots(keyword);
            return ResponseEntity.ok(ApiResponseDto.success(spots, "관광지 검색을 성공적으로 완료했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to search tourist spots: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("관광지 검색에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/destination/{destination}/theme/{theme}")
    public ResponseEntity<ApiResponseDto<List<TouristSpotDto>>> getTouristSpotsByDestinationAndTheme(
            @PathVariable String destination, @PathVariable String theme) {
        
        log.info("Retrieving tourist spots for destination: {} and theme: {}", destination, theme);
        
        try {
            List<TouristSpotDto> spots = touristSpotService.getTouristSpotsByDestination(destination, theme);
            return ResponseEntity.ok(ApiResponseDto.success(spots, "관광지 목록을 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to retrieve tourist spots: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("관광지 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponseDto.success("Tourist Spot Service is running", "서비스가 정상적으로 동작 중입니다."));
    }
} 