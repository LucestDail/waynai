package com.waynai.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 관광지 정보 DTO
 */
@Data
public class TouristSpotDto {
    
    @JsonProperty("baseYm")
    private String baseYm;
    
    @JsonProperty("mapX")
    private String mapX;
    
    @JsonProperty("mapY")
    private String mapY;
    
    @JsonProperty("areaCd")
    private String areaCd;
    
    @JsonProperty("areaNm")
    private String areaNm;
    
    @JsonProperty("signguCd")
    private String signguCd;
    
    @JsonProperty("signguNm")
    private String signguNm;
    
    @JsonProperty("hubTatsCd")
    private String hubTatsCd;
    
    @JsonProperty("hubTatsNm")
    private String hubTatsNm;
    
    @JsonProperty("hubCtgryLclsNm")
    private String hubCtgryLclsNm;
    
    @JsonProperty("hubCtgryMclsNm")
    private String hubCtgryMclsNm;
    
    @JsonProperty("hubCtgrySclsNm")
    private String hubCtgrySclsNm;
    
    @JsonProperty("hubRank")
    private String hubRank;
}
