package com.waynai.demo.client;

import com.waynai.demo.dto.tourist.TouristApiRequestDto;
import com.waynai.demo.dto.tourist.TouristApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TouristApiClient {
    
    private final WebClient webClient;
    
    @Value("${tourist.api.base-url:https://apis.data.go.kr/B551011/TarRlteTarService1}")
    private String baseUrl;
    
    @Value("${tourist.api.service-key}")
    private String serviceKey;
    
    /**
     * 지역기반 관광지별 연관 관광지 정보 목록 조회
     */
    public Mono<TouristApiResponseDto> getAreaBasedRelatedTouristSpots(
            String areaCd, 
            String signguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        TouristApiRequestDto request = TouristApiRequestDto.builder()
                .serviceKey(serviceKey)
                .pageNo(pageNo != null ? pageNo : 1)
                .numOfRows(numOfRows != null ? numOfRows : 10)
                .baseYm(getCurrentBaseYm())
                .areaCd(areaCd)
                .signguCd(signguCd)
                .build();
        
        return callAreaBasedApi(request);
    }
    
    /**
     * 키워드 검색 관광지별 연관 관광지 정보 목록 조회
     */
    public Mono<TouristApiResponseDto> getKeywordBasedRelatedTouristSpots(
            String keyword, 
            String areaCd, 
            String signguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        TouristApiRequestDto request = TouristApiRequestDto.builder()
                .serviceKey(serviceKey)
                .pageNo(pageNo != null ? pageNo : 1)
                .numOfRows(numOfRows != null ? numOfRows : 10)
                .baseYm(getCurrentBaseYm())
                .keyword(keyword)
                .areaCd(areaCd)
                .signguCd(signguCd)
                .build();
        
        return callKeywordSearchApi(request);
    }
    
    private Mono<TouristApiResponseDto> callAreaBasedApi(TouristApiRequestDto request) {
        String url = buildAreaBasedUrl(request);
        log.info("Calling area-based tourist API: {}", url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(TouristApiResponseDto.class)
                .doOnSuccess(response -> log.info("Area-based API call successful"))
                .doOnError(error -> log.error("Area-based API call failed: {}", error.getMessage()));
    }
    
    private Mono<TouristApiResponseDto> callKeywordSearchApi(TouristApiRequestDto request) {
        String url = buildKeywordSearchUrl(request);
        log.info("Calling keyword search tourist API: {}", url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(TouristApiResponseDto.class)
                .doOnSuccess(response -> log.info("Keyword search API call successful"))
                .doOnError(error -> log.error("Keyword search API call failed: {}", error.getMessage()));
    }
    
    private String buildAreaBasedUrl(TouristApiRequestDto request) {
        return UriComponentsBuilder.fromUriString(baseUrl + "/areaBasedList1")
                .queryParam("serviceKey", request.getServiceKey())
                .queryParam("pageNo", request.getPageNo())
                .queryParam("numOfRows", request.getNumOfRows())
                .queryParam("MobileOS", request.getMobileOS())
                .queryParam("MobileApp", request.getMobileApp())
                .queryParam("baseYm", request.getBaseYm())
                .queryParam("areaCd", request.getAreaCd())
                .queryParam("signguCd", request.getSignguCd())
                .queryParam("_type", request.get_type())
                .build()
                .toUriString();
    }
    
    private String buildKeywordSearchUrl(TouristApiRequestDto request) {
        return UriComponentsBuilder.fromUriString(baseUrl + "/searchKeyword1")
                .queryParam("serviceKey", request.getServiceKey())
                .queryParam("pageNo", request.getPageNo())
                .queryParam("numOfRows", request.getNumOfRows())
                .queryParam("MobileOS", request.getMobileOS())
                .queryParam("MobileApp", request.getMobileApp())
                .queryParam("baseYm", request.getBaseYm())
                .queryParam("areaCd", request.getAreaCd())
                .queryParam("signguCd", request.getSignguCd())
                .queryParam("keyword", request.getKeyword())
                .queryParam("_type", request.get_type())
                .build()
                .toUriString();
    }
    
    private String getCurrentBaseYm() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%d%02d", now.getYear(), now.getMonthValue());
    }
} 