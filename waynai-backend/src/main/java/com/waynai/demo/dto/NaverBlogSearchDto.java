package com.waynai.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 네이버 블로그 검색 결과 DTO
 */
@Data
public class NaverBlogSearchDto {
    
    @JsonProperty("lastBuildDate")
    private String lastBuildDate;
    
    @JsonProperty("total")
    private Integer total;
    
    @JsonProperty("start")
    private Integer start;
    
    @JsonProperty("display")
    private Integer display;
    
    @JsonProperty("items")
    private List<BlogItem> items;
    
    @Data
    public static class BlogItem {
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("link")
        private String link;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("bloggername")
        private String bloggerName;
        
        @JsonProperty("bloggerlink")
        private String bloggerLink;
        
        @JsonProperty("postdate")
        private String postDate;
    }
}
