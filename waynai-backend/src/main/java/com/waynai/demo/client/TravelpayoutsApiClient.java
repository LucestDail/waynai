package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.dto.FlightOfferDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Travelpayouts(Aviasales) 항공권 가격 데이터 API 클라이언트.
 *
 * <p>인증: {@code x-access-token} 헤더. 데이터는 캐시된 최저가이며 실시간 예약 인벤토리가 아니다.
 * 공유 {@code RestTemplate} 은 apis.data.go.kr 외 호스트의 TLS 검증을 막으므로,
 * {@link NaverApiClient} 와 동일하게 HttpURLConnection + 기본 TLS 를 사용한다.
 *
 * <p>참고: {@code /v1/prices/cheap} 응답 구조
 * <pre>{ "success": true, "data": { "OSA": { "0": { "price":.., "airline":"KE", ... } } } }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelpayoutsApiClient {

    @Value("${travelpayouts.api.token:}")
    private String token;

    @Value("${travelpayouts.api.cheap-url:https://api.travelpayouts.com/v1/prices/cheap}")
    private String cheapUrl;

    /** 로컬 개발용 TLS 검증 우회 (사내 MITM 프록시 대응). 운영은 false. */
    @Value("${gemini.tls.insecure:false}")
    private boolean tlsInsecure;

    private final ObjectMapper objectMapper;

    private SSLSocketFactory insecureSocketFactory;
    private HostnameVerifier insecureHostnameVerifier;

    @PostConstruct
    public void init() {
        if (token == null || token.isBlank()) {
            log.warn("[travelpayouts] TRAVELPAYOUTS_TOKEN 미설정 — 항공권 조회는 비활성화됩니다.");
        }
        if (!tlsInsecure) return;
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] c, String a) { }
                @Override public void checkServerTrusted(X509Certificate[] c, String a) { }
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            insecureSocketFactory = ctx.getSocketFactory();
            insecureHostnameVerifier = (h, s) -> true;
            log.warn("[travelpayouts] TLS 검증 우회 활성화 (로컬 개발 전용).");
        } catch (Exception e) {
            log.error("[travelpayouts] insecure TLS 설정 실패, 기본 TLS 로 진행", e);
        }
    }

    public boolean isEnabled() {
        return token != null && !token.isBlank();
    }

    /**
     * 출발-도착 IATA 간 최저가 항공권 목록 조회.
     *
     * @param origin      출발 IATA (예: SEL)
     * @param destination 도착 IATA (예: OSA)
     * @param departDate  출발일 (YYYY-MM 또는 YYYY-MM-DD, nullable)
     * @param returnDate  귀국일 (YYYY-MM 또는 YYYY-MM-DD, nullable → 편도 취급)
     * @param currency    통화 (예: krw)
     * @return 최저가 오퍼 목록 (실패 시 빈 리스트)
     */
    public List<FlightOfferDto> getCheapest(String origin, String destination,
                                            String departDate, String returnDate, String currency) {
        List<FlightOfferDto> out = new ArrayList<>();
        if (!isEnabled() || origin == null || destination == null) return out;
        try {
            StringBuilder url = new StringBuilder(cheapUrl)
                    .append("?origin=").append(origin.toUpperCase())
                    .append("&destination=").append(destination.toUpperCase());
            if (departDate != null && !departDate.isBlank()) url.append("&depart_date=").append(departDate);
            if (returnDate != null && !returnDate.isBlank()) url.append("&return_date=").append(returnDate);
            if (currency != null && !currency.isBlank()) url.append("&currency=").append(currency);

            log.info("[travelpayouts] 항공권 조회: {} → {} ({}~{})", origin, destination, departDate, returnDate);
            String body = get(url.toString());
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("success").asBoolean(false)) {
                log.warn("[travelpayouts] success=false, error={}", root.path("error").asText(""));
                return out;
            }
            JsonNode data = root.path("data").path(destination.toUpperCase());
            if (data.isMissingNode() || !data.isObject()) return out;

            for (Iterator<JsonNode> it = data.elements(); it.hasNext(); ) {
                JsonNode o = it.next();
                out.add(FlightOfferDto.builder()
                        .origin(origin.toUpperCase())
                        .destination(destination.toUpperCase())
                        .airline(o.path("airline").asText(null))
                        .flightNumber(o.hasNonNull("flight_number") ? o.get("flight_number").asInt() : null)
                        .transfers(o.hasNonNull("transfers") ? o.get("transfers").asInt() : null)
                        .price(o.hasNonNull("price") ? o.get("price").asInt() : null)
                        .currency(currency)
                        .departureAt(o.path("departure_at").asText(null))
                        .returnAt(o.path("return_at").asText(null))
                        .roundTrip(o.hasNonNull("return_at") && !o.path("return_at").asText("").isBlank())
                        .legMinutes(o.hasNonNull("duration_to") ? o.get("duration_to").asInt()
                                : (o.hasNonNull("duration") ? o.get("duration").asInt() : null))
                        .expiresAt(o.path("expires_at").asText(null))
                        .build());
            }
            log.info("[travelpayouts] 오퍼 {}건 수신", out.size());
        } catch (Exception e) {
            log.warn("[travelpayouts] 항공권 조회 실패 (무시): {}", e.getMessage());
        }
        return out;
    }

    private String get(String apiUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        try {
            if (tlsInsecure && con instanceof HttpsURLConnection https) {
                if (insecureSocketFactory != null) https.setSSLSocketFactory(insecureSocketFactory);
                if (insecureHostnameVerifier != null) https.setHostnameVerifier(insecureHostnameVerifier);
            }
            con.setRequestMethod("GET");
            con.setRequestProperty("x-access-token", token);
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(15000);
            int code = con.getResponseCode();
            InputStream is = (code == HttpURLConnection.HTTP_OK) ? con.getInputStream() : con.getErrorStream();
            String bodyStr = readBody(is);
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " - " + bodyStr);
            }
            return bodyStr;
        } finally {
            con.disconnect();
        }
    }

    private String readBody(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    @SuppressWarnings("unused")
    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
