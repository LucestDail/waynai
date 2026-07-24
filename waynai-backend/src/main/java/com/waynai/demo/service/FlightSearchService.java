package com.waynai.demo.service;

import com.waynai.demo.client.IataResolver;
import com.waynai.demo.client.TravelpayoutsApiClient;
import com.waynai.demo.dto.FlightOfferDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 항공권 검색 서비스.
 *
 * <p>지명 → IATA 변환({@link IataResolver}) + 최저가 조회({@link TravelpayoutsApiClient}) +
 * 제휴 예약 링크(marker) 생성을 묶는다. 출발지 미지정 시 기본 출발지(서울)를 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlightSearchService {

    private final TravelpayoutsApiClient client;
    private final IataResolver iataResolver;

    @Value("${travelpayouts.default-origin:SEL}")
    private String defaultOrigin;

    @Value("${travelpayouts.default-currency:krw}")
    private String defaultCurrency;

    @Value("${travelpayouts.api.marker:}")
    private String marker;

    public boolean isEnabled() {
        return client.isEnabled();
    }

    /**
     * 지명 기반 항공권 검색. 결과는 가격 오름차순, 최대 {@code limit}건.
     *
     * @param originName      출발 지명 (null 이면 기본 출발지)
     * @param destinationName 도착 지명 (필수)
     * @param departDate      YYYY-MM 또는 YYYY-MM-DD (nullable)
     * @param returnDate      YYYY-MM 또는 YYYY-MM-DD (nullable → 편도)
     * @param limit           최대 반환 개수
     */
    public List<FlightOfferDto> search(String originName, String destinationName,
                                       String departDate, String returnDate, int limit) {
        if (!isEnabled()) {
            log.debug("[flight] Travelpayouts 비활성화 상태 — 빈 결과 반환");
            return List.of();
        }
        String originIata = (originName == null || originName.isBlank())
                ? defaultOrigin : iataResolver.resolve(originName);
        // 도착지가 문장(공백 포함)이면 토큰에서 도시명을 추출한다.
        String destIata = (destinationName != null && destinationName.trim().contains(" "))
                ? iataResolver.resolveFromQuery(destinationName)
                : iataResolver.resolve(destinationName);
        if (originIata == null || destIata == null || originIata.equalsIgnoreCase(destIata)) {
            log.debug("[flight] IATA 해석 실패/동일 (origin={}, dest={}) — 스킵", originIata, destIata);
            return List.of();
        }

        List<FlightOfferDto> offers = client.getCheapest(originIata, destIata, departDate, returnDate, defaultCurrency);
        return offers.stream()
                .sorted(Comparator.comparing(o -> o.getPrice() == null ? Integer.MAX_VALUE : o.getPrice()))
                .limit(limit)
                .peek(o -> o.setBookingUrl(buildBookingUrl(o)))
                .collect(Collectors.toList());
    }

    /**
     * Aviasales 검색 딥링크 생성 (제휴 marker 포함).
     * 형식: /search/{ORIGIN}{DDMM}{DEST}{RET_DDMM}1
     */
    private String buildBookingUrl(FlightOfferDto o) {
        String base = "https://www.aviasales.com/search/";
        StringBuilder seg = new StringBuilder(o.getOrigin());
        String depDdmm = toDdmm(o.getDepartureAt());
        if (depDdmm != null) {
            seg.append(depDdmm).append(o.getDestination());
            String retDdmm = toDdmm(o.getReturnAt());
            if (retDdmm != null) seg.append(retDdmm);
            seg.append("1"); // 성인 1명
        } else {
            // 날짜 파싱 불가 → 라우트 검색만
            seg = new StringBuilder(o.getOrigin()).append(o.getDestination());
        }
        String url = base + seg;
        if (marker != null && !marker.isBlank()) {
            url += "?marker=" + marker;
        }
        return url;
    }

    /** ISO 8601("2026-08-02T21:20:00Z") → "0208"(DDMM). 실패 시 null. */
    private String toDdmm(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            String month = iso.substring(5, 7);
            String day = iso.substring(8, 10);
            return day + month;
        } catch (Exception e) {
            return null;
        }
    }
}
