package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayPlanDto {
    private Integer dayNumber;
    private String dayTitle;
    private String overview;
    private List<SpotVisitDto> spots;
    private String transportation;
    private String accommodation;
    private String meals;
    private String estimatedCost;
    private String tips;
} 