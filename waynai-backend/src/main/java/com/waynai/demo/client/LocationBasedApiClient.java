package com.waynai.demo.client;

import com.waynai.demo.dto.LocationBasedResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 한국관광공사 KorService2.locationBasedList2 클라이언트.
 * GPS 좌표 + 반경으로 주변 관광지/음식/숙박 등의 POI 를 조회한다.
 *
 * <p>주의: 서비스키는 application.properties 에서 tour.api.service-key 와 동일한 값을 공유한다
 * (공공데이터포털 한국관광공사 승인키는 동일 앱에서 모든 엔드포인트 공통).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationBasedApiClient {

    private final RestTemplate restTemplate;

    @Value("${data.api.url.KorService2.locationBasedList2}")
    private String apiUrlV2;

    @Value("${data.api.url.KorService1.locationBasedList1:https://apis.data.go.kr/B551011/KorService1/locationBasedList1}")
    private String apiUrlV1;

    @Value("${data.api.url.KorService2.locationBasedList2.serviceKey}")
    private String serviceKey;

    /**
     * 좌표 기반 주변 POI 조회.
     *
     * @param mapX          경도 (longitude)
     * @param mapY          위도 (latitude)
     * @param radiusMeters  반경(미터). KorService2 허용 범위: 20 ~ 20000
     * @param contentTypeId 콘텐츠 타입 (null 허용). 예: 12(관광지), 14(문화시설), 28(레포츠), 32(숙박), 38(쇼핑), 39(음식점)
     * @param pageNo        페이지 번호
     * @param numOfRows     페이지당 개수
     */
    public LocationBasedResponseDto getLocationBasedList(double mapX, double mapY,
                                                        int radiusMeters,
                                                        String contentTypeId,
                                                        int pageNo, int numOfRows) {
        // 1차: KorService2 (신버전 - 승인 권장)
        try {
            return callV2(mapX, mapY, radiusMeters, contentTypeId, pageNo, numOfRows);
        } catch (Exception e) {
            log.warn("[locationBasedList2] v2 실패 ({}). v1 폴백 시도합니다.", e.getMessage());
        }
        // 2차: KorService1 (구버전 폴백)
        return callV1(mapX, mapY, radiusMeters, contentTypeId, pageNo, numOfRows);
    }

    private LocationBasedResponseDto callV2(double mapX, double mapY, int radiusMeters,
                                            String contentTypeId, int pageNo, int numOfRows) {
        StringBuilder qp = new StringBuilder()
                .append("&numOfRows=").append(numOfRows)
                .append("&pageNo=").append(pageNo)
                .append("&MobileOS=WEB")
                .append("&MobileApp=WaynAI")
                .append("&mapX=").append(mapX)
                .append("&mapY=").append(mapY)
                .append("&radius=").append(radiusMeters)
                .append("&arrange=S")  // v2: S(거리순)
                .append("&_type=json");
        if (contentTypeId != null && !contentTypeId.isBlank()) {
            qp.append("&contentTypeId=").append(contentTypeId);
        }
        return doCall("v2", apiUrlV2, qp.toString());
    }

    private LocationBasedResponseDto callV1(double mapX, double mapY, int radiusMeters,
                                            String contentTypeId, int pageNo, int numOfRows) {
        // KorService1 파라미터: listYN=Y 필수, arrange 는 A/C/D/O/Q/R (거리순 없음).
        StringBuilder qp = new StringBuilder()
                .append("&numOfRows=").append(numOfRows)
                .append("&pageNo=").append(pageNo)
                .append("&MobileOS=WEB")
                .append("&MobileApp=WaynAI")
                .append("&listYN=Y")
                .append("&mapX=").append(mapX)
                .append("&mapY=").append(mapY)
                .append("&radius=").append(radiusMeters)
                .append("&_type=json");
        if (contentTypeId != null && !contentTypeId.isBlank()) {
            qp.append("&contentTypeId=").append(contentTypeId);
        }
        return doCall("v1", apiUrlV1, qp.toString());
    }

    private LocationBasedResponseDto doCall(String tag, String baseUrl, String queryParams) {
        URI uri = null;
        try {
            String encodedServiceKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
            String url = baseUrl + "?serviceKey=" + encodedServiceKey + queryParams;
            uri = new URI(url);
            log.info("[locationBased {}] URL: {}", tag, uri);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<LocationBasedResponseDto> resp =
                    restTemplate.exchange(uri, HttpMethod.GET, entity, LocationBasedResponseDto.class);

            LocationBasedResponseDto body = resp.getBody();
            if (body != null && body.getResponse() != null && body.getResponse().getHeader() != null) {
                log.info("[locationBased {}] 응답 resultCode={}, resultMsg={}", tag,
                        body.getResponse().getHeader().getResultCode(),
                        body.getResponse().getHeader().getResultMsg());
            }
            return body;
        } catch (Exception e) {
            log.error("[locationBased {}] 호출 실패 - URL: {}", tag, uri, e);
            throw new RuntimeException("좌표기반 관광지 조회 실패(" + tag + "): " + e.getMessage(), e);
        }
    }
}
