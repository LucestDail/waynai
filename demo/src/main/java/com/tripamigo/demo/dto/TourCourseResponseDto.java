package com.tripamigo.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourCourseResponseDto {
    
    private String courseId;
    private String title;
    private String destination;
    private Integer totalDays;
    private String theme;
    private String summary;
    private List<DayPlanDto> dayPlans;
    private LocalDateTime createdAt;
    private String estimatedBudget;
    private String transportationInfo;
    private String accommodationInfo;
    private List<String> tips;
    private String weatherInfo;
} 