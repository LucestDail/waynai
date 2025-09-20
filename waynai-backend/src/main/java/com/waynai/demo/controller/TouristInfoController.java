package com.waynai.demo.controller;

import com.waynai.demo.dto.AreaCodeDto;
import com.waynai.demo.dto.TouristSpotResponseDto;
import com.waynai.demo.service.TouristInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관광지 정보 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/tourist")
@RequiredArgsConstructor
public class TouristInfoController {
    
    private final TouristInfoService touristInfoService;
    
    /**
     * 특정 지역의 관광지 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 행 수 (기본값: 10)
     * @return 관광지 목록
     */
    @GetMapping("/spots")
    public ResponseEntity<TouristSpotResponseDto> getTouristSpots(
            @RequestParam String areaCode,
            @RequestParam String sigunguCode,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        try {
            TouristSpotResponseDto response = touristInfoService.getTouristSpots(
                    areaCode, sigunguCode, pageNo, numOfRows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("관광지 조회 실패", e);
            return ResponseEntity.badRequest().body(
                TouristSpotResponseDto.error("ERROR", "관광지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 무작위 지역의 관광지 목록 조회
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 행 수 (기본값: 10)
     * @return 관광지 목록
     */
    @GetMapping("/spots/random")
    public ResponseEntity<TouristSpotResponseDto> getRandomTouristSpots(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        try {
            TouristSpotResponseDto response = touristInfoService.getRandomTouristSpots(pageNo, numOfRows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("무작위 관광지 조회 실패", e);
            return ResponseEntity.badRequest().body(
                TouristSpotResponseDto.error("ERROR", "무작위 관광지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 특정 지역의 무작위 시군구 관광지 목록 조회
     * @param areaCode 지역 코드
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 행 수 (기본값: 10)
     * @return 관광지 목록
     */
    @GetMapping("/spots/random/{areaCode}")
    public ResponseEntity<TouristSpotResponseDto> getRandomSigunguTouristSpots(
            @PathVariable String areaCode,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        try {
            TouristSpotResponseDto response = touristInfoService.getRandomSigunguTouristSpots(
                    areaCode, pageNo, numOfRows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("지역 내 무작위 시군구 관광지 조회 실패", e);
            return ResponseEntity.badRequest().body(
                TouristSpotResponseDto.error("ERROR", "지역 내 무작위 시군구 관광지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 모든 지역 코드 목록 조회
     * @return 지역 코드 목록
     */
    @GetMapping("/area-codes")
    public ResponseEntity<List<AreaCodeDto>> getAllAreaCodes() {
        try {
            List<AreaCodeDto> areaCodes = touristInfoService.getAllAreaCodes();
            return ResponseEntity.ok(areaCodes);
        } catch (Exception e) {
            log.error("지역 코드 목록 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 지역명으로 시군구 목록 조회
     * @param areaName 지역명
     * @return 시군구 목록
     */
    @GetMapping("/area-codes/search")
    public ResponseEntity<List<AreaCodeDto>> getSigunguByAreaName(@RequestParam String areaName) {
        try {
            List<AreaCodeDto> sigunguList = touristInfoService.getSigunguByAreaName(areaName);
            return ResponseEntity.ok(sigunguList);
        } catch (Exception e) {
            log.error("지역명으로 시군구 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
