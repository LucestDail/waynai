package com.tripamigo.demo.dto.tourist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TouristApiResponseDto {
    
    @JsonProperty("response")
    private Response response;
    
    @Data
    public static class Response {
        private Header header;
        private Body body;
    }
    
    @Data
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }
    
    @Data
    public static class Body {
        private Integer pageNo;
        private Integer totalCount;
        private Integer numOfRows;
        private Items items;
    }
    
    @Data
    public static class Items {
        private List<TouristItem> item;
    }
    
    @Data
    public static class TouristItem {
        private String baseYm;
        private String tAtsCd;
        private String tAtsNm;
        private String areaCd;
        private String areaNm;
        private String signguCd;
        private String signguNm;
        private String rlteTatsCd;
        private String rlteTatsNm;
        private String rlteRegnCd;
        private String rlteRegnNm;
        private String rlteSignguCd;
        private String rlteSignguNm;
        private String rlteCtgryLclsNm;
        private String rlteCtgryMclsNm;
        private String rlteCtgrySclsNm;
        private String rlteRank;
    }
} 