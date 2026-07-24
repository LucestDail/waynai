package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenRouteService 경로 API 클라이언트.
 *
 * <p>좌표열(위/경도)을 받아 총 이동 거리(m)·소요 시간(초)을 계산한다.
 * 무료 API 키(openrouteservice.org) 필요. 미설정 시 자동 비활성(빈 결과).
 * 공유 RestTemplate 이 data.go.kr 외 TLS 를 막으므로 HttpURLConnection 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingApiClient {

    @Value("${openrouteservice.api.key:}")
    private String apiKey;

    @Value("${openrouteservice.api.base-url:https://api.openrouteservice.org/v2/directions}")
    private String baseUrl;

    private final ObjectMapper objectMapper;

    // 동일 좌표열+프로파일 경로는 캐시해 ORS 무료 한도(하루 2,000건) 낭비를 막는다.
    private final java.util.Map<String, RouteDetail> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String cacheKey(List<double[]> latLngs, String profile) {
        StringBuilder sb = new StringBuilder(profile).append('|');
        for (double[] p : latLngs) sb.append(Math.round(p[0] * 1e4)).append(',').append(Math.round(p[1] * 1e4)).append(';');
        return sb.toString();
    }

    /** 이동 요약. */
    public record RouteSummary(double distanceMeters, double durationSeconds) {}

    /**
     * 좌표열(순서대로) 간 총 경로 계산. 좌표 2개 미만이면 null.
     *
     * @param latLngs [lat, lng] 순서의 좌표 목록
     * @param profile driving-car | foot-walking | cycling-regular
     */
    public RouteSummary route(List<double[]> latLngs, String profile) {
        if (!isEnabled() || latLngs == null || latLngs.size() < 2) return null;
        String prof = (profile == null || profile.isBlank()) ? "driving-car" : profile;
        try {
            // ORS 는 [lon, lat] 순서.
            StringBuilder coords = new StringBuilder("[");
            for (int i = 0; i < latLngs.size(); i++) {
                double[] p = latLngs.get(i);
                if (i > 0) coords.append(",");
                coords.append("[").append(p[1]).append(",").append(p[0]).append("]");
            }
            coords.append("]");
            // radiuses=-1: 각 좌표를 가장 가까운 도로에 스냅(반경 무제한) → "routable point 없음" 404 완화
            StringBuilder rad = new StringBuilder("[");
            for (int ri = 0; ri < latLngs.size(); ri++) rad.append(ri > 0 ? ",-1" : "-1");
            rad.append("]");
            String body = "{\"coordinates\":" + coords + ",\"radiuses\":" + rad + "}";

            String resp = post(baseUrl + "/" + prof, body);
            JsonNode root = objectMapper.readTree(resp);
            JsonNode summary = root.path("routes").path(0).path("summary");
            if (summary.isMissingNode()) return null;
            return new RouteSummary(
                    summary.path("distance").asDouble(0),
                    summary.path("duration").asDouble(0));
        } catch (Exception e) {
            log.warn("[ors] 경로 계산 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    /** 경로 상세: 실제 도로 geometry(좌표열) + 구간(leg)별 거리/시간. */
    public record RouteDetail(List<double[]> geometry, double distanceMeters, double durationSeconds,
                              List<RouteSummary> legs) {}

    /**
     * 실제 도로 경로 상세 조회. GeoJSON 엔드포인트로 geometry + 구간 정보를 받는다.
     */
    public RouteDetail routeDetail(List<double[]> latLngs, String profile) {
        if (!isEnabled() || latLngs == null || latLngs.size() < 2) return null;
        String prof = (profile == null || profile.isBlank()) ? "driving-car" : profile;
        String key = cacheKey(latLngs, prof);
        RouteDetail cached = cache.get(key);
        if (cached != null) return cached;
        try {
            StringBuilder coords = new StringBuilder("[");
            for (int i = 0; i < latLngs.size(); i++) {
                double[] p = latLngs.get(i);
                if (i > 0) coords.append(",");
                coords.append("[").append(p[1]).append(",").append(p[0]).append("]");
            }
            coords.append("]");
            // radiuses=-1: 각 좌표를 가장 가까운 도로에 스냅(반경 무제한) → "routable point 없음" 404 완화
            StringBuilder rad = new StringBuilder("[");
            for (int ri = 0; ri < latLngs.size(); ri++) rad.append(ri > 0 ? ",-1" : "-1");
            rad.append("]");
            String body = "{\"coordinates\":" + coords + ",\"radiuses\":" + rad + "}";

            // GeoJSON 엔드포인트: geometry.coordinates + properties.segments 제공.
            String resp = post(baseUrl + "/" + prof + "/geojson", body);
            JsonNode feat = objectMapper.readTree(resp).path("features").path(0);
            JsonNode coordsNode = feat.path("geometry").path("coordinates");
            List<double[]> geometry = new ArrayList<>();
            if (coordsNode.isArray()) {
                for (JsonNode c : coordsNode) {
                    // GeoJSON 은 [lon, lat] → [lat, lng] 로 변환
                    geometry.add(new double[]{c.path(1).asDouble(), c.path(0).asDouble()});
                }
            }
            JsonNode props = feat.path("properties");
            JsonNode sum = props.path("summary");
            List<RouteSummary> legs = new ArrayList<>();
            for (JsonNode seg : props.path("segments")) {
                legs.add(new RouteSummary(seg.path("distance").asDouble(0), seg.path("duration").asDouble(0)));
            }
            RouteDetail detail = new RouteDetail(geometry, sum.path("distance").asDouble(0),
                    sum.path("duration").asDouble(0), legs);
            cache.put(key, detail);
            return detail;
        } catch (Exception e) {
            log.warn("[ors] 경로 상세 조회 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    private String post(String apiUrl, String jsonBody) throws IOException {
        // geojson 엔드포인트는 application/geo+json 을 요구(Accept application/json 이면 406).
        String accept = apiUrl.endsWith("/geojson")
                ? "application/geo+json, application/json"
                : "application/json";
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        try {
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", apiKey);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", accept);
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
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
