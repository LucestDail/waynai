package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.dto.tourist.TouristApiResponseDto;
import com.waynai.demo.util.AreaCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class TouristApiClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AreaCodeUtil areaCodeUtil;
    
    @Value("${tourist.api.base-url:https://apis.data.go.kr/B551011/TarRlteTarService1}")
    private String baseUrl;
    
    @Value("${tourist.api.service-key}")
    private String serviceKey;
    
    /**
     * 지역기반 관광지 정보 목록 조회 (새로운 API)
     */
    public Mono<TouristApiResponseDto> getAreaBasedTouristSpots(
            String areaCd, 
            String sigunguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        // 기본 지역코드 설정 (부산광역시 해운대구)
        String defaultAreaCd = "26"; // 부산광역시
        String defaultSigunguCd = "26350"; // 해운대구
        
        // 파라미터가 없으면 기본값 사용
        String finalAreaCd = (areaCd != null && !areaCd.isEmpty()) ? areaCd : defaultAreaCd;
        String finalSigunguCd = (sigunguCd != null && !sigunguCd.isEmpty()) ? sigunguCd : defaultSigunguCd;
        
        String url = buildAreaBasedUrl(finalAreaCd, finalSigunguCd, pageNo, numOfRows);
        log.info("Calling area-based tourist API: {}", url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    try {
                        log.debug("Raw API response: {}", responseBody);
                        
                        // 에러 응답 체크
                        if (responseBody.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || 
                            responseBody.contains("SERVICE ERROR")) {
                            log.warn("API service key error, using fallback data");
                            return createDynamicFallbackResponse(finalAreaCd, finalSigunguCd);
                        }
                        
                        TouristApiResponseDto response = objectMapper.readValue(responseBody, TouristApiResponseDto.class);
                        log.info("Area-based API call successful: {} items found", 
                                response.getResponse() != null && response.getResponse().getBody() != null ? 
                                response.getResponse().getBody().getItems() != null ? 
                                response.getResponse().getBody().getItems().getItem() != null ? 
                                response.getResponse().getBody().getItems().getItem().size() : 0 : 0 : 0);
                        return Mono.just(response);
                    } catch (Exception e) {
                        log.error("Failed to parse API response: {}", e.getMessage());
                        log.warn("Using fallback data due to parsing error");
                        return createDynamicFallbackResponse(finalAreaCd, finalSigunguCd);
                    }
                })
                .doOnError(error -> {
                    log.error("Area-based API call failed: {}", error.getMessage());
                    log.warn("Using fallback data due to API error");
                })
                .onErrorResume(error -> createDynamicFallbackResponse(finalAreaCd, finalSigunguCd));
    }
    
    /**
     * 키워드 검색 관광지 정보 목록 조회 (새로운 API)
     */
    public Mono<TouristApiResponseDto> getKeywordBasedTouristSpots(
            String keyword, 
            String areaCd, 
            String sigunguCd, 
            Integer pageNo, 
            Integer numOfRows) {
        
        // 키워드에서 지역 정보 추출
        AreaCodeUtil.AreaCodeInfo areaInfo = areaCodeUtil.extractAreaInfoFromKeyword(keyword);
        
        // 파라미터가 없으면 추출된 값 사용, 없으면 기본값 사용
        String finalAreaCd = (areaCd != null && !areaCd.isEmpty()) ? areaCd : 
                            (areaInfo != null ? areaInfo.getAreaCode() : "26");
        String finalSigunguCd = (sigunguCd != null && !sigunguCd.isEmpty()) ? sigunguCd : 
                              (areaInfo != null ? areaInfo.getSignguCode() : "26350");
        
        log.info("Using area code: {} (sigungu: {}) for keyword: {}", finalAreaCd, finalSigunguCd, keyword);
        
        String url = buildKeywordSearchUrl(keyword, finalAreaCd, finalSigunguCd, pageNo, numOfRows);
        log.info("Calling keyword search tourist API: {}", url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    try {
                        log.debug("Raw API response: {}", responseBody);
                        
                        // 에러 응답 체크
                        if (responseBody.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || 
                            responseBody.contains("SERVICE ERROR")) {
                            log.warn("API service key error, using fallback data");
                            return createDynamicFallbackResponse(finalAreaCd, finalSigunguCd);
                        }
                        
                        TouristApiResponseDto response = objectMapper.readValue(responseBody, TouristApiResponseDto.class);
                        log.info("Keyword search API call successful: {} items found", 
                                response.getResponse() != null && response.getResponse().getBody() != null ? 
                                response.getResponse().getBody().getItems() != null ? 
                                response.getResponse().getBody().getItems().getItem() != null ? 
                                response.getResponse().getBody().getItems().getItem().size() : 0 : 0 : 0);
                        return Mono.just(response);
                    } catch (Exception e) {
                        log.error("Failed to parse API response: {}", e.getMessage());
                        log.warn("Using fallback data due to parsing error");
                        return createDynamicFallbackResponse(finalAreaCd, finalSigunguCd);
                    }
                })
                .doOnError(error -> {
                    log.error("Keyword search API call failed: {}", error.getMessage());
                    log.warn("Using fallback data due to API error");
                })
                .onErrorResume(error -> createDynamicFallbackResponse(finalAreaCd, finalSigunguCd));
    }
    
    private String buildAreaBasedUrl(String areaCd, String sigunguCd, Integer pageNo, Integer numOfRows) {
        return UriComponentsBuilder.fromUriString(baseUrl + "/areaBasedList1")
                .queryParam("serviceKey", URLEncoder.encode(serviceKey, StandardCharsets.UTF_8))
                .queryParam("pageNo", pageNo != null ? pageNo : 1)
                .queryParam("numOfRows", numOfRows != null ? numOfRows : 100)
                .queryParam("MobileOS", "WEB")
                .queryParam("MobileApp", "test")
                .queryParam("baseYm", "202506")
                .queryParam("areaCd", areaCd)
                .queryParam("sigunguCd", sigunguCd)
                .queryParam("_type", "json")
                .build()
                .toUriString();
    }
    
    private String buildKeywordSearchUrl(String keyword, String areaCd, String sigunguCd, Integer pageNo, Integer numOfRows) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + "/areaBasedList1")
                .queryParam("serviceKey", URLEncoder.encode(serviceKey, StandardCharsets.UTF_8))
                .queryParam("pageNo", pageNo != null ? pageNo : 1)
                .queryParam("numOfRows", numOfRows != null ? numOfRows : 100)
                .queryParam("MobileOS", "WEB")
                .queryParam("MobileApp", "test")
                .queryParam("baseYm", "202506")
                .queryParam("areaCd", areaCd)
                .queryParam("sigunguCd", sigunguCd)
                .queryParam("_type", "json");
        
        return builder.build().toUriString();
    }
    
    /**
     * 기본 Fallback 응답 생성 (Gemini 호출 없이 정적 데이터 사용)
     */
    private Mono<TouristApiResponseDto> createDynamicFallbackResponse(String areaCd, String sigunguCd) {
        try {
            // 지역 코드로 지역명과 시군구명 조회
            String areaName = getAreaNameByCode(areaCd);
            String sigunguName = getSigunguNameByCode(sigunguCd);
            
            // Gemini 호출 없이 정적 데이터로 fallback 응답 생성
            return Mono.just(createBasicFallbackResponse(areaCd, sigunguCd, areaName, sigunguName));
                    
        } catch (Exception e) {
            log.error("Failed to create fallback response: {}", e.getMessage());
            return Mono.just(createBasicFallbackResponse(areaCd, sigunguCd, "부산광역시", "해운대구"));
        }
    }
    
    /**
     * 기본 Fallback 응답 생성
     */
    private TouristApiResponseDto createBasicFallbackResponse(String areaCd, String sigunguCd, String areaName, String sigunguName) {
        TouristApiResponseDto response = new TouristApiResponseDto();
        TouristApiResponseDto.Response responseBody = new TouristApiResponseDto.Response();
        TouristApiResponseDto.Header header = new TouristApiResponseDto.Header();
        TouristApiResponseDto.Body body = new TouristApiResponseDto.Body();
        TouristApiResponseDto.Items items = new TouristApiResponseDto.Items();
        
        // 헤더 설정
        header.setResultCode("0000");
        header.setResultMsg("OK");
        responseBody.setHeader(header);
        
        // 기본 관광지 정보 생성
        java.util.List<TouristApiResponseDto.TouristItem> itemList = new java.util.ArrayList<>();
        
        // 지역별 대표 관광지 생성
        TouristApiResponseDto.TouristItem item1 = new TouristApiResponseDto.TouristItem();
        item1.setBaseYm("202506");
        item1.setTAtsCd("9951cd14afadc30de7a66860ad5be803");
        item1.setTAtsNm(areaName + " 대표 관광지");
        item1.setAreaCd(areaCd);
        item1.setAreaNm(areaName);
        item1.setSignguCd(sigunguCd);
        item1.setSignguNm(sigunguName);
        item1.setRlteTatsCd("4a86c619b46e66efe9ac02b85090ecac");
        item1.setRlteTatsNm(areaName + " 대표 관광지");
        item1.setRlteRegnNm(areaName);
        item1.setRlteSignguNm(sigunguName);
        item1.setRlteCtgryLclsNm("관광지");
        item1.setRlteCtgryMclsNm("문화관광");
        item1.setRlteCtgrySclsNm("문화시설");
        item1.setRlteRank("1");
        itemList.add(item1);
        
        items.setItem(itemList);
        body.setItems(items);
        responseBody.setBody(body);
        response.setResponse(responseBody);
        
        return response;
    }
    

    
    /**
     * 지역 코드로 지역명 조회
     */
    private String getAreaNameByCode(String areaCd) {
        switch (areaCd) {
            case "11": return "서울특별시";
            case "26": return "부산광역시";
            case "27": return "대구광역시";
            case "28": return "인천광역시";
            case "29": return "광주광역시";
            case "30": return "대전광역시";
            case "31": return "울산광역시";
            case "36": return "세종특별자치시";
            case "41": return "경기도";
            case "43": return "충청북도";
            case "44": return "충청남도";
            case "45": return "전라북도";
            case "46": return "전라남도";
            case "47": return "경상북도";
            case "48": return "경상남도";
            case "50": return "제주특별자치도";
            case "51": return "강원특별자치도";
            default: return "부산광역시";
        }
    }
    
    /**
     * 시군구 코드로 시군구명 조회
     */
    private String getSigunguNameByCode(String sigunguCd) {
        // 주요 시군구 코드 매핑
        switch (sigunguCd) {
            case "11110": return "종로구";
            case "11680": return "강남구";
            case "26350": return "해운대구";
            case "50110": return "제주시";
            case "51110": return "춘천시";
            case "43111": return "청주시";
            case "44131": return "천안시";
            case "52111": return "전주시";
            case "46110": return "목포시";
            case "47111": return "포항시";
            case "48121": return "창원시";
            default: return "해운대구";
        }
    }
} 