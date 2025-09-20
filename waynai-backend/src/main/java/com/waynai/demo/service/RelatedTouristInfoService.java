package com.waynai.demo.service;

import com.waynai.demo.client.RelatedTouristApiClient;
import com.waynai.demo.dto.AreaCodeDto;
import com.waynai.demo.dto.RelatedTouristApiResponseDto;
import com.waynai.demo.dto.RelatedTouristSpotResponseDto;
import com.waynai.demo.util.AreaCodeUtil;
import com.waynai.demo.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 연관 관광지 정보 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelatedTouristInfoService {
    
    private final RelatedTouristApiClient relatedTouristApiClient;
    private final AreaCodeUtil areaCodeUtil;
    
    /**
     * 지역기반 중심 관광지 정보 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 연관 관광지 목록 응답
     */
    public RelatedTouristSpotResponseDto getAreaBasedRelatedSpots(String areaCode, String sigunguCode, 
                                                                  Integer pageNo, Integer numOfRows) {
        
        // 유효성 검증
        if (!ValidationUtil.isValidAreaCode(areaCode)) {
            throw new IllegalArgumentException("유효하지 않은 지역 코드입니다: " + areaCode);
        }
        
        if (!ValidationUtil.isValidSigunguCode(sigunguCode)) {
            throw new IllegalArgumentException("유효하지 않은 시군구 코드입니다: " + sigunguCode);
        }
        
        if (!ValidationUtil.isValidPageNo(pageNo)) {
            pageNo = 1; // 기본값 설정
        }
        
        if (!ValidationUtil.isValidNumOfRows(numOfRows)) {
            numOfRows = 10; // 기본값 설정
        }
        
        // 지역 정보 확인
        AreaCodeDto areaInfo = areaCodeUtil.getAreaBySigunguCode(sigunguCode);
        if (areaInfo == null) {
            throw new IllegalArgumentException("존재하지 않는 시군구 코드입니다: " + sigunguCode);
        }
        
        if (!areaInfo.getAreaCode().equals(areaCode)) {
            throw new IllegalArgumentException("지역 코드와 시군구 코드가 일치하지 않습니다");
        }
        
        log.info("지역기반 연관 관광지 조회 요청 - 지역: {} {}, 시군구: {} {}", 
                areaInfo.getAreaName(), areaCode, areaInfo.getSigunguName(), sigunguCode);
        
        try {
            RelatedTouristApiResponseDto apiResponse = relatedTouristApiClient.getAreaBasedList(areaCode, sigunguCode, pageNo, numOfRows);
            return RelatedTouristSpotResponseDto.success(apiResponse);
        } catch (Exception e) {
            log.error("지역기반 연관 관광지 조회 실패 - 지역: {}, 시군구: {}", areaCode, sigunguCode, e);
            return RelatedTouristSpotResponseDto.error("ERROR", "지역기반 연관 관광지 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 키워드 검색 관광지별 연관 관광지 정보 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param keyword 검색 키워드
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 연관 관광지 목록 응답
     */
    public RelatedTouristSpotResponseDto searchKeywordRelatedSpots(String areaCode, String sigunguCode, String keyword,
                                                                   Integer pageNo, Integer numOfRows) {
        
        // 유효성 검증
        if (!ValidationUtil.isValidAreaCode(areaCode)) {
            throw new IllegalArgumentException("유효하지 않은 지역 코드입니다: " + areaCode);
        }
        
        if (!ValidationUtil.isValidSigunguCode(sigunguCode)) {
            throw new IllegalArgumentException("유효하지 않은 시군구 코드입니다: " + sigunguCode);
        }
        
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드를 입력해주세요");
        }
        
        if (!ValidationUtil.isValidPageNo(pageNo)) {
            pageNo = 1; // 기본값 설정
        }
        
        if (!ValidationUtil.isValidNumOfRows(numOfRows)) {
            numOfRows = 10; // 기본값 설정
        }
        
        // 지역 정보 확인
        AreaCodeDto areaInfo = areaCodeUtil.getAreaBySigunguCode(sigunguCode);
        if (areaInfo == null) {
            throw new IllegalArgumentException("존재하지 않는 시군구 코드입니다: " + sigunguCode);
        }
        
        if (!areaInfo.getAreaCode().equals(areaCode)) {
            throw new IllegalArgumentException("지역 코드와 시군구 코드가 일치하지 않습니다");
        }
        
        log.info("키워드 검색 연관 관광지 조회 요청 - 지역: {} {}, 시군구: {} {}, 키워드: {}", 
                areaInfo.getAreaName(), areaCode, areaInfo.getSigunguName(), sigunguCode, keyword);
        
        try {
            RelatedTouristApiResponseDto apiResponse = relatedTouristApiClient.searchKeyword(areaCode, sigunguCode, keyword, pageNo, numOfRows);
            return RelatedTouristSpotResponseDto.success(apiResponse);
        } catch (Exception e) {
            log.error("키워드 검색 연관 관광지 조회 실패 - 지역: {}, 시군구: {}, 키워드: {}", areaCode, sigunguCode, keyword, e);
            return RelatedTouristSpotResponseDto.error("ERROR", "키워드 검색 연관 관광지 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 무작위 지역의 연관 관광지 목록 조회
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 연관 관광지 목록 응답
     */
    public RelatedTouristSpotResponseDto getRandomAreaBasedRelatedSpots(Integer pageNo, Integer numOfRows) {
        AreaCodeDto randomArea = areaCodeUtil.getRandomAreaCode();
        
        if (randomArea == null) {
            throw new RuntimeException("지역 코드 정보가 없습니다");
        }
        
        log.info("무작위 지역 연관 관광지 조회 - 지역: {} {}, 시군구: {} {}", 
                randomArea.getAreaName(), randomArea.getAreaCode(), 
                randomArea.getSigunguName(), randomArea.getSigunguCode());
        
        return getAreaBasedRelatedSpots(randomArea.getAreaCode(), randomArea.getSigunguCode(), pageNo, numOfRows);
    }
    
    /**
     * 특정 지역의 무작위 시군구 연관 관광지 목록 조회
     * @param areaCode 지역 코드
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 연관 관광지 목록 응답
     */
    public RelatedTouristSpotResponseDto getRandomSigunguAreaBasedRelatedSpots(String areaCode, Integer pageNo, Integer numOfRows) {
        AreaCodeDto randomSigungu = areaCodeUtil.getRandomSigunguByAreaCode(areaCode);
        
        if (randomSigungu == null) {
            throw new IllegalArgumentException("해당 지역의 시군구 정보가 없습니다: " + areaCode);
        }
        
        log.info("지역 내 무작위 시군구 연관 관광지 조회 - 지역: {} {}, 시군구: {} {}", 
                randomSigungu.getAreaName(), randomSigungu.getAreaCode(), 
                randomSigungu.getSigunguName(), randomSigungu.getSigunguCode());
        
        return getAreaBasedRelatedSpots(areaCode, randomSigungu.getSigunguCode(), pageNo, numOfRows);
    }
    
    /**
     * 무작위 지역의 연관 관광지 목록 조회 (간단한 이름)
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 연관 관광지 목록 응답
     */
    public RelatedTouristSpotResponseDto getRandomRelatedTouristSpots(Integer pageNo, Integer numOfRows) {
        return getRandomAreaBasedRelatedSpots(pageNo, numOfRows);
    }
}
