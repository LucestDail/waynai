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
 * Tavily 웹 검색 API 클라이언트 (해외 여행 정보 RAG 강화용).
 *
 * <p>AI 검색 전용 API. {@code POST https://api.tavily.com/search} 로 질의하고
 * title/url/content 결과를 받는다. 무료 키(tavily.com) 필요, 미설정 시 자동 비활성.
 * 공유 RestTemplate 이 data.go.kr 외 TLS 를 막으므로 HttpURLConnection 사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TavilyApiClient {

    @Value("${tavily.api.key:}")
    private String apiKey;

    @Value("${tavily.api.url:https://api.tavily.com/search}")
    private String apiUrl;

    @Value("${tavily.max-results:5}")
    private int maxResults;

    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 검색 결과 한 건. */
    public record WebResult(String title, String url, String content) {}

    /**
     * 웹 검색. 미설정/실패 시 빈 리스트.
     */
    public List<WebResult> search(String query) {
        List<WebResult> out = new ArrayList<>();
        if (!isEnabled() || query == null || query.isBlank()) return out;
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "api_key", apiKey,
                    "query", query,
                    "search_depth", "basic",
                    "max_results", maxResults,
                    "include_answer", false
            ));
            log.info("[tavily] 웹 검색: {}", query);
            String resp = post(apiUrl, body);
            JsonNode results = objectMapper.readTree(resp).path("results");
            if (results.isArray()) {
                for (JsonNode r : results) {
                    out.add(new WebResult(
                            r.path("title").asText(""),
                            r.path("url").asText(""),
                            r.path("content").asText("")));
                }
            }
            log.info("[tavily] 결과 {}건", out.size());
        } catch (Exception e) {
            log.warn("[tavily] 웹 검색 실패 (무시): {}", e.getMessage());
        }
        return out;
    }

    private String post(String url, String jsonBody) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(8000);
            con.setReadTimeout(15000);
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
