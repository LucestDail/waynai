package com.waynai.demo.service;

import com.waynai.demo.client.TouristApiClient;
import com.waynai.demo.dto.tourist.TouristApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TouristInfoService {
    
    private final TouristApiClient touristApiClient;
    
    /**
     * 키워드 기반 관광지 정보 수집 (RAG용)
     */
    public Mono<String> buildTouristContextForRAG(String keyword, String areaCd, String signguCd) {
        log.info("=== TOURIST INFO SERVICE: KEYWORD SEARCH ===");
        log.info("Keyword: '{}'", keyword);
        log.info("Area Code: '{}'", areaCd);
        log.info("Signgu Code: '{}'", signguCd);
        
        try {
            log.info("Calling Tourist API for keyword search");
            
            return touristApiClient.getKeywordBasedTouristSpots(keyword, areaCd, signguCd, 1, 20)
                    .map(response -> {
                        log.info("Tourist API response received. Items count: {}", 
                                response != null && response.getResponse() != null && 
                                response.getResponse().getBody() != null && 
                                response.getResponse().getBody().getItems() != null ? 
                                response.getResponse().getBody().getItems().getItem().size() : 0);
                        
                        String context = buildContextFromResponse(response, keyword);
                        log.info("Context built successfully. Length: {}", context.length());
                        
                        return context;
                    })
                    .onErrorResume(error -> {
                        log.error("Failed to build tourist context for keyword '{}': {}", keyword, error.getMessage(), error);
                        return Mono.just("해당 키워드로 관광지 정보를 찾을 수 없습니다.");
                    });
                    
        } catch (Exception e) {
            log.error("Exception in buildTouristContextForRAG: {}", e.getMessage(), e);
            return Mono.just("관광지 정보 검색 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 지역 기반 관광지 정보 수집 (RAG용)
     */
    public Mono<String> buildAreaBasedContextForRAG(String areaCd, String signguCd) {
        log.info("=== TOURIST INFO SERVICE: AREA SEARCH ===");
        log.info("Area Code: '{}'", areaCd);
        log.info("Signgu Code: '{}'", signguCd);
        
        try {
            log.info("Calling Tourist API for area-based search");
            
            return touristApiClient.getAreaBasedTouristSpots(areaCd, signguCd, 1, 20)
                    .map(response -> {
                        log.info("Tourist API area response received. Items count: {}", 
                                response != null && response.getResponse() != null && 
                                response.getResponse().getBody() != null && 
                                response.getResponse().getBody().getItems() != null ? 
                                response.getResponse().getBody().getItems().getItem().size() : 0);
                        
                        String context = buildContextFromResponse(response, "지역 관광지");
                        log.info("Area context built successfully. Length: {}", context.length());
                        
                        return context;
                    })
                    .onErrorResume(error -> {
                        log.error("Failed to build area-based tourist context: {}", error.getMessage(), error);
                        return Mono.just("해당 지역의 관광지 정보를 찾을 수 없습니다.");
                    });
                    
        } catch (Exception e) {
            log.error("Exception in buildAreaBasedContextForRAG: {}", e.getMessage(), e);
            return Mono.just("지역 관광지 정보 검색 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * API 응답을 컨텍스트 문자열로 변환
     */
    private String buildContextFromResponse(TouristApiResponseDto response, String searchType) {
        if (response == null || response.getResponse() == null || 
            response.getResponse().getBody() == null || 
            response.getResponse().getBody().getItems() == null ||
            response.getResponse().getBody().getItems().getItem() == null ||
            response.getResponse().getBody().getItems().getItem().isEmpty()) {
            log.warn("Empty response for search type: {}", searchType);
            return "관광지 정보가 없습니다.";
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        List<TouristApiResponseDto.TouristItem> items = response.getResponse().getBody().getItems().getItem();
        
        log.info("Building context from {} items for search type: {}", items.size(), searchType);
        
        for (int i = 0; i < items.size(); i++) {
            TouristApiResponseDto.TouristItem item = items.get(i);
            
            try {
                contextBuilder.append("=== 관광지 ").append(i + 1).append(" ===\n");
                contextBuilder.append("이름: ").append(item.getRlteTatsNm()).append("\n");
                contextBuilder.append("지역: ").append(item.getRlteRegnNm()).append(" ").append(item.getRlteSignguNm()).append("\n");
                
                if (item.getRlteCtgryLclsNm() != null && !item.getRlteCtgryLclsNm().isEmpty()) {
                    contextBuilder.append("카테고리: ").append(item.getRlteCtgryLclsNm());
                    if (item.getRlteCtgryMclsNm() != null && !item.getRlteCtgryMclsNm().isEmpty()) {
                        contextBuilder.append(" > ").append(item.getRlteCtgryMclsNm());
                    }
                    if (item.getRlteCtgrySclsNm() != null && !item.getRlteCtgrySclsNm().isEmpty()) {
                        contextBuilder.append(" > ").append(item.getRlteCtgrySclsNm());
                    }
                    contextBuilder.append("\n");
                }
                
                if (item.getRlteRank() != null && !item.getRlteRank().isEmpty()) {
                    contextBuilder.append("순위: ").append(item.getRlteRank()).append("\n");
                }
                
                if (item.getRlteTatsCd() != null && !item.getRlteTatsCd().isEmpty()) {
                    contextBuilder.append("코드: ").append(item.getRlteTatsCd()).append("\n");
                }
                
                contextBuilder.append("\n");
                
            } catch (Exception e) {
                log.error("Error processing item {}: {}", i, e.getMessage());
                contextBuilder.append("=== 관광지 ").append(i + 1).append(" ===\n");
                contextBuilder.append("정보 처리 중 오류가 발생했습니다.\n\n");
            }
        }
        
        String result = contextBuilder.toString();
        log.info("Context built successfully for {} items. Final length: {}", items.size(), result.length());
        
        return result;
    }
    
    /**
     * 지역기반 관광지 정보 조회 (Controller용)
     */
    public Mono<List<TouristApiResponseDto.TouristItem>> getAreaBasedSpots(
            String areaCd, 
            String sigunguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        log.info("=== TOURIST INFO SERVICE: AREA SPOTS FOR CONTROLLER ===");
        log.info("Area Code: '{}', Signgu Code: '{}', Page: {}, Rows: {}", areaCd, sigunguCd, pageNo, numOfRows);
        
        return touristApiClient.getAreaBasedTouristSpots(areaCd, sigunguCd, pageNo, numOfRows)
                .map(response -> {
                    if (response.getResponse() != null && 
                        response.getResponse().getBody() != null && 
                        response.getResponse().getBody().getItems() != null) {
                        return response.getResponse().getBody().getItems().getItem();
                    }
                    return List.<TouristApiResponseDto.TouristItem>of();
                })
                .doOnSuccess(items -> log.info("Retrieved {} area-based tourist spots", items.size()))
                .doOnError(error -> log.error("Failed to retrieve area-based tourist spots: {}", error.getMessage()));
    }
    
    /**
     * 키워드 기반 관광지 정보 조회 (Controller용)
     */
    public Mono<List<TouristApiResponseDto.TouristItem>> getKeywordBasedSpots(
            String keyword, 
            String areaCd, 
            String sigunguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        log.info("=== TOURIST INFO SERVICE: KEYWORD SPOTS FOR CONTROLLER ===");
        log.info("Keyword: '{}', Area Code: '{}', Signgu Code: '{}', Page: {}, Rows: {}", 
                keyword, areaCd, sigunguCd, pageNo, numOfRows);
        
        return touristApiClient.getKeywordBasedTouristSpots(keyword, areaCd, sigunguCd, pageNo, numOfRows)
                .map(response -> {
                    if (response.getResponse() != null && 
                        response.getResponse().getBody() != null && 
                        response.getResponse().getBody().getItems() != null) {
                        return response.getResponse().getBody().getItems().getItem();
                    }
                    return List.<TouristApiResponseDto.TouristItem>of();
                })
                .doOnSuccess(items -> log.info("Retrieved {} keyword-based tourist spots for keyword: {}", items.size(), keyword))
                .doOnError(error -> log.error("Failed to retrieve keyword-based tourist spots: {}", error.getMessage()));
    }
    
    /**
     * 카테고리별 관광지 정보 조회 (Controller용)
     */
    public Mono<String> getCategoryBasedTouristInfo(String areaCd, String sigunguCd, String category) {
        log.info("=== TOURIST INFO SERVICE: CATEGORY SEARCH ===");
        log.info("Area Code: '{}', Signgu Code: '{}', Category: '{}'", areaCd, sigunguCd, category);
        
        return getAreaBasedSpots(areaCd, sigunguCd, 1, 50)
                .map(items -> {
                    List<TouristApiResponseDto.TouristItem> filteredItems = items.stream()
                            .filter(item -> category.equals(item.getRlteCtgryLclsNm()) || 
                                          category.equals(item.getRlteCtgryMclsNm()) ||
                                          category.equals(item.getRlteCtgrySclsNm()))
                            .toList();
                    
                    return buildContextFromResponse(createResponseFromItems(filteredItems), category);
                })
                .onErrorResume(error -> {
                    log.error("Failed to get category-based tourist info: {}", error.getMessage(), error);
                    return Mono.just("해당 카테고리의 관광지 정보를 찾을 수 없습니다.");
                });
    }
    
    /**
     * TouristItem 리스트로부터 TouristApiResponseDto 생성
     */
    private TouristApiResponseDto createResponseFromItems(List<TouristApiResponseDto.TouristItem> items) {
        TouristApiResponseDto response = new TouristApiResponseDto();
        TouristApiResponseDto.Response responseBody = new TouristApiResponseDto.Response();
        TouristApiResponseDto.Body body = new TouristApiResponseDto.Body();
        TouristApiResponseDto.Items itemsWrapper = new TouristApiResponseDto.Items();
        
        itemsWrapper.setItem(items);
        body.setItems(itemsWrapper);
        responseBody.setBody(body);
        response.setResponse(responseBody);
        
        return response;
    }
} 