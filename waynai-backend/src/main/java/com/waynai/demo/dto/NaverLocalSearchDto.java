package com.waynai.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 네이버 검색 Local API 응답.
 * mapx/mapy 는 WGS84 경위도를 1e7 배 곱한 정수 문자열 (예: "1269780000" = 126.9780).
 */
@Data
public class NaverLocalSearchDto {

    @JsonProperty("lastBuildDate")
    private String lastBuildDate;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("start")
    private Integer start;

    @JsonProperty("display")
    private Integer display;

    @JsonProperty("items")
    private List<LocalItem> items;

    @Data
    public static class LocalItem {
        @JsonProperty("title") private String title;
        @JsonProperty("link") private String link;
        @JsonProperty("category") private String category;
        @JsonProperty("description") private String description;
        @JsonProperty("telephone") private String telephone;
        @JsonProperty("address") private String address;
        @JsonProperty("roadAddress") private String roadAddress;
        @JsonProperty("mapx") private String mapx;
        @JsonProperty("mapy") private String mapy;
    }
}
