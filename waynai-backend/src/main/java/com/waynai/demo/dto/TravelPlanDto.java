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
    private String accommodation;   // 숙박 추천

    private List<DayPlan> itinerary; // 여행 일정
    private List<String> tips;       // 여행 팁
    private String weatherInfo;      // 날씨 정보
    private List<String> warnings;   // 폴백/파싱 경고 등 시스템 메시지

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private Integer day;                 // 일차
        private String title;                // 일차 제목
        private String overview;             // 일차 개요
        private List<Spot> spots;            // 방문지 목록 (구조화)
        private List<String> activities;     // 활동 설명 (자유 서술)
        private String transportation;       // 당일 교통수단
        private String meals;                // 식사 정보
        private String estimatedCost;        // 예상 비용 (문자열)
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
