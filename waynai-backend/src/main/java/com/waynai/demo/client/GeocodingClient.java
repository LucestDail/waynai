package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenStreetMap Nominatim 지오코딩 (무료·키 불필요). 장소명 → 좌표.
 *
 * <p>이용정책상 User-Agent 필수 + 초당 1회 제한 → 호출 간 간격 + 캐시. best-effort.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeocodingClient {

    private final ObjectMapper objectMapper;
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();
    private volatile long lastCallMs = 0;

    /** 장소명(+지역 힌트) → [lat, lng]. 실패 시 null. */
    public synchronized double[] geocode(String name, String regionHint) {
        if (name == null || name.isBlank()) return null;
        String q = regionHint != null && !regionHint.isBlank() ? name + ", " + regionHint : name;
        String key = q.toLowerCase();
        if (cache.containsKey(key)) return cache.get(key);
        try {
            // 초당 1회 제한 준수 (간단 스로틀).
            long since = System.currentTimeMillis() - lastCallMs;
            if (since < 1100) Thread.sleep(1100 - since);
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
                    + URLEncoder.encode(q, StandardCharsets.UTF_8);
            String body = get(url);
            lastCallMs = System.currentTimeMillis();
            JsonNode arr = objectMapper.readTree(body);
            if (arr.isArray() && arr.size() > 0) {
                double lat = arr.get(0).path("lat").asDouble();
                double lon = arr.get(0).path("lon").asDouble();
                double[] p = new double[]{lat, lon};
                cache.put(key, p);
                return p;
            }
        } catch (Exception e) {
            log.debug("[geocode] 실패 '{}': {}", q, e.getMessage());
        }
        cache.put(key, null);
        return null;
    }

    private String get(String apiUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "WaynAI/1.0 (travel planner)"); // Nominatim 정책상 필수
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(6000);
            con.setReadTimeout(8000);
            int code = con.getResponseCode();
            InputStream is = (code == 200) ? con.getInputStream() : con.getErrorStream();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                if (code != 200) throw new IOException("HTTP " + code);
                return sb.toString();
            }
        } finally {
            con.disconnect();
        }
    }
}
