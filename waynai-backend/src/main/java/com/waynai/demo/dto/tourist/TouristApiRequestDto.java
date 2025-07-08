package com.waynai.demo.dto.tourist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TouristApiRequestDto {
    
    @Builder.Default
    private String serviceKey = "";
    
    @Builder.Default
    private Integer pageNo = 1;
    
    @Builder.Default
    private Integer numOfRows = 10;
    
    @Builder.Default
    private String mobileOS = "ETC";
    
    @Builder.Default
    private String mobileApp = "WaynAI";
    
    @Builder.Default
    private String baseYm = "";
    
    private String areaCd;
    private String signguCd;
    private String keyword;
    
    @Builder.Default
    private String _type = "json";
} 