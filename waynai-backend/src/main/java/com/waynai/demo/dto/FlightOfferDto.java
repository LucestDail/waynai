package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 항공권 가격 오퍼 DTO (Travelpayouts / Aviasales 데이터 기반).
 *
 * <p>실제 예약 인벤토리가 아니라 캐시된 최저가 데이터이므로 "여행 계획의 비용 추정 +
 * 제휴 예약 링크" 용도로 사용한다. {@code bookingUrl} 은 제휴 마커가 포함된 검색 링크로,
 * 사용자가 이 링크로 예약 시 커미션이 발생한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightOfferDto {

    private String origin;        // 출발 IATA (ex. SEL)
    private String destination;   // 도착 IATA (ex. OSA)
    private String airline;       // 항공사 코드 (ex. KE)
    private Integer flightNumber; // 편명
    private Integer transfers;    // 경유 횟수 (0 = 직항)
    private Integer price;        // 가격 (currency 기준)
    private String currency;      // 통화 (ex. krw)
    private String departureAt;   // 출발 일시 (ISO 8601)
    private String returnAt;      // 귀국 일시 (ISO 8601, 편도면 null)
    private Integer legMinutes;   // 편도 비행 시간(분)
    private Boolean roundTrip;    // 왕복 여부 (return_at 존재 시 true)
    private String expiresAt;     // 가격 캐시 만료 시각 (ISO 8601)
    private String bookingUrl;    // 제휴 예약 링크 (marker 포함)
}
