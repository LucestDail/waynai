package com.waynai.demo.service;

import com.waynai.demo.client.TouristApiClient;
import com.waynai.demo.dto.tourist.TouristApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TouristInfoService {
    
    private final TouristApiClient touristApiClient;
    
    /**
     * 지역기반 연관 관광지 정보 조회
     */
    public Mono<List<TouristApiResponseDto.TouristItem>> getAreaBasedRelatedSpots(
            String areaCd, 
            String signguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        return touristApiClient.getAreaBasedRelatedTouristSpots(areaCd, signguCd, pageNo, numOfRows)
                .map(response -> {
                    if (response.getResponse() != null && 
                        response.getResponse().getBody() != null && 
                        response.getResponse().getBody().getItems() != null) {
                        return response.getResponse().getBody().getItems().getItem();
                    }
                    return List.<TouristApiResponseDto.TouristItem>of();
                })
                .doOnSuccess(items -> log.info("Retrieved {} area-based related tourist spots", items.size()))
                .doOnError(error -> log.error("Failed to retrieve area-based related tourist spots: {}", error.getMessage()));
    }
    
    /**
     * 키워드 기반 연관 관광지 정보 조회
     */
    public Mono<List<TouristApiResponseDto.TouristItem>> getKeywordBasedRelatedSpots(
            String keyword, 
            String areaCd, 
            String signguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        return touristApiClient.getKeywordBasedRelatedTouristSpots(keyword, areaCd, signguCd, pageNo, numOfRows)
                .map(response -> {
                    if (response.getResponse() != null && 
                        response.getResponse().getBody() != null && 
                        response.getResponse().getBody().getItems() != null) {
                        return response.getResponse().getBody().getItems().getItem();
                    }
                    return List.<TouristApiResponseDto.TouristItem>of();
                })
                .doOnSuccess(items -> log.info("Retrieved {} keyword-based related tourist spots for keyword: {}", items.size(), keyword))
                .doOnError(error -> log.error("Failed to retrieve keyword-based related tourist spots: {}", error.getMessage()));
    }
    
    /**
     * RAG를 위한 관광지 정보 컨텍스트 생성
     */
    public Mono<String> buildTouristContextForRAG(String keyword, String areaCd, String signguCd) {
        return getKeywordBasedRelatedSpots(keyword, areaCd, signguCd, 1, 20)
                .map(this::formatTouristInfoForRAG)
                .map(context -> {
                    if (context.isEmpty()) {
                        return "해당 지역의 관광지 정보를 찾을 수 없습니다.";
                    }
                    return context;
                });
    }
    
    /**
     * 지역 기반 관광지 정보 컨텍스트 생성
     */
    public Mono<String> buildAreaBasedContextForRAG(String areaCd, String signguCd) {
        return getAreaBasedRelatedSpots(areaCd, signguCd, 1, 20)
                .map(this::formatTouristInfoForRAG)
                .map(context -> {
                    if (context.isEmpty()) {
                        return "해당 지역의 관광지 정보를 찾을 수 없습니다.";
                    }
                    return context;
                });
    }
    
    /**
     * 관광지 정보를 RAG 프롬프트용으로 포맷팅
     */
    private String formatTouristInfoForRAG(List<TouristApiResponseDto.TouristItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("=== 관광지별 연관 관광지 정보 ===\n\n");
        
        // 관광지별로 그룹화
        items.stream()
                .collect(Collectors.groupingBy(TouristApiResponseDto.TouristItem::getTAtsNm))
                .forEach((mainSpot, relatedSpots) -> {
                    context.append("【").append(mainSpot).append("】\n");
                    context.append("지역: ").append(relatedSpots.get(0).getAreaNm()).append(" ").append(relatedSpots.get(0).getSignguNm()).append("\n");
                    context.append("연관 관광지:\n");
                    
                    relatedSpots.stream()
                            .limit(10) // 상위 10개만 표시
                            .forEach(spot -> {
                                context.append("  • ").append(spot.getRlteTatsNm())
                                        .append(" (순위: ").append(spot.getRlteRank()).append(")")
                                        .append(" - ").append(spot.getRlteCtgryLclsNm())
                                        .append(" > ").append(spot.getRlteCtgryMclsNm())
                                        .append(" > ").append(spot.getRlteCtgrySclsNm())
                                        .append(" (").append(spot.getRlteRegnNm()).append(" ").append(spot.getRlteSignguNm()).append(")\n");
                            });
                    context.append("\n");
                });
        
        return context.toString();
    }
    
    /**
     * 관광지 카테고리별 정보 추출
     */
    public Mono<String> getCategoryBasedTouristInfo(String areaCd, String signguCd, String category) {
        return getAreaBasedRelatedSpots(areaCd, signguCd, 1, 50)
                .map(items -> {
                    List<TouristApiResponseDto.TouristItem> filteredItems = items.stream()
                            .filter(item -> category.equals(item.getRlteCtgryLclsNm()) || 
                                          category.equals(item.getRlteCtgryMclsNm()) ||
                                          category.equals(item.getRlteCtgrySclsNm()))
                            .collect(Collectors.toList());
                    
                    return formatTouristInfoForRAG(filteredItems);
                });
    }
} 