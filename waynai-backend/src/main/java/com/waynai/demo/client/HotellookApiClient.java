package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.dto.TravelPlanDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Travelpayouts Hotellook 호텔 검색 클라이언트 (숙소 실데이터).
 *
 * <p>Travelpayouts 토큰을 공유한다. lookup(도시→locationId) → cache(호텔 목록) 2단계.
 * 미설정/실패 시 빈 결과. HttpURLConnection 사용(공유 RestTemplate TLS 제약 회피).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotellookApiClient {

    @Value("${hotellook.token:}")
    private String token;

    @Value("${hotellook.marker:}")
    private String marker;

    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        return token != null && !token.isBlank();
    }

    /**
     * 목적지 도시의 추천 호텔 상위 N개를 Accommodation 형태로 반환.
     */
    public List<TravelPlanDto.Accommodation> search(String destination, int limit) {
        List<TravelPlanDto.Accommodation> out = new ArrayList<>();
        if (!isEnabled() || destination == null || destination.isBlank()) return out;
        try {
            // 1) 도시명 → locationId
            String lookupUrl = "https://engine.hotellook.com/api/v2/lookup.json?query="
                    + enc(destination) + "&lang=ko&lookFor=city&limit=1&token=" + enc(token);
            JsonNode loc = objectMapper.readTree(get(lookupUrl))
                    .path("results").path("locations").path(0);
            String locationId = loc.path("id").asText(null);
            String cityFull = loc.path("fullName").asText(destination);
            if (locationId == null) {
                log.info("[hotellook] 도시 locationId 미발견: {}", destination);
                return out;
            }
            // 2) locationId → 호텔 캐시
            String cacheUrl = "https://engine.hotellook.com/api/v2/cache.json?locationId="
                    + enc(locationId) + "&currency=krw&limit=" + limit + "&token=" + enc(token);
            JsonNode hotels = objectMapper.readTree(get(cacheUrl));
            if (hotels.isArray()) {
                for (JsonNode h : hotels) {
                    int stars = h.path("stars").asInt(0);
                    Integer price = h.hasNonNull("priceFrom")
                            ? (int) Math.round(h.get("priceFrom").asDouble()) : null;
                    out.add(TravelPlanDto.Accommodation.builder()
                            .name(h.path("hotelName").asText(""))
                            .area(cityFull)
                            .type(stars > 0 ? stars + "성급 호텔" : "호텔")
                            .pricePerNightKrw(price)
                            .bookingUrl(buildBookingUrl(destination))
                            .build());
                }
            }
            log.info("[hotellook] '{}' 호텔 {}건", destination, out.size());
        } catch (Exception e) {
            log.warn("[hotellook] 호텔 검색 실패 (무시): {}", e.getMessage());
        }
        return out;
    }

    private String buildBookingUrl(String destination) {
        return searchLink(destination);
    }

    /** Hotellook 웹 호텔검색 딥링크 (API 불필요, 마커 포함 → 예약 시 커미션). */
    public String searchLink(String destination) {
        String url = "https://search.hotellook.com/hotels?destination=" + enc(destination == null ? "" : destination);
        if (marker != null && !marker.isBlank()) url += "&marker=" + marker;
        return url;
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String get(String apiUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            int code = con.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " - " + sb);
                return sb.toString();
            }
        } finally {
            con.disconnect();
        }
    }
}
