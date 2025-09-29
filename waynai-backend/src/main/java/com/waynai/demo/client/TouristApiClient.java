package com.waynai.demo.client;

import com.waynai.demo.dto.TouristApiResponseDto;
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
 * 관광공사 API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TouristApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${data.api.url.LocgoHubTarService1.areaBasedList1}")
    private String apiUrl;
    
    @Value("${data.api.url.LocgoHubTarService1.areaBasedList1.serviceKey}")
    private String serviceKey;
    
    /**
     * 지역 기반 관광지 목록 조회
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지당 행 수
     * @return 관광지 목록 응답
     */
    public TouristApiResponseDto getAreaBasedList(String areaCode, String sigunguCode, 
                                                  Integer pageNo, Integer numOfRows) {
        URI uri = null;
        try {
            // 직접 URL 구성하여 정확한 인코딩 보장
            String encodedServiceKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
            String previousMonth = DateUtil.getPreviousMonth();
            String url = apiUrl + "?serviceKey=" + encodedServiceKey +
                        "&pageNo=" + pageNo +
                        "&numOfRows=" + numOfRows +
                        "&MobileOS=WEB" +
                        "&MobileApp=WaynAI" +
                        "&baseYm=" + previousMonth +
                        "&areaCd=" + areaCode +
                        "&signguCd=" + sigunguCode +
                        "&_type=json";
            
            uri = new URI(url);
            
            log.info("API 호출 URL: {}", uri);
            
            // Accept 헤더를 설정하여 JSON 응답을 요청
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<TouristApiResponseDto> responseEntity = 
                restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, entity, TouristApiResponseDto.class);
            
            TouristApiResponseDto response = responseEntity.getBody();
            
            if (response != null && response.getResponse() != null) {
                log.info("API 호출 성공 - 결과 코드: {}, 메시지: {}", 
                        response.getResponse().getHeader().getResultCode(),
                        response.getResponse().getHeader().getResultMsg());
            }
            
            return response;
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("API 호출 실패 (네트워크/SSL 오류) - 지역코드: {}, 시군구코드: {}, URL: {}", areaCode, sigunguCode, uri, e);
            throw new RuntimeException("관광공사 API 호출 실패 (네트워크 오류): " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("API 호출 실패 (HTTP 오류) - 지역코드: {}, 시군구코드: {}, 상태코드: {}, URL: {}", 
                     areaCode, sigunguCode, e.getStatusCode(), uri, e);
            throw new RuntimeException("관광공사 API 호출 실패 (HTTP 오류): " + e.getStatusCode() + " - " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("API 호출 실패 (서버 오류) - 지역코드: {}, 시군구코드: {}, 상태코드: {}, URL: {}", 
                     areaCode, sigunguCode, e.getStatusCode(), uri, e);
            throw new RuntimeException("관광공사 API 호출 실패 (서버 오류): " + e.getStatusCode() + " - " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("API 호출 실패 (기타 오류) - 지역코드: {}, 시군구코드: {}, URL: {}", areaCode, sigunguCode, uri, e);
            throw new RuntimeException("관광공사 API 호출 실패: " + e.getMessage(), e);
        }
    }
}
