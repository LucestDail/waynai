package com.waynai.demo.client;

import com.waynai.demo.dto.RelatedTouristApiResponseDto;
import com.waynai.demo.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 연관 관광지 API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelatedTouristApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${data.api.url.TarRlteTarService1.areaBasedList1}")
    private String areaBasedApiUrl;
    
    @Value("${data.api.url.TarRlteTarService1.areaBasedList1.serviceKey}")
    private String areaBasedServiceKey;
    
    @Value("${data.api.url.TarRlteTarService1.searchKeyword1}")
    private String searchKeywordApiUrl;
    
    @Value("${data.api.url.TarRlteTarService1.searchKeyword1.serviceKey}")
    private String searchKeywordServiceKey;
    
    /**
     * 지역기반 중심 관광지 정보 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 연관 관광지 목록 응답
     */
    public RelatedTouristApiResponseDto getAreaBasedList(String areaCode, String sigunguCode, 
                                                         Integer pageNo, Integer numOfRows) {
        URI uri = null;
        try {
            // 직접 URL 구성하여 정확한 인코딩 보장
            String encodedServiceKey = URLEncoder.encode(areaBasedServiceKey, StandardCharsets.UTF_8);
            String previousMonth = DateUtil.getPreviousMonth();
            String url = areaBasedApiUrl + "?serviceKey=" + encodedServiceKey +
                        "&pageNo=" + pageNo +
                        "&numOfRows=" + numOfRows +
                        "&MobileOS=WEB" +
                        "&MobileApp=WaynAI" +
                        "&baseYm=" + previousMonth +
                        "&areaCd=" + areaCode +
                        "&signguCd=" + sigunguCode +
                        "&_type=json";
            
            uri = new URI(url);
            
            log.info("지역기반 연관 관광지 API 호출 URL: {}", uri);
            
            // Accept 헤더를 설정하여 JSON 응답을 요청
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<RelatedTouristApiResponseDto> responseEntity = 
                restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, entity, RelatedTouristApiResponseDto.class);
            
            RelatedTouristApiResponseDto response = responseEntity.getBody();
            
            if (response != null && response.getResponse() != null) {
                log.info("지역기반 연관 관광지 API 호출 성공 - 결과 코드: {}, 메시지: {}", 
                        response.getResponse().getHeader().getResultCode(),
                        response.getResponse().getHeader().getResultMsg());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("지역기반 연관 관광지 API 호출 실패 - 지역코드: {}, 시군구코드: {}, URL: {}", areaCode, sigunguCode, uri, e);
            throw new RuntimeException("지역기반 연관 관광지 API 호출 실패: " + e.getMessage(), e);
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
    public RelatedTouristApiResponseDto searchKeyword(String areaCode, String sigunguCode, String keyword,
                                                      Integer pageNo, Integer numOfRows) {
        URI uri = null;
        try {
            // 직접 URL 구성하여 정확한 인코딩 보장
            String encodedServiceKey = URLEncoder.encode(searchKeywordServiceKey, StandardCharsets.UTF_8);
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String previousMonth = DateUtil.getPreviousMonth();
            String url = searchKeywordApiUrl + "?serviceKey=" + encodedServiceKey +
                        "&pageNo=" + pageNo +
                        "&numOfRows=" + numOfRows +
                        "&MobileOS=WEB" +
                        "&MobileApp=WaynAI" +
                        "&baseYm=" + previousMonth +
                        "&areaCd=" + areaCode +
                        "&signguCd=" + sigunguCode +
                        "&keyword=" + encodedKeyword +
                        "&_type=json";
            
            uri = new URI(url);
            
            log.info("키워드 검색 연관 관광지 API 호출 URL: {}", uri);
            
            // Accept 헤더를 설정하여 JSON 응답을 요청
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<RelatedTouristApiResponseDto> responseEntity = 
                restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, entity, RelatedTouristApiResponseDto.class);
            
            RelatedTouristApiResponseDto response = responseEntity.getBody();
            
            if (response != null && response.getResponse() != null) {
                log.info("키워드 검색 연관 관광지 API 호출 성공 - 결과 코드: {}, 메시지: {}", 
                        response.getResponse().getHeader().getResultCode(),
                        response.getResponse().getHeader().getResultMsg());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("키워드 검색 연관 관광지 API 호출 실패 - 지역코드: {}, 시군구코드: {}, 키워드: {}, URL: {}", 
                     areaCode, sigunguCode, keyword, uri, e);
            throw new RuntimeException("키워드 검색 연관 관광지 API 호출 실패: " + e.getMessage(), e);
        }
    }
}
