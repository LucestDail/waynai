package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * daero(대로) 대중교통 경로엔진 연동 클라이언트.
 * 국내 좌표 구간의 대중교통 소요시간·환승·요금을 조회한다. daero 미기동 시 조용히 비활성.
 * (daero: 자체 RAPTOR+GTFS 엔진, 기본 http://localhost:8090)
 */
@Slf4j
@Component
public class DaeroClient {

    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public DaeroClient(@Value("${daero.base-url:}") String baseUrl, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    /** 두 좌표 사이 대중교통 요약. 실패/무경로 시 null. */
    public Transit transit(double fromLat, double fromLon, double toLat, double toLon, String time) {
        if (!isEnabled()) return null;
        try {
            String url = String.format("%s/api/plan/coords?fromLat=%f&fromLon=%f&toLat=%f&toLon=%f&time=%s",
                    baseUrl, fromLat, fromLon, toLat, toLon, URLEncoder.encode(time, StandardCharsets.UTF_8));
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(1500);
            con.setReadTimeout(4000);
            int code = con.getResponseCode();
            if (code != 200) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                JsonNode n = objectMapper.readTree(r);
                if (!n.path("found").asBoolean(false)) return null;
                String firstRoute = "";
                String firstMode = "";
                // 도보 제외 전 구간의 수단을 등장순으로 수집(예: 지하철·버스)
                java.util.LinkedHashSet<String> modeSet = new java.util.LinkedHashSet<>();
                for (JsonNode leg : n.path("legs")) {
                    String mode = leg.path("mode").asText();
                    if (!"WALK".equals(mode)) {
                        if (firstMode.isEmpty()) { firstMode = mode; firstRoute = leg.path("route").asText(""); }
                        modeSet.add(modeKo(mode));
                    }
                }
                return new Transit(n.path("durationMin").asInt(), n.path("transfers").asInt(),
                        n.path("estimatedFareKrw").asInt(), firstMode, firstRoute, String.join("·", modeSet));
            }
        } catch (Exception e) {
            log.debug("[daero] 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /** GTFS 모드 코드 → 한글 표기. */
    static String modeKo(String mode) {
        return switch (mode) {
            case "BUS" -> "버스"; case "SUBWAY" -> "지하철"; case "RAIL" -> "기차";
            case "AIR" -> "항공"; case "FERRY" -> "여객선"; default -> "대중교통";
        };
    }

    /** modeSummary: 도보 제외 사용 수단(등장순, "지하철·버스"). */
    public record Transit(int durationMin, int transfers, int fareKrw,
                          String firstMode, String firstRoute, String modeSummary) {
        public String note() {
            String route = firstRoute != null && !firstRoute.isBlank() ? " " + firstRoute : "";
            String m = modeSummary != null && !modeSummary.isBlank() ? modeSummary : modeKo(firstMode);
            return String.format("대중교통 약 %d분·환승 %d회·~%,d원(%s%s)", durationMin, transfers, fareKrw, m, route);
        }
    }
}
