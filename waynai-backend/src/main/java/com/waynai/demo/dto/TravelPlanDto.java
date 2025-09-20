package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여행 계획 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanDto {
    
    private String type; // "travel_plan"
    private String destination; // 여행지명
    private String duration; // 여행 기간
    private String summary; // 여행 계획 요약
    private String budget; // 예상 비용
    private String transportation; // 교통수단
    private String accommodation; // 숙박 추천
    private List<DayPlan> itinerary; // 여행 일정
    private List<String> tips; // 여행 팁
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private Integer day; // 일차
        private String title; // 일차 제목
        private String overview; // 일차 개요
        private List<String> activities; // 활동 목록
        private List<String> spots; // 방문지 목록
    }
}
