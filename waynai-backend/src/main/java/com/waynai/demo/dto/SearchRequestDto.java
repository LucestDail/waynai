package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {
    private String query;
    private String searchType; // "keyword" or "sentence"
    private String destination;
    private String theme;
    private Integer days;
    private String budget;
    private String transportation;
    private String travelStyle;
} 