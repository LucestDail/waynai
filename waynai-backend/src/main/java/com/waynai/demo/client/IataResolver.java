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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 도시/지역명 → IATA 코드 변환기.
 *
 * <p>Travelpayouts autocomplete(places2, 토큰 불필요)로 조회하되, 자주 쓰는 한국어 지명은
 * 정적 오버라이드로 즉시 매핑하고 결과는 캐시한다. 실패 시 null.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IataResolver {

    @Value("${travelpayouts.autocomplete-url:https://autocomplete.travelpayouts.com/places2}")
    private String autocompleteUrl;

    private final ObjectMapper objectMapper;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** 자주 쓰는 지명 정적 매핑 (autocomplete 모호성 회피). key 는 소문자. */
    private static final Map<String, String> OVERRIDES = Map.ofEntries(
            Map.entry("서울", "SEL"), Map.entry("seoul", "SEL"),
            Map.entry("부산", "PUS"), Map.entry("busan", "PUS"),
            Map.entry("제주", "CJU"), Map.entry("제주도", "CJU"), Map.entry("jeju", "CJU"),
            Map.entry("도쿄", "TYO"), Map.entry("동경", "TYO"), Map.entry("tokyo", "TYO"),
            Map.entry("오사카", "OSA"), Map.entry("osaka", "OSA"),
            Map.entry("후쿠오카", "FUK"), Map.entry("fukuoka", "FUK"),
            Map.entry("방콕", "BKK"), Map.entry("bangkok", "BKK"),
            Map.entry("싱가포르", "SIN"), Map.entry("singapore", "SIN"),
            Map.entry("파리", "PAR"), Map.entry("paris", "PAR"),
            Map.entry("런던", "LON"), Map.entry("london", "LON"),
            Map.entry("뉴욕", "NYC"), Map.entry("new york", "NYC"),
            Map.entry("로마", "ROM"), Map.entry("rome", "ROM"),
            Map.entry("다낭", "DAD"), Map.entry("danang", "DAD"),
            Map.entry("타이베이", "TPE"), Map.entry("대만", "TPE"), Map.entry("taipei", "TPE")
    );

    /**
     * 지명을 IATA 코드로 변환. 못 찾으면 null.
     */
    public String resolve(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toLowerCase();

        // 이미 IATA 코드(ASCII 대문자 3자리)면 그대로 사용.
        // 주의: 한글 3음절(오사카/제주도 등)이 코드로 오인되지 않도록 A-Z 만 허용.
        if (name.trim().matches("[A-Z]{3}")) {
            return name.trim();
        }
        if (OVERRIDES.containsKey(key)) return OVERRIDES.get(key);
        if (cache.containsKey(key)) return cache.get(key);

        try {
            String url = autocompleteUrl
                    + "?locale=ko&types%5B%5D=city&term="
                    + URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
            String body = get(url);
            JsonNode arr = objectMapper.readTree(body);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    String code = node.path("code").asText(null);
                    if (code != null && !code.isBlank()) {
                        cache.put(key, code);
                        log.info("[iata] '{}' → {}", name, code);
                        return code;
                    }
                }
            }
            log.warn("[iata] '{}' 에 대한 IATA 코드를 찾지 못했습니다.", name);
        } catch (Exception e) {
            log.warn("[iata] autocomplete 실패 '{}': {}", name, e.getMessage());
        }
        return null;
    }

    /**
     * 자유 질의 문장에서 도시 IATA 를 추출한다. (예: "도쿄 3박4일 커플 여행" → TYO)
     * 1) 각 토큰을 오버라이드/캐시에서 먼저 조회(네트워크 없음),
     * 2) 없으면 첫 토큰만 autocomplete 로 1회 시도.
     */
    public String resolveFromQuery(String query) {
        if (query == null || query.isBlank()) return null;
        String[] tokens = query.trim().split("\\s+");
        for (String t : tokens) {
            String key = t.toLowerCase();
            if (OVERRIDES.containsKey(key)) return OVERRIDES.get(key);
            if (cache.containsKey(key)) return cache.get(key);
        }
        return resolve(tokens[0]);
    }

    private String get(String apiUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(8000);
            con.setReadTimeout(10000);
            int code = con.getResponseCode();
            InputStream is = (code == HttpURLConnection.HTTP_OK) ? con.getInputStream() : con.getErrorStream();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
                return sb.toString();
            }
        } finally {
            con.disconnect();
        }
    }
}
