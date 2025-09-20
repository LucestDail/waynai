package com.waynai.demo.controller;

import com.waynai.demo.dto.RelatedTouristSpotResponseDto;
import com.waynai.demo.service.RelatedTouristInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 연관 관광지 정보 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/related-tourist")
@RequiredArgsConstructor
public class RelatedTouristInfoController {
    
    private final RelatedTouristInfoService relatedTouristInfoService;
    
    /**
     * 지역기반 중심 관광지 정보 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 행 수 (기본값: 10)
     * @return 연관 관광지 목록
     */
    @GetMapping("/area-based")
    public ResponseEntity<RelatedTouristSpotResponseDto> getAreaBasedRelatedSpots(
            @RequestParam String areaCode,
            @RequestParam String sigunguCode,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        try {
            RelatedTouristSpotResponseDto response = relatedTouristInfoService.getAreaBasedRelatedSpots(
                    areaCode, sigunguCode, pageNo, numOfRows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("지역기반 연관 관광지 조회 실패", e);
            return ResponseEntity.badRequest().body(
                RelatedTouristSpotResponseDto.error("ERROR", "지역기반 연관 관광지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 키워드 검색 관광지별 연관 관광지 정보 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param keyword 검색 키워드
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 행 수 (기본값: 10)
     * @return 연관 관광지 목록
     */
    @GetMapping("/keyword-search")
    public ResponseEntity<RelatedTouristSpotResponseDto> searchKeywordRelatedSpots(
            @RequestParam String areaCode,
            @RequestParam String sigunguCode,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        try {
            RelatedTouristSpotResponseDto response = relatedTouristInfoService.searchKeywordRelatedSpots(
                    areaCode, sigunguCode, keyword, pageNo, numOfRows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("키워드 검색 연관 관광지 조회 실패", e);
            return ResponseEntity.badRequest().body(
                RelatedTouristSpotResponseDto.error("ERROR", "키워드 검색 연관 관광지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 무작위 지역의 연관 관광지 목록 조회
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 행 수 (기본값: 10)
     * @return 연관 관광지 목록
     */
    @GetMapping("/area-based/random")
    public ResponseEntity<RelatedTouristSpotResponseDto> getRandomAreaBasedRelatedSpots(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        try {
            RelatedTouristSpotResponseDto response = relatedTouristInfoService.getRandomAreaBasedRelatedSpots(pageNo, numOfRows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("무작위 지역 연관 관광지 조회 실패", e);
            return ResponseEntity.badRequest().body(
                RelatedTouristSpotResponseDto.error("ERROR", "무작위 지역 연관 관광지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 특정 지역의 무작위 시군구 연관 관광지 목록 조회
     * @param areaCode 지역 코드
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 페이지당 행 수 (기본값: 10)
     * @return 연관 관광지 목록
     */
    @GetMapping("/area-based/random/{areaCode}")
    public ResponseEntity<RelatedTouristSpotResponseDto> getRandomSigunguAreaBasedRelatedSpots(
            @PathVariable String areaCode,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer numOfRows) {
        
        try {
            RelatedTouristSpotResponseDto response = relatedTouristInfoService.getRandomSigunguAreaBasedRelatedSpots(
                    areaCode, pageNo, numOfRows);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("지역 내 무작위 시군구 연관 관광지 조회 실패", e);
            return ResponseEntity.badRequest().body(
                RelatedTouristSpotResponseDto.error("ERROR", "지역 내 무작위 시군구 연관 관광지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
