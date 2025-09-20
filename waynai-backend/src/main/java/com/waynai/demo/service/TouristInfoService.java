package com.waynai.demo.service;

import com.waynai.demo.client.TouristApiClient;
import com.waynai.demo.dto.AreaCodeDto;
import com.waynai.demo.dto.TouristApiResponseDto;
import com.waynai.demo.dto.TouristSpotResponseDto;
import com.waynai.demo.util.AreaCodeUtil;
import com.waynai.demo.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 관광지 정보 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TouristInfoService {
    
    private final TouristApiClient touristApiClient;
    private final AreaCodeUtil areaCodeUtil;
    
    /**
     * 특정 지역의 관광지 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 관광지 목록 응답
     */
    public TouristSpotResponseDto getTouristSpots(String areaCode, String sigunguCode, 
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
        
        log.info("관광지 조회 요청 - 지역: {} {}, 시군구: {} {}", 
                areaInfo.getAreaName(), areaCode, areaInfo.getSigunguName(), sigunguCode);
        
        try {
            TouristApiResponseDto apiResponse = touristApiClient.getAreaBasedList(areaCode, sigunguCode, pageNo, numOfRows);
            return TouristSpotResponseDto.success(apiResponse);
        } catch (Exception e) {
            log.error("관광지 조회 실패 - 지역: {}, 시군구: {}", areaCode, sigunguCode, e);
            return TouristSpotResponseDto.error("ERROR", "관광지 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 무작위 지역의 관광지 목록 조회
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 관광지 목록 응답
     */
    public TouristSpotResponseDto getRandomTouristSpots(Integer pageNo, Integer numOfRows) {
        AreaCodeDto randomArea = areaCodeUtil.getRandomAreaCode();
        
        if (randomArea == null) {
            throw new RuntimeException("지역 코드 정보가 없습니다");
        }
        
        log.info("무작위 관광지 조회 - 지역: {} {}, 시군구: {} {}", 
                randomArea.getAreaName(), randomArea.getAreaCode(), 
                randomArea.getSigunguName(), randomArea.getSigunguCode());
        
        return getTouristSpots(randomArea.getAreaCode(), randomArea.getSigunguCode(), pageNo, numOfRows);
    }
    
    /**
     * 특정 지역의 무작위 시군구 관광지 목록 조회
     * @param areaCode 지역 코드
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 관광지 목록 응답
     */
    public TouristSpotResponseDto getRandomSigunguTouristSpots(String areaCode, Integer pageNo, Integer numOfRows) {
        AreaCodeDto randomSigungu = areaCodeUtil.getRandomSigunguByAreaCode(areaCode);
        
        if (randomSigungu == null) {
            throw new IllegalArgumentException("해당 지역의 시군구 정보가 없습니다: " + areaCode);
        }
        
        log.info("지역 내 무작위 시군구 관광지 조회 - 지역: {} {}, 시군구: {} {}", 
                randomSigungu.getAreaName(), randomSigungu.getAreaCode(), 
                randomSigungu.getSigunguName(), randomSigungu.getSigunguCode());
        
        return getTouristSpots(areaCode, randomSigungu.getSigunguCode(), pageNo, numOfRows);
    }
    
    /**
     * 모든 지역 코드 목록 조회
     * @return 지역 코드 목록
     */
    public List<AreaCodeDto> getAllAreaCodes() {
        return areaCodeUtil.getAllAreaCodes();
    }
    
    /**
     * 지역명으로 시군구 목록 조회
     * @param areaName 지역명
     * @return 시군구 목록
     */
    public List<AreaCodeDto> getSigunguByAreaName(String areaName) {
        return areaCodeUtil.getSigunguByAreaName(areaName);
    }
}
