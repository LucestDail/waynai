package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotVisitDto {
    private TouristSpotDto spot;
    private String visitTime;
    private Integer duration; // 분 단위
    private String activity;
    private String notes;
    private Integer order;
} 