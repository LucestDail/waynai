package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 구조화된 여행 계획 DTO.
 * <p>AI가 반환하는 JSON 응답을 그대로 매핑할 수 있도록 PLAN.md 2.1 에서 요구한
 * 필드(일자/시간/장소/교통/비용/좌표)를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanDto {

    private String type;            // "travel_plan"
    private String destination;     // 여행지명
    private String duration;        // 여행 기간 (ex. "2박 3일")
    private Integer days;           // 총 일수
    private String summary;         // 여행 계획 요약
    private String theme;           // 여행 테마
    private String budget;          // 예상 비용 (사람 친화적 문자열)
    private Integer estimatedBudgetKrw; // 예상 비용 (원화, 계산용)
    private String transportation;  // 주요 교통수단 설명
    private Accommodation accommodation;   // 추천 숙소 (구조화)

    private List<FlightOfferDto> flights; // 항공권 오퍼 (Travelpayouts 실데이터, 국제/국내)

    private List<DayPlan> itinerary; // 여행 일정
    private List<String> tips;       // 여행 팁
    private String weatherInfo;      // 여행 기간 전반 날씨/기후 요약
    private List<String> packingList;// 출발 전 준비물 체크리스트
    private String localInfo;        // 현지 상황(치안·교통·팁·주의사항 등)
    private CostBreakdown costBreakdown; // 항목별 예상 비용
    private List<String> warnings;   // 폴백/파싱 경고 등 시스템 메시지

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostBreakdown {
        private Integer flightsKrw;       // 항공
        private Integer accommodationKrw; // 숙박
        private Integer foodKrw;          // 식비
        private Integer transportKrw;     // 현지 교통
        private Integer activitiesKrw;    // 입장료/액티비티
        private Integer etcKrw;           // 기타
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Accommodation {
        private String name;              // 호텔/숙소명
        private String area;              // 지역/위치
        private String type;              // 호텔/게스트하우스/료칸 등
        private Integer pricePerNightKrw; // 1박 가격(원)
        private String bookingUrl;        // 예약 링크
        private Boolean priceEstimated;   // true=추정(LLM/규칙), false=실가격(크롤). null→추정 취급
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meal {
        private String type;      // 아침|점심|저녁|간식
        private String name;      // 식당명 (실제 상호)
        private String location;  // 위치/주소
        private String menu;      // 대표 메뉴
        private Integer priceKrw; // 1인 예상가격(원)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostItem {
        private String label;  // 항목명
        private Integer krw;    // 금액(원)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private Integer day;                 // 일차
        private String title;                // 일차 제목
        private String overview;             // 일차 개요 (구체적으로)
        private List<Spot> spots;            // 방문지 목록 (구조화)
        private List<String> activities;     // 활동 설명 (자유 서술)
        private String transportation;       // 당일 교통수단/이동 경로 요약
        private List<Meal> meals;            // 식사(식당명·위치·메뉴·가격)
        private String accommodation;        // 그날 묵는 숙소명
        private String weather;              // 그날 예상 날씨
        private String estimatedCost;        // 그날 예상 비용(요약 문자열)
        private List<CostItem> costItems;    // 그날 항목별 비용
        private String tips;                 // 일차 팁
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Spot {
        private String name;        // 관광지명
        private String visitTime;   // 방문 시간대 (ex. "09:00")
        private Integer durationMin;// 머무는 시간 (분)
        private String activity;    // 활동 내용
        private String notes;       // 참고 사항
        private String address;     // 주소 (가능시)
        private Double latitude;    // 좌표
        private Double longitude;   // 좌표
    }
}
