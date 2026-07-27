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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 국내 숙소 실가격 수집(여기어때 검색 페이지의 SSR __NEXT_DATA__ 파싱).
 *
 * <p>Hotellook 종료(2025-10) 후 대체. 헤드리스 없이 plain HTTP + 임베드 JSON 파싱이라 가볍다.
 * best-effort — 실패/차단 시 빈 결과(호출측이 추정치로 폴백). 지역별 캐시(TTL)로 재요청 최소화.
 * ⚠️ 개인용. 대상 사이트 구조 변경 시 깨질 수 있음(그때 빈 결과로 안전 폴백). 국내 전용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelCrawlClient {

    @Value("${hotel.crawl.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper;

    private static final String UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final Pattern NEXT_DATA =
            Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL);
    private static final long TTL_MS = 6 * 3600_000L; // 6시간 캐시

    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(List<TravelPlanDto.Accommodation> list, long ts) {}

    public boolean isEnabled() {
        return enabled;
    }

    /** 지역명으로 국내 숙소 실가격 상위 N개. 실패/미지원 시 빈 리스트. */
    public List<TravelPlanDto.Accommodation> search(String region, int limit) {
        if (!enabled || region == null || region.isBlank()) return List.of();
        String key = region.trim();
        Cached c = cache.get(key);
        if (c != null && System.currentTimeMillis() - c.ts() < TTL_MS) return capped(c.list(), limit);

        List<TravelPlanDto.Accommodation> out = new ArrayList<>();
        try {
            String url = "https://www.yeogi.com/domestic-accommodations?keyword="
                    + URLEncoder.encode(region, StandardCharsets.UTF_8);
            String html = get(url);
            Matcher m = NEXT_DATA.matcher(html);
            if (!m.find()) { log.info("[hotel-crawl] __NEXT_DATA__ 미발견: {}", region); return List.of(); }
            JsonNode accs = objectMapper.readTree(m.group(1))
                    .path("props").path("pageProps").path("accommodationsData");
            if (accs.isArray()) {
                for (JsonNode a : accs) {
                    TravelPlanDto.Accommodation acc = toAccommodation(a);
                    if (acc != null) out.add(acc);
                }
            }
            cache.put(key, new Cached(out, System.currentTimeMillis()));
            log.info("[hotel-crawl] '{}' 국내 숙소 {}건 수집", region, out.size());
        } catch (Exception e) {
            log.warn("[hotel-crawl] 실패 (무시): {}", e.getMessage());
            return List.of();
        }
        return capped(out, limit);
    }

    /** 여기어때 지역 검색 딥링크(예약 CTA용). */
    public String searchLink(String region) {
        return "https://www.yeogi.com/domestic-accommodations?keyword="
                + URLEncoder.encode(region == null ? "" : region, StandardCharsets.UTF_8);
    }

    private TravelPlanDto.Accommodation toAccommodation(JsonNode a) {
        JsonNode meta = a.path("meta");
        String name = meta.path("name").asText(null);
        if (name == null || name.isBlank()) return null;
        int price = minPrice(a.path("room"));
        if (price <= 0) return null;

        String grade = meta.path("grade").asText("호텔");
        JsonNode rev = meta.path("review");
        String rating = rev.hasNonNull("rate")
                ? String.format(" · ★%.1f(%d)", rev.path("rate").asDouble(), rev.path("count").asInt(0)) : "";
        JsonNode addr = meta.path("address");
        String area = addr.path("address").asText("");
        String traffic = addr.path("traffic").asText("");
        String areaStr = (area + (traffic.isBlank() ? "" : (area.isBlank() ? "" : " · ") + traffic)).trim();
        String id = meta.path("id").asText("");

        return TravelPlanDto.Accommodation.builder()
                .name(name)
                .area(areaStr.isBlank() ? null : areaStr)
                .type(grade + rating)
                .pricePerNightKrw(price)
                .priceEstimated(false) // 크롤 실가격
                .bookingUrl(id.isBlank() ? searchLink(name) : "https://www.yeogi.com/domestic-accommodations/" + id)
                .build();
    }

    /** room.stay / room.rent 의 discountPrice 중 최저(0 제외). */
    private int minPrice(JsonNode room) {
        int best = Integer.MAX_VALUE;
        for (String kind : new String[]{"stay", "rent"}) {
            int p = room.path(kind).path("price").path("discountPrice").asInt(0);
            if (p > 0 && p < best) best = p;
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    private List<TravelPlanDto.Accommodation> capped(List<TravelPlanDto.Accommodation> list, int limit) {
        return list.size() <= limit ? list : new ArrayList<>(list.subList(0, limit));
    }

    private String get(String apiUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", UA);
            con.setRequestProperty("Accept", "text/html,application/xhtml+xml");
            con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9");
            con.setConnectTimeout(3000);
            con.setReadTimeout(7000);
            con.setInstanceFollowRedirects(true);
            int code = con.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
                return sb.toString();
            }
        } finally {
            con.disconnect();
        }
    }
}
