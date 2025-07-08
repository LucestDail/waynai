package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourCourseRequestDto {
    
    @NotBlank(message = "여행 목적지는 필수입니다")
    private String destination;
    
    @NotNull(message = "여행 일수는 필수입니다")
    @Min(value = 1, message = "여행 일수는 최소 1일 이상이어야 합니다")
    @Max(value = 30, message = "여행 일수는 최대 30일까지 가능합니다")
    private Integer days;
    
    @NotBlank(message = "여행 테마는 필수입니다")
    private String theme; // 문화, 자연, 맛집, 쇼핑 등
    
    private String budget;
    private String transportation; // 대중교통, 렌트카, 도보 등
    private List<String> interests; // 관심사 목록
    private String accommodation; // 숙박 유형
    private String travelStyle; // 여행 스타일 (느긋한, 빠른, 경제적 등)
    private String specialRequirements; // 특별 요구사항
} 