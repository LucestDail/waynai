package com.waynai.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 연관 관광지 정보 DTO
 */
@Data
public class RelatedTouristSpotDto {
    
    @JsonProperty("baseYm")
    private String baseYm;
    
    @JsonProperty("tAtsCd")
    private String tAtsCd;
    
    @JsonProperty("tAtsNm")
    private String tAtsNm;
    
    @JsonProperty("areaCd")
    private String areaCd;
    
    @JsonProperty("areaNm")
    private String areaNm;
    
    @JsonProperty("signguCd")
    private String signguCd;
    
    @JsonProperty("signguNm")
    private String signguNm;
    
    @JsonProperty("rlteTatsCd")
    private String rlteTatsCd;
    
    @JsonProperty("rlteTatsNm")
    private String rlteTatsNm;
    
    @JsonProperty("rlteRegnCd")
    private String rlteRegnCd;
    
    @JsonProperty("rlteRegnNm")
    private String rlteRegnNm;
    
    @JsonProperty("rlteSignguCd")
    private String rlteSignguCd;
    
    @JsonProperty("rlteSignguNm")
    private String rlteSignguNm;
    
    @JsonProperty("rlteCtgryLclsNm")
    private String rlteCtgryLclsNm;
    
    @JsonProperty("rlteCtgryMclsNm")
    private String rlteCtgryMclsNm;
    
    @JsonProperty("rlteCtgrySclsNm")
    private String rlteCtgrySclsNm;
    
    @JsonProperty("rlteRank")
    private String rlteRank;
}
