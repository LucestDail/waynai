package com.waynai.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 연관 관광지 API 응답 DTO
 */
@Data
public class RelatedTouristApiResponseDto {
    
    @JsonProperty("response")
    private Response response;
    
    @Data
    public static class Response {
        @JsonProperty("header")
        private Header header;
        
        @JsonProperty("body")
        private Body body;
    }
    
    @Data
    public static class Header {
        @JsonProperty("resultCode")
        private String resultCode;
        
        @JsonProperty("resultMsg")
        private String resultMsg;
    }
    
    @Data
    public static class Body {
        @JsonProperty("items")
        private Items items;
        
        @JsonProperty("numOfRows")
        private Integer numOfRows;
        
        @JsonProperty("pageNo")
        private Integer pageNo;
        
        @JsonProperty("totalCount")
        private Integer totalCount;
    }
    
    @Data
    public static class Items {
        @JsonProperty("item")
        private List<RelatedTouristSpotDto> item;
    }
}
