package com.waynai.demo.util;

import com.waynai.demo.dto.AreaCodeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 지역 코드 유틸리티 클래스
 * CSV 파일에서 지역 정보를 로드하고 관리
 */
@Slf4j
@Component
public class AreaCodeUtil {
    
    private List<AreaCodeDto> areaCodeList = new ArrayList<>();
    private final Random random = new Random();
    
    /**
     * 애플리케이션 시작 시 CSV 파일에서 지역 코드 정보를 로드
     */
    @PostConstruct
    public void loadAreaCodes() {
        try {
            ClassPathResource resource = new ClassPathResource("reference/한국관광공사_TourAPI_관광지_시군구_코드정보_v1.csv");
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    // 첫 번째 줄(헤더) 건너뛰기
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    
                    // CSV 파싱
                    String[] columns = line.split(",");
                    if (columns.length >= 4) {
                        AreaCodeDto areaCode = new AreaCodeDto(
                            columns[0].trim(),
                            columns[1].trim(),
                            columns[2].trim(),
                            columns[3].trim()
                        );
                        areaCodeList.add(areaCode);
                    }
                }
                
                log.info("지역 코드 정보 로드 완료: {} 개", areaCodeList.size());
                
            } catch (IOException e) {
                log.error("CSV 파일 읽기 실패", e);
                throw new RuntimeException("지역 코드 정보 로드 실패", e);
            }
            
        } catch (Exception e) {
            log.error("지역 코드 정보 초기화 실패", e);
            throw new RuntimeException("지역 코드 정보 초기화 실패", e);
        }
    }
    
    /**
     * 모든 지역 코드 정보 반환
     * @return 지역 코드 리스트
     */
    public List<AreaCodeDto> getAllAreaCodes() {
        return new ArrayList<>(areaCodeList);
    }
    
    /**
     * 지역 코드로 필터링된 시군구 목록 반환
     * @param areaCode 지역 코드
     * @return 해당 지역의 시군구 목록
     */
    public List<AreaCodeDto> getSigunguByAreaCode(String areaCode) {
        return areaCodeList.stream()
                .filter(area -> area.getAreaCode().equals(areaCode))
                .collect(Collectors.toList());
    }
    
    /**
     * 시군구 코드로 지역 정보 조회
     * @param sigunguCode 시군구 코드
     * @return 해당 시군구 정보
     */
    public AreaCodeDto getAreaBySigunguCode(String sigunguCode) {
        return areaCodeList.stream()
                .filter(area -> area.getSigunguCode().equals(sigunguCode))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 무작위 지역 코드 반환
     * @return 무작위 지역 코드
     */
    public AreaCodeDto getRandomAreaCode() {
        if (areaCodeList.isEmpty()) {
            return null;
        }
        return areaCodeList.get(random.nextInt(areaCodeList.size()));
    }
    
    /**
     * 특정 지역의 무작위 시군구 반환
     * @param areaCode 지역 코드
     * @return 해당 지역의 무작위 시군구
     */
    public AreaCodeDto getRandomSigunguByAreaCode(String areaCode) {
        List<AreaCodeDto> sigunguList = getSigunguByAreaCode(areaCode);
        if (sigunguList.isEmpty()) {
            return null;
        }
        return sigunguList.get(random.nextInt(sigunguList.size()));
    }
    
    /**
     * 지역명으로 지역 코드 검색
     * @param areaName 지역명
     * @return 해당 지역의 시군구 목록
     */
    public List<AreaCodeDto> getSigunguByAreaName(String areaName) {
        return areaCodeList.stream()
                .filter(area -> area.getAreaName().contains(areaName))
                .collect(Collectors.toList());
    }
    
    /**
     * 시군구명으로 지역 정보 검색
     * @param sigunguName 시군구명
     * @return 해당 시군구 정보
     */
    public AreaCodeDto getAreaBySigunguName(String sigunguName) {
        return areaCodeList.stream()
                .filter(area -> area.getSigunguName().contains(sigunguName))
                .findFirst()
                .orElse(null);
    }
}
